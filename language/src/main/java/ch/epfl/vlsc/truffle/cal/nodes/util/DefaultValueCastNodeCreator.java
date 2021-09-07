package ch.epfl.vlsc.truffle.cal.nodes.util;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

public class DefaultValueCastNodeCreator extends ValueCastNodeCreator {
    @Override
    public CALExpressionNode create(CALExpressionNode value) {
        return value;
    }
}
