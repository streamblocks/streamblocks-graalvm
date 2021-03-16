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

public class ActorTransformer extends Transformer<ActorNode> {

    CalActor actor;
    String name;
    private FrameDescriptor frameDescriptor;
    private LexicalScope lexicalScope;

    public ActorTransformer(CALLanguage language, Source source, CalActor actor, String name) {
        super(language, source);
        this.actor = actor;
        this.name = name;
        frameDescriptor = new FrameDescriptor();
        lexicalScope = new LexicalScope(null);

    }

    public ActorNode transform() {
        ActionNode[] actions = this.actor.getActions().map(x -> transformAction(x)).toArray(new ActionNode[0]);
        SourceSection actorSrc = source.createSection(actor.getFromLineNumber(), actor.getFromColumnNumber(),
                actor.getToLineNumber());
        return new ActorNode(language, frameDescriptor, actions, actorSrc, name);
    }

    public ActionNode transformAction(Action action) {
        return (new ActionTransformer(language, source, lexicalScope, action, frameDescriptor)).transform();
    }

}
