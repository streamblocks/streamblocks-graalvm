package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifo;
import com.oracle.truffle.api.frame.VirtualFrame;

public class CALFifoTransactionCommit extends CALFifoTransactionConclude{
    public CALFifoTransactionCommit(CALExpressionNode readNode) {
        super(readNode);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        CALFifo list = (CALFifo) fifo.executeGeneric(frame);
        list.commit();
    }
}
