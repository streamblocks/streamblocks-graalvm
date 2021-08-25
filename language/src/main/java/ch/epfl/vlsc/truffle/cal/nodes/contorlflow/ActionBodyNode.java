package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;

@NodeInfo(shortName = "body")
public final class ActionBodyNode extends CALExpressionNode {

    /**
     * The body of the function.
     */
    @Node.Child
    private CALStatementNode bodyNode;

    private final BranchProfile exceptionTaken = BranchProfile.create();
    private final BranchProfile nullTaken = BranchProfile.create();

    public ActionBodyNode(CALStatementNode bodyNode) {
        this.bodyNode = bodyNode;
        addRootTag();
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return (tag == StandardTags.RootBodyTag.class) || super.hasTag(tag);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // TODO we probably want to get our value from last expression
        try {
            /* Execute the function body. */
            bodyNode.executeVoid(frame);

        } catch (ReturnException ex) {
            /*
             * In the interpreter, record profiling information that the function has an explicit
             * return.
             */
            exceptionTaken.enter();
            /* The exception transports the actual return value. */
            return ex.getResult();
        }

        /*
         * In the interpreter, record profiling information that the function ends without an
         * explicit return.
         */
        nullTaken.enter();
        /* Return the default null value. */
        return CALNull.SINGLETON;
    }
}

