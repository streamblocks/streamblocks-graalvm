package ch.epfl.vlsc.truffle.cal.nodes.local.lists;

import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.GenericArrayListList;

public class UnknownSizeListInitNode extends CALExpressionNode {

    public UnknownSizeListInitNode() {
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return new GenericArrayListList(0);
    }

}

