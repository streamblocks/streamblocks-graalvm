package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;

public class StmtGroupNode extends CALStatementNode{
    private final CALStatementNode[] children;
    public StmtGroupNode(CALStatementNode[] children) {
        this.children = children;
    }

    public void executeVoid(VirtualFrame frame) {
        for (CALStatementNode child : children) {
            child.executeVoid(frame);
        }
    }
}
