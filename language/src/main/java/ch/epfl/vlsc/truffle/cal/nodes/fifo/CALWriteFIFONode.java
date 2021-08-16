package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.runtime.CALFifoFanout;
import ch.epfl.vlsc.truffle.cal.runtime.FifoConsumer;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifo;

import com.oracle.truffle.api.CompilerDirectives;

public class CALWriteFIFONode extends CALStatementNode {

    @Child private CALExpressionNode fifo;
    @Child private CALExpressionNode value;
    public CALWriteFIFONode(CALExpressionNode fifo, CALExpressionNode value) {
        super();
        this.fifo = fifo;
        this.value = value;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        Object fifoObject = fifo.executeGeneric(frame);
        if (fifoObject instanceof FifoConsumer)
            ((FifoConsumer) fifoObject).add(value.executeGeneric(frame));
        else
            throw new Error("not good type");
    }

	
}
