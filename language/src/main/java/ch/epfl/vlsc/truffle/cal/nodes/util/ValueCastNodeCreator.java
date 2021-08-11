package ch.epfl.vlsc.truffle.cal.nodes.util;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

public abstract class ValueCastNodeCreator {
    public abstract CALExpressionNode create(CALExpressionNode value);
}
