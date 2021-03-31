package ch.epfl.vlsc.truffle.cal.ast;

import com.oracle.truffle.api.frame.FrameSlot;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

public interface FrameSlotAndDepth {
    public FrameSlot getSlot();

    public int getDepth();

    public CALExpressionNode createReadNode(int currentDepth);

    public CALExpressionNode createWriteNode(CALExpressionNode value, CALExpressionNode nameNode, boolean newVariable,
            int currentDepth);

}
