package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class CALReturnException  extends ControlFlowException {

    private static final long serialVersionUID = 4073191346281369231L;

    private final Object result;

    public CALReturnException(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
}
