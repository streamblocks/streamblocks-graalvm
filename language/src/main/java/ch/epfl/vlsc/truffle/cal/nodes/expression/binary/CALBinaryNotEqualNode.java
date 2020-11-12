package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import ch.epfl.vlsc.truffle.cal.nodes.expression.ExprNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ShortCircuitNode;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "or")
public class CALBinaryNotEqualNode extends ShortCircuitNode {

    public CALBinaryNotEqualNode(ExprNode left, ExprNode right) {
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
