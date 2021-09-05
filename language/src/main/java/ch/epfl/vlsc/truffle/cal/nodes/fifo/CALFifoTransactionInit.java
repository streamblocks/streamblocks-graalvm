package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifo;
import com.oracle.truffle.api.frame.VirtualFrame;

public class CALFifoTransactionInit extends CALStatementNode {
    private final CALExpressionNode readNode;

    public CALFifoTransactionInit(CALExpressionNode readNode) {
        super();
        this.readNode = readNode;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        CALFifo fifo = (CALFifo) readNode.executeGeneric(frame);
        fifo.startTransaction();
    }
}
