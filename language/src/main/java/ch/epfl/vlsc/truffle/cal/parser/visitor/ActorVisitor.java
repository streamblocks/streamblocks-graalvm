package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.utils.ActorNodeUtils;
import ch.epfl.vlsc.truffle.cal.parser.utils.PartialOrderViolationException;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Singleton class that provides an implementation for an actor sub-tree.
 */
public class ActorVisitor extends CALParserBaseVisitor<Object> {

    private static ActorVisitor instance;

    private ActorVisitor() {}

    public static ActorVisitor getInstance() {
        if (instance == null) {
            instance = new ActorVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public ActorNode visitActorDeclaration(CALParser.ActorDeclarationContext ctx) {
        ScopeEnvironment.getInstance().pushScope();

        String actorName = ctx.name.getText();

        List<CALStatementNode> headStatementNodes = new ArrayList<>();
        int startIndex = 0;
        if (ctx.formalParameters() != null) {
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headStatementNodes.addAll(formalParameterNodes);
            startIndex += ctx.formalParameters().formalParameter().size();
        }
        if (ctx.inputPorts != null) {
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<InitializeArgNode> inputPortNodes = CollectionVisitor.getInstance().visitPortDeclarations(ctx.inputPorts);
            headStatementNodes.addAll(inputPortNodes);
            startIndex += ctx.inputPorts.portDeclaration().size();
        }
        if (ctx.outputPorts != null) {
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<InitializeArgNode> outputPortNodes = CollectionVisitor.getInstance().visitPortDeclarations(ctx.outputPorts);
            headStatementNodes.addAll(outputPortNodes);
            //startIndex += ctx.outputPorts.portDeclaration().size();
        }
        if (ctx.time != null) {
            // TODO Add support for actor time
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(CALLanguage.showWarnings)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Actor time is not yet supported");
            }
        }
        if (ctx.localVariableDeclaration() != null) {
            for (CALParser.LocalVariableDeclarationContext localVariableCtx: ctx.localVariableDeclaration()) {
                headStatementNodes.add(VariableVisitor.getInstance().visitLocalVariableDeclaration(localVariableCtx));
            }
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
            headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(actionIndexSlotName, new LongLiteralNode(0), null));

            String currStateSlotName = "$" + actorName + "#fsmCurrState";
            headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(currStateSlotName, new LongLiteralNode(0), null));

            actorIndSlot = ScopeEnvironment.getInstance().findFrameSlot(actionIndexSlotName);
            currStateSlot = ScopeEnvironment.getInstance().findFrameSlot(currStateSlotName);

            FSMDescription fsm = visitActionSchedule(ctx.actionSchedule().get(0));
            ArrayList<HashMap<Integer, Integer>> transitions = TransitionsToMap(fsm.getTransitions(), actionNodes, fsm.getInitialState());

            fsmStateCheckNode = new FsmStateCheckNode(transitions, currStateSlot, actorIndSlot);
            fsmStateTransitionNode = new FsmStateTransitionNode(transitions, currStateSlot, actorIndSlot);
        } else if (ctx.actionSchedule().size() != 0) {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Action Schedule Size found to be more than 1");
        }

        if (ctx.processDescription().size() > 0) {
            // TODO Add support for process descriptions
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(CALLanguage.showWarnings)) {
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
                currStateSlot
        );
        // TODO Add RootTag / CallTag for actorNode

        ScopeEnvironment.getInstance().popScope();

        return actorNode;
    }

    private ArrayList<HashMap<Integer, Integer>> TransitionsToMap(List<Transition> allTransitions, List<ActionNode> actions, String initialState) {
        int i = 0;
        HashMap<String, Integer> stateToIndex = new HashMap<String, Integer>();
        stateToIndex.put(initialState, i++);
        for(Transition t: allTransitions){
            if(!stateToIndex.containsKey(t.getSource())) stateToIndex.put(t.getSource(), i++);
            if(!stateToIndex.containsKey(t.getDestination())) stateToIndex.put(t.getDestination(), i++);
        }
        ArrayList<HashMap<Integer, Integer>> transitions = new ArrayList<HashMap<Integer, Integer>>(stateToIndex.size());
        for(int j = 0; j < stateToIndex.size(); ++j) transitions.add(new HashMap<Integer, Integer>());
        for(Transition t: allTransitions){
            HashMap<Integer, Integer> m = transitions.get(stateToIndex.get(t.getSource()));
            int finalStateIndex = stateToIndex.get(t.getDestination());
            QualifiedID tQID = t.getActionTag();
            for(int j = 0; j < actions.size(); ++j){
                if(tQID.isPrefixOf(actions.get(j).getQID())){
                    m.put(j, finalStateIndex);
                }
            }
        }
        return transitions;
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
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Schedule Regex not supported currently");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitRegExp(CALParser.RegExpContext ctx) {
        // TODO First resolve #visitScheduleRegExp
        // Note: Unreachable for now
        return super.visitRegExp(ctx);
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

    private class Transition {
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