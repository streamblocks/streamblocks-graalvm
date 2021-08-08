package ch.epfl.vlsc.truffle.cal.nodes.expression.unary;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import ch.epfl.vlsc.truffle.cal.runtime.ListLibrary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "#")
public abstract class CALUnaryListSizeNode extends ExprUnaryNode {
    @Specialization(guards = "lists.isList(list)", limit = "2")
    Object doDefault(Object list, @CachedLibrary("list") ListLibrary lists) {
        return lists.size(list);
    }

    @Fallback
    protected Object typeError(Object value) {
        throw CALException.typeError(this, value);
    }
}
