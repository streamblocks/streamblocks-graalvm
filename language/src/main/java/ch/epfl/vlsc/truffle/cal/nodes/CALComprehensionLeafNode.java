package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALArguments;
import ch.epfl.vlsc.truffle.cal.runtime.ListLibrary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;

@NodeChild(type = CALExpressionNode.class, value="list")
public abstract class CALComprehensionLeafNode extends CALComprehensionNode {
    @Child private CALWriteLocalVariableNode genVarWriter;
    @Child private CALStatementNode body;
    // TODO: Change to loopnode based loop
    // @Child private LoopNode loopNode;

    public CALComprehensionLeafNode(CALWriteLocalVariableNode genVarWriter, CALStatementNode body) {
        super();
        this.genVarWriter = genVarWriter;
        this.body = body;

        // TODO: Change to loopnode based loop
        // this.loopNode = Truffle.getRuntime().createLoopNode(new StmtWhileRepeatingNode(conditionNode, bodyNode));
    }

    @Specialization(guards = "lists.isList(list)", limit = "2")
    public void execute(VirtualFrame frame, Object list, @CachedLibrary("list") ListLibrary lists) {
        // TODO
        // loopNode.execute(frame);

        int size = lists.size(list);
        for (int i=0; i < size; i++) {
            Object next = lists.read(list, i);
            genVarWriter.executeWrite(frame, next);
            body.executeVoid(frame);
        }
    }
}
