package ch.epfl.vlsc.truffle.cal.nodes.expression.unary;

import com.oracle.truffle.api.dsl.NodeChild;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

@NodeChild("valueNode")
public abstract class ExprUnaryNode extends CALExpressionNode {
}
