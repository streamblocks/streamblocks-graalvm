package ch.epfl.vlsc.truffle.cal.builtins;

import ch.epfl.vlsc.truffle.cal.runtime.CALBigDecimal;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.CALContext;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.math.BigDecimal;

@NodeInfo(shortName = "sin")
public abstract class CALSinBuiltin extends CALBuiltinNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    @CompilerDirectives.TruffleBoundary
    protected CALBigDecimal sin(long value) {
        return new CALBigDecimal(new BigDecimal(Math.sin(value)));
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigDecimal sin(CALBigNumber value) {
        return new CALBigDecimal(new BigDecimal(Math.sin(value.getValue().doubleValue())));
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigDecimal sin(CALBigDecimal value) {
        return new CALBigDecimal(new BigDecimal(Math.sin(value.getValue().doubleValue())));
    }
}
