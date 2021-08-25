package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;

@NodeInfo(shortName = "&")
public abstract class CALBinaryBitAndNode extends CALBinaryNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected long bitAnd(long left, long right) {
        return left & right;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigNumber bitAnd(CALBigNumber left, CALBigNumber right) {
        return new CALBigNumber(left.getValue().and(right.getValue()));
    }

    @CompilerDirectives.TruffleBoundary
    protected String bitAnd(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }
}

