package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLogicalAndNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALScopedNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListInitNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListRangeInitNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListComprehensionVisitor extends CALParserBaseVisitor {
    private final String tempListName;
    private final String listIndexVarName;

    public ListComprehensionVisitor(String tempListName, String listIndexVarName) {
        this.tempListName = tempListName;
        this.listIndexVarName = listIndexVarName;
    }

    @Override public CALStatementNode visitListComprehension(CALParser.ListComprehensionContext ctx) {
        ScopeEnvironment.getInstance().pushScope(false, true);
        CALStatementNode nestedComprehensions = createNestedComprehensions(ctx, 0);

        nestedComprehensions.setSourceSection(
                ScopeEnvironment.getInstance().getSource().createSection(ctx.generators().start.getLine(), ctx.generators().start.getCharPositionInLine() + 1, ctx.generators().getText().length())
        );

        CALRootNode root = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                new ReturnsLastBodyNode(nestedComprehensions),
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                ScopeEnvironment.generateVariableName()
        );
        ScopeEnvironment.getInstance().popScope();
        final CALComprehensionContinueNode calComprehensionContinueNode = CALComprehensionContinueNodeGen.create(null, root, new ListInitNode(new CALExpressionNode[]{new BigIntegerLiteralNode(new BigInteger("0"))}));
        return calComprehensionContinueNode;
    }

    private CALStatementNode createNestedComprehensions(CALParser.ListComprehensionContext ctx, int i) {
        int n = ctx.generators().generator().size();

        if (n == i) {
            ArrayList<CALStatementNode> listWrites = new ArrayList<>();
            listWrites.ensureCapacity(2 * ctx.computations.expression().size());
            for(CALParser.ExpressionContext computation : ctx.computations.expression()) {
                CALExpressionNode expr = ExpressionVisitor.getInstance().visit(computation);
                expr.addExpressionTag();
                expr.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(computation));

                listWrites.add(ListWriteNodeGen.create(
                        ScopeEnvironment.getInstance().createReadNode(tempListName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()),
                        ScopeEnvironment.getInstance().createReadNode(listIndexVarName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()),
                        expr));
                listWrites.add(ScopeEnvironment.getInstance().createExistingVariableWriteNode(
                        listIndexVarName,
                        CALBinaryAddNodeGen.create(ScopeEnvironment.getInstance().createReadNode(listIndexVarName, null), new BigIntegerLiteralNode(new BigInteger("1"))),
                        ScopeEnvironment.getInstance().getSource().createUnavailableSection()
                ));
            }

            CALStatementNode currBody = new StmtBlockNode(listWrites.toArray(new CALStatementNode[0]));
            currBody.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.computations));

            return currBody;
        }

        CALParser.GeneratorContext generator = ctx.generators().generator(i);

        if (generator.generatorBody().variables.size() != 1) {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), generator.generatorBody(), "Support for multiple variables in Generators are not supported yet");
        }

        NullLiteralNode nullLiteralNode = new NullLiteralNode();
        nullLiteralNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(generator));
        nullLiteralNode.addExpressionTag();

        // Note: Custom source section to precisely specify a variable token
        CALExpressionNode write = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                generator.generatorBody().variables.get(0).getText(),
                nullLiteralNode,
                ScopeEnvironment.getInstance().getSource().createSection(generator.generatorBody().variables.get(0).getLine(), generator.generatorBody().variables.get(0).getCharPositionInLine() + 1, generator.generatorBody().variables.get(0).getText().length())
        );

        write.setSourceSection(ScopeEnvironment.getInstance().getSource().createSection(
                generator.generatorBody().variables.get(0).getLine(),
                generator.generatorBody().variables.get(0).getCharPositionInLine(),
                generator.generatorBody().variables.get(0).getText().length()));

        CALExpressionNode collection = ExpressionVisitor.getInstance().visit(generator.generatorBody().generatorExpressions().collection);
        collection.addExpressionTag();
        collection.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(generator.generatorBody().generatorExpressions().collection));

        ScopeEnvironment.getInstance().pushScope();

        List<CALExpressionNode> filters = generator.generatorBody().generatorExpressions().filters.stream().map(filter -> ExpressionVisitor.getInstance().visit(filter)).collect(Collectors.toList());
        CALExpressionNode filter;

        if(filters.size() == 0){
            filter = new BooleanLiteralNode(true);
        }else if(filters.size() == 1){
            filter = filters.get(0);
            filter.addExpressionTag();
            filter.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(generator.generatorBody().generatorExpressions().filters.get(0)));
        }else{
            filter = filters.get(0);
            filter.addExpressionTag();
            filter.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(generator.generatorBody().generatorExpressions().filters.get(0)));
            for(int j = 1; j < filters.size(); ++j){
                filter = new CALBinaryLogicalAndNode(filter, filters.get(i));
                filter.addExpressionTag();
                filter.setSourceSection(ScopeEnvironment.getInstance().getSource().createSection(
                        generator.generatorBody().generatorExpressions().filters.get(0).start.getLine(),
                        generator.generatorBody().generatorExpressions().filters.get(0).start.getCharPositionInLine(),
                        generator.generatorBody().generatorExpressions().filters.get(j).stop.getLine(),
                        generator.generatorBody().generatorExpressions().filters.get(j).stop.getCharPositionInLine()
                ));
            }
        }

        CALStatementNode ifBody = createNestedComprehensions(ctx, i + 1);
        CALRootNode forBody = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                new ReturnsLastBodyNode(new StmtIfNode(filter, ifBody, null)),
                ifBody.getSourceSection(),
                ScopeEnvironment.generateVariableName()
        );
        ScopeEnvironment.getInstance().popScope();

        CALStatementNode currBody = CALComprehensionContinueNodeGen.create(
                (CALWriteLocalVariableNode) write,
                forBody,
                collection);
        return currBody;
    }
}
