package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALCreateFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALEntityAddInputPortNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALEntityAddOutputPortNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoFanoutAddFifo;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public CALStatementNode visitStructureStatements(CALParser.StructureStatementsContext ctx) {
        CALStatementNode stmtBlock = new StmtBlockNode(
                ctx.structureStatement().stream().map(structCtx -> visitStructureStatement(structCtx)).collect(Collectors.toList()).toArray(new CALStatementNode[0])
        );
        stmtBlock.addStatementTag();
        stmtBlock.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        return stmtBlock;
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
                    visitEntityReference(ctx.source.entityReference()),
                    ScopeEnvironment.getInstance().createReadNode(newFIFOName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
            stmts.add(new CALEntityAddInputPortNode(
                    ctx.dest.portReference().name.getText(),
                    visitEntityReference(ctx.dest.entityReference()),
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
                    visitEntityReference(ctx.dest.entityReference()),
                    ScopeEnvironment.getInstance().createReadNode(newFIFOName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
        } else if (ctx.source.entityReference() != null && ctx.dest.entityReference() == null) {
            stmts.add(new CALEntityAddOutputPortNode(
                    ctx.source.portReference().name.getText(),
                    visitEntityReference(ctx.source.entityReference()),
                    ScopeEnvironment.getInstance().createReadNode(ctx.dest.portReference().name.getText(), ScopeEnvironment.getInstance().getSource().createUnavailableSection())));
        } else
            throw new RuntimeException("Network Local Fifos not supported");
        CALStatementNode stmtBlock = new StmtBlockNode(stmts.toArray(new CALStatementNode[0]));
        stmtBlock.addStatementTag();
        stmtBlock.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        return stmtBlock;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitStructureForeachStatement(CALParser.StructureForeachStatementContext ctx) {
        CALWriteVariableNode variableNode = null;
        CALExpressionNode collectionNode = null;

        if (ctx.foreachGenerators().foreachGenerator().size() > 1) {
            // TODO Add support for multiple generators
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx.foreachGenerators(), "Multiple foreach generators are not yet supported");
            }
        }
        for (CALParser.ForeachGeneratorContext generatorCtx: ctx.foreachGenerators().foreachGenerator()) {
            if (generatorCtx.generatorBody().variables.size() > 1) {
                // TODO Add support for multiple variables in a generator
                if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                    throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Multiple variables in a foreach generator are not yet supported");
                }
            }
            for (Token variable: generatorCtx.generatorBody().variables) {
                CALExpressionNode placeholderValue = generatorCtx.generatorBody().type() != null ? VariableVisitor.fetchDefaultValue(generatorCtx.generatorBody().type()) : new NullLiteralNode();
                placeholderValue.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
                placeholderValue.addExpressionTag();

                // Note: Custom source section to precisely specify a variable token
                variableNode = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                        variable.getText(),
                        placeholderValue,
                        TypeCastVisitor.getInstance().visitType(generatorCtx.generatorBody().type()),
                        ScopeEnvironment.getInstance().getSource().createSection(variable.getLine(), variable.getCharPositionInLine() + 1, variable.getText().length())
                );
                variableNode.addStatementTag();
                variableNode.setHasWriteVarTag();

                if (!(variableNode instanceof CALWriteLocalVariableNode)) {
                    throw new CALParseError(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Variable name re-use in a foreach generator is not yet supported");
                }
            }
            // List<CALExpressionNode> expressionNodes = ((ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitExpressions(generatorCtx.generatorBody().));
            collectionNode = ExpressionVisitor.getInstance().visit(generatorCtx.generatorBody().generatorExpressions().collection);

            if (generatorCtx.generatorBody().generatorExpressions().filters.size() > 1) {
                // TODO Add support for generator filters
                if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                    throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Foreach generator filters are not yet supported");
                }
            }
        }

        CALStatementNode stmtBlockNode = visitStructureStatements(ctx.structureStatements());
        stmtBlockNode.addStatementTag();
        ForeacheNode foreachNode = ForeacheNodeGen.create(stmtBlockNode, (CALWriteLocalVariableNode) variableNode, collectionNode);
        foreachNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        foreachNode.addStatementTag();

        return foreachNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitStructureIfStatement(CALParser.StructureIfStatementContext ctx) {
        CALStatementNode stmtIf = new StmtIfNode(
                ExpressionVisitor.getInstance().visit(ctx.condition),
                visitStructureStatements(ctx.thenPart),
                ctx.elsePart != null ? visitStructureStatements(ctx.elsePart) : visitStructureElseIfStatement(ctx.structureElseIfStatement()));
        stmtIf.addStatementTag();
        stmtIf.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        return stmtIf;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitStructureElseIfStatement(CALParser.StructureElseIfStatementContext ctx) {
        CALStatementNode stmtIf = new StmtIfNode(
                ExpressionVisitor.getInstance().visit(ctx.condition),
                visitStructureStatements(ctx.thenPart),
                ctx.elsePart != null ? visitStructureStatements(ctx.elsePart) : visitStructureElseIfStatement(ctx.structureElseIfStatement()));
        stmtIf.addStatementTag();
        stmtIf.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        return stmtIf;
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
    @Override public CALExpressionNode visitEntityReference(CALParser.EntityReferenceContext ctx) {
        CALExpressionNode resultNode = ScopeEnvironment.getInstance().createReadNode(ctx.name.getText(), ScopeEnvironment.getInstance().getSource().createSection(ctx.name.getLine(), ctx.name.getCharPositionInLine()+1, ctx.name.getLine(), ctx.name.getCharPositionInLine()+ctx.name.getText().length()));
        List<CALExpressionNode> indices = ctx.expression().stream().map(expr -> ExpressionVisitor.getInstance().visit(expr)).collect(Collectors.toList());
        int i = 0;
        for (CALExpressionNode indexNode: indices) {
            resultNode = ListReadNodeGen.create(resultNode, indexNode);
            // Note: Custom source section to precisely specify a part of indexer expression
            resultNode.setSourceSection(ScopeEnvironment.getInstance().getSource().createSection(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine() + 1,
                    ctx.expression(i).getStop().getLine(),
                    ctx.expression(i).getStop().getCharPositionInLine() + 1
            ));
            resultNode.addExpressionTag();
            ++i;
        }

        return resultNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPortReference(CALParser.PortReferenceContext ctx) {
        // Note: Unreachable for now, only children directly accessed in #visitStructureConnectorStatement
        return super.visitPortReference(ctx);
    }
}