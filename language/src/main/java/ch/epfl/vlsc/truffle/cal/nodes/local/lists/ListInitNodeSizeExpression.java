package ch.epfl.vlsc.truffle.cal.nodes.local.lists;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALTypesGen;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.GenericBufferList;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.CompilerDirectives;

public class ListInitNodeSizeExpression extends CALExpressionNode {
    @Child CALExpressionNode sizeExpression;
    @Child CALExpressionNode defaultValue;

    public ListInitNodeSizeExpression(CALExpressionNode size, CALExpressionNode defaultVal) {
        this.sizeExpression = size;
        this.defaultValue = defaultVal;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        int size = CALTypesGen.asImplicitCALBigNumber(sizeExpression.executeGeneric(frame)).getValue().intValue();
        Object[] values = new Object[size];
        for(int i = 0; i < size; i++)
            values[i] = defaultValue.executeGeneric(frame);
        return new GenericBufferList(values);
    }

}
