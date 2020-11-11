package ch.epfl.vlsc.truffle.cal.builtins;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.runtime.CALContext;
import ch.epfl.vlsc.truffle.cal.runtime.CALLanguageView;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;

public abstract class CALPrintlnBuiltin extends CALBuiltinNode{
    @Specialization
    @CompilerDirectives.TruffleBoundary
    public Object println(Object value,
                          @CachedLibrary(limit = "3") InteropLibrary interop,
                          @CachedContext(CALLanguage.class) CALContext context) {
        context.getOutput().println(interop.toDisplayString(CALLanguageView.forValue(value)));
        return value;
    }
}
