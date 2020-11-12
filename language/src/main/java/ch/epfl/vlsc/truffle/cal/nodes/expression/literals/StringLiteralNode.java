package ch.epfl.vlsc.truffle.cal.nodes.expression.literals;

import ch.epfl.vlsc.truffle.cal.nodes.expression.ExprNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "const")
public final class StringLiteralNode extends ExprNode {

    private final String value;

    public StringLiteralNode(String value) {
        this.value = value;
    }

    @Override
    public String executeGeneric(VirtualFrame frame) {
        return value;
    }
}
