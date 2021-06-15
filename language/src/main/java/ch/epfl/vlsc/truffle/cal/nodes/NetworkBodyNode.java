package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class NetworkBodyNode extends CALExpressionNode {
    @Children
    private final CALExpressionNode[] actors;

    public NetworkBodyNode(CALExpressionNode[] actors) {
        this.actors= actors;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // Run all actors until three is not a single runnable
        // action remaining
        // The return value of the actor is true iff the actor
        // could run an action
        boolean remaining = true;
        while (remaining) {
            remaining = false;
            for (CALExpressionNode actor : actors) {
                try {
                    remaining |= actor.executeBoolean(frame);
                } catch (UnexpectedResultException e) {
                    throw new Error("internal error");
                }
            }
        }
        return remaining;
    }
}
