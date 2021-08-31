package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.source.SourceSection;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.entity.cal.ScheduleFSM;
import se.lth.cs.tycho.ir.entity.cal.Transition;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.transformation.regexp.RegExpConverter;

import java.util.*;

public class ActorTransformer extends ScopedTransformer<ActorNode> {

    CalActor actor;
    QID name;

    public ActorTransformer(CalActor actor, QID name, TransformContext context) {
    	super(context);
    	// We want a clean frame
    	// TODO support global variables
    	context.clearLexicalScopeAndFrame();
        this.actor = actor;
        this.name = name;
    }

    public ActorNode transform() {
        List<CALStatementNode> headStatements = new ArrayList<CALStatementNode>(actor.getVarDecls().size()
                + actor.getValueParameters().size() + actor.getOutputPorts().size() + actor.getInputPorts().size());
        int i = 0;

        // TODO we are making assumptions about the number of arguments
        // and that EVERY argument and port is effectively passed

        // WARNING keep as the first declaration as it needs to match the arguments
        // position
        // Prepend arguments so they are specialized the same way as in the body
        for (VarDecl varDecl : actor.getValueParameters()) {
            headStatements.add(transformArgument(varDecl, i));
            i++;
        }

        for (PortDecl in : actor.getInputPorts()) {
            // Input ports are passed as arguments
            headStatements.add(transformPortDecl(in, i));
            i++;
        }
        for (PortDecl out : actor.getOutputPorts()) {
            // Input ports are passed as arguments
            headStatements.add(transformPortDecl(out, i));
            i++;
        }
        for (LocalVarDecl varDecl : actor.getVarDecls()) {
            headStatements.add(transformVarDecl(varDecl));
            i++;
        }

        ActionNode[] actions = this.actor.getActions().map(x -> transformAction(x)).toArray(new ActionNode[0]);
        ActionNode[] initializeractions = this.actor.getInitializers().map(x -> transformAction(x)).toArray(new ActionNode[0]);

        // Order the actions by priorities
        if(!this.actor.getPriorities().isEmpty()) {
            topologicalSortByPriorities(actions, this.actor.getPriorities());
            topologicalSortByPriorities(initializeractions, this.actor.getPriorities());
        }

        // Variables for Handling of Regex/FSM schedules
        FsmStateCheckNode fsmStateCheckNode = null;
        FsmStateTransitionNode fsmStateTransitionNode = null;
        FrameSlot actorIndSlot = null;
        FrameSlot currStateSlot = null;
        ScheduleFSM actorScheduleFsm = null;

        // If the schedule is given as a Regex, convert it to FSM
        if(actor.getScheduleRegExp() != null){
            RegExpConverter regExpConverter = new RegExpConverter(actor.getScheduleRegExp());
            actorScheduleFsm = regExpConverter.convert();
        }else if(actor.getScheduleFSM() != null){
            // If it is given as an FSM, then store it for processing
            actorScheduleFsm = actor.getScheduleFSM();
        }

        // If a schedule is present in the actor, convert it
        if(actorScheduleFsm != null){
            // Create Frameslot for storing actor index during processing checks
            // See the implementation of doIndirect in CALActorInstance for more details
            String actionIndexSlotName = String.valueOf(actor.hashCode() + "#fsmActorIndex");
            headStatements.add(createAssignment(actionIndexSlotName, new LongLiteralNode(0)));
            ++i;

            // Create Frameslot for storing the number of current state
            // See the implementation of doIndirect in CALActorInstance for more details
            String currStateSlotName = String.valueOf(actor.hashCode()) + "#fsmCurrState";
            headStatements.add(createAssignment(currStateSlotName, new LongLiteralNode(0)));
            ++i;

            actorIndSlot = context.getFrameDescriptor().findFrameSlot(actionIndexSlotName);
            currStateSlot = context.getFrameDescriptor().findFrameSlot(currStateSlotName);

            // Convert the states from strings to numbers and transitions correspondingly
            List<Map<Integer, Integer>> transitions = transformFsm(actorScheduleFsm, actions, headStatements, actorIndSlot);

            fsmStateCheckNode = new FsmStateCheckNode(transitions, currStateSlot, actorIndSlot);
            fsmStateTransitionNode = new FsmStateTransitionNode(transitions, currStateSlot, actorIndSlot);
        }

        // FIXME we can probably use a StmtBlockNode
        CALStatementNode head = new StmtBlockNode(headStatements.toArray(new CALStatementNode[headStatements.size()]));

        SourceSection actorSrc = getSourceSection(actor);
        return new ActorNode(context.getLanguage(), context.getFrameDescriptor(), actions, initializeractions, head, actorSrc, name.toString(), fsmStateCheckNode, fsmStateTransitionNode, actorIndSlot, currStateSlot);
    }

    private List<Map<Integer, Integer>> transformFsm(ScheduleFSM actorScheduleFsm, ActionNode[] actions, List<CALStatementNode> headStatements, FrameSlot actorIndSlot) {
        int i = 0;
        HashMap<String, Integer> stateToIndex = new HashMap<String, Integer>();
        stateToIndex.put(actorScheduleFsm.getInitialState(), i++);
        for(Transition t: actorScheduleFsm.getTransitions()){
            if(!stateToIndex.containsKey(t.getSourceState())) stateToIndex.put(t.getSourceState(), i++);
            if(!stateToIndex.containsKey(t.getDestinationState())) stateToIndex.put(t.getDestinationState(), i++);
        }
        List<Map<Integer, Integer>> transitions = new ArrayList<>(stateToIndex.size());
        for(int j = 0; j < stateToIndex.size(); ++j) transitions.add(new HashMap<Integer, Integer>());
        for(Transition t: actorScheduleFsm.getTransitions()){
            Map<Integer, Integer> m = transitions.get(stateToIndex.get(t.getSourceState()));
            int finalStateIndex = stateToIndex.get(t.getDestinationState());
            for(QID tempQID: t.getActionTags()){
                QualifiedID tQID = new QualifiedID(tempQID.parts());
                for(int j = 0; j < actions.length; ++j){
                    if(tQID.isPrefixOf(actions[j].getQID())){
                        m.put(j, finalStateIndex);
                    }
                }
            }
        }
        return transitions;
    }

    /**
     * Sort the array @param actions in an order that respects the partial order induced by @param priorities
     */
    private void topologicalSortByPriorities(ActionNode[] actions, ImmutableList<ImmutableList<QID>> priorities){
        // TODO : A Lot of optimizations can be performed in this method
        // Reducing space usage by considering only actions[i:] instead of the full array
        // Change recursive method to iterative
        // Modify the algorithm to permute the array

        int i = 0;

        // Move all the unnamed actions to the front of the list to give them maximum priority
        while(i < actions.length && actions[i].getName().equals(ActionTransformer.UNNAMED_ACTION)) ++i;
        for(int j = i+1; j < actions.length; ++j){
            if(actions[j].getName().equals(ActionTransformer.UNNAMED_ACTION)){
                ActionNode temp = actions[i];
                actions[i++] = actions[j];
                actions[j] = temp;
            }
        }

        // Now topologically sort the remaining elements in the array actions[i:] to get an
        // ordering of actions based on priority

        // Create a mapping from All QID prefixes to action names
        Map<List<String>, List<Integer>> tagToActions = new HashMap<List<String>, List<Integer>>();
        for(int j = i; j < actions.length; ++j){
            QualifiedID q = actions[j].getQID();
            while(q.getNameCount() > 0){
                if(tagToActions.containsKey(q.parts())) tagToActions.get(q.parts()).add(j);
                else tagToActions.put(q.parts(), new LinkedList(Arrays.asList(j)));
                q = q.getButLast();
            }
        }

        // Create adjacency list of neighbours based on priority order
        List<Integer>[] neighbours = new List[actions.length]; // TODO i space slots can be saved here
        Arrays.setAll(neighbours, x -> new LinkedList<>());
        for(ImmutableList<QID> priority: priorities){
            for(int k = 0; k + 1 < priority.size(); ++k){
                for(int sourceAction: tagToActions.getOrDefault(priority.get(k+1).parts(), Collections.emptyList())){
                    for(int targetAction: tagToActions.getOrDefault(priority.get(k).parts(), Collections.emptyList())){
                        neighbours[sourceAction].add(targetAction);
                    }
                }
            }
        }

        // Perform the topological sorting by DFS over the graph
        ArrayList<Integer> topologicallySorted = new ArrayList<Integer>(actions.length);
        // TODO this loop can be easily removed by making changes as per the next todo
        for(int j = 0; j < i; ++j) topologicallySorted.add(j);
        // TODO i slots space can be saved at this point, since we only need to process actions.length - i nodes
        NodeStatus visited[] = new NodeStatus[actions.length];
        for (int j = i; j < actions.length; j++)
            visited[j] = NodeStatus.Unvisited;

        for (int j = i; j < actions.length; j++)
            if (visited[j] == NodeStatus.Unvisited)
                recursiveTopologicalSort(j, visited, neighbours, topologicallySorted);

        // Permute the original array as per the ordering obtained by topological sorting
        // The array topologicallySorted is a permutation on the indices of the array actions
        // We use the permutation cycle algorithm to swap elements in succession to obtain the permuted array
        for (int j = 0; j < actions.length; j++) {
            int next = j;
            while (topologicallySorted.get(next) >= 0) {
                ActionNode t = actions[j];
                actions[j] = actions[topologicallySorted.get(next)];
                actions[topologicallySorted.get(next)] = t;
                int temp = topologicallySorted.get(next);
                topologicallySorted.set(next, temp - actions.length);
                next = temp;
            }
        }
    }

    private enum NodeStatus {Unvisited, Inprogress, Visited}

    private void recursiveTopologicalSort(int v, NodeStatus[] visited, List<Integer>[] neighbours, List<Integer> topologicallySorted) {
        visited[v] = NodeStatus.Inprogress;
        Iterator<Integer> it = neighbours[v].iterator();
        while (it.hasNext())
        {
            int i = it.next();
            if (visited[i] == NodeStatus.Unvisited)
                recursiveTopologicalSort(i, visited, neighbours, topologicallySorted);
            else if(visited[i] == NodeStatus.Inprogress)
                throw new RuntimeException("Action priority order defined is not an irreflexive partial order. The priority inequalities are valid iff the induced relation on the actions is an irreflexive partial order, i.e. it is antisymmetric and transitive.");
        }
        visited[v] = NodeStatus.Visited;
        topologicallySorted.add(v);
    }

    public ActionNode transformAction(Action action) {
        return (new ActionTransformer(action, context.deeper(false))).transform();
    }

}
