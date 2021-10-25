package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ExprIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.ActorLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListInitNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton class that provides an implementation for an entity sub-tree.
 */
public class EntityVisitor extends CALParserBaseVisitor<Object> {
    private static EntityVisitor instance;

    private EntityVisitor() {}

    public static EntityVisitor getInstance() {
        if (instance == null) {
            instance = new EntityVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Pair<CALStatementNode, CALExpressionNode> visitEntityDeclaration(CALParser.EntityDeclarationContext ctx) {
        CALExpressionNode instanceNode = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                ctx.name.getText(),
                visitEntityExpression(ctx.entityExpression()),
                DefaultValueCastNodeCreator.getInstance(),
                ScopeEnvironment.getInstance().createSourceSection(ctx));

        ScopeEnvironment.getInstance().getCurrentScope().increaseDepth();
        CALExpressionNode entityNode = ScopeEnvironment.getInstance().createReadNode(ctx.name.getText(), ScopeEnvironment.getInstance().createSourceSection(ctx));

        CALInvokeNode entityInvokeNode = CALInvokeNodeGen.create(new CALExpressionNode[0], entityNode);
        entityInvokeNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        entityInvokeNode.addStatementTag();
        ScopeEnvironment.getInstance().getCurrentScope().decreaseDepth();

        return Pair.of(instanceNode, entityInvokeNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitEntityExpression(CALParser.EntityExpressionContext ctx) {
        if (ctx.entityInstanceExpression() != null) {
            return visitEntityInstanceExpression(ctx.entityInstanceExpression());
        } else if (ctx.entityIfExpression() != null) {
            return visitEntityIfExpression(ctx.entityIfExpression());
        } else if (ctx.entityListExpression() != null) {
            return visitEntityListExpression(ctx.entityListExpression());
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized entity expression type"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitEntityInstanceExpression(CALParser.EntityInstanceExpressionContext ctx) {
        CALExpressionNode actorLiteralNode = new ActorLiteralNode(ScopeEnvironment.getInstance().getEntityFullName(ctx.actor.getText()));
        actorLiteralNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        actorLiteralNode.addExpressionTag();

        List<CALExpressionNode> argumentNodes = new ArrayList<>();
        // TODO Add support for named parameters
        if (ctx.entityParameters() != null)
            argumentNodes.addAll(ctx.entityParameters().entityParameter().stream().map(parameterCtx -> ExpressionVisitor.getInstance().visit(parameterCtx.value)).collect(Collectors.toList()));

        CALExpressionNode valueNode = CALInvokeNodeGen.create(argumentNodes.toArray(new CALExpressionNode[0]), actorLiteralNode);
        valueNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        valueNode.addExpressionTag();
        return valueNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitEntityIfExpression(CALParser.EntityIfExpressionContext ctx) {
        CALExpressionNode exprIf = new ExprIfNode(ExpressionVisitor.getInstance().visit(ctx.expression()), visitEntityExpression(ctx.entityExpression(0)), visitEntityExpression(ctx.entityExpression(1)));
        exprIf.addExpressionTag();
        exprIf.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        return exprIf;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitEntityListExpression(CALParser.EntityListExpressionContext ctx) {
        if (ctx.generators() == null)
            return visitSimpleEntityListComprehension(ctx);
        else
            return visitEntityListComprehensionWithGenerators(ctx);
    }

    private CALExpressionNode visitSimpleEntityListComprehension(CALParser.EntityListExpressionContext ctx) {
        // Simple list collection expression
        CALExpressionNode[] valueNodes;
        if (ctx.entityExpressions() != null) {
            valueNodes = ctx.entityExpressions().entityExpression().stream().map(entityCtx -> visitEntityExpression(entityCtx)).collect(Collectors.toList()).toArray(new CALExpressionNode[0]);
        } else {
            valueNodes = new CALExpressionNode[0];
        }

        ListInitNode simpleComprehensionNode = new ListInitNode(valueNodes);
        simpleComprehensionNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        simpleComprehensionNode.addExpressionTag();

        return simpleComprehensionNode;
    }

    private CALExpressionNode visitEntityListComprehensionWithGenerators(CALParser.EntityListExpressionContext ctx) {
        CALStatementNode[] init = new CALStatementNode[3];
        // tempList=[]
        String tempListName = ScopeEnvironment.generateVariableName();
        init[0] = ScopeEnvironment.getInstance().createNewVariableWriteNode(tempListName, new UnknownSizeListInitNode(), DefaultValueCastNodeCreator.getInstance(), ScopeEnvironment.getInstance().getSource().createUnavailableSection());

        // i=0
        String listIndexVarName = ScopeEnvironment.generateVariableName();
        init[1] = ScopeEnvironment.getInstance().createNewVariableWriteNode(listIndexVarName, new BigIntegerLiteralNode(new BigInteger("0")), DefaultValueCastNodeCreator.getInstance(), ScopeEnvironment.getInstance().getSource().createUnavailableSection());

        // The Comprehension nodes will generate the content into the above list
        init[2] = (new EntityListComprehensionVisitor(tempListName, listIndexVarName)).visitEntityListComprehension(ctx);
        ReturnsLastBodyNode list = new ReturnsLastBodyNode(
                new StmtBlockNode(init),
                ScopeEnvironment.getInstance().createReadNode(tempListName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()));

        list.addExpressionTag();
        list.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));

        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitEntityParameter(CALParser.EntityParameterContext ctx) {
        // Note: Unreachable for now, only tokens directly accessed in #visitEntityInstanceExpression
        return super.visitEntityParameter(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitAttributeDeclaration(CALParser.AttributeDeclarationContext ctx) {
        // TODO First resolve #visitEntityInstanceExpression
        // Note: Unreachable for now
        return super.visitAttributeDeclaration(ctx);
    }

}