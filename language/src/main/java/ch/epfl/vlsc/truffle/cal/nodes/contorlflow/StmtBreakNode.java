package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import ch.epfl.vlsc.truffle.cal.nodes.StmtNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "break", description = "The node implementing a break statement")

public final class StmtBreakNode extends StmtNode {

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw BreakException.SINGLETON;
    }
}
