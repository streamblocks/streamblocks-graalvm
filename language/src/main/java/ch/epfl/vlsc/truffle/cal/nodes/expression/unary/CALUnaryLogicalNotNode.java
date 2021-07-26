package ch.epfl.vlsc.truffle.cal.nodes.expression.unary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.CALException;

@NodeInfo(shortName = "not")
public abstract class CALUnaryLogicalNotNode extends ExprUnaryNode {
    @Specialization
    protected boolean doBoolean(boolean value) {
        return !value;
    }

    @Fallback
    protected Object typeError(Object value) {
        throw CALException.typeError(this, value);
    }
}
