package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;

public final class ActionNode extends CALRootNode {
    @Child
    private CALExpressionNode body;
    private final String name;
    private boolean isCloningAllowed;
    private final SourceSection sourceSection;
    public ActionNode(CALLanguage language, FrameDescriptor frameDescriptor, CALExpressionNode body, SourceSection sourceSection, String name) {
        super(language, frameDescriptor, body, sourceSection, name);
        this.body= body;
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
        //getNextAction().executeGeneric(frame);
        // fix frame arguments
        return body.executeGeneric(frame);

    }

    @Override
    public CALExpressionNode getBodyNode() {
        return body;
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
