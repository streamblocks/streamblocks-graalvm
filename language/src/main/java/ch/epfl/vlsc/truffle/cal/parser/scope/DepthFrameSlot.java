package ch.epfl.vlsc.truffle.cal.parser.scope;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.*;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.source.SourceSection;

public class DepthFrameSlot implements Cloneable {

    public enum SlotKind {
        RW,
        RO
    }

    private final FrameSlot slot;

    private final int depth;

    private final SlotKind kind;

    public DepthFrameSlot(FrameSlot slot, int depth) {
        this.slot = slot;
        this.depth = depth;
        this.kind = SlotKind.RW;
    }

    public DepthFrameSlot(DepthFrameSlot parentSlot) {
        this.slot = parentSlot.slot;
        this.depth = parentSlot.depth;
        this.kind = SlotKind.RO;
    }


    public FrameSlot getSlot() {
        return slot;
    }

    public int getDepth() {
        return depth;
    }

    public SlotKind getKind() {
        return kind;
    }

    public CALExpressionNode createReadNode(int currentScopeDepth, SourceSection sourceSection) {
        int outerScopeDepth = currentScopeDepth - depth;
        assert outerScopeDepth >= 0;
        if (outerScopeDepth == 0) {
            // Read a local variable
            CALReadLocalVariableNode variableNode = new CALReadLocalVariableNode(slot);
            variableNode.setSourceSection(sourceSection);
            // TODO Change to ReadVariableTag
            variableNode.addExpressionTag();

            return variableNode;
        } else {
            // Read a variable from <outerScopeDepth>th outer scope
            CALReadCapturedVariableNode variableNode = CALReadCapturedVariableNodeGen.create(slot, outerScopeDepth);
            variableNode.setSourceSection(sourceSection);
            // TODO Change to ReadVariableTag
            variableNode.addExpressionTag();

            return variableNode;
        }
    }

    public CALExpressionNode createWriteNode(CALExpressionNode nameNode, CALExpressionNode valueNode, boolean isNewVariable, int currentDepth, SourceSection sourceSection) {
        if (kind == SlotKind.RO) {
            // Only outer scope variables that are read-only
            throw new Error("Writing to a read-only variable is not allowed");
        }

        int outerScopeDepth = currentDepth - depth;
        assert outerScopeDepth >= 0;
        if (outerScopeDepth == 0) {
            // Write to a local variable
            CALWriteLocalVariableNode variableNode = CALWriteLocalVariableNodeGen.create(slot, nameNode, isNewVariable, valueNode);
            variableNode.setSourceSection(sourceSection);
            // TODO Change to WriteVariableTag
            variableNode.addStatementTag();

            return variableNode;
        } else {
            // Write to a variable from <outerScopeDepth>th outer scope
            CALWriteCapturedVariableNode variableNode = CALWriteCapturedVariableNodeGen.create(slot, nameNode, isNewVariable, valueNode, outerScopeDepth);
            variableNode.setSourceSection(sourceSection);
            // TODO Change to WriteVariableTag
            variableNode.addStatementTag();

            return variableNode;
        }
    }

}
