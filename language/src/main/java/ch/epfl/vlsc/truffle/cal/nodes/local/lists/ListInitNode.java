package ch.epfl.vlsc.truffle.cal.nodes.local.lists;

import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.GenericBufferList;

public class ListInitNode extends CALExpressionNode {
    @Children CALExpressionNode[] valuesNodes;

    public ListInitNode(CALExpressionNode[] v) {
        valuesNodes = v;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] values = new Object[valuesNodes.length];
        for(int i = 0; i < valuesNodes.length; i++)
            values[i] = valuesNodes[i].executeGeneric(frame);
        return new GenericBufferList(values);
    }

}
