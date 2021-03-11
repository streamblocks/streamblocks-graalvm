package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;

public class ActorNode extends CALRootNode {

    @Children ActionNode[] actions;
    private final String name;
    private boolean isCloningAllowed;
    private final SourceSection sourceSection;

    public ActorNode(CALLanguage language, FrameDescriptor frameDescriptor, ActionNode[] actions, SourceSection sourceSection, String name) {
        // FIXME null-hack
        super(language, frameDescriptor, null, sourceSection, name);
        this.actions = actions;
        this.sourceSection = sourceSection;
        this.name = name;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    // FIXME return
    @Override
    public Object execute(VirtualFrame frame) {
        assert lookupContextReference(CALLanguage.class).get() != null;
        getNextAction().executeGeneric(frame);
        return null;
    }

    @Override
    public CALExpressionNode getBodyNode() {
        return getCurrentAction();
    }

    private ActionNode getCurrentAction() {
        assert 0 == actions.length;
        return actions[0];
    }

    private ActionNode getNextAction() {
        assert 0 == actions.length;
        return actions[0];
    }

    public ActionNode[] getActions() {
        return actions;
    }

    @Override
    public String getName() {
        return name;
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

}
