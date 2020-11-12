package ch.epfl.vlsc.truffle.cal.nodes.expression.unary;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "-")
public abstract class CALUnaryMinusNode extends ExprUnaryNode {

    @Specialization(rewriteOn = ArithmeticException.class)
    protected long minus(long value) {
        return -value;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigNumber minus(CALBigNumber value) {
        return new CALBigNumber(value.getValue().negate());
    }

    @Fallback
    protected Object typeError(Object value) {
        throw CALException.typeError(this, value);
    }
}