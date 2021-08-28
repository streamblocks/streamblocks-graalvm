package ch.epfl.vlsc.truffle.cal.parser.utils;

import ch.epfl.vlsc.truffle.cal.ast.ActionTransformer;
import ch.epfl.vlsc.truffle.cal.ast.ActorTransformer;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.*;

public class ActorNodeUtils {
    /**
     * Sort the array @param actions in an order that respects the partial order induced by @param priorities
     */
    public static List<ActionNode> topologicalSortByPriorities(List<ActionNode> actionsList, List<List<QualifiedID>> priorities) throws PartialOrderViolationException {
        ActionNode[] actions = actionsList.toArray(new ActionNode[actionsList.size()]);
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
        for(List<QualifiedID> priority: priorities){
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
        List<ActionNode> list = new ArrayList<>();
        for (ActionNode action : actions) {
            list.add(action);
        }
        return list;
    }

    private enum NodeStatus {Unvisited, Inprogress, Visited}

    private static void recursiveTopologicalSort(int v, NodeStatus[] visited, List<Integer>[] neighbours, List<Integer> topologicallySorted) throws PartialOrderViolationException {
        visited[v] = NodeStatus.Inprogress;
        Iterator<Integer> it = neighbours[v].iterator();
        while (it.hasNext())
        {
            int i = it.next();
            if (visited[i] == NodeStatus.Unvisited)
                recursiveTopologicalSort(i, visited, neighbours, topologicallySorted);
            else if(visited[i] == NodeStatus.Inprogress)
                throw new PartialOrderViolationException("Order defined by priorities is not a Partial Order");
        }
        visited[v] = NodeStatus.Visited;
        topologicallySorted.add(v);
    }

//    public ArrayList<HashMap<Integer, Integer>> transformFsm(CalActor actor, ActionNode[] actions) {
//        int i = 0;
//        HashMap<String, Integer> stateToIndex = new HashMap<String, Integer>();
//        stateToIndex.put(actor.getScheduleFSM().getInitialState(), i++);
//        for(Transition t: actor.getScheduleFSM().getTransitions()){
//            if(!stateToIndex.containsKey(t.getSourceState())) stateToIndex.put(t.getSourceState(), i++);
//            if(!stateToIndex.containsKey(t.getDestinationState())) stateToIndex.put(t.getDestinationState(), i++);
//        }
//        ArrayList<HashMap<Integer, Integer>> transitions = new ArrayList<HashMap<Integer, Integer>>(stateToIndex.size());
//        for(int j = 0; j < stateToIndex.size(); ++j) transitions.add(new HashMap<Integer, Integer>());
//        for(Transition t: actor.getScheduleFSM().getTransitions()){
//            HashMap<Integer, Integer> m = transitions.get(stateToIndex.get(t.getSourceState()));
//            int finalStateIndex = stateToIndex.get(t.getDestinationState());
//            for(QID tempQID: t.getActionTags()){
//                QualifiedID tQID = new QualifiedID(tempQID.parts());
//                for(int j = 0; j < actions.length; ++j){
//                    if(tQID.isPrefixOf(actions[j].getQID())){
//                        m.put(j, finalStateIndex);
//                    }
//                }
//            }
//        }
//        return transitions;
//    }
}
