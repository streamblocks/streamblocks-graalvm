package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALWriteFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Singleton class that provides an implementation for collection rules.
 */
public class CollectionVisitor extends CALParserBaseVisitor<Collection<?>> {

    private static CollectionVisitor instance;

    private CollectionVisitor() {}

    public static CollectionVisitor getInstance() {
        if (instance == null) {
            instance = new CollectionVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<Token> visitQualifiedID(CALParser.QualifiedIDContext ctx) {
        Collection<Token> parts = new ArrayList<>();

        //parts.add(ctx.ID);
        for (TerminalNode id: ctx.ID()) {
            parts.add(id.getSymbol());
        }

        return parts;
    }

    public static String qualifiedIdToString(Collection<Token> qualifiedId) {
        return qualifiedId.stream().map(Token::getText).collect(Collectors.joining("."));
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<InitializeArgNode> visitPortDeclarations(CALParser.PortDeclarationsContext ctx) {
        Collection<InitializeArgNode> ports = new ArrayList<>();
        for (CALParser.PortDeclarationContext portCtx: ctx.portDeclaration()) {
            ports.add(ActorVisitor.getInstance().visitPortDeclaration(portCtx));
        }

        return ports;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALExpressionNode> visitInputPatterns(CALParser.InputPatternsContext ctx) {
        Collection<CALExpressionNode> patterns = new ArrayList<>();
        for (CALParser.InputPatternContext patternCtx: ctx.inputPattern()) {
            patterns.add(ActionVisitor.getInstance().visitInputPattern(patternCtx));
        }

        return patterns;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitPatterns(CALParser.PatternsContext ctx) {
        // Note: Unreachable for now, only children directly accessed in ActionVisitor#visitInputPattern
        return super.visitPatterns(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitSubPatterns(CALParser.SubPatternsContext ctx) {
        // TODO First resolve ActionVisitor#visitInputPattern
        // Note: Unreachable for now
        return super.visitSubPatterns(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALStatementNode> visitOutputExpressions(CALParser.OutputExpressionsContext ctx) {
        Collection<CALStatementNode> expressions = new ArrayList<>();
        for (CALParser.OutputExpressionContext expressionCtx: ctx.outputExpression()) {
            expressions.add(ActionVisitor.getInstance().visitOutputExpression(expressionCtx));
        }

        return expressions;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<Collection<Token>> visitActionTags(CALParser.ActionTagsContext ctx) {
        Collection<Collection<Token>> tags = new ArrayList<>();
        for (CALParser.ActionTagContext tagCtx: ctx.actionTag()) {
            tags.add(ActionVisitor.getInstance().visitActionTag(tagCtx));
        }

        return tags;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALExpressionNode> visitBlockVariableDeclarations(CALParser.BlockVariableDeclarationsContext ctx) {
        Collection<CALExpressionNode> variableNodes = new ArrayList<>();
        for (CALParser.BlockVariableDeclarationContext variableCtx: ctx.blockVariableDeclaration()) {
            variableNodes.add(VariableVisitor.getInstance().visitBlockVariableDeclaration(variableCtx));
        }

        return variableNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALStatementNode> visitFormalParameters(CALParser.FormalParametersContext ctx) {
        Collection<CALStatementNode> formalParameterNodes = new ArrayList<>();
        for (CALParser.FormalParameterContext formalParameterCtx: ctx.formalParameter()) {
            formalParameterNodes.add(VariableVisitor.getInstance().visitFormalParameter(formalParameterCtx));
        }

        return formalParameterNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitTypes(CALParser.TypesContext ctx) {
        // TODO First resolve TypeVisitor#visitType
        // Note: Unreachable for now
        return super.visitTypes(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitTypeParameters(CALParser.TypeParametersContext ctx) {
        // TODO First resolve TypeVisitor#visitType
        // Note: Unreachable for now
        return super.visitTypeParameters(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitTypeAttributes(CALParser.TypeAttributesContext ctx) {
        // TODO First resolve TypeVisitor#visitType
        // Note: Unreachable for now
        return super.visitTypeAttributes(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitGenerators(CALParser.GeneratorsContext ctx) {
        // Note: Unreachable for now, only children directly accessed in ExpressionVisitor#visitListComprehension
        return super.visitGenerators(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitForeachGenerators(CALParser.ForeachGeneratorsContext ctx) {
        // Note: Unreachable for now, only children directly accessed in StatementVisitor#visitForeachStatement
        return super.visitForeachGenerators(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitChooseGenerators(CALParser.ChooseGeneratorsContext ctx) {
        // Note: Unreachable for now, StatementVisitor#visitChooseStatement is not yet implemented
        return super.visitChooseGenerators(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALExpressionNode> visitExpressions(CALParser.ExpressionsContext ctx) {
        Collection<CALExpressionNode> expressionNodes = new ArrayList<>();
        for (CALParser.ExpressionContext expressionCtx: ctx.expression()) {
            expressionNodes.add(ExpressionVisitor.getInstance().visit(expressionCtx));
        }

        return expressionNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitLvalues(CALParser.LvaluesContext ctx) {
        // Note: Unreachable for now
        return super.visitLvalues(ctx);
    }

}