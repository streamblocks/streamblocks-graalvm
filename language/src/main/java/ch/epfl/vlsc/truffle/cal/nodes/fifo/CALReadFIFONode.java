package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.CALException;
import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifo;

public class CALReadFIFONode extends CALExpressionNode {
   
    @Child private CALExpressionNode fifo;
    public CALReadFIFONode(CALExpressionNode fifo) {
        super();
        this.fifo = fifo;
    }
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object fifoObject = fifo.executeGeneric(frame);
        if (fifoObject instanceof CALFifo)
            return ((CALFifo) fifoObject).removeFirst();
        else
            throw CALException.typeError(fifo, fifoObject);
    }
}
