package ch.epfl.vlsc.truffle.cal.nodes.expression.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;

@NodeInfo(shortName = "const")
public class NullLiteralNode extends CALExpressionNode {

    public NullLiteralNode() {
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return CALNull.SINGLETON;
    }
}

