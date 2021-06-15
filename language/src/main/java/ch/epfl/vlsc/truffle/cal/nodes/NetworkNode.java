package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;

public class NetworkNode extends CALRootNode {

    @Child NetworkInstantiateNode instantiateNode;
    private final String name;
    private boolean isCloningAllowed;
    private final SourceSection sourceSection;

    public NetworkNode(CALLanguage language, FrameDescriptor frameDescriptor, CALStatementNode head, CALRootNode body, SourceSection sourceSection, String name) {
        // FIXME null-hack
        super(language, frameDescriptor, null, sourceSection, name);
        this.sourceSection = sourceSection;
        this.name = name;
        this.instantiateNode = new NetworkInstantiateNode(this, head, body);
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    // FIXME return
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
