package ch.epfl.vlsc.truffle.cal.nodes.expression.literals;

import ch.epfl.vlsc.truffle.cal.nodes.expression.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.math.BigInteger;

@NodeInfo(shortName = "const")
public class CALBigIntegerLiteralNode extends CALExpressionNode {

    private final CALBigNumber value;

    public CALBigIntegerLiteralNode(BigInteger value) {
        this.value = new CALBigNumber(value);
    }

    @Override
    public CALBigNumber executeGeneric(VirtualFrame frame) {
        return value;
    }
}
