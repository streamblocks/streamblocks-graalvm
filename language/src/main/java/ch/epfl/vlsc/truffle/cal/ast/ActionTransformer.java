package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.sl.nodes.local.CALReadLocalVariableNodeGen;

import se.lth.cs.tycho.ir.entity.cal.CalActor;
import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;

import java.math.BigInteger;
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
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.expr.ExprBinaryOp;
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

    // FIXME we should use different frameDescriptor as the one used in actor is
    // persistent and this one should not
    public ActionTransformer(CALLanguage language, Source source, LexicalScope parentScope, Action action,
            FrameDescriptor frameDescriptor) {
        super(language, source);
        this.frameDescriptor = frameDescriptor;// new FrameDescriptor();
        // lexical scope must include parent scope
        lexicalScope = new LexicalScope(parentScope);

        this.action = action;

    }

    public ActionNode transform() {
         
        CALStatementNode[] body = new CALStatementNode[action.getBody().size()+action.getVarDecls().size()];
        int i = 0;
        // Prepend local variable declarations to the body nodes
        for (LocalVarDecl varDecl : action.getVarDecls()) {
            body[i] = transformVarDecl(varDecl);
            i++;
        }
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

            return createAssignment(name, stmtAssignment.getExpression());
        } else {
            throw new Error("unknown lvalue" + stmtAssignment.getLValue().getClass().getName());
        }
    }

    private CALExpressionNode createAssignment(String name, Expression value) {
            FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(name, FrameSlotKind.Illegal);
            FrameSlot existingSlot = lexicalScope.locals.put(name, frameSlot);
            boolean newVariable = existingSlot == null;
            CALExpressionNode valueNode = transformExpr(value);
            CALExpressionNode lvalueNode = new StringLiteralNode(name);

            final CALExpressionNode result = CALWriteLocalVariableNodeGen.create(valueNode, frameSlot, lvalueNode,
                    newVariable);

            if (valueNode.hasSource()) {
                final int start = lvalueNode.getSourceCharIndex();
                final int length = valueNode.getSourceEndIndex() - start;
                result.setSourceSection(start, length);
            }
            result.addExpressionTag();

            return result;

    }
    public CALExpressionNode transformVarDecl(LocalVarDecl varDecl) {
        // TODO handle type with varDecl.getType
        String name = varDecl.getName();
        Expression value = varDecl.getValue();

        return createAssignment(name, value);
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
            else if (((ExprLiteral) expr).getKind() == ExprLiteral.Kind.Integer)
                return new BigIntegerLiteralNode(new BigInteger(((ExprLiteral) expr).getText()));
            else
                throw new Error("unknown expr litteral " + expr.getClass().getName());
        } else if (expr instanceof ExprVariable) {
            ExprVariable v = (ExprVariable) expr;
            // For now we assume that we only read variables names,
            // we have to implement local variables and scopes
            String name = v.getVariable().getName();
            final CALExpressionNode result;
            final FrameSlot frameSlot = lexicalScope.locals.get(name);
            if (frameSlot != null) {
                /* Read of a local variable. */
                result = CALReadLocalVariableNodeGen.create(frameSlot);
            } else {
                /*
                 * Read of a global name. In our language, the only global names are functions.
                 */
                result = new FunctionLiteralNode(name);
            }
            // result.setSourceSection(nameNode.getSourceCharIndex(),
            // nameNode.getSourceLength());
            result.addExpressionTag();
            return result;
        } else if (expr instanceof ExprBinaryOp) {
            return transformBinaryExpr((ExprBinaryOp) expr);
        } else {
            throw new Error("unknown expr " + expr.getClass().getName());
        }
    }

    public CALExpressionNode transformBinaryExpr(ExprBinaryOp expr) {
        assert expr.getOperations().size() == 1;
        String opeString = expr.getOperations().get(0);
        CALBinaryNode result;

        assert expr.getOperands().size() == 2;
        CALExpressionNode left = transformExpr(expr.getOperands().get(0));
        CALExpressionNode right = transformExpr(expr.getOperands().get(1));

        switch (opeString) {
        case "+":
            result = CALBinaryAddNodeGen.create(left, right);
            break;
        default:
            throw new Error("unimplemented bin op " + opeString);
        }
        return result;
    }
}
