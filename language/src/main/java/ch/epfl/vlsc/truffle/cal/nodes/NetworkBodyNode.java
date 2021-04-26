package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class NetworkBodyNode extends CALStatementNode {
    @Children
    private final CALExpressionNode[] actors;

    public NetworkBodyNode(CALExpressionNode[] actors) {
        this.actors= actors;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        // Run all actors until the is not a single runnable
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
    }
}
