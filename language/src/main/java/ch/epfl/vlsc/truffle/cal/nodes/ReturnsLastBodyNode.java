package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import org.graalvm.polyglot.Value;

import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;
public class ReturnsLastBodyNode extends  CALExpressionNode {
    @Child private CALStatementNode statementsNode;
    @Child private CALExpressionNode returnNode;
    public ReturnsLastBodyNode(CALExpressionNode[] expressions) {
        CALStatementNode[] stmts = new CALStatementNode[expressions.length - 1];
        for (int i = 0; i < expressions.length - 1; i++)
            stmts[i] = expressions[i];
        this.statementsNode = new StmtBlockNode(stmts);
        this.returnNode = expressions[expressions.length-1];
    }
    public ReturnsLastBodyNode(CALStatementNode body) {
        this.statementsNode = body;
        this.returnNode = null;
    }
    public ReturnsLastBodyNode(StmtBlockNode body, CALExpressionNode returnNode) {
        this.statementsNode = body;
        this.returnNode = returnNode;
    }

    public Object executeGeneric(VirtualFrame frame) {
        statementsNode.executeVoid(frame);
        if (returnNode != null)
        return returnNode.executeGeneric(frame);
        else 
            return CALNull.SINGLETON;
    }
}

