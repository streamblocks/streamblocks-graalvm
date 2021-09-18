package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.util.ValueCastNodeCreator;
import com.oracle.truffle.api.frame.FrameSlot;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadCapturedVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteCapturedVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;

public class FrameSlotAndDepthRW implements FrameSlotAndDepth{
	final FrameSlot slot;
	final int declarationDepth;

	public FrameSlotAndDepthRW(FrameSlot frameSlot) {
		this(frameSlot, 0);
	}

	public FrameSlotAndDepthRW(FrameSlot frameSlot, int depth) {
		this.slot = frameSlot;
		this.declarationDepth = depth;
	}

	public FrameSlot getSlot() {
		return slot;
	}

	public int getDepth() {
		return declarationDepth;
	}

	public CALExpressionNode createReadNode(int currentDepth) {
		int rewindDepth = currentDepth - declarationDepth;
        assert rewindDepth >= 0;
		if (rewindDepth == 0) {
			return new CALReadLocalVariableNode(slot);
		} else {
			return CALReadCapturedVariableNodeGen.create(slot, rewindDepth);
		}
	}

	public CALExpressionNode createWriteNode(CALExpressionNode value, CALExpressionNode nameNode, boolean newVariable, int currentDepth) {
		int rewindDepth = currentDepth - declarationDepth;
        assert rewindDepth >= 0;
		if (rewindDepth == 0) {
			return CALWriteLocalVariableNodeGen.create(slot, nameNode, newVariable, ((ValueCastNodeCreator) slot.getInfo()).create(value));
		} else {
			return CALWriteCapturedVariableNodeGen.create(slot, nameNode, newVariable, ((ValueCastNodeCreator) slot.getInfo()).create(value), rewindDepth);
		}
	}

}
