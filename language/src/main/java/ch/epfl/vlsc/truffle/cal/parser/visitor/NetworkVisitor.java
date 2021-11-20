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
        List<CALExpressionNode> bodyStatementNodes = new LinkedList<>();

        int startIndex = 0;
        if (ctx.formalParameters() != null) {
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headStatementNodes.addAll(formalParameterNodes);
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

        ctx.entityDeclaration().forEach(entityCtx -> {
            Pair<CALStatementNode, CALExpressionNode> p = EntityVisitor.getInstance().visitEntityDeclaration(entityCtx);
            headStatementNodes.add(p.getLeft());
            bodyStatementNodes.add(p.getRight());
        });

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