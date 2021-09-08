package ch.epfl.vlsc.truffle.cal.nodes.util;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

public class DefaultValueCastNodeCreator extends ValueCastNodeCreator {
    private static DefaultValueCastNodeCreator instance = null;
    private DefaultValueCastNodeCreator(){}
    public static DefaultValueCastNodeCreator getInstance(){
        if (instance == null) {
            instance = new DefaultValueCastNodeCreator();
        }
        return instance;
    }

    @Override
    public CALExpressionNode create(CALExpressionNode value) {
        return value;
    }
}
