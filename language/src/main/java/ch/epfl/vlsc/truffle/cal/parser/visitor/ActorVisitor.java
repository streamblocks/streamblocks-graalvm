package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLogicalAndNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALCreateFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoFanoutNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.utils.ActorNodeUtils;
import ch.epfl.vlsc.truffle.cal.parser.utils.PartialOrderViolationException;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import com.oracle.truffle.api.frame.FrameSlot;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Singleton class that provides an implementation for an actor sub-tree.
 */
public class ActorVisitor extends CALParserBaseVisitor<Object> {

    private static ActorVisitor instance;

    private ActorVisitor() {}

    private static Map<String, Pair<List<String>, List<String>>> actorToPortInfo = new HashMap<>();
    private static String currentlyProcessingActor;

    public static ActorVisitor getInstance() {
        if (instance == null) {
            instance = new ActorVisitor();
        }

        return instance;
    }

    public static List<String> getActorInputPorts(String name) {
        return actorToPortInfo.get(name).getLeft();
    }

    public static List<String> getActorOutputPorts(String name) {
        return actorToPortInfo.get(name).getRight();
    }

    /**
     * {@inheritDoc}
     */
    @Override public ActorNode visitActorDeclaration(CALParser.ActorDeclarationContext ctx) {
        // TODO We make the following assumptions regarding the way arguments are passed to the actors:
        // 1. The arguments contain the list of actor parameters, input ports, output ports and variable declarations sequentially
        // 2. The list of input ports passed in the arguments is arranged lexicographically with respect to port name
        // 3. Each input port(even if redundant) specified in the actor definition is passed in the argument
        // TODO: Come up with a better method for passing of arguments, and handling cases where redundant actor ports are not passed

        ScopeEnvironment.getInstance().pushScope();

        String actorName = ctx.name.getText();

        List<String> inputPortNames = new LinkedList<>();
        List<String> outputPortNames = new LinkedList<>();
        actorToPortInfo.put(ScopeEnvironment.getInstance().getEntityFullName(actorName), new ImmutablePair<>(inputPortNames, outputPortNames));
        this.currentlyProcessingActor = ScopeEnvironment.getInstance().getEntityFullName(actorName);

        List<CALStatementNode> headStatementNodes = new ArrayList<>();
        int startIndex = 0;
        if (ctx.formalParameters() != null) {
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headStatementNodes.addAll(formalParameterNodes);
            startIndex += ctx.formalParameters().formalParameter().size();
        }

        if (ctx.inputPorts != null) {
            ctx.inputPorts.portDeclaration().forEach(inpPort -> {
                inputPortNames.add(inpPort.name.getText());
                headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(
                        inpPort.name.getText(),
                        new CALCreateFIFONode(),
                        DefaultValueCastNodeCreator.getInstance(),
                        ScopeEnvironment.getInstance().createSourceSection(inpPort)));
            });
        }

        if (ctx.outputPorts != null) {
            ctx.outputPorts.portDeclaration().forEach(port -> {
                outputPortNames.add(port.name.getText());
                headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(
                        port.name.getText(),
                        new CALFifoFanoutNode(),
                        DefaultValueCastNodeCreator.getInstance(),
                        ScopeEnvironment.getInstance().createSourceSection(port)));
            });
        }

        if (ctx.time != null) {
            // TODO Add support for actor time
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Actor time is not yet supported");
            }
        }
        if (ctx.localVariableDeclaration() != null) {
            for (CALParser.LocalVariableDeclarationContext localVariableCtx: ctx.localVariableDeclaration()) {
                headStatementNodes.add(VariableVisitor.getInstance().visitLocalVariableDeclaration(localVariableCtx));
            }
        }

        CALActorInvariantNode actorInvariantNode = null;
        if (ctx.invariantDeclaration().size() > 1) {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx.invariantDeclaration(1), "There can only be a maximum of 1 invariant statement specified in the actor");
        } else if (ctx.invariantDeclaration().size() == 1 && ctx.invariantDeclaration(0).expression().size() > 0) {
            CALExpressionNode invariantCondition;
            List<CALParser.ExpressionContext> exprs = ctx.invariantDeclaration(0).expression();
            invariantCondition = ExpressionVisitor.getInstance().visit(exprs.remove(exprs.size() - 1));
            for (int i = exprs.size() - 1; i >= 0; --i) {
                CALExpressionNode condition = ExpressionVisitor.getInstance().visit(exprs.remove(i));
                invariantCondition = new CALBinaryLogicalAndNode(condition, invariantCondition);
                invariantCondition.setUnavailableSourceSection();
                invariantCondition.addExpressionTag();
            }
            actorInvariantNode = new CALActorInvariantNode(invariantCondition);
        }

        StmtBlockNode headNode = new StmtBlockNode(headStatementNodes.toArray(new CALStatementNode[0]));
        headNode.setUnavailableSourceSection();
        headNode.addStatementTag();

        List<ActionNode> actionNodes = new ArrayList<>();
        if (ctx.actionDefinition() != null) {
            for (CALParser.ActionDefinitionContext actionCtx: ctx.actionDefinition()) {
                actionNodes.add(ActionVisitor.getInstance().visitActionDefinition(actionCtx));
            }
        }

        List<ActionNode> initactions = new ArrayList<>();
        if (ctx.initializationActionDefinition().size() > 0) {
            for (CALParser.InitializationActionDefinitionContext actionCtx: ctx.initializationActionDefinition()) {
                initactions.add(InitializeActionVisitor.getInstance().visitInitializationActionDefinition(actionCtx));
            }
        }

        if (ctx.priorityOrder().size() > 0) {
            // Order the actions by priorities
            List<List<QualifiedID>> priorities = ctx.priorityOrder().stream().map(po -> visitPriorityOrder(po)).flatMap(x -> x).collect(Collectors.toList());
            try {
                actionNodes = ActorNodeUtils.topologicalSortByPriorities(actionNodes, priorities);
                initactions = ActorNodeUtils.topologicalSortByPriorities(initactions, priorities);
            } catch (PartialOrderViolationException e) {
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, e.getMessage());
            }
        }

        FsmStateCheckNode fsmStateCheckNode = null;
        FsmStateTransitionNode fsmStateTransitionNode = null;
        FrameSlot actorIndSlot = null;
        FrameSlot currStateSlot = null;
        if (ctx.actionSchedule().size() == 1) {
            String actionIndexSlotName = "$" + actorName + "#fsmActorIndex";
            headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(
                    actionIndexSlotName, new LongLiteralNode(0), DefaultValueCastNodeCreator.getInstance(), null));

            String currStateSlotName = "$" + actorName + "#fsmCurrState";
            headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(currStateSlotName, new LongLiteralNode(0), DefaultValueCastNodeCreator.getInstance(), null));

            actorIndSlot = ScopeEnvironment.getInstance().findFrameSlot(actionIndexSlotName);
            currStateSlot = ScopeEnvironment.getInstance().findFrameSlot(currStateSlotName);

            FSMDescription fsm = visitActionSchedule(ctx.actionSchedule().get(0));
            List<Map<Integer, Integer>> transitions = ActorNodeUtils.TransitionsToMap(fsm.getTransitions(), actionNodes, fsm.getInitialState());

            fsmStateCheckNode = new FsmStateCheckNode(transitions, currStateSlot, actorIndSlot);
            fsmStateTransitionNode = new FsmStateTransitionNode(transitions, currStateSlot, actorIndSlot);
        } else if (ctx.actionSchedule().size() != 0) {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Action Schedule Size found to be more than 1");
        }

        if (ctx.processDescription().size() > 0) {
            // TODO Add support for process descriptions
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Process description is not yet supported");
            }
        }

        ActorNode actorNode = new ActorNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                actionNodes.toArray(new ActionNode[0]),
                initactions.toArray(new ActionNode[0]),
                headNode,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                actorName,
                fsmStateCheckNode,
                fsmStateTransitionNode,
                actorIndSlot,
                currStateSlot,
                actorInvariantNode
        );
        // TODO Add RootTag / CallTag for actorNode

        ScopeEnvironment.getInstance().popScope();

        return actorNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitProcessDescription(CALParser.ProcessDescriptionContext ctx) {
        // TODO Create Process Description node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Process description is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public FSMDescription visitActionSchedule(CALParser.ActionScheduleContext ctx) {
        if (ctx.scheduleFSM() != null) {
            return visitScheduleFSM(ctx.scheduleFSM());
        } else if (ctx.scheduleRegExp() != null) {
            // If the schedule is given as a Regex, convert it to FSM
            return visitScheduleRegExp(ctx.scheduleRegExp());
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unexpected Schedule Type found");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public FSMDescription visitScheduleFSM(CALParser.ScheduleFSMContext ctx) {
        return new FSMDescription(ctx.name.getText(), ctx.stateTransition().stream().flatMap(transition -> visitStateTransition(transition)).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override public Stream<Transition> visitStateTransition(CALParser.StateTransitionContext ctx) {
        String sourceName = ctx.source.getText();
        return ctx.transitionTargetList().transitionTarget().stream().flatMap(tr -> visitTransitionTarget(tr)).map(tr -> new Transition(sourceName, tr));
    }

    /**
     * {@inheritDoc}
     */
    @Override public Stream<TransitionTarget> visitTransitionTarget(CALParser.TransitionTargetContext ctx) {
        String targetName = ctx.target.getText();
        return ctx.actionTags().actionTag().stream().map(actionTag -> new TransitionTarget(targetName, visitActionTag(actionTag)));
    }

    /**
     * {@inheritDoc}
     */
    @Override public FSMDescription visitScheduleRegExp(CALParser.ScheduleRegExpContext ctx) {
        Pair<BidiMap<QualifiedID, Character>, String> p = (new RegexVisitor()).visitRegExp(ctx.regExp());
        RegExp regex = new RegExp(p.getRight());
        Automaton automaton = regex.toAutomaton(true);

        BidiMap<State, String> state2name = new DualHashBidiMap<>();
        int stateCounter = 0;
        String initStateName = "s" + (stateCounter++);
        state2name.put(automaton.getInitialState(), initStateName);
        ArrayList<Transition> transitions = new ArrayList<>();
        transitions.ensureCapacity(automaton.getNumberOfTransitions());

        for(State s: automaton.getStates()) {
            if (!state2name.containsKey(s)) {
                String stateName = "s" + (stateCounter++);
                state2name.put(s, stateName);
            }
        }

        for(State s: automaton.getStates()) {
            String stateName = state2name.get(s);
            for(dk.brics.automaton.Transition t: s.getTransitions()) {
                for(char c = t.getMin(); c <= t.getMax(); ++c) {
                    transitions.add(new Transition(stateName, new TransitionTarget(state2name.get(t.getDest()), p.getLeft().getKey(c))));
                }
            }
        }

        transitions.trimToSize();
        return new FSMDescription(initStateName, transitions);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Stream<List<QualifiedID>> visitPriorityOrder(CALParser.PriorityOrderContext ctx) {
        return ctx.priorityInequality().stream().map(po -> visitPriorityInequality(po));
    }

    /**
     * {@inheritDoc}
     */
    @Override public List<QualifiedID> visitPriorityInequality(CALParser.PriorityInequalityContext ctx) {
        return ctx.actionTag().stream().map(tag -> visitActionTag(tag)).collect(Collectors.toList());
    }

    @Override
    public QualifiedID visitActionTag(CALParser.ActionTagContext ctx) {
        return CollectionVisitor.qualifiedIdCreator(ActionVisitor.getInstance().visitActionTag(ctx));
    }

    public static String getCurrentlyProcessingActor() {
        return currentlyProcessingActor;
    }

    public class Transition {
        public Transition(String source, TransitionTarget tr) {
            this.source = source;
            this.tr = tr;
        }
        TransitionTarget tr;
        public String getSource() {
            return source;
        }

        private String source;

        public String getDestination() {
            return tr.getDestination();
        }

        public QualifiedID getActionTag() {
            return tr.getActionTag();
        }
    }

    private class TransitionTarget {
        public TransitionTarget(String target, QualifiedID actionTag) {
            this.target = target;
            this.actionTag = actionTag;
        }

        public String getDestination() {
            return target;
        }

        private String target;

        public QualifiedID getActionTag() {
            return actionTag;
        }

        private QualifiedID actionTag;
    }

    private class FSMDescription {
        public String getInitialState() {
            return initialState;
        }

        private String initialState;

        public List<Transition> getTransitions() {
            return transitions;
        }

        private List<Transition> transitions;
        public FSMDescription(String initialState, List<Transition> transitions) {
            this.initialState = initialState;
            this.transitions = transitions;
        }
    }
}