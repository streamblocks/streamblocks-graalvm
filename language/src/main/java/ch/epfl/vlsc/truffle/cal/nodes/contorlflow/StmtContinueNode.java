package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;

@NodeInfo(shortName = "continue", description = "The node implementing a continue statement")
public class StmtContinueNode extends CALStatementNode {

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw ContinueException.SINGLETON;
    }
}

