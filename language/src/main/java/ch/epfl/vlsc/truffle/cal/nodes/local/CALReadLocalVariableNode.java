package ch.epfl.vlsc.truffle.cal.nodes.local;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.instrumentation.StandardTags.ReadVariableTag;
import ch.epfl.vlsc.truffle.cal.nodes.interop.NodeObjectDescriptor;

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

	@Override
	public boolean hasTag(Class<? extends Tag> tag) {
		return tag == ReadVariableTag.class || super.hasTag(tag);
	}

	// TODO: Check if this is needed
	@Override
	public Object getNodeObject() {
		return NodeObjectDescriptor.readVariable(getSlot().getIdentifier().toString());
	}
}
