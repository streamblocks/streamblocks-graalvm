package ch.epfl.vlsc.truffle.cal.runtime;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.types.CALType;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;


@ExportLibrary(value = InteropLibrary.class, delegateTo = "delegate")
@SuppressWarnings("static-method")
public final class CALLanguageView implements TruffleObject {

    final Object delegate;

    CALLanguageView(Object delegate) {
        this.delegate = delegate;
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    /*
     * Language views must always associate with the language they were created for. This allows
     * tooling to take a primitive or foreign value and create a value of simple language of it.
     */
    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return CALLanguage.class;
    }

    @ExportMessage
    @ExplodeLoop
    boolean hasMetaObject(@CachedLibrary("this.delegate") InteropLibrary interop) {
        /*
         * We use the isInstance method to find out whether one of the builtin simple language types
         * apply. If yes, then we can provide a meta object in getMetaObject. The interop contract
         * requires to be precise.
         *
         * Since language views are only created for primitive values and values of other languages,
         * values from simple language itself directly implement has/getMetaObject. For example
         * SLFunction is already associated with the SLLanguage and therefore the language view will
         * not be used.
         */
        for (CALType type : CALType.PRECEDENCE) {
            if (type.isInstance(delegate, interop)) {
                return true;
            }
        }
        return false;
    }

    @ExportMessage
    @ExplodeLoop
    Object getMetaObject(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        /*
         * We do the same as in hasMetaObject but actually return the type this time.
         */
        for (CALType type : CALType.PRECEDENCE) {
            if (type.isInstance(delegate, interop)) {
                return type;
            }
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    @ExplodeLoop
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects, @CachedLibrary("this.delegate") InteropLibrary interop) {
        for (CALType type : CALType.PRECEDENCE) {
            if (type.isInstance(this.delegate, interop)) {
                try {
                    /*
                     * The type is a partial evaluation constant here as we use @ExplodeLoop. So
                     * this if-else cascade should fold after partial evaluation.
                     */
                    if (type == CALType.NUMBER) {
                        return longToString(interop.asLong(delegate));
                    } else if (type == CALType.BOOLEAN) {
                        return Boolean.toString(interop.asBoolean(delegate));
                    } else if (type == CALType.STRING) {
                        return interop.asString(delegate);
                    } else {
                        /* We use the type name as fallback for any other type */
                        return type.getName();
                    }
                } catch (UnsupportedMessageException e) {
                    throw shouldNotReachHere(e);
                }
            }
        }
        return "Unsupported to string conversion";
    }

    /*
     * Long.toString is not safe for partial evaluation and therefore needs to be called behind a
     * boundary.
     */
    @TruffleBoundary
    private static String longToString(long l) {
        return Long.toString(l);
    }

    public static Object create(Object value) {
        assert isPrimitiveOrFromOtherLanguage(value);
        return new CALLanguageView(value);
    }

    /*
     * Language views are intended to be used only for primitives and other language values.
     */
    private static boolean isPrimitiveOrFromOtherLanguage(Object value) {
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
        try {
            return !interop.hasLanguage(value) || interop.getLanguage(value) != CALLanguage.class;
        } catch (UnsupportedMessageException e) {
            throw shouldNotReachHere(e);
        }
    }

    /**
     * Returns a language view for primitive or foreign values. Returns the same value for values
     * that are already originating from SimpleLanguage. This is useful to view values from the
     * perspective of simple language in slow paths, for example, printing values in error messages.
     */
    @TruffleBoundary
    public static Object forValue(Object value) {
        if (value == null) {
            return null;
        }
        InteropLibrary lib = InteropLibrary.getFactory().getUncached(value);
        try {
            if (lib.hasLanguage(value) && lib.getLanguage(value) == CALLanguage.class) {
                return value;
            } else {
                return create(value);
            }
        } catch (UnsupportedMessageException e) {
            throw shouldNotReachHere(e);
        }
    }

}
