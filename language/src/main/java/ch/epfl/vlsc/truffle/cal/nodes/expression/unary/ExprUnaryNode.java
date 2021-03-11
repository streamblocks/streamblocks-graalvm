package ch.epfl.vlsc.truffle.cal.nodes.expression.unary;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild("valueNode")
public abstract class ExprUnaryNode extends CALExpressionNode {
}
