package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
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
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFIFOSizeNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALReadFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALWriteFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
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
        // TODO write nodeeeeees HEEERE
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
        // Enough tokens to bind
        for (InputPattern input : action.getInputPatterns()) {
            // TODO implement patterns
            // FIXME
            Pattern pat = input.getMatches().get(0).getExpression().getAlternatives().get(0).getPattern();
            String name;
            if (pat instanceof PatternBinding)
                name = ((PatternBinding) pat).getDeclaration().getName();
            else
                throw new UnsupportedOperationException("Pattern not implemented");
            firingConditions.add(CALBinaryLessOrEqualNodeGen.create(new LongLiteralNode(1),
                    new CALFIFOSizeNode(getReadNode(input.getPort().getName()))));
        }
        for (Expression guard : action.getGuards()) {
            firingConditions.add(transformExpr(guard));
        }
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
        } else {
            throw new Error("unknown statement " + statement.getClass().getName());
        }
    }

    public CALStatementNode transformStmtAssignment(StmtAssignment stmtAssignment) {
        if (stmtAssignment.getLValue() instanceof LValueVariable) {
            LValueVariable lvalue = (LValueVariable) stmtAssignment.getLValue();
            String name = lvalue.getVariable().getName();

            return createAssignment(name, stmtAssignment.getExpression());
        } else {
            throw new Error("unknown lvalue" + stmtAssignment.getLValue().getClass().getName());
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
