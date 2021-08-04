package ch.epfl.vlsc.truffle.cal.nodes.local;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

@NodeChild("valueNode")
public abstract class CALWriteLocalVariableNode extends CALExpressionNode implements FrameSlotNode {

	@Child CALWriteFrameSlotNode writeFrameSlotNode;

	public CALWriteLocalVariableNode(FrameSlot slot, CALExpressionNode name, boolean newVariable) {
		this.writeFrameSlotNode = CALWriteFrameSlotNodeGen.create(slot, name, newVariable);
	}

	public FrameSlot getSlot() {
		return writeFrameSlotNode.getSlot();
	}

	public CALExpressionNode getNameNode() {
		return writeFrameSlotNode.getNameNode();
	}

	@Override
	public String toString() {
		return super.toString() + " " + getSlot().getIdentifier();
	}

	@Specialization
	public Object write(VirtualFrame frame, Object value) {
		writeFrameSlotNode.executeWrite(frame, value);
		return null;
	}
	
	public abstract void executeWrite(VirtualFrame topFrame, Object value);

}