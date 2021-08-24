package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtWhileRepeatingNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALArguments;
import ch.epfl.vlsc.truffle.cal.runtime.ListLibrary;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LoopNode;

import java.util.Collections;

@NodeChild(type = CALExpressionNode.class, value="list")
public abstract class CALComprehensionContinueNode extends CALComprehensionNode {
    @Child private CALWriteLocalVariableNode genVarWriter;
    @Child private CALExpressionNode filter;
    @Child private CALRootNode body;
    // TODO: Change to loopnode based loop
    // @Child private LoopNode loopNode;

    public CALComprehensionContinueNode(CALWriteLocalVariableNode genVarWriter, CALExpressionNode filter, CALRootNode body) {
        super();
        this.genVarWriter = genVarWriter;
        this.filter = filter;
        this.body = body;
        // TODO: Change to loopnode based loop
        // this.loopNode = Truffle.getRuntime().createLoopNode(new StmtWhileRepeatingNode(conditionNode, bodyNode));
    }

    @Specialization(guards = "lists.isList(list)", limit = "2")
    public void execute(VirtualFrame frame, Object list, @CachedLibrary("list") ListLibrary lists) {
        // This method can be optimized by the use of loopnode with profiling information
        // loopNode.execute(frame);

        int size = lists.size(list);
        for (int i=0; i < size; i++) {
            Object next = lists.read(list, i);
            genVarWriter.executeWrite(frame, next);
            Boolean toExecute = (Boolean) filter.executeGeneric(frame);
            if(toExecute) {
                CompilerDirectives.transferToInterpreter();
                Truffle.getRuntime().createIndirectCallNode().call(
                        Truffle.getRuntime().createCallTarget(body),
                        CALArguments.pack(frame.materialize(), new Object[0])
                );
            }
        }
    }
}