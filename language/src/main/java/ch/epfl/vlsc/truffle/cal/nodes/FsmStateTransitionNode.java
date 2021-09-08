package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.runtime.CALNull;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;

// This node makes the transition from current state to next state depending on the executed action
public class FsmStateTransitionNode extends CALExpressionNode{
    private final FrameSlot stateSlot;
    private final FrameSlot actorIndexSlot;
    private final List<Map<Integer, Integer>> transitions;
    public FsmStateTransitionNode(List<Map<Integer, Integer>> transitionsArg, FrameSlot stateSlotArg, FrameSlot actorIndSlot) {
        this.stateSlot = stateSlotArg;
        this.actorIndexSlot = actorIndSlot;
        this.transitions = transitionsArg;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        int currState = (int) FrameUtil.getLongSafe(frame, stateSlot);
        int actorIndex = (int) FrameUtil.getLongSafe(frame, actorIndexSlot);
        CompilerDirectives.transferToInterpreter();
        frame.setLong(stateSlot, transitions.get(currState).get(actorIndex));
        return CALNull.SINGLETON;
    }
}
