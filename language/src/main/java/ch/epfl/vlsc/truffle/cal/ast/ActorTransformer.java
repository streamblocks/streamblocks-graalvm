package ch.epfl.vlsc.truffle.cal.ast;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.entity.cal.CalActor;

public class ActorTransformer extends ScopedTransformer<ActorNode> {

    CalActor actor;
    QID name;

    public ActorTransformer(CalActor actor, QID name, TransformContext context) {
    	super(context);
    	// We want a clean frame
    	// TODO support global variables
    	context.clearLexicalScopeAndFrame();
        this.actor = actor;
        this.name = name;
    }

    public ActorNode transform() {
        List<CALStatementNode> headStatements = new ArrayList<CALStatementNode>(actor.getVarDecls().size()
                + actor.getValueParameters().size() + actor.getOutputPorts().size() + actor.getInputPorts().size());
        int i = 0;

        // TODO we are making assumptions about the number of arguments
        // and that EVERY argument and port is effectively passed

        // WARNING keep as the first declaration as it needs to match the arguments
        // position
        // Prepend arguments so they are specialized the same way as in the body
        for (VarDecl varDecl : actor.getValueParameters()) {
            headStatements.add(transformArgument(varDecl, i));
            i++;
        }

        for (PortDecl in : actor.getInputPorts()) {
            // Input ports are passed as arguments
            headStatements.add(transformPortDecl(in, i));
            i++;
        }
        for (PortDecl out : actor.getOutputPorts()) {
            // Input ports are passed as arguments
            headStatements.add(transformPortDecl(out, i));
            i++;
        }
        for (LocalVarDecl varDecl : actor.getVarDecls()) {
            headStatements.add(transformVarDecl(varDecl));
            i++;
        }

        // FIXME we can probably use a StmtBlockNode
        CALStatementNode head = new StmtBlockNode(headStatements.toArray(new CALStatementNode[headStatements.size()]));
        ActionNode[] actions = this.actor.getActions().map(x -> transformAction(x)).toArray(new ActionNode[0]);
        SourceSection actorSrc = context.getSource().createUnavailableSection(); /*.createSection(actor.getFromLineNumber(), actor.getFromColumnNumber(),
                actor.getToLineNumber());*/
        return new ActorNode(context.getLanguage(), context.getFrameDescriptor(), actions, head, actorSrc, name.toString());
    }

    public ActionNode transformAction(Action action) {
        return (new ActionTransformer(action, context.deeper(false))).transform();
    }

}
