package ch.epfl.vlsc.truffle.cal;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.runtime.CALContext;
import ch.epfl.vlsc.truffle.cal.runtime.CALLanguageView;

public class CALException extends AbstractTruffleException {
    private static final long serialVersionUID = -6799734410727348507L;
    private static final InteropLibrary UNCACHED_LIB = InteropLibrary.getFactory().getUncached();

    @CompilerDirectives.TruffleBoundary
    public CALException(String message, Node location) {
        super(message, location);
    }

    /**
     * Provides a user-readable message for run-time type errors. SL is strongly typed, i.e., there
     * are no automatic type conversions of values.
     */
    @CompilerDirectives.TruffleBoundary
    public static CALException typeError(Node operation, Object... values) {
        StringBuilder result = new StringBuilder();
        result.append("Type error");

        if (operation != null) {
            SourceSection ss = operation.getEncapsulatingSourceSection();
            if (ss != null && ss.isAvailable()) {
                result.append(" at ").append(ss.getSource().getName()).append(" line ").append(ss.getStartLine()).append(" col ").append(ss.getStartColumn());
            }
        }

        result.append(": operation");
        if (operation != null) {
            NodeInfo nodeInfo = CALContext.lookupNodeInfo(operation.getClass());
            if (nodeInfo != null) {
                result.append(" \"").append(nodeInfo.shortName()).append("\"");
            }
        }

        result.append(" not defined for");

        String sep = " ";
        for (int i = 0; i < values.length; i++) {
            /*
             * For primitive or foreign values we request a language view so the values are printed
             * from the perspective of simple language and not another language. Since this is a
             * rather rarely invoked exceptional method, we can just create the language view for
             * primitive values and then conveniently request the meta-object and display strings.
             * Using the language view for core builtins like the typeOf builtin might not be a good
             * idea for performance reasons.
             */
            Object value = CALLanguageView.forValue(values[i]);
            result.append(sep);
            sep = ", ";
            if (value == null) {
                result.append("ANY");
            } else {
                InteropLibrary valueLib = InteropLibrary.getFactory().getUncached(value);
                if (valueLib.hasMetaObject(value) && !valueLib.isNull(value)) {
                    String qualifiedName;
                    try {
                        qualifiedName = UNCACHED_LIB.asString(UNCACHED_LIB.getMetaQualifiedName(valueLib.getMetaObject(value)));
                    } catch (UnsupportedMessageException e) {
                        throw shouldNotReachHere(e);
                    }
                    result.append(qualifiedName);
                    result.append(" ");
                }
                if (valueLib.isString(value)) {
                    result.append("\"");
                }
                result.append(valueLib.toDisplayString(value));
                if (valueLib.isString(value)) {
                    result.append("\"");
                }
            }
        }
        return new CALException(result.toString(), operation);
    }

}
