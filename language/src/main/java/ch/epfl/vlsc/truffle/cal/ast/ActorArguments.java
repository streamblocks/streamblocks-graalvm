package ch.epfl.vlsc.truffle.cal.ast;

import java.util.LinkedList;
import java.util.List;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;

public class ActorArguments {
    public String actorName;
    public CALExpressionNode[] arguments;
    public List<CALExpressionNode> inputs;
    public List<CALExpressionNode> outputs;

    public ActorArguments(String actorName, CALExpressionNode[] arguments) {
        this.actorName = actorName;
        this.arguments = arguments;
        this.inputs = new LinkedList<>();
        this.outputs = new LinkedList<>();
    }
}
