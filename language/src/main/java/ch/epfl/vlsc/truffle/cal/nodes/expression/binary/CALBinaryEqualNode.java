package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

import ch.epfl.vlsc.truffle.cal.runtime.CALBigDecimal;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.CALFunction;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;

@NodeInfo(shortName = "=")
public abstract class CALBinaryEqualNode  extends CALBinaryNode {
    @Specialization
    protected boolean doLong(long left, long right) {
        return left == right;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected boolean doBigNumber(CALBigNumber left, CALBigNumber right) {
        return left.equals(right);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected boolean doBigDecimal(CALBigDecimal left, CALBigDecimal right) {
        return left.equals(right);
    }

    @Specialization
    protected boolean doBoolean(boolean left, boolean right) {
        return left == right;
    }

    @Specialization
    protected boolean doString(String left, String right) {
        return left.equals(right);
    }

    @Specialization
    protected boolean doNull(CALNull left, CALNull right) {
        return left == right;
    }

    @Specialization
    protected boolean doFunction(CALFunction left, Object right) {
        return left == right;
    }

    @Specialization(limit = "4")
    public boolean doGeneric(Object left, Object right,
                             @CachedLibrary("left") InteropLibrary leftInterop,
                             @CachedLibrary("right") InteropLibrary rightInterop) {
        /*
         * This method looks very inefficient. In practice most of these branches fold as the
         * interop type checks typically return a constant when using a cached library.
         *
         * Exercise: Try looking at what happens to this method during partial evaluation in IGV.
         * Tip: comment out all the previous @Specialization annotations to make it easier to
         * activate this specialization.
         */
        try {
            if (leftInterop.isBoolean(left) && rightInterop.isBoolean(right)) {
                return doBoolean(leftInterop.asBoolean(left), rightInterop.asBoolean(right));
            } else if (leftInterop.isString(left) && rightInterop.isString(right)) {
                return doString(leftInterop.asString(left), (rightInterop.asString(right)));
            } else if (leftInterop.isNull(left) && rightInterop.isNull(right)) {
                return true;
            } else if (leftInterop.fitsInLong(left) && rightInterop.fitsInLong(right)) {
                return doLong(leftInterop.asLong(left), (rightInterop.asLong(right)));
            } else if (left instanceof CALBigNumber && right instanceof CALBigNumber) {
                return doBigNumber((CALBigNumber) left, (CALBigNumber) right);
            } else if (leftInterop.hasIdentity(left) && rightInterop.hasIdentity(right)) {
                return leftInterop.isIdentical(left, right, rightInterop);
            } else {
                /*
                 * We return false in good dynamic language manner. Stricter languages might throw
                 * an error here.
                 */
                return false;
            }
        } catch (UnsupportedMessageException e) {
            throw shouldNotReachHere(e);
        }
    }

}
