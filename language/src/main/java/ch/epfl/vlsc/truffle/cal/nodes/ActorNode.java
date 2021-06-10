package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;

public class ActorNode extends CALRootNode {

    @Child ActorInstantiateNode instantiateNode;
    private ActionNode[] actions;
    private final String name;
    private boolean isCloningAllowed;
    private final SourceSection sourceSection;

    public ActorNode(CALLanguage language, FrameDescriptor frameDescriptor, ActionNode[] actions, CALStatementNode head, SourceSection sourceSection, String name) {
        // FIXME null-hack
        super(language, frameDescriptor, null, sourceSection, name);
        this.actions = actions;
        this.sourceSection = sourceSection;
        this.name = name;
        this.instantiateNode = new ActorInstantiateNode(this, head);
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
