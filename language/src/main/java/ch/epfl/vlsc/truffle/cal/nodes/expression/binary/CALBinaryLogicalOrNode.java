package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import ch.epfl.vlsc.truffle.cal.nodes.expression.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALShortCircuitNode;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "or")
public class CALBinaryLogicalOrNode extends CALShortCircuitNode {

    public CALBinaryLogicalOrNode(CALExpressionNode left, CALExpressionNode right) {
        super(left, right);
    }

    @Override
    protected boolean isEvaluateRight(boolean left) {
        return !left;
    }

    @Override
    protected boolean execute(boolean left, boolean right) {
        return left || right;
    }

}
