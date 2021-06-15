package ch.epfl.vlsc.truffle.cal.nodes.expression.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

@NodeInfo(shortName = "const")
public final class StringLiteralNode extends CALExpressionNode {

    private final String value;

    public StringLiteralNode(String value) {
        this.value = value;
    }

    @Override
    public String executeGeneric(VirtualFrame frame) {
        return value;
    }
}
