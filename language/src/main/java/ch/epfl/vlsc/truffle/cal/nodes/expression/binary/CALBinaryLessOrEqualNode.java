package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import ch.epfl.vlsc.truffle.cal.runtime.CALBigDecimal;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;

@NodeInfo(shortName = "<=")
public abstract class CALBinaryLessOrEqualNode extends CALBinaryNode {
    @Specialization
    protected boolean lessOrEqual(long left, long right) {
        return left <= right;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected boolean lessOrEqual(CALBigNumber left, CALBigNumber right) {
        return left.compareTo(right) <= 0;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected boolean lessOrEqual(CALBigDecimal left, CALBigDecimal right) {
        return left.compareTo(right) <= 0;
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }
}
