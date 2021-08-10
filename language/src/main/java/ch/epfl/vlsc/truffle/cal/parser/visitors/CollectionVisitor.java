package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALWriteFIFONode;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Singleton class that provides an implementation for a collection rules.
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
    @Override public Collection<CALWriteFIFONode> visitOutputExpressions(CALParser.OutputExpressionsContext ctx) {
        Collection<CALWriteFIFONode> expressions = new ArrayList<>();

        //expressions.add(ActionVisitor.getInstance().visitOutputExpression(ctx.outputExpression));
        for (CALParser.OutputExpressionContext context: ctx.outputExpression()) {
            expressions.add(ActionVisitor.getInstance().visitOutputExpression(context));
        }

        return expressions;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<Collection<Token>> visitActionTags(CALParser.ActionTagsContext ctx) {
        Collection<Collection<Token>> tags = new ArrayList<>();

        //tags.add(ActionVisitor.getInstance().visitActionTag(ctx.actionTag));
        for (CALParser.ActionTagContext context: ctx.actionTag()) {
            tags.add(ActionVisitor.getInstance().visitActionTag(context));
        }

        return tags;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALExpressionNode> visitBlockVariableDeclarations(CALParser.BlockVariableDeclarationsContext ctx) {
        Collection<CALExpressionNode> variableNodes = new ArrayList<>();

        //variableNodes.add(VariableVisitor.getInstance().visitBlockVariableDeclaration(ctx.blockVariableDeclaration));
        for (CALParser.BlockVariableDeclarationContext context: ctx.blockVariableDeclaration()) {
            variableNodes.add(VariableVisitor.getInstance().visitBlockVariableDeclaration(context));
        }

        return variableNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALStatementNode> visitFormalParameters(CALParser.FormalParametersContext ctx) {
        Collection<CALStatementNode> formalParameterNodes = new ArrayList<>();

        //formalParameterNodes.add(VariableVisitor.getInstance().visitFormalParameter(ctx.formalParameter));
        for (CALParser.FormalParameterContext context: ctx.formalParameter()) {
            formalParameterNodes.add(VariableVisitor.getInstance().visitFormalParameter(context));
        }

        return formalParameterNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALExpressionNode> visitExpressions(CALParser.ExpressionsContext ctx) {
        Collection<CALExpressionNode> expressionNodes = new ArrayList<>();

        for (CALParser.ExpressionContext context: ctx.expression()) {
            expressionNodes.add((CALExpressionNode) ExpressionVisitor.getInstance().visit(context));
        }

        return expressionNodes;
    }


}