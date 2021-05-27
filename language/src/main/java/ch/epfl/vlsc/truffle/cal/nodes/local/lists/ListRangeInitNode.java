package ch.epfl.vlsc.truffle.cal.nodes.local.lists;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.GenericBufferList;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeChild(type = CALExpressionNode.class, value = "bottom")
@NodeChild(type = CALExpressionNode.class, value = "top")
public abstract class ListRangeInitNode extends CALExpressionNode {

    // FIXME
    @Specialization
    public Object executeList(CALBigNumber bottom, CALBigNumber top) {
        Integer[] values = new Integer[top.getValue().intValue()-bottom.getValue().intValue()];
        for(int i = bottom.getValue().intValue(); i < top.getValue().intValue(); i++)
            values[i-bottom.getValue().intValue()] = i;
        return new GenericBufferList(values);
    }

}
