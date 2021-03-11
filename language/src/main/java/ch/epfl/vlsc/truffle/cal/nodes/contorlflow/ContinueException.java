package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class ContinueException extends ControlFlowException {

    public static final ContinueException SINGLETON = new ContinueException();

    private static final long serialVersionUID = 5329687983726237188L;

    /* Prevent instantiation from outside. */
    private ContinueException() {
    }
}
