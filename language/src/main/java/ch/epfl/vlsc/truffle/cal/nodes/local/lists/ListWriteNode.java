package ch.epfl.vlsc.truffle.cal.nodes.local.lists;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.ListLibrary;

@NodeChild(value = "list", type = CALExpressionNode.class)
@NodeChild(value = "index", type = CALExpressionNode.class)
@NodeChild(value = "value", type = CALExpressionNode.class)
public abstract class ListWriteNode extends CALStatementNode {
    @Specialization(guards = "lists.isList(list)", limit = "2")
    public void write(Object list, CALBigNumber index, Object value, @CachedLibrary("list") ListLibrary lists) {
        lists.write(list, index.getValue().intValue(), value);
    }
}