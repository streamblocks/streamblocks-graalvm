package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifo;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifoFanout;
import ch.epfl.vlsc.truffle.cal.runtime.FifoConsumer;
import com.oracle.truffle.api.frame.VirtualFrame;

public class CALFifoFanoutAddFifo extends CALStatementNode {
    @Child private CALExpressionNode fanOutNode;
    @Child private CALExpressionNode fifoNode;

    public CALFifoFanoutAddFifo(CALExpressionNode fanOutNodeArg, CALExpressionNode fifoNodeArg) {
        super();
        this.fanOutNode = fanOutNodeArg;
        this.fifoNode = fifoNodeArg;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object receiver = this.fanOutNode.executeGeneric(frame);
        FifoConsumer fifo = (FifoConsumer) this.fifoNode.executeGeneric(frame);

        if (receiver instanceof CALFifoFanout) {
            CALFifoFanout fanout = (CALFifoFanout) receiver;
            fanout.addFifo(fifo);
            fifo.setFanout(fanout);
        } else if(receiver instanceof CALFifo) {
            CALFifoFanout fanout = ((CALFifo) receiver).getFanout();
            fanout.addFifo(fifo);
            fifo.setFanout(fanout);
        } else
            throw CALException.typeError(fanOutNode,receiver);
    }
}
