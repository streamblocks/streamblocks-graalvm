package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.ActorLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALCreateFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoFanoutAddFifo;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoFanoutNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteFrameSlotNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton class that provides an implementation for a network sub-tree.
 */
public class NetworkVisitor extends CALParserBaseVisitor<NetworkNode> {

    private static NetworkVisitor instance;

    private NetworkVisitor() {}

    public static NetworkVisitor getInstance() {
        if (instance == null) {
            instance = new NetworkVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public NetworkNode visitNetworkDeclaration(CALParser.NetworkDeclarationContext ctx) {
        ScopeEnvironment.getInstance().pushScope();

        String networkName = ctx.name.getText();

        List<CALStatementNode> headStatementNodes = new ArrayList<>();
        int startIndex = 0;
        if (ctx.formalParameters() != null) {
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headStatementNodes.addAll(formalParameterNodes);
            startIndex += ctx.formalParameters().formalParameter().size();
        }

        if (ctx.inputPorts != null) {
            ctx.inputPorts.portDeclaration().forEach(port -> {
                headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(
                        port.name.getText(),
                        new CALFifoFanoutNode(),
                        DefaultValueCastNodeCreator.getInstance(),
                        ScopeEnvironment.getInstance().createSourceSection(port)));
            });
        }
        if (ctx.outputPorts != null) {
            ctx.outputPorts.portDeclaration().forEach(port -> {
                headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(
                        port.name.getText(),
                        new CALFifoFanoutNode(),
                        DefaultValueCastNodeCreator.getInstance(),
                        ScopeEnvironment.getInstance().createSourceSection(port)));
            });
        }
        if (ctx.localVariableDeclaration() != null) {
            for (CALParser.LocalVariableDeclarationContext localVariableCtx: ctx.localVariableDeclaration()) {
                headStatementNodes.add(VariableVisitor.getInstance().visitLocalVariableDeclaration(localVariableCtx));
            }
        }

        Map<String, EntityVisitor.EntityInstance> entities = new HashMap<>();
        if (ctx.entityDeclaration() != null) {
            for (CALParser.EntityDeclarationContext entityCtx: ctx.entityDeclaration()) {
                EntityVisitor.EntityInstance instance = EntityVisitor.getInstance().visitEntityDeclaration(entityCtx);
                entities.put(instance.name, instance);
            }
        }

        // Instantiate actors
        for (Map.Entry<String, EntityVisitor.EntityInstance> entry : entities.entrySet()) {
            EntityVisitor.EntityInstance instance = entry.getValue();

            CALExpressionNode actorLiteralNode = new ActorLiteralNode(ScopeEnvironment.getInstance().getEntityFullName(instance.actor));
            actorLiteralNode.setSourceSection(instance.sourceSection);
            actorLiteralNode.addExpressionTag();

            List<CALExpressionNode> argumentNodes = new ArrayList<>();
            // TODO Add support for named parameters
            argumentNodes.addAll(instance.parameters.stream().map(parameter -> parameter.valueNode).collect(Collectors.toList()));

            CALExpressionNode valueNode = new CALInvokeNode(actorLiteralNode, argumentNodes.toArray(new CALExpressionNode[0]));
            valueNode.setSourceSection(instance.sourceSection);
            valueNode.addExpressionTag();

            CALExpressionNode instanceNode = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                    instance.name,
                    valueNode,
                    DefaultValueCastNodeCreator.getInstance(),
                    instance.sourceSection);
            headStatementNodes.add(instanceNode);
        }

        Map<String, String> outputPortToFanoutMapping = new HashMap<>();

        // In CAL, the output from one entity can be input for multiple entities,
        // in which case the output token is copied to all the entities input ports.
        // This behaviour is simulated by creating a node which holds references to multiple FIFOs
        // and add then on receiving a token, pushes the token to all the FIFOs
        // TODO: The current Fanout implementation may not work appropriately when the root level network has input ports
        if (ctx.structureStatement() != null) {
            ctx.structureStatement().forEach(structure -> {
                headStatementNodes.add(StructureVisitor.getInstance().visitStructureStatement(structure));
            });
        }

        ScopeEnvironment.getInstance().getCurrentScope().increaseDepth();

        // Run actors
        List<CALExpressionNode> bodyStatementNodes = new LinkedList<>();
        for (String instanceName : entities.keySet()) {
            CALExpressionNode entityNode = ScopeEnvironment.getInstance().createReadNode(instanceName, entities.get(instanceName).sourceSection);

            CALInvokeNode entityInvokeNode = new CALInvokeNode(entityNode, new CALExpressionNode[0]);
            entityInvokeNode.setSourceSection(entities.get(instanceName).sourceSection);
            entityInvokeNode.addStatementTag();

            bodyStatementNodes.add(entityInvokeNode);
        }

        StmtBlockNode headNode = new StmtBlockNode(headStatementNodes.toArray(new CALStatementNode[0]));
        headNode.setUnavailableSourceSection();
        headNode.addStatementTag();

        CALExpressionNode bodyNode = new NetworkBodyNode(bodyStatementNodes.toArray(new CALExpressionNode[0]));
        headNode.setUnavailableSourceSection();
        headNode.addStatementTag();


        CALRootNode bodyRootNode = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                bodyNode,
                ScopeEnvironment.getInstance().getSource().createUnavailableSection(),
                networkName
        );
        // TODO Add RootTag / CallTag for bodyRootNode

        ScopeEnvironment.getInstance().getCurrentScope().decreaseDepth();

        NetworkNode networkNode = new NetworkNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                headNode,
                bodyRootNode,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                networkName
        );
        // TODO Add RootTag / CallTag for networkNode

        ScopeEnvironment.getInstance().popScope();

        return networkNode;
    }

}