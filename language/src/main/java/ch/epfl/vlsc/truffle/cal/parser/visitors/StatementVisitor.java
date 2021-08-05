package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.parser.antlr.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.antlr.CALParserBaseVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class that provides an implementation for a statement(s) sub-tree.
 */
public class StatementVisitor extends CALParserBaseVisitor<CALStatementNode> {

    private static StatementVisitor instance;

    private StatementVisitor() {}

    public static StatementVisitor getInstance() {
        if (instance == null) {
            instance = new StatementVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public StmtBlockNode visitStatements(CALParser.StatementsContext ctx) {
        List<CALStatementNode> statementNodes = new ArrayList<>();

        for (CALParser.StatementContext c: ctx.statement()) {
            statementNodes.add(visit(c));
        }

        return new StmtBlockNode(statementNodes.toArray(new CALStatementNode[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitAssignmentStatement(CALParser.AssignmentStatementContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitCallStatement(CALParser.CallStatementContext ctx) {
        return new CALInvokeNode(
            (CALExpressionNode) ExpressionVisitor.getInstance().visit(ctx.function),
            ctx.arguments != null ? CollectionVisitor.getInstance().visitExpressions(ctx.arguments).toArray(new CALExpressionNode[0]) : new CALExpressionNode[0]
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitBlockStatement(CALParser.BlockStatementContext ctx) {
        // TODO Use unnamed proc closure

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public StmtIfNode visitIfStatement(CALParser.IfStatementContext ctx) {
        return new StmtIfNode(
            (CALExpressionNode) ExpressionVisitor.getInstance().visit(ctx.condition),
            visitStatements(ctx.then),
            ctx.elseIf != null ? visitElseIfStatement(ctx.elseIf): visitStatements(ctx.elze)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override public StmtIfNode visitElseIfStatement(CALParser.ElseIfStatementContext ctx) {
        return new StmtIfNode(
            (CALExpressionNode) ExpressionVisitor.getInstance().visit(ctx.condition),
            visitStatements(ctx.then),
            ctx.elseIf != null ? visitElseIfStatement(ctx.elseIf) : visitStatements(ctx.elze)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitWhileStatement(CALParser.WhileStatementContext ctx) {
        // TODO Create CALWhileStatementNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitForeachStatement(CALParser.ForeachStatementContext ctx) {
        // TODO Add support for multiple variables in a generator
        // TODO Add support for multiple generators
        // TODO Add support for generator filters?
       /* CALExpressionNode variable = variables.get(0).get(0);
        CALExpressionNode collection = collections.get(0);

        if (variable instanceof CALWriteLocalVariableNode) {
            return ForeacheNodeGen.create(body, (CALWriteLocalVariableNode) variable, collection);
        } else {
            throw new Error("Foreach Statement: Variable name re-use is unsupported.");
        }*/

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitChooseStatement(CALParser.ChooseStatementContext ctx) {
        // TODO Create CALChooseStatementNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitCaseStatement(CALParser.CaseStatementContext ctx) {
        // TODO Create CALCaseStatementNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitAlternativeStatement(CALParser.AlternativeStatementContext ctx) {
        // TODO First resolve {@link visitCaseStatement} dependencies

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitReadStatement(CALParser.ReadStatementContext ctx) {
        // TODO Create CALReadStatementNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitWriteStatement(CALParser.WriteStatementContext ctx) {
        // TODO Create CALWriteStatementNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitActionSelectionStatement(CALParser.ActionSelectionStatementContext ctx) {
        // TODO Create Action Selection statement

        return null;
    }
}