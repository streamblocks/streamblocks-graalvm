package ch.epfl.vlsc.truffle.cal.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.CALContext;


/**
 * Builtin function to write a value to the {@link CALContext#getOutput() standard output}. The
 * different specialization leverage the typed {@code println} methods available in Java, i.e.,
 * primitive values are printed without converting them to a {@link String} first.
 * <p>
 * Printing involves a lot of Java code, so we need to tell the optimizing system that it should not
 * unconditionally inline everything reachable from the println() method. This is done via the
 * {@link TruffleBoundary} annotations.
 */
@NodeInfo(shortName = "lshift")
public abstract class CALLshiftBuiltin extends CALBuiltinNode {

    @Specialization
    @TruffleBoundary
    public Object lshift(CALBigNumber value, CALBigNumber amount,
                    @CachedLibrary(limit = "3") InteropLibrary interop,
                    @CachedContext(CALLanguage.class) CALContext context) {
        return new CALBigNumber(value.getValue().shiftLeft(amount.getValue().intValue()));
    }

}

