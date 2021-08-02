package ch.epfl.vlsc.truffle.cal.nodes.expression;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import com.oracle.truffle.api.frame.VirtualFrame;

public class CALComprehensionRootNode extends CALExpressionNode {
    private final CALExpressionNode listReadNode;
    @Child private StmtBlockNode init;

    public CALComprehensionRootNode(StmtBlockNode init, CALExpressionNode listReadNode) {
        super();
        this.init = init;
        this.listReadNode = listReadNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        init.executeVoid(frame);
        return listReadNode.executeGeneric(frame);
    }
}
