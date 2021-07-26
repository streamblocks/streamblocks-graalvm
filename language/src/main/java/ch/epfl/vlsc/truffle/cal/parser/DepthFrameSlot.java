package ch.epfl.vlsc.truffle.cal.parser;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadCapturedVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteCapturedVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;
import com.oracle.truffle.api.frame.FrameSlot;

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

    public CALExpressionNode createReadNode(int currentScopeDepth) {
        int outerScopeDepth = currentScopeDepth - depth;
        assert outerScopeDepth >= 0;
        if (outerScopeDepth == 0) {
            // Read a local variable
            return new CALReadLocalVariableNode(slot);
        } else {
            // Read a variable from <outerScopeDepth>th outer scope
            return CALReadCapturedVariableNodeGen.create(slot, outerScopeDepth);
        }
    }

    public CALExpressionNode createWriteNode(CALExpressionNode value, CALExpressionNode nameNode, boolean newVariable, int currentDepth) throws Error {
        if (kind == SlotKind.RO) {
            // Only outer scope variables that are read-only
            throw new Error("Trying to write to a non-writable value.");
        }

        int outerScopeDepth = currentDepth - depth;
        assert outerScopeDepth >= 0;
        if (outerScopeDepth == 0) {
            // Write to a local variable
            return CALWriteLocalVariableNodeGen.create(slot, nameNode, newVariable, value);
        } else {
            // Write to a variable from <outerScopeDepth>th outer scope
            return CALWriteCapturedVariableNodeGen.create(slot, nameNode, newVariable, value, outerScopeDepth);
        }
    }

}
