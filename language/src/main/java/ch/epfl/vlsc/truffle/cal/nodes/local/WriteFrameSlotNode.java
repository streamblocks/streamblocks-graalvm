package ch.epfl.vlsc.truffle.cal.nodes.local;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

@ImportStatic(FrameSlotKind.class)
public abstract class WriteFrameSlotNode extends Node implements WriteNode, FrameSlotNode {

	private final FrameSlot slot;

	public WriteFrameSlotNode(FrameSlot slot) {
		this.slot = slot;
	}

	public FrameSlot getSlot() {
		return slot;
	}

	public void write(VirtualFrame frame, Object value) {
		executeWrite(frame, value);
	}

	public abstract void executeWrite(Frame frame, Object value);

	@Specialization(guards = "isKind(frame, Long)")
	protected void writeLong(Frame frame, long value) {
		frame.setLong(slot, value);
	}

	@Specialization(guards = "isKind(frame, Double)")
	protected void writeDouble(Frame frame, double value) {
		frame.setDouble(slot, value);
	}

	@Specialization(guards = "isKind(frame, Boolean)")
	protected void writeBoolean(Frame frame, boolean value) {
		frame.setBoolean(slot, value);
	}

	@Specialization(replaces = { "writeLong", "writeDouble", "writeBoolean" })
	protected void write(Frame frame, Object value) {
		if (frame.getFrameDescriptor().getFrameSlotKind(slot) != FrameSlotKind.Object) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			frame.getFrameDescriptor().setFrameSlotKind(slot, FrameSlotKind.Object);
		}
		frame.setObject(slot, value);
	}

	protected boolean isKind(Frame frame, FrameSlotKind kind) {
		FrameSlotKind actual = frame.getFrameDescriptor().getFrameSlotKind(slot);
		if (actual == kind) {
			return true;
		} else if (actual == FrameSlotKind.Illegal) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			frame.getFrameDescriptor().setFrameSlotKind(slot, kind);
			return true;
		} else {
			return false;
		}
	}

}
