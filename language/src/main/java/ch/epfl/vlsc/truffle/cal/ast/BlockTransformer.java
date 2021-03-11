package ch.epfl.vlsc.truffle.cal.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtCallNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StmtCall;

public class BlockTransformer {

    Source source;
    CALLanguage language;

    public BlockTransformer(CALLanguage language, Source source) {
        this.source = source;
        this.language = language;
    }

    public Map<String, RootCallTarget> transformActors(NamespaceDecl namespace) {
        Map<String, RootCallTarget> nsFunctions = new HashMap<>();
        for (GlobalEntityDecl entity : namespace.getEntityDecls()) {
            if (entity.getEntity() instanceof CalActor) {
                // FIXME getname
                ActorNode actor = transformActor((CalActor) entity.getEntity(), namespace.getQID().toString());
                nsFunctions.put(actor.getName(), Truffle.getRuntime().createCallTarget(actor));
            }
        }
        return nsFunctions;
    }

    public ActorNode transformActor(CalActor actor, String name) {
        // start a block and a rootcalltarget
        // new lexical scope
        // FIXME
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        ActionNode[] actions = actor.getActions().map(x -> transformAction(x)).toArray(new ActionNode[0]);
        SourceSection actorSrc = source.createSection(actor.getFromLineNumber(), actor.getFromColumnNumber(),
                actor.getToLineNumber());
        return new ActorNode(language, frameDescriptor, actions, actorSrc, name);
    }

    public ActionNode transformAction(Action action) {
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
        } else {
            throw new Error("unknown statement " + statement.getClass().getName());
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
