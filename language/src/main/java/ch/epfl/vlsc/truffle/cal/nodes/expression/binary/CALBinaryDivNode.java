package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigDecimal;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;

import java.math.RoundingMode;

@NodeInfo(shortName = "/")

public abstract class CALBinaryDivNode extends CALBinaryNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected long div(long left, long right) throws ArithmeticException {
        long result = left / right;
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
    protected CALBigNumber div(CALBigNumber left, CALBigNumber right) {
        return new CALBigNumber(left.getValue().divide(right.getValue()));
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigDecimal div(CALBigDecimal left, CALBigDecimal right) {
        return new CALBigDecimal(left.getValue().divide(right.getValue(), CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.DIVISIONSCALE_KEY), RoundingMode.HALF_EVEN));
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }
}
