package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.nodes.ControlFlowException;

public class InvariantViolationException extends ControlFlowException {
    private final SourceSection sourceSection;

    public InvariantViolationException(SourceSection sourceSection) {
        super();
        this.sourceSection = sourceSection;
    }

    @Override
    public String getMessage() {
        return "The Invariant at: " + sourceSection.toString() + " does not hold. Aborting";
    }
}
