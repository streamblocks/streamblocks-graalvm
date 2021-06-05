package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "<=")
public abstract class CALBinaryBiggerOrEqualNode extends CALBinaryNode {

    @Specialization
    protected boolean biggerOrEqual(long left, long right) {
        return left >= right;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected boolean biggerOrEqual(CALBigNumber left, CALBigNumber right) {
        return left.compareTo(right) >= 0;
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }
}

