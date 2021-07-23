package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import se.lth.cs.tycho.ir.QID;

public final class ActionNode extends CALRootNode {
    @Child
    private CALExpressionNode body;
    @Child
    private CALExpressionNode firingCondition;
    private final QID name;
    private boolean isCloningAllowed;
    private final SourceSection sourceSection;

    public ActionNode(CALLanguage language, FrameDescriptor frameDescriptor, CALExpressionNode body,
                      CALExpressionNode firingCondition, SourceSection sourceSection, QID name) {
        super(language, frameDescriptor, body, sourceSection, name.toString());
        this.body = body;
        this.firingCondition = firingCondition;
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
        try {
            boolean fireable = (Boolean) firingCondition.executeBoolean(frame);
            if (fireable) {
                body.executeGeneric(frame);
                return true;
            } else {
                return false;
            }
        } catch (UnexpectedResultException e) {
            throw new Error("internal error");
        }

    }

    @Override
    public CALExpressionNode getBodyNode() {
        return body;
    }

    @Override
    public String getName() {
        return name.toString();
    }

    public QID getQID() {
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
