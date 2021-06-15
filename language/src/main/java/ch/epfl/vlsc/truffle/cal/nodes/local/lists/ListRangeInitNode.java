package ch.epfl.vlsc.truffle.cal.nodes.local.lists;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.GenericBufferList;

@NodeChild(type = CALExpressionNode.class, value = "bottom")
@NodeChild(type = CALExpressionNode.class, value = "top")
public abstract class ListRangeInitNode extends CALExpressionNode {

    // FIXME use an implementation only for functional lists
    @Specialization
    public Object executeList(CALBigNumber bottom, CALBigNumber top) {
        CALBigNumber[] values = new CALBigNumber[top.getValue().intValue()-bottom.getValue().intValue()+1];
        for(int i = bottom.getValue().intValue(); i <= top.getValue().intValue(); i++)
            values[i-bottom.getValue().intValue()] = new CALBigNumber(i);
        return new GenericBufferList(values);
    }

}
