package ch.epfl.vlsc.truffle.cal.runtime;

import ch.epfl.vlsc.truffle.cal.CALException;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;

public class CALUndefinedNameException extends CALException {

    private static final long serialVersionUID = 1L;

    @CompilerDirectives.TruffleBoundary
    public static CALUndefinedNameException undefinedFunction(Node location, Object name) {
        throw new CALUndefinedNameException("Undefined function: " + name, location);
    }

    @CompilerDirectives.TruffleBoundary
    public static CALUndefinedNameException undefinedProperty(Node location, Object name) {
        throw new CALUndefinedNameException("Undefined property: " + name, location);
    }

    private CALUndefinedNameException(String message, Node node) {
        super(message, node);
    }
}