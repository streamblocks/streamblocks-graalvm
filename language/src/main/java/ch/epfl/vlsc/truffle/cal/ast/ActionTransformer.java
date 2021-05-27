package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLessOrEqualNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLogicalAndNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFIFOSizeNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALReadFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALWriteFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListInitNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListRangeInitNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.entity.cal.InputPattern;
import se.lth.cs.tycho.ir.entity.cal.OutputExpression;
import se.lth.cs.tycho.ir.expr.ExprBinaryOp;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.expr.pattern.Pattern;
import se.lth.cs.tycho.ir.expr.pattern.PatternBinding;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.StmtForeach;
import se.lth.cs.tycho.ir.stmt.StmtIf;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueIndexer;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;

public class ActionTransformer extends ScopedTransformer<ActionNode> {

    Action action;

    // FIXME we should use different frameDescriptor as the one used in actor is
    // persistent and this one should not
    public ActionTransformer(CALLanguage language, Source source, LexicalScope parentScope, Action action,
            FrameDescriptor frameDescriptor, int depth) {
        super(language, source, parentScope, frameDescriptor, depth);
        this.action = action;

    }

    public CALExpressionNode batchReadInput(InputPattern input) {
        CALStatementNode[] init = new CALStatementNode[3];
        // Create tempList
        // TODO in fact type might be known and size is probably also known
        init[0] = createAssignment("$tempList", new UnknownSizeListInitNode());
        // FIXME
        init[1] = createAssignment("$comprehensionCounter", new BigIntegerLiteralNode(new BigInteger("0")));

        // Create assignment to variable
        CALExpressionNode list = ListRangeInitNodeGen.create(new BigIntegerLiteralNode(new BigInteger("0")),
                transformExpr(input.getRepeatExpr()));
        //
        CALStatementNode[] bodyNodes = new CALStatementNode[2];
        bodyNodes[0] = ListWriteNodeGen.create(getReadNode("$tempList"), getReadNode("$comprehensionCounter"),
                new CALReadFIFONode(getReadNode(input.getPort().getName())));
        bodyNodes[1] = createAssignment("$comprehensionCounter", CALBinaryAddNodeGen
                .create(getReadNode("$comprehensionCounter"), new BigIntegerLiteralNode(new BigInteger("1"))));

        CALStatementNode body = new StmtBlockNode(bodyNodes);

        init[2] = ForeacheNodeGen.create(body, null, list);
        return new ReturnsLastBodyNode(new StmtBlockNode(init), getReadNode("$tempList"));
    }

    public ActionNode transform() {
        CALStatementNode[] body = new CALStatementNode[action.getInputPatterns().size()
                + action.getOutputExpressions().size() + action.getBody().size() + action.getVarDecls().size()];
        int i = 0;

        for (InputPattern input : action.getInputPatterns()) {
            // TODO implement patterns
            // FIXME
            Pattern pat = input.getMatches().get(0).getExpression().getAlternatives().get(0).getPattern();
            String name;
            if (pat instanceof PatternBinding)
                name = ((PatternBinding) pat).getDeclaration().getName();
            else
                throw new UnsupportedOperationException("Pattern not implemented");

            if (input.getRepeatExpr() != null)
                body[i] = createAssignment(name, batchReadInput(input));
            else
                body[i] = createAssignment(name, new CALReadFIFONode(getReadNode(input.getPort().getName())));
            i++;
        }
        // Prepend local variable declarations to the body nodes
        for (LocalVarDecl varDecl : action.getVarDecls()) {
            body[i] = transformVarDecl(varDecl);
            i++;
        }
        for (Statement statement : action.getBody()) {
            body[i] = transformSatement(statement);
            i++;
        }
        // get the variables name linked to the output and add a write to the FIFO
        // in the tail of the action
        for (OutputExpression output : action.getOutputExpressions()) {
            CALExpressionNode fifo = getReadNode(output.getPort().getName());
            if (output.getExpressions().size() > 1)
                throw new UnsupportedOperationException("More than one output expr not supported");
            CALExpressionNode value = transformExpr(output.getExpressions().get(0));
            body[i] = new CALWriteFIFONode(fifo, value);
            i++;
        }

        // Firing conditions
        List<CALExpressionNode> firingConditions = new LinkedList<>();
        // check there is enough tokens to bind
        for (InputPattern input : action.getInputPatterns()) {
            // TODO implement patterns
            Pattern pat = input.getMatches().get(0).getExpression().getAlternatives().get(0).getPattern();
            String name;
            if (pat instanceof PatternBinding)
                name = ((PatternBinding) pat).getDeclaration().getName();
            else
                throw new UnsupportedOperationException("Pattern not implemented");
            if (input.getRepeatExpr() == null)
                firingConditions.add(CALBinaryLessOrEqualNodeGen.create(new LongLiteralNode(1),
                        new CALFIFOSizeNode(getReadNode(input.getPort().getName()))));
            else
                firingConditions.add(CALBinaryLessOrEqualNodeGen.create(transformExpr(input.getRepeatExpr()),
                        new CALFIFOSizeNode(getReadNode(input.getPort().getName()))));

                
        }
        // Parse guards
        for (Expression guard : action.getGuards()) {
            firingConditions.add(transformExpr(guard));
        }
        // Build the boolean expression to determine whether an action is fireable
        CALExpressionNode firingCondition;
        if (firingConditions.size() > 0) {
            firingCondition = firingConditions.remove(0);
            for (CALExpressionNode cond : firingConditions)
                firingCondition = new CALBinaryLogicalAndNode(firingCondition, cond);
        } else {
            firingCondition = new BooleanLiteralNode(true);
        }

        StmtBlockNode block = new StmtBlockNode(body);
        ActionBodyNode bodyNode = new ActionBodyNode(block);
        SourceSection actionSrc = source.createSection(action.getFromLineNumber(), action.getFromColumnNumber(),
                action.getToLineNumber());
        // FIXME name
        return new ActionNode(language, frameDescriptor, bodyNode, firingCondition, actionSrc, "action-1");
    }

    public CALStatementNode transformSatement(Statement statement) {
        if (statement instanceof StmtCall) {
            return transformStmtCall((StmtCall) statement);
        } else if (statement instanceof StmtAssignment) {
            return transformStmtAssignment((StmtAssignment) statement);
        } else if (statement instanceof StmtForeach) {
            return transformStmtForeach((StmtForeach) statement);
        } else if (statement instanceof StmtIf) {
            return transformStmtIf((StmtIf) statement);
        } else {
            throw new Error("unknown statement " + statement.getClass().getName());
        }
    }

    private CALStatementNode transformStatementsList(List<Statement> statements) {
        CALStatementNode[] statementNodes = new CALStatementNode[statements.size()];
        for (int i = 0; i < statements.size(); i++) {
            statementNodes[i] = transformSatement(statements.get(i));
        }
        return new StmtBlockNode(statementNodes);
    }

    private CALStatementNode transformStmtIf(StmtIf stmtIf) {
        CALStatementNode elze = null;
        if (stmtIf.getElseBranch() != null)
            elze = transformStatementsList(stmtIf.getElseBranch());
        return new StmtIfNode(transformExpr(stmtIf.getCondition()), transformStatementsList(stmtIf.getThenBranch()),
                elze);
    }

    public CALStatementNode transformStmtForeach(StmtForeach foreach) {
        Generator generator = foreach.getGenerator();
        CALExpressionNode list = transformExpr(generator.getCollection());
        if (generator.getVarDecls().size() != 1) {
            throw new Error("unsupported multiple var decls in for loop");
        }
        // transformVarDecl NEEDS to be called before body nodes transformations
        // in order to get the read of the variable right
        CALExpressionNode write = transformVarDecl(generator.getVarDecls().get(0));
        CALStatementNode[] bodyNodes = new CALStatementNode[foreach.getBody().size()];
        for (int i = 0; i < foreach.getBody().size(); i++) {
            bodyNodes[i] = transformSatement(foreach.getBody().get(i));
        }

        CALStatementNode statement = new StmtBlockNode(bodyNodes);
        if (write instanceof CALWriteLocalVariableNode) {
            ForeacheNode f = ForeacheNodeGen.create(statement, (CALWriteLocalVariableNode) write, list);
            return f;
        } else {
            // FIXME once createAssignment is fixed
            throw new Error("unsupported variable name reuse");
        }
    }

    public CALStatementNode transformStmtAssignment(StmtAssignment stmtAssignment) {
        if (stmtAssignment.getLValue() instanceof LValueVariable) {
            LValueVariable lvalue = (LValueVariable) stmtAssignment.getLValue();
            String name = lvalue.getVariable().getName();

            return createAssignment(name, stmtAssignment.getExpression());
        } else if (stmtAssignment.getLValue() instanceof LValueIndexer) {
            LValueIndexer lvalue = (LValueIndexer) stmtAssignment.getLValue();
            String name = ((LValueVariable) lvalue.getStructure()).getVariable().getName();
            return ListWriteNodeGen.create(getReadNode(name), transformExpr(lvalue.getIndex()),
                    transformExpr(stmtAssignment.getExpression()));
        } else {
            throw new Error("unknown lvalue " + stmtAssignment.getLValue().getClass().getName());
        }
    }

    public CALInvokeNode transformStmtCall(StmtCall stmtCall) {

        return new CALInvokeNode(transformExpr(stmtCall.getProcedure()),
                stmtCall.getArgs().map(new Function<Expression, CALExpressionNode>() {
                    public CALExpressionNode apply(Expression expr) {
                        return transformExpr(expr);
                    }
                }).toArray(new CALExpressionNode[0]));
    }

}
