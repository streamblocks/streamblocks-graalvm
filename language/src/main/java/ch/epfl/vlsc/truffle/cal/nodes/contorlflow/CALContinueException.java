package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class CALContinueException extends ControlFlowException {

    public static final CALContinueException SINGLETON = new CALContinueException();

    private static final long serialVersionUID = 5329687983726237188L;

    /* Prevent instantiation from outside. */
    private CALContinueException() {
    }
}
