package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;

public final class ActionNode extends CALExpressionNode {
    @Child
    private StmtBlockNode block;
    public ActionNode(CALLanguage language, CALStatementNode[] bodyNodes) {
        super();
        block = new StmtBlockNode(bodyNodes);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        block.executeVoid(frame);
        // FIXME
        return null;
    }
}
