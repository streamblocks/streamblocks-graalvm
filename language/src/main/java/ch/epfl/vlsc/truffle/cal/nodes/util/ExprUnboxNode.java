package ch.epfl.vlsc.truffle.cal.nodes.util;

import ch.epfl.vlsc.truffle.cal.types.Types;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.CALFunction;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@TypeSystemReference(Types.class)
@NodeChild
public abstract class ExprUnboxNode extends CALExpressionNode {

    static final int LIMIT = 5;
    
    @Specialization
    protected static String fromString(String value) {
        return value;
    }

    @Specialization
    protected static boolean fromBoolean(boolean value) {
        return value;
    }

    @Specialization
    protected static long fromLong(long value) {
        return value;
    }

    @Specialization
    protected static CALBigNumber fromBigNumber(CALBigNumber value) {
        return value;
    }

    @Specialization
    protected static CALFunction fromFunction(CALFunction value) {
        return value;
    }

    @Specialization
    protected static CALNull fromFunction(CALNull value) {
        return value;
    }

    @Specialization(limit = "LIMIT")
    public static Object fromForeign(Object value, @CachedLibrary("value") InteropLibrary interop) {
        try {
            if (interop.fitsInLong(value)) {
                return interop.asLong(value);
            } else if (interop.fitsInDouble(value)) {
                return (long) interop.asDouble(value);
            } else if (interop.isString(value)) {
                return interop.asString(value);
            } else if (interop.isBoolean(value)) {
                return interop.asBoolean(value);
            } else {
                return value;
            }
        } catch (UnsupportedMessageException e) {
            throw shouldNotReachHere(e);
        }
    }

}
