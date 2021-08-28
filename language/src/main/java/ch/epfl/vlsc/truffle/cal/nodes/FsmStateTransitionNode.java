package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;
import java.util.HashMap;

public class FsmStateTransitionNode extends CALExpressionNode{
    FrameSlot stateSlot;
    FrameSlot actorIndexSlot;
    ArrayList<HashMap<Integer, Integer>> transitions;
    public FsmStateTransitionNode(ArrayList<HashMap<Integer, Integer>> transitionsArg, FrameSlot stateSlotArg, FrameSlot actorIndSlot) {
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
