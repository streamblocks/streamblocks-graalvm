package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import ch.epfl.vlsc.truffle.cal.nodes.StmtNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@NodeInfo(shortName = "block", description = "The node implementing a source code block")
public final class StmtBlockNode extends StmtNode implements BlockNode.ElementExecutor<StmtNode> {
    @Node.Child
    private BlockNode<StmtNode> block;

    public StmtBlockNode(StmtNode[] bodyNodes) {
        this.block = bodyNodes.length > 0 ? BlockNode.create(bodyNodes, this) : null;
    }


    @Override
    public void executeVoid(VirtualFrame frame) {
        if (this.block != null) {
            this.block.executeVoid(frame, BlockNode.NO_ARGUMENT);
        }
    }

    public List<StmtNode> getStatements() {
        if (block == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(block.getElements()));
    }

    public void executeVoid(VirtualFrame frame, StmtNode node, int index, int argument) {
        node.executeVoid(frame);
    }

}