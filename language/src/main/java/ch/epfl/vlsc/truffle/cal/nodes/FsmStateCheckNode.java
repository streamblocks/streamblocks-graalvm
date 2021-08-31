package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FsmStateCheckNode extends CALExpressionNode{
    private final List<Map<Integer, Integer>> transitions;
    private final FrameSlot stateSlot;
    private final FrameSlot actorIndexSlot;
    public FsmStateCheckNode(List<Map<Integer, Integer>> transitionsArg, FrameSlot stateSlotArg, FrameSlot actorIndSlot) {
        this.stateSlot = stateSlotArg;
        this.actorIndexSlot = actorIndSlot;
        this.transitions = transitionsArg;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        int actorIndex = (int) FrameUtil.getLongSafe(frame, actorIndexSlot);
        int currState = (int) FrameUtil.getLongSafe(frame, stateSlot);
        return transitions.get(currState).containsKey(actorIndex);
    }
}
