package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "continue", description = "The node implementing a continue statement")
public class CALContinueNode extends CALStatementNode {

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw CALContinueException.SINGLETON;
    }
}

