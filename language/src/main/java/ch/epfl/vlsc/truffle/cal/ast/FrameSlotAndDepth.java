package ch.epfl.vlsc.truffle.cal.ast;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadCapturedVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadCapturedVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteCapturedVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteCapturedVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteFrameSlotNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.WriteNode;

public class FrameSlotAndDepth {
	final FrameSlot slot;
	final int declarationDepth;

	public FrameSlotAndDepth(FrameSlot frameSlot) {
		this(frameSlot, 0);
	}

	public FrameSlotAndDepth(FrameSlot frameSlot, int depth) {
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
			return CALWriteLocalVariableNodeGen.create(slot, nameNode, newVariable, value);
		} else {
			return CALWriteCapturedVariableNodeGen.create(slot, nameNode, newVariable, value, rewindDepth);
		}
	}

}
