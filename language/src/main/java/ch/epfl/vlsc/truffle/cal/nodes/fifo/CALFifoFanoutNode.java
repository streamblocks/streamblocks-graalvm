package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifoFanout;
import com.oracle.truffle.api.frame.VirtualFrame;

public class CALFifoFanoutNode extends CALExpressionNode {
    public CALFifoFanoutNode(){
        super();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return new CALFifoFanout();
    }
}
