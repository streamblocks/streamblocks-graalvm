package ch.epfl.vlsc.truffle.cal.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

public class ExprParenNode extends CALExpressionNode {

    @Node.Child
    private CALExpressionNode expression;

    public ExprParenNode(CALExpressionNode expression) {
        this.expression = expression;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return expression.executeGeneric(frame);
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return expression.executeLong(frame);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return expression.executeBoolean(frame);
    }
}

