package ch.epfl.vlsc.truffle.cal.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;

public final class LetExprNode extends CALExpressionNode {
    @Child private CALExpressionNode body;
    @Child private CALStatementNode head;

    // FIXME add parameters and return type
    public LetExprNode(CALStatementNode head, CALExpressionNode body) {
        this.head = head;
        this.body = body;
    }

    // FIXME return
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        head.executeVoid(frame);
        return body.executeGeneric(frame);
    }
}
