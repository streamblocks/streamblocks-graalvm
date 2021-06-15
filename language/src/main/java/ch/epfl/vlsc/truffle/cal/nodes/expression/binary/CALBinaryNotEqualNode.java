package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ShortCircuitNode;

@NodeInfo(shortName = "or")
public class CALBinaryNotEqualNode extends ShortCircuitNode {

    public CALBinaryNotEqualNode(CALExpressionNode left, CALExpressionNode right) {
        super(left, right);
    }

    @Override
    protected boolean isEvaluateRight(boolean left) {
        return !left;
    }

    @Override
    protected boolean execute(boolean left, boolean right) {
        return left != right;
    }

}
