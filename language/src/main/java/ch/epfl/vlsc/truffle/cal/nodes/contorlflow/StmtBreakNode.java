package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;

@NodeInfo(shortName = "break", description = "The node implementing a break statement")

public final class StmtBreakNode extends CALStatementNode {

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw BreakException.SINGLETON;
    }
}
