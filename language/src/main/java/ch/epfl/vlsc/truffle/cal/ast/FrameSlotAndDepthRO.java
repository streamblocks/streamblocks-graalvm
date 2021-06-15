package ch.epfl.vlsc.truffle.cal.ast;

import com.oracle.truffle.api.frame.FrameSlot;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

class FrameSlotAndDepthRO implements FrameSlotAndDepth {
    private FrameSlotAndDepth frameSlotAndDepth;

    FrameSlotAndDepthRO(FrameSlotAndDepth frameSlotAndDepth) {
        if (frameSlotAndDepth instanceof FrameSlotAndDepthRO)
            this.frameSlotAndDepth = ((FrameSlotAndDepthRO)frameSlotAndDepth).frameSlotAndDepth;
        else
            this.frameSlotAndDepth = frameSlotAndDepth;
    }

    @Override
    public int getDepth() {
        return frameSlotAndDepth.getDepth();
    }
    @Override
    public FrameSlot getSlot() {
        return frameSlotAndDepth.getSlot();
    }
    @Override
    public CALExpressionNode createReadNode(int currentDepth) {
        return frameSlotAndDepth.createReadNode(currentDepth);
    }
    @Override
    public CALExpressionNode createWriteNode(CALExpressionNode value, CALExpressionNode nameNode, boolean newVariable,
            int currentDepth) {
        // TODO imporve
        throw new Error("trying to write a non-writable value");
    }
}
