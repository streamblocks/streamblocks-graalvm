package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.parser.utils.PartialOrderViolationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class CALActorInvariantNode extends CALStatementNode {
    CALExpressionNode invariant;
    public CALActorInvariantNode(CALExpressionNode inv) {
        this.invariant = inv;
    }
    @Override
    public void executeVoid(VirtualFrame frame) throws InvariantViolationException{
        boolean res = false;
        try {
            res = invariant.executeBoolean(frame);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Expected boolean from: " + invariant.getSourceSection() + " but got type: " + e.getResult().getClass().getName());
        }
        if (!res)
            throw new InvariantViolationException(invariant.getSourceSection());
    }
}
