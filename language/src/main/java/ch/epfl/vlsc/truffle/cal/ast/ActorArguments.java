package ch.epfl.vlsc.truffle.cal.ast;

import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import org.apache.commons.lang3.tuple.Pair;

public class ActorArguments {
    public String actorName;
    public CALExpressionNode[] arguments;
    public List<Pair<String, CALExpressionNode>> inputs;
    public List<Pair<String, CALExpressionNode>> outputs;
	public SourceSection sourceSection;

    public ActorArguments(String actorName, CALExpressionNode[] arguments, SourceSection sourceSection) {
        this.actorName = actorName;
        this.arguments = arguments;
        this.sourceSection = sourceSection;
        this.inputs = new LinkedList<>();
        this.outputs = new LinkedList<>();
    }
}
