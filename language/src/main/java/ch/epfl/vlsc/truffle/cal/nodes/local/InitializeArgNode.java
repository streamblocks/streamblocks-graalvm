package ch.epfl.vlsc.truffle.cal.nodes.local;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALArguments;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class InitializeArgNode extends CALStatementNode implements FrameSlotNode {

	@Child WriteFrameSlotNode writeFrameSlotNode;

	private final int index;

	public InitializeArgNode(FrameSlot slot, int index) {
		this.writeFrameSlotNode = WriteFrameSlotNodeGen.create(slot);
		this.index = index;
	}

	public FrameSlot getSlot() {
		return writeFrameSlotNode.getSlot();
	}

    @Override
	public void executeVoid(VirtualFrame frame) {
		Object value = CALArguments.getArgument(frame, index);
		writeFrameSlotNode.write(frame, value);
	}

}
