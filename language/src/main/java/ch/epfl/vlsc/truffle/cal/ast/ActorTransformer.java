package ch.epfl.vlsc.truffle.cal.ast;

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
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteFrameSlotNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
public class ActorTransformer extends ScopedTransformer<ActorNode> {

    CalActor actor;
    String name;

    public ActorTransformer(CALLanguage language, Source source, CalActor actor, String name, int depth) {
        super(language, source, new LexicalScopeRW(null), new FrameDescriptor(), depth);
        this.actor = actor;
        this.name = name;
    }

    public ActorNode transform() {
        CALExpressionNode[] headStatements = new CALExpressionNode[actor.getVarDecls().size()];
        int i = 0;
        for (LocalVarDecl varDecl : actor.getVarDecls()) {
            headStatements[i] = transformVarDecl(varDecl);
            i++;
        }

        StmtBlockNode head = new StmtBlockNode(headStatements);
        ActionNode[] actions = this.actor.getActions().map(x -> transformAction(x)).toArray(new ActionNode[0]);
        SourceSection actorSrc = source.createSection(actor.getFromLineNumber(), actor.getFromColumnNumber(),
                actor.getToLineNumber());
        return new ActorNode(language, frameDescriptor, actions, head, actorSrc, name);
    }

    public ActionNode transformAction(Action action) {
        return (new ActionTransformer(language, source, lexicalScope, action, frameDescriptor, depth)).transform();
    }

}
