package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        frame.setLong(stateSlot, transitions.get(currState).get(actorIndex));
        return null;
    }
}
