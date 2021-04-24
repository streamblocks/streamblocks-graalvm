package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALArguments;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifo;

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
        Object fifoObject = fifo.executeGeneric(frame);
        if (fifoObject instanceof CALFifo)
            ((CALFifo) fifoObject).add(value.executeGeneric(frame));
        else
            throw new Error("not good type");
    }

	
}
