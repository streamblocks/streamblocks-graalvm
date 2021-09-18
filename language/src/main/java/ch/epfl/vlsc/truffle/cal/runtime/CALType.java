package ch.epfl.vlsc.truffle.cal.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import ch.epfl.vlsc.truffle.cal.CALLanguage;

@ExportLibrary(InteropLibrary.class)
@SuppressWarnings("static-method")
public class CALType implements TruffleObject {
    /*
     * These are the sets of builtin types in simple languages. In case of simple language the types
     * nicely match those of the types in InteropLibrary. This might not be the case and more
     * additional checks need to be performed (similar to number checking for SLBigNumber).
     */
    public static final CALType NUMBER = new CALType("Number", (l, v) -> l.fitsInLong(v) || v instanceof CALBigNumber);
    public static final CALType NULL = new CALType("NULL", (l, v) -> l.isNull(v));
    public static final CALType STRING = new CALType("String", (l, v) -> l.isString(v));
    public static final CALType BOOLEAN = new CALType("Boolean", (l, v) -> l.isBoolean(v));
    public static final CALType OBJECT = new CALType("Object", (l, v) -> l.hasMembers(v));
    public static final CALType FUNCTION = new CALType("Function", (l, v) -> l.isExecutable(v) && v instanceof CALFunction);
    public static final CALType ACTOR = new CALType("Function", (l, v) -> l.isExecutable(v) && v instanceof CALActorInstance);
    // TODO: Add Network as Type?
    // FIXME: Should PROCEDURE be added as a CALType?
    public static final CALType PROCEDURE = new CALType("Procedure", (l, v) -> l.isExecutable(v) && v instanceof CALProcedure);
    // FIXME
    // public static final CALType LIST = new CALType("List", (l, v) -> l.isList(v) && v instanceof CALActorInstance);

    /*
     * This array is used when all types need to be checked in a certain order. While most interop
     * types like number or string are exclusive, others traits like members might not be. For
     * example, an object might be a function. In SimpleLanguage we decided to make functions,
     * functions and not objects.
     */
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final CALType[] PRECEDENCE = new CALType[]{NULL, NUMBER, STRING, BOOLEAN, FUNCTION, ACTOR, OBJECT};

    private final String name;
    private final TypeCheck isInstance;

    /*
     * We don't allow dynamic instances of CALType. Real languages might want to expose this for
     * types that are user defined.
     */
    private CALType(String name, TypeCheck isInstance) {
        this.name = name;
        this.isInstance = isInstance;
    }

    /**
     * Checks whether this type is of a certain instance. If used on fast-paths it is required to
     * cast {@link CALType} to a constant.
     */
    public boolean isInstance(Object value, InteropLibrary interop) {
        CompilerAsserts.partialEvaluationConstant(this);
        return isInstance.check(interop, value);
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return CALLanguage.class;
    }

    /*
     * All CALTypes are declared as interop meta-objects. Other example for meta-objects are Java
     * classes, or JavaScript prototypes.
     */
    @ExportMessage
    boolean isMetaObject() {
        return true;
    }

    /*
     * SL does not have the notion of a qualified or simple name, so we return the same type name
     * for both.
     */
    @ExportMessage(name = "getMetaQualifiedName")
    @ExportMessage(name = "getMetaSimpleName")
    public Object getName() {
        return name;
    }

    @ExportMessage(name = "toDisplayString")
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return name;
    }

    @Override
    public String toString() {
        return "CALType[" + name + "]";
    }

    /*
     * The interop message isMetaInstance might be used from other languages or by the {@link
     * SLIsInstanceBuiltin isInstance} builtin. It checks whether a given value, which might be a
     * primitive, foreign or SL value is of a given SL type. This allows other languages to make
     * their instanceOf interopable with foreign values.
     */
    @ExportMessage
    static class IsMetaInstance {

        /*
         * We assume that the same type is checked at a source location. Therefore we use an inline
         * cache to specialize for observed types to be constant. The limit of "3" specifies that we
         * specialize for 3 different types until we rewrite to the doGeneric case. The limit in
         * this example is somewhat arbitrary and should be determined using careful tuning with
         * real world benchmarks.
         */
        @Specialization(guards = "type == cachedType", limit = "3")
        static boolean doCached(@SuppressWarnings("unused") CALType type, Object value,
                                @Cached("type") CALType cachedType,
                                @CachedLibrary("value") InteropLibrary valueLib) {
            return cachedType.isInstance.check(valueLib, value);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(replaces = "doCached")
        static boolean doGeneric(CALType type, Object value) {
            return type.isInstance.check(InteropLibrary.getFactory().getUncached(), value);
        }
    }

    /*
     * A convenience interface for type checks. Alternatively this could have been solved using
     * subtypes of CALType.
     */
    @FunctionalInterface
    interface TypeCheck {

        boolean check(InteropLibrary lib, Object value);

    }
}
