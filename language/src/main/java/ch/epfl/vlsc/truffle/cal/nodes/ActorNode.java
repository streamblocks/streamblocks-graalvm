package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import se.lth.cs.tycho.ir.QID;

public class ActorNode extends CALRootNode {
    @Child ActorInstantiateNode instantiateNode;
    @Children private final ActionNode[] actions;
    @Children private final ActionNode[] initializeractions;
    @Child private FsmStateCheckNode fsmNode;
    @Child private FsmStateTransitionNode fsmStateTransitionNode;
    private final QID name;
    private boolean isCloningAllowed;
    private final SourceSection sourceSection;
    private final FrameSlot actorIndexSlot;

    public ActorNode(CALLanguage language, FrameDescriptor frameDescriptor, ActionNode[] actions, ActionNode[] initactions, CALStatementNode head, SourceSection sourceSection, QID name, FsmStateCheckNode fsmNodeLoc, FsmStateTransitionNode fsmStateTransitionNodeArg, FrameSlot actorIndSlot) {
        // FIXME null-hack
        super(language, frameDescriptor, null, sourceSection, name.toString());
        this.actions = actions;
        this.initializeractions = initactions;
        this.sourceSection = sourceSection;
        this.name = name;
        this.instantiateNode = new ActorInstantiateNode(this, head);
        this.fsmNode = fsmNodeLoc;
        this.actorIndexSlot = actorIndSlot;
        this.fsmStateTransitionNode = fsmStateTransitionNodeArg;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        assert lookupContextReference(CALLanguage.class).get() != null;
        //getNextAction().executeGeneric(frame);
        // fix frame arguments
        return instantiateNode.executeGeneric(frame);
    }

    @Override
    public CALExpressionNode getBodyNode() {
        return instantiateNode;
    }

    public ActionNode[] getActions() {
        return actions;
    }

    public ActionNode[] getInitializerActions() { return initializeractions; }

    @Override
    public String getName() {
        return name.toString();
    }

    public void setCloningAllowed(boolean isCloningAllowed) {
        this.isCloningAllowed = isCloningAllowed;
    }

    @Override
    public boolean isCloningAllowed() {
        return isCloningAllowed;
    }

    @Override
    public String toString() {
        return "root " + name;
    }

    public FsmStateCheckNode getFsmStateCheckNode() {
        return this.fsmNode;
    }

    public FrameSlot getActorIndexSlot() {
        return this.actorIndexSlot;
    }

    public FsmStateTransitionNode getFsmStateTransitionNode() {
        return this.fsmStateTransitionNode;
    }
}
