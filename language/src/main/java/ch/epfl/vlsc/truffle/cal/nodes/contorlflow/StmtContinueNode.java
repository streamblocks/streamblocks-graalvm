package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import ch.epfl.vlsc.truffle.cal.nodes.StmtNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "continue", description = "The node implementing a continue statement")
public class StmtContinueNode extends StmtNode {

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw ContinueException.SINGLETON;
    }
}

