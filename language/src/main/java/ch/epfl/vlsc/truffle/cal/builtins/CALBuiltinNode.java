package ch.epfl.vlsc.truffle.cal.builtins;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

@NodeChild(value = "arguments", type = CALExpressionNode[].class)
@GenerateNodeFactory
public abstract class CALBuiltinNode extends CALExpressionNode {

    @Override
    public final Object executeGeneric(VirtualFrame frame) {
        try {
            return execute(frame);
        } catch (UnsupportedSpecializationException e) {
            throw CALException.typeError(e.getNode(), e.getSuppliedValues());
        }
    }

    @Override
    public final boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return super.executeBoolean(frame);
    }

    @Override
    public final long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return super.executeLong(frame);
    }

    @Override
    public final void executeVoid(VirtualFrame frame) {
        super.executeVoid(frame);
    }

    protected abstract Object execute(VirtualFrame frame);
}