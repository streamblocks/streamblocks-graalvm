package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import ch.epfl.vlsc.truffle.cal.runtime.CALBigDecimal;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;

@NodeInfo(shortName = "-")
public abstract class CALBinarySubNode extends CALBinaryNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected long sub(long left, long right) {
        return Math.subtractExact(left, right);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigNumber sub(CALBigNumber left, CALBigNumber right) {
        return new CALBigNumber(left.getValue().subtract(right.getValue()));
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigDecimal add(CALBigDecimal left, CALBigDecimal right) {
        return new CALBigDecimal(left.getValue().subtract(right.getValue()));
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }
}

