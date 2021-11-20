package ch.epfl.vlsc.truffle.cal.nodes.expression.literals;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigDecimal;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;

public class BigDecimalLiteralNode extends CALExpressionNode {
    private final CALBigDecimal value;
    public BigDecimalLiteralNode(BigDecimal bigDecimal) {
        this.value = new CALBigDecimal(bigDecimal);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}
