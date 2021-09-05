package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoTransactionCommit;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFifoTransactionRollback;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.CALLexer;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.tuple.Triple;
import org.jgrapht.alg.color.SaturationDegreeColoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    public static QualifiedID qualifiedIdCreator(Collection<Token> visitActionTag) {
        return new QualifiedID(visitActionTag.stream().map(token -> token.getText()).collect(Collectors.toList()));
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
    @Override public Collection<?> visitEntityExpressions(CALParser.EntityExpressionsContext ctx) {
        // TODO First resolve EntityVisitor#visitEntityIfExpression and EntityVisitor#visitEntityListExpression
        // Note: Unreachable for now
        return super.visitEntityExpressions(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitEntityParameters(CALParser.EntityParametersContext ctx) {
        // Note: Unreachable for now, only children directly accessed in EntityVisitor#visitEntityInstanceExpression
        return super.visitEntityParameters(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<?> visitAttributeSection(CALParser.AttributeSectionContext ctx) {
        // TODO First resolve EntityVisitor#visitEntityInstanceExpression
        // Note: Unreachable for now
        return super.visitAttributeSection(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<InitializeArgNode> visitPortDeclarations(CALParser.PortDeclarationsContext ctx) {
        Collection<InitializeArgNode> ports = new ArrayList<>();
        for (CALParser.PortDeclarationContext portCtx: ctx.portDeclaration()) {
            ports.add(VariableVisitor.getInstance().visitPortDeclaration(portCtx));
        }

        return ports;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<Triple<CALExpressionNode, CALFifoTransactionCommit, CALFifoTransactionRollback>> visitInputPatterns(CALParser.InputPatternsContext ctx) {
        Collection<Triple<CALExpressionNode, CALFifoTransactionCommit, CALFifoTransactionRollback>> patterns = new ArrayList<>();

        if (ctx.inputPattern().stream().allMatch(inpExp -> inpExp.port == null)) {
            List<String> inputPorts = ActorVisitor.getActorInputPorts(ActorVisitor.getCurrentlyProcessingActor());
            if (ctx.inputPattern().size() != inputPorts.size()) {
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Implicit port mapping in action input does not have same number of output ports as actor");
            }
            for(int i = 0; i < ctx.inputPattern().size(); ++i) {
                ActionVisitor.setPortName(inputPorts.get(i));
                patterns.add(ActionVisitor.getInstance().visitInputPattern(ctx.inputPattern(i)));
            }
        } else if (ctx.inputPattern().stream().allMatch(inpExp -> inpExp.port != null)) {
            for (CALParser.InputPatternContext patternCtx: ctx.inputPattern()) {
                patterns.add(ActionVisitor.getInstance().visitInputPattern(patternCtx));
            }
        } else
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Either all input expression ports must be named or none");

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
        if (ctx.outputExpression().stream().allMatch(outExp -> outExp.port == null)) {
            List<String> outputPorts = ActorVisitor.getActorOutputPorts(ActorVisitor.getCurrentlyProcessingActor());
            if (ctx.outputExpression().size() != outputPorts.size()) {
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Implicit port mapping in action output does not have same number of output ports as actor");
            }
            for(int i = 0; i < ctx.outputExpression().size(); ++i) {
                ActionVisitor.setPortName(outputPorts.get(i));
                expressions.add(ActionVisitor.getInstance().visitOutputExpression(ctx.outputExpression(i)));
            }
        } else if (ctx.outputExpression().stream().allMatch(outExp -> outExp.port != null)) {
            for (CALParser.OutputExpressionContext expressionCtx: ctx.outputExpression()) {
                expressions.add(ActionVisitor.getInstance().visitOutputExpression(expressionCtx));
            }
        } else
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Either all output expression ports must be named or none");
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