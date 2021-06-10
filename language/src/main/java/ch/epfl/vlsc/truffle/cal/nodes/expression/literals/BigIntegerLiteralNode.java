package ch.epfl.vlsc.truffle.cal.nodes.expression.literals;

import java.math.BigInteger;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;

@NodeInfo(shortName = "const")
public class BigIntegerLiteralNode extends CALExpressionNode {

    private final CALBigNumber value;

    public BigIntegerLiteralNode(BigInteger value) {
        this.value = new CALBigNumber(value);
    }

    @Override
    public CALBigNumber executeGeneric(VirtualFrame frame) {
        return value;
    }
}
