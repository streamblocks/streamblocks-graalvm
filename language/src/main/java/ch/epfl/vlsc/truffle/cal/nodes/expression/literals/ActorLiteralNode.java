package ch.epfl.vlsc.truffle.cal.nodes.expression.literals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALActor;

public final class ActorLiteralNode extends CALExpressionNode {

    /**
     * The name of the actor.
     */
    private final String actorName;


    @CompilerDirectives.CompilationFinal
    private CALActor cachedActor;

    public ActorLiteralNode(String actorName) {
        this.actorName = actorName;
    }

    @Override
    public CALActor executeGeneric(VirtualFrame frame) {
        if (cachedActor == null) {
            /* We are about to change a @CompilationFinal field. */
            CompilerDirectives.transferToInterpreterAndInvalidate();
            /* First execution of the node: lookup the actor in the actor registry. */
            cachedActor = lookupContextReference(CALLanguage.class).get().getActorRegistry().lookup(actorName, true);
        }
        return cachedActor;
    }

}
