package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;

@NodeInfo(shortName = "%")
public abstract class CALBinaryModNode extends CALBinaryNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected long mod(long left, long right) throws ArithmeticException {
        long result = left % right;
        /*
         * The division overflows if left is Long.MIN_VALUE and right is -1.
         */
        if ((left & right & result) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return result;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigNumber mod(CALBigNumber left, CALBigNumber right) {
        return new CALBigNumber(left.getValue().mod(right.getValue()));
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }
}

