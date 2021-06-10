package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import com.oracle.truffle.api.dsl.NodeChild;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

@NodeChild("leftNode")
@NodeChild("rightNode")
public abstract class CALBinaryNode extends CALExpressionNode {
}
