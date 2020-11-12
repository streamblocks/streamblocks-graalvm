package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class CALBreakException extends ControlFlowException {

    public static final CALBreakException SINGLETON = new CALBreakException();

    private static final long serialVersionUID = -91013036379258890L;

    /* Prevent instantiation from outside. */
    private CALBreakException() {
    }
}
