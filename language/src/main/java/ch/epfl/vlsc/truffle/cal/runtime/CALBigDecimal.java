package ch.epfl.vlsc.truffle.cal.runtime;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.math.BigDecimal;
import java.math.BigInteger;

@ExportLibrary(InteropLibrary.class)
@SuppressWarnings("static-method")
public class CALBigDecimal implements TruffleObject, Comparable<CALBigDecimal> {
    private static final long LONG_MAX_SAFE_DOUBLE = 9007199254740991L; // 2 ** 53 - 1
    private static final int INT_MAX_SAFE_FLOAT = 16777215; // 2 ** 24 - 1

    private static boolean inSafeDoubleRange(long l) {
        return l >= -LONG_MAX_SAFE_DOUBLE && l <= LONG_MAX_SAFE_DOUBLE;
    }

    private static boolean inSafeFloatRange(int i) {
        return i >= -INT_MAX_SAFE_FLOAT && i <= INT_MAX_SAFE_FLOAT;
    }

    private final BigDecimal value;

    public CALBigDecimal(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getValue() {
        return value;
    }

    @CompilerDirectives.TruffleBoundary
    public int compareTo(CALBigDecimal o) {
        return value.compareTo(o.getValue());
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return value.toString();
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public boolean equals(Object obj) {
        if (obj instanceof CALBigDecimal) {
            return value.equals(((CALBigDecimal) obj).getValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isNumber() {
        return true;
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    boolean fitsInByte() {
        try {
            value.byteValueExact();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    boolean fitsInShort() {
        try {
            value.shortValueExact();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    boolean fitsInFloat() {
        // https://stackoverflow.com/questions/59123861/how-to-know-if-a-bigdecimal-can-exactly-convert-to-float-or-double
        float result = value.floatValue();
        if (!(Float.isNaN(result) || Float.isInfinite(result))) {
            if (new BigDecimal(String.valueOf(result)).compareTo(value) == 0) {
                return true;
            }
        }
        return false;
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    boolean fitsInLong() {
        try {
            value.longValueExact();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    boolean fitsInInt() {
        try {
            value.intValueExact();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    boolean fitsInDouble() {
        // https://stackoverflow.com/questions/59123861/how-to-know-if-a-bigdecimal-can-exactly-convert-to-float-or-double
        double result = value.doubleValue();
        if (!(Double.isNaN(result) || Double.isInfinite(result))) {
            if (new BigDecimal(String.valueOf(result)).compareTo(value) == 0) {
                return true;
            }
        }
        return false;
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    double asDouble() throws UnsupportedMessageException {
        if (fitsInDouble()) {
            return value.doubleValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    long asLong() throws UnsupportedMessageException {
        if (fitsInLong()) {
            return value.longValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    byte asByte() throws UnsupportedMessageException {
        if (fitsInByte()) {
            return value.byteValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    int asInt() throws UnsupportedMessageException {
        if (fitsInInt()) {
            return value.intValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    float asFloat() throws UnsupportedMessageException {
        if (fitsInFloat()) {
            return value.floatValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    short asShort() throws UnsupportedMessageException {
        if (fitsInShort()) {
            return value.shortValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return CALLanguage.class;
    }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return CALType.NUMBER;
    }

    @ExportMessage
    @CompilerDirectives.TruffleBoundary
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return value.toString();
    }
}
