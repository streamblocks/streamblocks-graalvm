package ch.epfl.vlsc.truffle.cal.nodes.local;



import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALArguments;


public abstract class CALReadCapturedVariableNode extends CALExpressionNode {

	public abstract Object executeRead(VirtualFrame frame);

	final FrameSlot slot;
	final int depth;

	public CALReadCapturedVariableNode(FrameSlot slot, int depth) {
		this.slot = slot;
		this.depth = depth;
	}

	/*@Specialization(guards = "shouldCacheRead()")
	protected Object readWithCache(VirtualFrame frame,
			@Cached("createCachingReadNode(slot)") CachingReadFrameSlotNode readNode) {
		Frame parentFrame = OzArguments.getParentFrame(frame, depth);
		return readNode.executeRead(parentFrame);
	}*/

	//@Specialization(guards = "!shouldCacheRead()")
	//protected Object readWithoutCache(VirtualFrame frame,
	@Specialization
	protected Object read(VirtualFrame frame,
			@Cached("createReadNode(slot)") CALReadFrameSlotNode readNode) {
		Frame parentFrame = CALArguments.getParentFrame(frame, depth);
		return readNode.executeRead(parentFrame);
	}

	protected boolean shouldCacheRead() {
		return false; //CALLanguage.getOptions().get(Options.CACHE_READ);
	}

	protected CALReadFrameSlotNode createReadNode(FrameSlot slot) {
		return CALReadFrameSlotNodeGen.create(slot);
	}

	/*protected CachingReadFrameSlotNode createCachingReadNode(FrameSlot slot) {
		return CALCachingReadFrameSlotNodeGen.create(slot);
	}*/

}