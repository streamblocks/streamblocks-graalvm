package ch.epfl.vlsc.truffle.cal.nodes.util;

import ch.epfl.vlsc.truffle.cal.nodes.CALTypes;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;


@TypeSystemReference(CALTypes.class)
@GenerateUncached
public abstract class CALToMemberNode extends Node {

    static final int LIMIT = 5;

    public abstract String execute(Object value) throws UnknownIdentifierException;

    @Specialization
    protected static String fromString(String value) {
        return value;
    }

    @Specialization
    protected static String fromBoolean(boolean value) {
        return String.valueOf(value);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected static String fromLong(long value) {
        return String.valueOf(value);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected static String fromBigNumber(CALBigNumber value) {
        return value.toString();
    }

    @Specialization(limit = "LIMIT")
    protected static String fromInterop(Object value, @CachedLibrary("value") InteropLibrary interop) throws UnknownIdentifierException {
        try {
            if (interop.fitsInLong(value)) {
                return longToString(interop.asLong(value));
            } else if (interop.isString(value)) {
                return interop.asString(value);
            } else if (interop.isNumber(value) && value instanceof CALBigNumber) {
                return bigNumberToString((CALBigNumber) value);
            } else {
                throw error(value);
            }
        } catch (UnsupportedMessageException e) {
            throw shouldNotReachHere(e);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static UnknownIdentifierException error(Object value) {
        return UnknownIdentifierException.create(value.toString());
    }

    @CompilerDirectives.TruffleBoundary
    private static String bigNumberToString(CALBigNumber value) {
        return value.toString();
    }

    @CompilerDirectives.TruffleBoundary
    private static String longToString(long longValue) {
        return String.valueOf(longValue);
    }

}

