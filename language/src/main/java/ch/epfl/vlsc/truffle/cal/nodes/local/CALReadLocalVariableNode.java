package ch.epfl.vlsc.truffle.cal.nodes.local;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.StandardTags.ReadVariableTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.interop.NodeObjectDescriptor;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

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
