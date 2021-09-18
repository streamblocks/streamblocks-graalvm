package ch.epfl.vlsc.truffle.cal.nodes.util;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.IntCastNode;

public class IntCastNodeCreator extends ValueCastNodeCreator {
    private final CALExpressionNode intsize;
    private final boolean signed;

    // TODO: Implement Singleton pattern
    public IntCastNodeCreator(CALExpressionNode intsize, boolean signed) {
        super();
        this.intsize = intsize;
        this.signed = signed;
    }

    public CALExpressionNode create(CALExpressionNode value){
        return new IntCastNode(intsize, value, signed);
    }
}
