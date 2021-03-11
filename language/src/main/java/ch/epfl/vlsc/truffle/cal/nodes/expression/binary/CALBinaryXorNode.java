package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "^")
public abstract class CALBinaryXorNode extends CALBinaryNode {

    @Specialization(rewriteOn = ArithmeticException.class)
    protected long xor(long left, long right) {
        return left ^ right;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected CALBigNumber xor(CALBigNumber left, CALBigNumber right) {
        return new CALBigNumber(left.getValue().xor(right.getValue()));
    }

    @Specialization(guards = "isString(left, right)")
    @CompilerDirectives.TruffleBoundary
    protected String xor(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }

    protected boolean isString(Object a, Object b) {
        throw CALException.typeError(this, a, b);
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw CALException.typeError(this, left, right);
    }
}

