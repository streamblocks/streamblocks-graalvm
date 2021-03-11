package ch.epfl.vlsc.truffle.cal.nodes.expression.binary;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild("leftNode")
@NodeChild("rightNode")
public abstract class CALBinaryNode extends CALExpressionNode {
}
