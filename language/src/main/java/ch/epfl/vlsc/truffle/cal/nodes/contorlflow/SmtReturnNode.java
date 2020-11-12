package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import ch.epfl.vlsc.truffle.cal.nodes.expression.ExprNode;
import ch.epfl.vlsc.truffle.cal.nodes.StmtNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "return", description = "The node implementing a return statement")
public final class SmtReturnNode extends StmtNode {

    @Node.Child
    private ExprNode valueNode;

    public SmtReturnNode(ExprNode valueNode) {
        this.valueNode = valueNode;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object result;
        if (valueNode != null) {
            result = valueNode.executeGeneric(frame);
        } else {
            /*
             * Return statement that was not followed by an expression, so return the SL null value.
             */
            result = CALNull.SINGLETON;
        }
        throw new ReturnException(result);
    }
}