package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALCreateFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALEntityAddInputPortNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALEntityAddOutputPortNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoFanoutAddFifo;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;

import java.util.LinkedList;
import java.util.List;

/**
 * Singleton class that provides an implementation for a structure sub-tree.
 */
public class StructureVisitor extends CALParserBaseVisitor<Object> {

    public static class StructureConnection {

        public static class StructureConnector {
            public String entity;

            public String port;
        }

        public StructureConnector source;

        public StructureConnector destination;

        public StructureConnection(StructureConnector source, StructureConnector destination) {
            this.source = source;
            this.destination = destination;
        }

    }

    private static StructureVisitor instance;

    private StructureVisitor() {}

    public static StructureVisitor getInstance() {
        if (instance == null) {
            instance = new StructureVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitStructureStatement(CALParser.StructureStatementContext ctx) {
        if (ctx.structureConnectorStatement() != null) {
            return visitStructureConnectorStatement(ctx.structureConnectorStatement());
        } else if (ctx.structureIfStatement() != null) {
            return visitStructureIfStatement(ctx.structureIfStatement());
        } else if (ctx.structureForeachStatement() != null) {
            return visitStructureForeachStatement(ctx.structureForeachStatement());
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized structure statement type"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitStructureConnectorStatement(CALParser.StructureConnectorStatementContext ctx) {
        List<CALStatementNode> stmts = new LinkedList<>();
        if (ctx.source.entityReference() != null && ctx.dest.entityReference() != null) {
            String newFIFOName = ScopeEnvironment.generateFIFOName();
            CALCreateFIFONode newFIFOValueNode = new CALCreateFIFONode();
            newFIFOValueNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            CALExpressionNode newFIFONode = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                    newFIFOName,
                    newFIFOValueNode,
                    DefaultValueCastNodeCreator.getInstance(),
                    ScopeEnvironment.getInstance().createSourceSection(ctx));
            stmts.add(newFIFONode);
            stmts.add(new CALEntityAddOutputPortNode(
                    ctx.source.portReference().name.getText(),
                    ScopeEnvironment.getInstance().createReadNode(ctx.source.entityReference().name.getText(), ScopeEnvironment.getInstance().createSourceSection(ctx.source.entityReference())),
                    ScopeEnvironment.getInstance().createReadNode(newFIFOName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
            stmts.add(new CALEntityAddInputPortNode(
                    ctx.dest.portReference().name.getText(),
                    ScopeEnvironment.getInstance().createReadNode(ctx.dest.entityReference().name.getText(), ScopeEnvironment.getInstance().createSourceSection(ctx.dest.entityReference())),
                    ScopeEnvironment.getInstance().createReadNode(newFIFOName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
        } else if (ctx.source.entityReference() == null && ctx.dest.entityReference() != null) {
            String newFIFOName = ScopeEnvironment.generateFIFOName();
            CALCreateFIFONode newFIFOValueNode = new CALCreateFIFONode();
            newFIFOValueNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            CALExpressionNode newFIFONode = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                    newFIFOName,
                    newFIFOValueNode,
                    DefaultValueCastNodeCreator.getInstance(),
                    ScopeEnvironment.getInstance().createSourceSection(ctx));
            stmts.add(newFIFONode);
            stmts.add(new CALFifoFanoutAddFifo(
                    ScopeEnvironment.getInstance().createReadNode(ctx.source.portReference().name.getText(), ScopeEnvironment.getInstance().getSource().createUnavailableSection()),
                    ScopeEnvironment.getInstance().createReadNode(newFIFOName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())
            ));
            stmts.add(new CALEntityAddInputPortNode(
                    ctx.dest.portReference().name.getText(),
                    ScopeEnvironment.getInstance().createReadNode(ctx.dest.entityReference().name.getText(), ScopeEnvironment.getInstance().createSourceSection(ctx.dest.entityReference())),
                    ScopeEnvironment.getInstance().createReadNode(newFIFOName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
        } else if (ctx.source.entityReference() != null && ctx.dest.entityReference() == null) {
            stmts.add(new CALEntityAddOutputPortNode(
                    ctx.source.portReference().name.getText(),
                    ScopeEnvironment.getInstance().createReadNode(ctx.source.entityReference().name.getText(), ScopeEnvironment.getInstance().createSourceSection(ctx.source.entityReference())),
                    ScopeEnvironment.getInstance().createReadNode(ctx.dest.portReference().name.getText(), ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
        } else
            throw new RuntimeException("Network Local Fifos not supported");
        return new StmtBlockNode(stmts.toArray(new CALStatementNode[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitStructureForeachStatement(CALParser.StructureForeachStatementContext ctx) {
        // TODO Create Structure Foreach statement
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Structure foreach statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitStructureIfStatement(CALParser.StructureIfStatementContext ctx) {
        // TODO Create Structure If statement
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Structure conditional statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitStructureElseIfStatement(CALParser.StructureElseIfStatementContext ctx) {
        // TODO First resolve #visitStructureIfStatement
        // Note: Unreachable for now
        return super.visitStructureElseIfStatement(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitConnector(CALParser.ConnectorContext ctx) {
        // Note: Unreachable for now, only children directly accessed in #visitStructureConnectorStatement
        return super.visitConnector(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitEntityReference(CALParser.EntityReferenceContext ctx) {
        // Note: Unreachable for now, only children directly accessed in #visitStructureConnectorStatement
        return super.visitEntityReference(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPortReference(CALParser.PortReferenceContext ctx) {
        // Note: Unreachable for now, only children directly accessed in #visitStructureConnectorStatement
        return super.visitPortReference(ctx);
    }

}