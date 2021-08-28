package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import com.oracle.truffle.api.CompilerDirectives;

import ch.epfl.vlsc.truffle.cal.runtime.CALActorInstance;

/* Instantiate an actor
 * Takes care of assigning the variable declaration and the actor state
 */
class ActorInstantiateNode extends CALExpressionNode {
    ActorNode actor;
    // Variable declaration now, but maybe other stuff too later
    @Child CALStatementNode head;
    public ActorInstantiateNode(ActorNode actor, CALStatementNode head) {
        this.head = head;
        this.actor = actor;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        // create a new frame with the actor's frame descriptor
        MaterializedFrame actorFrame = Truffle.getRuntime().createMaterializedFrame(frame.getArguments(), actor.getFrameDescriptor());
        head.executeVoid(actorFrame);
        for (ActionNode initializeraction : this.actor.getInitializerActions()) {
            CallTarget target = Truffle.getRuntime().createCallTarget(initializeraction);
            Boolean executed = (Boolean) Truffle.getRuntime().createIndirectCallNode().call(target, actorFrame);
            if (executed)
                break;
        }
        if(actor.getCurrStateSlot() != null) actorFrame.setLong(actor.getCurrStateSlot(), 0);
        CALActorInstance actorInstance = new CALActorInstance(actor, actorFrame);
        return actorInstance;
    }
}
