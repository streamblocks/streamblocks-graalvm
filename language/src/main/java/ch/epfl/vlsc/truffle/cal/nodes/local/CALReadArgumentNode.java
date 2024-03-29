package ch.epfl.vlsc.truffle.cal.nodes.local;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALArguments;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;

public class CALReadArgumentNode extends CALExpressionNode {
    /** The argument number, i.e., the index into the array of arguments. */
    private final int index;

    /**
     * Profiling information, collected by the interpreter, capturing whether the function was
     * called with fewer actual arguments than formal arguments.
     */
    private final BranchProfile outOfBoundsTaken = BranchProfile.create();

    public CALReadArgumentNode(int index) {
        this.index = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (index < CALArguments.getArgumentCount(frame)) {
            return CALArguments.getArgument(frame, index);
        } else {
            // TODO should we fail with an arity exception?
            /* In the interpreter, record profiling information that the branch was used. */
            outOfBoundsTaken.enter();
            /* Use the default null value. */
            return CALNull.SINGLETON;
        }
    }

}
