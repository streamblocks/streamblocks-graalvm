package ch.epfl.vlsc.truffle.cal.nodes.local;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

public class CALReadLocalVariableNode extends CALExpressionNode implements FrameSlotNode {

	@Child CALReadFrameSlotNode readFrameSlotNode;

	public CALReadLocalVariableNode(FrameSlot slot) {
		this.readFrameSlotNode = CALReadFrameSlotNodeGen.create(slot);
	}

	public FrameSlot getSlot() {
		return readFrameSlotNode.getSlot();
	}


	@Override
	public String toString() {
		return super.toString() + " " + getSlot().getIdentifier();
	}

	@Override
	public Object executeGeneric(VirtualFrame frame) {
		return readFrameSlotNode.executeRead(frame);
	}

}
