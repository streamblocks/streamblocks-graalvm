package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtWhileRepeatingNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.runtime.ListLibrary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LoopNode;

@NodeChild(type = CALExpressionNode.class, value="list")
public abstract class CALComprehensionNode extends CALStatementNode {
    @Child private CALWriteLocalVariableNode genVarWriter;
    @Child private CALExpressionNode filter;
    @Child private CALStatementNode body;
//    @Child private LoopNode loopNode;

    public CALComprehensionNode(CALWriteLocalVariableNode genVarWriter, CALExpressionNode filter, CALStatementNode body) {
        super();
        this.genVarWriter = genVarWriter;
        this.filter = filter;
        this.body = body;
//        this.loopNode = Truffle.getRuntime().createLoopNode(new StmtWhileRepeatingNode(conditionNode, bodyNode));
    }

    @Specialization(guards = "lists.isList(list)", limit = "2")
    public void execute(VirtualFrame frame, Object list, @CachedLibrary("list") ListLibrary lists) {
//        loopNode.execute(frame);
        int size = lists.size(list);
        for (int i=0; i < size; i++) {
            genVarWriter.executeWrite(frame, lists.read(list, i));
            Boolean toExecute = (Boolean) filter.executeGeneric(frame);
            if(toExecute)
                body.executeVoid(frame);
        }
    }
}
