package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;

import java.math.BigInteger;

@TypeSystem({long.class, boolean.class})
public abstract class CALTypes {

    /**
     * Example of a manually specified type check that replaces the automatically generated type
     * check that the Truffle DSL would generate. For {@link CALNull}, we do not need an
     * {@code instanceof} check, because we know that there is only a {@link CALNull#SINGLETON
     * singleton} instance.
     */
    @TypeCheck(CALNull.class)
    public static boolean isCALNull(Object value) {
        return value == CALNull.SINGLETON;
    }

    /**
     * Example of a manually specified type cast that replaces the automatically generated type cast
     * that the Truffle DSL would generate. For {@link CALNull}, we do not need an actual cast,
     * because we know that there is only a {@link CALNull#SINGLETON singleton} instance.
     */
    @TypeCast(CALNull.class)
    public static CALNull asCALNull(Object value) {
        assert isCALNull(value);
        return CALNull.SINGLETON;
    }

    /**
     * Informs the Truffle DSL that a primitive {@code long} value can be used in all
     * specializations where a {@link CALBigNumber} is expected. This models the semantic of SL: It
     * only has an arbitrary precision Number type (implemented as {@link CALBigNumber}, and
     * {@code long} is only used as a performance optimization to avoid the costly
     * {@link CALBigNumber} arithmetic for values that fit into a 64-bit primitive value.
     */
    @ImplicitCast
    @CompilerDirectives.TruffleBoundary
    public static CALBigNumber castBigNumber(long value) {
        return new CALBigNumber(BigInteger.valueOf(value));
    }
}