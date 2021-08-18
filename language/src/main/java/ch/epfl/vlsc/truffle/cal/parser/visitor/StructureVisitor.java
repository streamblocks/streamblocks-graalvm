package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;

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
    @Override public Object visitStructureStatement(CALParser.StructureStatementContext ctx) {
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
    @Override public StructureConnection visitStructureConnectorStatement(CALParser.StructureConnectorStatementContext ctx) {
        CALParser.ConnectorContext sourceCtx = ctx.connector(0);
        StructureConnection.StructureConnector source = new StructureConnection.StructureConnector();
        if (sourceCtx.entityReference() != null) {
            source.entity = sourceCtx.entityReference().name.getText();
        } else {
            source.entity = null;
        }
        source.port = sourceCtx.portReference().name.getText();


        CALParser.ConnectorContext destinationCtx = ctx.connector(1);
        StructureConnection.StructureConnector destination = new StructureConnection.StructureConnector();
        if (destinationCtx.entityReference() != null) {
            destination.entity = destinationCtx.entityReference().name.getText();
        } else {
            destination.entity = null;
        }
        destination.port = destinationCtx.portReference().name.getText();

        if (ctx.attributeSection() != null) {
            // TODO Add support for attribute section
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Structure attributes are not yet supported");
        }

        return new StructureConnection(source, destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitStructureForeachStatement(CALParser.StructureForeachStatementContext ctx) {
        // TODO Create Structure Foreach statement
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Structure foreach statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitStructureIfStatement(CALParser.StructureIfStatementContext ctx) {
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