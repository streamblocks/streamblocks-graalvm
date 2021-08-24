package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;

@NodeInfo(shortName = "-")
public abstract class CALBinarySubNode extends CALBinaryNode {

    @Specialization(rewriteOn = ArithmeticException.class)
    protected int sub(int left, int right) {
        return Math.subtractExact(left, right);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected long sub(long left, long right) {
        return Math.subtractExact(left, right);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigNumber sub(CALBigNumber left, CALBigNumber right) {
        return new CALBigNumber(left.getValue().subtract(right.getValue()));
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }

}

