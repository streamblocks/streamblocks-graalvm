package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALLambda;

public final class LambdaNode extends CALExpressionNode {
    @Child
    private CALRootNode body;
    // FIXME add parameters and return type
    public LambdaNode(CALRootNode body) {
        this.body= body;
    }
    
    // FIXME return
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        MaterializedFrame lambdaFrame = frame.materialize();
        return new CALLambda(body, lambdaFrame);

    }
}
