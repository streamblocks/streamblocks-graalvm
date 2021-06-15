package ch.epfl.vlsc.truffle.cal.nodes.local;


import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALArguments;

@NodeChild("valueNode")
@NodeField(name = "depth", type = int.class)
public abstract class CALWriteCapturedVariableNode extends CALExpressionNode implements FrameSlotNode {

	@Child CALWriteFrameSlotNode writeFrameSlotNode;
	
	public CALWriteCapturedVariableNode(FrameSlot slot, CALExpressionNode name, boolean newVariable) {
		this.writeFrameSlotNode = CALWriteFrameSlotNodeGen.create(slot, name, newVariable);
	}

	
    protected abstract int getDepth();


	public FrameSlot getSlot() {
		return writeFrameSlotNode.getSlot();
	}
	
	@Specialization
	public Object write(VirtualFrame topFrame, Object value) {
		Frame parentFrame = CALArguments.getParentFrame(topFrame, getDepth());
		writeFrameSlotNode.executeWrite(parentFrame, value);
		return null;
	}
	
	public abstract void executeWrite(VirtualFrame topFrame, Object value);

}

