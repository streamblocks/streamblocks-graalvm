package ch.epfl.vlsc.truffle.cal.nodes.expression;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;
import ch.epfl.vlsc.truffle.cal.runtime.CALProcedure;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class ProcNode extends CALExpressionNode {
    @Child private CALRootNode body;

    public ProcNode(CALRootNode body) {
        this.body = body;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return new CALProcedure(body, frame.materialize());
    }
}