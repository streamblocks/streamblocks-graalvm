package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFifo;

public class CALCreateFIFONode extends CALExpressionNode {
    public Object executeGeneric(VirtualFrame frame) {
        return new CALFifo();
    }
}
