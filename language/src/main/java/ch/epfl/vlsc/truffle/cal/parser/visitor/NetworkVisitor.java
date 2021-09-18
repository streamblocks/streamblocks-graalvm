package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.ast.TransformException;
import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.ActorLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALCreateFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoFanoutAddFifo;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoFanoutNode;
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
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<InitializeArgNode> inputPortNodes = CollectionVisitor.getInstance().visitPortDeclarations(ctx.inputPorts);
            headStatementNodes.addAll(inputPortNodes);
            startIndex += ctx.inputPorts.portDeclaration().size();
        }
        if (ctx.outputPorts != null) {
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<InitializeArgNode> outputPortNodes = CollectionVisitor.getInstance().visitPortDeclarations(ctx.outputPorts);
            headStatementNodes.addAll(outputPortNodes);
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

        Map<String, String> outputPortToFanoutMapping = new HashMap<>();

        // In CAL, the output from one entity can be input for multiple entities,
        // in which case the output token is copied to all the entities input ports.
        // This behaviour is simulated by creating a node which holds references to multiple FIFOs
        // and add then on receiving a token, pushes the token to all the FIFOs
        // TODO: The current Fanout implementation may not work appropriately when the root level network has input ports
        if (ctx.structureStatement() != null) {
            for (CALParser.StructureStatementContext structureCtx: ctx.structureStatement()) {
                // TODO Change after implementing complex statements (if/foreach)
                StructureVisitor.StructureConnection connection = (StructureVisitor.StructureConnection) StructureVisitor.getInstance().visitStructureStatement(structureCtx);

                CALExpressionNode FIFONode;
                if (connection.source.entity != null && connection.destination.entity != null) {
                    // Connect entities = create new FIFO between them
                    String newFIFOName = ScopeEnvironment.generateFIFOName();
                    CALCreateFIFONode newFIFOValueNode = new CALCreateFIFONode();
                    newFIFOValueNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(structureCtx));
                    newFIFOValueNode.addExpressionTag();

                    CALExpressionNode newFIFONode = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                            newFIFOName,
                            newFIFOValueNode,
                            DefaultValueCastNodeCreator.getInstance(),
                            ScopeEnvironment.getInstance().createSourceSection(structureCtx));
                    headStatementNodes.add(newFIFONode);

                    String fanoutNodeName;
                    String portName = connection.source.entity + "." + connection.source.port;
                    if(!outputPortToFanoutMapping.containsKey(portName)){
                        // We are seeing the output port for the first time
                        fanoutNodeName = ScopeEnvironment.generateFifoFanoutName();
                        outputPortToFanoutMapping.put(portName, fanoutNodeName);
                        headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(
                                fanoutNodeName,
                                new CALFifoFanoutNode(),
                                DefaultValueCastNodeCreator.getInstance(),
                                ScopeEnvironment.getInstance().getSource().createUnavailableSection()));
                        entities.get(connection.source.entity).outputs.add(Pair.of(connection.source.port, ScopeEnvironment.getInstance().createReadNode(fanoutNodeName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
                    } else
                        fanoutNodeName = outputPortToFanoutMapping.get(portName);
                    // Add the fifo to the fanout
                    headStatementNodes.add(new CALFifoFanoutAddFifo(ScopeEnvironment.getInstance().createReadNode(fanoutNodeName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()), ScopeEnvironment.getInstance().createReadNode(newFIFOName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
                    
                    FIFONode = ScopeEnvironment.getInstance().createReadNode(newFIFOName, ScopeEnvironment.getInstance().createSourceSection(structureCtx));
                    entities.get(connection.destination.entity).inputs.add(Pair.of(connection.destination.port, FIFONode));
                } else {
                    // Connect entity with network port = use network's existing FIFO to communicate with the entity
                    if(connection.source.entity == null && connection.destination.entity == null) {
                        // This is when both the source and destination are external ports of the network
                        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Network Local Fifo's are not supported yet");
                    } else if (connection.source.entity == null) {
                        // This is when the source is an external port and destination is an entity within network
                        String fifoName = ScopeEnvironment.generateFIFOName();
                        headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(
                                fifoName,
                                new CALCreateFIFONode(),
                                DefaultValueCastNodeCreator.getInstance(),
                                ScopeEnvironment.getInstance().getSource().createUnavailableSection()));
                        headStatementNodes.add(new CALFifoFanoutAddFifo(ScopeEnvironment.getInstance().createReadNode(connection.source.port, ScopeEnvironment.getInstance().getSource().createUnavailableSection()), ScopeEnvironment.getInstance().createReadNode(fifoName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
                        entities.get(connection.destination.entity).inputs.add(Pair.of(connection.destination.port, ScopeEnvironment.getInstance().createReadNode(fifoName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
                    } else if (connection.destination.entity == null) {
                        // This is when the source is a port of an entity in the network and destination is an external port of the network
                        String portName = connection.source.entity + "." + connection.source.port;
                        String fanoutNodeName;
                        if(!outputPortToFanoutMapping.containsKey(portName)){
                            // We are seeing the output port for the first time
                            fanoutNodeName = ScopeEnvironment.generateFifoFanoutName();
                            outputPortToFanoutMapping.put(portName, fanoutNodeName);
                            headStatementNodes.add(ScopeEnvironment.getInstance().createNewVariableWriteNode(
                                    fanoutNodeName,
                                    new CALFifoFanoutNode(),
                                    DefaultValueCastNodeCreator.getInstance(),
                                    ScopeEnvironment.getInstance().getSource().createUnavailableSection()));
                            entities.get(connection.source.entity).outputs.add(Pair.of(connection.source.port, ScopeEnvironment.getInstance().createReadNode(fanoutNodeName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
                        } else
                            fanoutNodeName = outputPortToFanoutMapping.get(portName);

                        // Add the fifo to the fanout
                        headStatementNodes.add(new CALFifoFanoutAddFifo(ScopeEnvironment.getInstance().createReadNode(fanoutNodeName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()), ScopeEnvironment.getInstance().createReadNode(connection.destination.port, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
                    } else
                        throw new RuntimeException("Unreachable code");
                }
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
            argumentNodes.addAll(instance.inputs.stream().sorted((o1, o2) -> o1.getLeft().compareTo(o2.getLeft())).map(p -> p.getRight()).collect(Collectors.toList()));
            argumentNodes.addAll(instance.outputs.stream().sorted((o1, o2) -> o1.getLeft().compareTo(o2.getLeft())).map(p -> p.getRight()).collect(Collectors.toList()));

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