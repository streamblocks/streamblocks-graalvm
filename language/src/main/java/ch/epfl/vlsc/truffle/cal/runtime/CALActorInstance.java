package ch.epfl.vlsc.truffle.cal.runtime;

import ch.epfl.vlsc.truffle.cal.nodes.CALActorInvariantNode;
import ch.epfl.vlsc.truffle.cal.nodes.FsmStateCheckNode;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.api.utilities.TriState;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import com.oracle.truffle.dsl.processor.java.compiler.Compiler;


// An actor instance
@ExportLibrary(InteropLibrary.class)
public class CALActorInstance extends CALEntityInstance {
    public @CompilationFinal ActorNode entityDecl;

    public static final int INLINE_CACHE_SIZE = 2;
    // FIXME
    private static String name = "Test";
    private final CyclicAssumption callTargetStable;

    public CALActorInstance(ActorNode actorDecl, MaterializedFrame frame) {
        this.entityDecl = actorDecl;
        this.frameDecl = frame;
        this.callTargetStable = new CyclicAssumption(name);
    }

    @Override
    public void addInputPort(String portName, FifoConsumer fifo) {
        CompilerDirectives.transferToInterpreter();
        frameDecl.setObject(frameDecl.getFrameDescriptor().findFrameSlot(portName), fifo);
    }

    @Override
    public void addOutputPort(String portName, FifoConsumer fifo) {
        try {
            CompilerDirectives.transferToInterpreter();
            CALFifoFanout fanout = (CALFifoFanout) frameDecl.getObject(frameDecl.getFrameDescriptor().findFrameSlot(portName));
            fanout.addFifo(fifo);
            fifo.setFanout(fanout);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException("Unexpected frameslot type: " + e);
        }
    }

    // TODO how to get called?
    // TODO add child annotation
    protected RootCallTarget getCallTarget() {
        ActionNode[] actions = entityDecl.getActions();
        // TODO do FSM-thingy here and invalidate callTargetStable when
        // the action changes
        // For new we assume actions.length == 1, so we never invalidate
        assert actions.length == 1;
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(actions[0]);
        return callTarget;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }
    //
    /**
     * This method is, e.g., called when using a function literal in a string concatenation. So
     * changing it has an effect on CAL programs.
     */
    @Override
    public String toString() {
        return name;
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return CALLanguage.class;
    }

    /**
     * {@link CALActorInstance} instances are always visible as executable to other languages.
     */
    @SuppressWarnings("static-method")
    @ExportMessage
    @TruffleBoundary
    SourceSection getSourceLocation() {
        return getCallTarget().getRootNode().getSourceSection();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasSourceLocation() {
        return true;
    }

    /**
     * {@link CALActorInstance} instances are always visible as executable to other languages.
     */
    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return CALType.ACTOR;
    }

    @ExportMessage
    @SuppressWarnings("unused")
    static final class IsIdenticalOrUndefined {
        @Specialization
        static TriState doCALFunction(CALActorInstance receiver, CALActorInstance other) {
            /*
             * SLFunctions are potentially identical to other SLFunctions.
             */
            return receiver == other ? TriState.TRUE : TriState.FALSE;
        }

        @Fallback
        static TriState doOther(CALActorInstance receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    @TruffleBoundary
    static int identityHashCode(CALActorInstance receiver) {
        return System.identityHashCode(receiver);
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return name;
    }

    /**
     * We allow languages to execute this function. We implement the interop execute message that
     * forwards to a function dispatch.
     *
     * Since invocations are potentially expensive (result in an indirect call, which is expensive
     * by itself but also limits function inlining which can hinder other optimisations) if the node
     * turns megamorphic (i.e. cache limit is exceeded) we annotate it with {@ReportPolymorphism}.
     * This ensures that the runtime is notified when this node turns polymorphic. This, in turn,
     * may, under certain conditions, cause the runtime to attempt to make node monomorphic again by
     * duplicating the entire AST containing that node and specialising it for a particular call
     * site.
     */
    @ReportPolymorphism
    @ExportMessage
    abstract static class Execute {

        /**
         * Inline cached specialization of the dispatch.
         *
         * <p>
         * Since SL is a quite simple language, the benefit of the inline cache seems small: after
         * checking that the actual function to be executed is the same as the cachedFuntion, we can
         * safely execute the cached call target. You can reasonably argue that caching the call
         * target is overkill, since we could just retrieve it via {@code function.getCallTarget()}.
         * However, caching the call target and using a {@link DirectCallNode} allows Truffle to
         * perform method inlining. In addition, in a more complex language the lookup of the call
         * target is usually much more complicated than in SL.
         * </p>
         *
         * <p>
         * {@code limit = "INLINE_CACHE_SIZE"} Specifies the limit number of inline cache
         * specialization instantiations.
         * </p>
         * <p>
         * {@code guards = "function.getCallTarget() == cachedTarget"} The inline cache check. Note
         * that cachedTarget is a final field so that the compiler can optimize the check.
         * </p>
         * <p>
         * {@code assumptions = "callTargetStable"} Support for function redefinition: When a
         * function is redefined, the call target maintained by the SLFunction object is changed. To
         * avoid a check for that, we use an Assumption that is invalidated by the SLFunction when
         * the change is performed. Since checking an assumption is a no-op in compiled code, the
         * assumption check performed by the DSL does not add any overhead during optimized
         * execution.
         * </p>
         *
         * @see Cached
         * @see Specialization
         *
         * @param function the dynamically provided function
         * @param arguments the arguments to the function
         * @param callTargetStable The assumption object assuming the function was not redefined.
         * @param cachedTarget The call target we aim to invoke
         * @param callNode the {@link DirectCallNode} specifically created for the
         *            {@link CallTarget} in cachedFunction.
         */
        /*
        @Specialization(limit = "INLINE_CACHE_SIZE", //
                guards = "function.getCallTarget() == cachedTarget", //
                assumptions = "callTargetStable")
        @SuppressWarnings("unused")
        protected static Object doDirect(CALActorInstance function, Object[] arguments,
                                         @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                         @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                         @Cached("create(cachedTarget)") DirectCallNode callNode) {

            *//* Inline cache hit, we are safe to execute the cached call target. */
            //Object returnValue = callNode.call(/*CALArguments.pack(*/function.frameDecl/*, arguments)*/);
            /*return returnValue;
        }*/

        /**
         * Slow-path code for a call, used when the polymorphic inline cache exceeded its maximum
         * size specified in <code>INLINE_CACHE_SIZE</code>. Such calls are not optimized any
         * further, e.g., no method inlining is performed.
         */
        //@Specialization(replaces = "doDirect")
        @Specialization
        protected static Object doIndirect(CALActorInstance function, Object[] arguments,
                                           @Cached IndirectCallNode callNode) {
            // Check Actor Invariant
            CALActorInvariantNode inv = function.entityDecl.getActorInvariant();
            if (inv != null) inv.executeVoid(function.frameDecl);

            // Return true if any action was executed, otherwise false

            // Loop over all actions till one of the actions is executed.
            // The order of actions in the array is based on decreasing order of priorities
            for(int i = 0; i < function.entityDecl.getActions().length; ++i){
                FsmStateCheckNode fsmTarget = function.entityDecl.getFsmStateCheckNode();
                ActionNode action = function.entityDecl.getActions()[i];
                Boolean actionFsmFireable = true;
                if(function.entityDecl.getFsmStateCheckNode() != null) {
                    // FSM present
                    function.frameDecl.setLong(function.entityDecl.getActorIndexSlot(), i);
                    try {
                        actionFsmFireable = !action.isQidTagged() || ((Boolean) fsmTarget.executeBoolean(function.frameDecl));
                    } catch (UnexpectedResultException e) {
                        e.printStackTrace();
                    }
                }

                // Action is fireable if there is no action with higher priority that is fireable and if the current action tag follows from the current FSM state
                if (!actionFsmFireable) continue;
                CompilerDirectives.transferToInterpreter();
                CallTarget target = Truffle.getRuntime().createCallTarget(action);
                // Attempt to execute the action: Guard expressions and token bindings are checked by the action and returns true if it fires, otherwise false
                Boolean executed = (Boolean) callNode.call(target, /*CALArguments.pack(*/function.frameDecl/*, arguments)*/);
                if (executed) {
                    if(function.entityDecl.getFsmStateCheckNode() != null) function.entityDecl.getFsmStateTransitionNode().executeVoid(function.frameDecl);
                    return true;
                }
            }
            // No action was fired, hence the actor did not execute. Return false.
            return false;
        }
    }
}
