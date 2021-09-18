package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifo;
import com.oracle.truffle.api.frame.VirtualFrame;

public class CALFifoTransactionRollback extends CALFifoTransactionConclude{
    public CALFifoTransactionRollback(CALExpressionNode readNode) {
        super(readNode);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        CALFifo list = (CALFifo) fifo.executeGeneric(frame);
        list.rollback();
    }
}
