package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.parser.antlr.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.antlr.CALParserBaseVisitor;

import java.util.ArrayList;
import java.util.Collection;

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
    @Override public Collection<CALExpressionNode> visitBlockVariableDeclarations(CALParser.BlockVariableDeclarationsContext ctx) {
        Collection<CALExpressionNode> variableNodes = new ArrayList<>();

        variableNodes.add(VariableVisitor.getInstance().visitBlockVariableDeclaration(ctx.blockVariableDeclaration));
        for (CALParser.BlockVariableDeclarationContext c: ctx.blockVariableDeclaration()) {
            variableNodes.add(VariableVisitor.getInstance().visitBlockVariableDeclaration(c));
        }

        return variableNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALStatementNode> visitFormalParameters(CALParser.FormalParametersContext ctx) {
        Collection<CALStatementNode> formalParameterNodes = new ArrayList<>();

        formalParameterNodes.add(VariableVisitor.getInstance().visitFormalParameter(ctx.formalParameter));
        for (CALParser.FormalParameterContext c: ctx.formalParameter()) {
            formalParameterNodes.add(VariableVisitor.getInstance().visitFormalParameter(c));
        }

        return formalParameterNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<CALExpressionNode> visitExpressions(CALParser.ExpressionsContext ctx) {
        Collection<CALExpressionNode> expressionNodes = new ArrayList<>();

        for (CALParser.ExpressionContext c: ctx.expression()) {
            expressionNodes.add((CALExpressionNode) ExpressionVisitor.getInstance().visit(c));
        }

        return expressionNodes;
    }


}