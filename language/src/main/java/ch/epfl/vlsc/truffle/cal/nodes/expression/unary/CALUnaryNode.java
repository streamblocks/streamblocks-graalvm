package ch.epfl.vlsc.truffle.cal.nodes.expression.unary;

import ch.epfl.vlsc.truffle.cal.nodes.expression.CALExpressionNode;
import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild("valueNode")
public abstract class CALUnaryNode extends CALExpressionNode {
}
