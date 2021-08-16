package ch.epfl.vlsc.truffle.cal.nodes.local.lists;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.ListLibrary;
import com.oracle.truffle.api.CompilerDirectives;

@NodeChild(value = "list")
@NodeChild(value = "index")
public abstract class ListReadNode extends CALExpressionNode {
    @Specialization(guards = "lists.isList(list)", limit = "2")
    Object doDefault(Object list, Long index, @CachedLibrary("list") ListLibrary lists) {
        return lists.read(list, index.intValue());
    }

    @Specialization(guards = "lists.isList(list)", limit = "2")
    Object doDefault(Object list, CALBigNumber index, @CachedLibrary("list") ListLibrary lists) {
        CompilerDirectives.transferToInterpreter();
        return lists.read(list, index.getValue().intValue());
    }
}
