package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;
import java.util.HashMap;

// This node returns boolean indicating whether the queried action can follow from the current FSM state
public class FsmStateCheckNode extends CALExpressionNode{
    ArrayList<HashMap<Integer, Integer>> transitions;
    FrameSlot stateSlot;
    FrameSlot actorIndexSlot;
    public FsmStateCheckNode(ArrayList<HashMap<Integer, Integer>> transitionsArg, FrameSlot stateSlotArg, FrameSlot actorIndSlot) {
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
