package ch.epfl.vlsc.truffle.cal.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node.Child;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.runtime.ListLibrary;

@NodeChild(type = CALExpressionNode.class, value="list")
public abstract class ForeacheNode extends CALStatementNode {
    @Child private CALStatementNode statement;
    @Child private CALWriteLocalVariableNode write;
    public ForeacheNode(CALStatementNode statement, CALWriteLocalVariableNode write) {
        this.statement = statement;
        this.write = write;
    }
    @Specialization(guards = "lists.isList(list)", limit = "2")
    public void execute(VirtualFrame frame, Object list, @CachedLibrary("list") ListLibrary lists) {
        int size = lists.size(list);
        for (int i=0; i < size; i++) {
            // TODO really not sure
            write.executeWrite(frame, lists.read(list, i));
            statement.executeVoid(frame);
        }
    }
}
