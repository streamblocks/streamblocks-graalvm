package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "while", description = "The node implementing a while loop")
public final class StmtWhileNode extends CALStatementNode {

    @Child private LoopNode loopNode;

    public StmtWhileNode(CALExpressionNode conditionNode, CALStatementNode bodyNode) {
        this.loopNode = Truffle.getRuntime().createLoopNode(new StmtWhileRepeatingNode(conditionNode, bodyNode));
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        loopNode.execute(frame);
    }
}
