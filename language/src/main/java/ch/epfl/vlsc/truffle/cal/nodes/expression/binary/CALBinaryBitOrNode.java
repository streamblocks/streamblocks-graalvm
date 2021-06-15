package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;

@NodeInfo(shortName = "|")
public abstract class CALBinaryBitOrNode extends CALBinaryNode {

    @Specialization(rewriteOn = ArithmeticException.class)
    protected long bitOr(long left, long right) {
        return left & right;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigNumber bitOr(CALBigNumber left, CALBigNumber right) {
        return new CALBigNumber(left.getValue().or(right.getValue()));
    }

    @CompilerDirectives.TruffleBoundary
    protected String bitOr(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }
}

