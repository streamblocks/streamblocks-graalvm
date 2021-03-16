package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import se.lth.cs.tycho.ir.entity.cal.CalActor;
import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtCallNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;

public class ActionTransformer extends Transformer<ActionNode> {

    Action action;
    private FrameDescriptor frameDescriptor;
    private LexicalScope lexicalScope;
    // FIXME we should use different frameDescriptor as the one used in actor is persistent and this one should not
    public ActionTransformer(CALLanguage language, Source source, LexicalScope parentScope, Action action, FrameDescriptor frameDescriptor) {
        super(language, source);
        this.frameDescriptor = frameDescriptor;//new FrameDescriptor();
        // lexical scope must include parent scope
        lexicalScope = new LexicalScope(parentScope);

        this.action = action;

    }

    public ActionNode transform() {

        CALStatementNode[] body = new CALStatementNode[action.getBody().size()];
        int i = 0;
        for (Statement statement : action.getBody()) {
            body[i] = transformSatement(statement);
            i++;
        }
        return new ActionNode(language, body);
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

    public CALExpressionNode transformStmtAssignment(StmtAssignment stmtAssignment) {
        if (stmtAssignment.getLValue() instanceof LValueVariable) {
            LValueVariable lvalue = (LValueVariable) stmtAssignment.getLValue();
            String name = lvalue.getVariable().getName();

            FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(name, FrameSlotKind.Illegal);
            FrameSlot existingSlot = lexicalScope.locals.put(name, frameSlot);
            boolean newVariable = existingSlot == null;
            CALExpressionNode valueNode = transformExpr(stmtAssignment.getExpression());
            CALExpressionNode lvalueNode = new StringLiteralNode(lvalue.getVariable().getName());

            final CALExpressionNode result = CALWriteLocalVariableNodeGen.create(valueNode, frameSlot, lvalueNode,
                    newVariable);

            if (valueNode.hasSource()) {
                final int start = lvalueNode.getSourceCharIndex();
                final int length = valueNode.getSourceEndIndex() - start;
                result.setSourceSection(start, length);
            }
            result.addExpressionTag();

            return result;
        } else {
            throw new Error("unknown lvalue" + stmtAssignment.getLValue().getClass().getName());
        }
    }

    public StmtCallNode transformStmtCall(StmtCall stmtCall) {


        return new StmtCallNode(transformExpr(stmtCall.getProcedure()),
                stmtCall.getArgs().map(new Function<Expression, CALExpressionNode>() {
                    public CALExpressionNode apply(Expression expr) {
                        return transformExpr(expr);
                    }
                }).toArray(new CALExpressionNode[0]));
    }

    public CALExpressionNode transformExpr(Expression expr) {
        if (expr instanceof ExprLiteral) {
            if (((ExprLiteral) expr).getKind() == ExprLiteral.Kind.String)
                return new StringLiteralNode(((ExprLiteral) expr).getText());
            else
                throw new Error("unknown expr litteral " + expr.getClass().getName());
        } else if (expr instanceof ExprVariable) {
            ExprVariable v = (ExprVariable) expr;
            // For now we assume that we only read variables names,
            // we have to implement local variables and scopes
            return new FunctionLiteralNode(v.getVariable().getName());
        } else {
            throw new Error("unknown expr " + expr.getClass().getName());
        }
    }
}
