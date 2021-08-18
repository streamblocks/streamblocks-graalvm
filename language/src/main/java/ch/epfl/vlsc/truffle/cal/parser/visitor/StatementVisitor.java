package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

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
        for (CALParser.StatementContext statementCtx: ctx.statement()) {
            CALStatementNode statementNode = visitStatement(statementCtx);
            statementNodes.add(statementNode);
        }

        return new StmtBlockNode(statementNodes.toArray(new CALStatementNode[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitStatement(CALParser.StatementContext ctx) {
        if (ctx.assignmentStatement() != null) {
            return visitAssignmentStatement(ctx.assignmentStatement());
        } else if (ctx.callStatement() != null) {
            return visitCallStatement(ctx.callStatement());
        } else if (ctx.blockStatement() != null) {
            return visitBlockStatement(ctx.blockStatement());
        } else if (ctx.ifStatement() != null) {
            return visitIfStatement(ctx.ifStatement());
        } else if (ctx.whileStatement() != null) {
            return visitWhileStatement(ctx.whileStatement());
        } else if (ctx.foreachStatement() != null) {
            return visitForeachStatement(ctx.foreachStatement());
        } else if (ctx.chooseStatement() != null) {
            return visitChooseStatement(ctx.chooseStatement());
        } else if (ctx.caseStatement() != null) {
            return visitCaseStatement(ctx.caseStatement());
        } else if (ctx.readStatement() != null) {
            return visitReadStatement(ctx.readStatement());
        } else if (ctx.writeStatement() != null) {
            return visitWriteStatement(ctx.writeStatement());
        } else if (ctx.actionSelectionStatement() != null) {
            return visitActionSelectionStatement(ctx.actionSelectionStatement());
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized statement type"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitAssignmentStatement(CALParser.AssignmentStatementContext ctx) {
        String variableName = ctx.lvalue().variable().name.getText();
        CALExpressionNode valueNode = ExpressionVisitor.getInstance().visit(ctx.value);

        // Note: Using all children instead of field()/expression() non-terminals to preserve access order
        List<ParseTree> accessors = new ArrayList<>(ctx.lvalue().children);
        accessors.remove(ctx.lvalue().variable());
        accessors.removeIf(accessor -> accessor instanceof TerminalNode);
        if (accessors.size() > 0) {
            CALExpressionNode resultNode = ScopeEnvironment.getInstance().createReadNode(variableName);
            for (ParseTree accessor : accessors.subList(0, accessors.size() - 1)) {
                if (accessor instanceof CALParser.FieldContext) {
                    // TODO Add support for accessing fields
                    throw new CALParseError(ScopeEnvironment.getInstance().getSource(), (CALParser.FieldContext) accessor, "Lvalue field access is not yet supported");
                } else if (accessor instanceof CALParser.ExpressionContext) {
                    CALExpressionNode indexNode = ExpressionVisitor.getInstance().visit(accessor);
                    resultNode = ListReadNodeGen.create(resultNode, indexNode);
                } else {
                    throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized lvalue accessor type - Lvalue"); // Note: Unreachable case
                }
            }

            ParseTree lastAccessor = accessors.get(accessors.size() - 1);
            if (lastAccessor instanceof CALParser.FieldContext) {
                // TODO Add support for accessing fields
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), (CALParser.FieldContext) lastAccessor, "Lvalue field access is not yet supported");
            } else if (lastAccessor instanceof CALParser.ExpressionContext) {
                CALExpressionNode lastIndexNode = ExpressionVisitor.getInstance().visit(lastAccessor);
                return ListWriteNodeGen.create(resultNode, lastIndexNode, valueNode);
            } else {
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized lvalue accessor type"); // Note: Unreachable case
            }
        } else {
            return ScopeEnvironment.getInstance().createWriteNode(variableName, valueNode);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitCallStatement(CALParser.CallStatementContext ctx) {
        CALExpressionNode functionNode = ExpressionVisitor.getInstance().visitVariableExpression(ctx.function);

        CALExpressionNode[] argumentNodes;
        if (ctx.arguments != null) {
            argumentNodes = CollectionVisitor.getInstance().visitExpressions(ctx.arguments).toArray(new CALExpressionNode[0]);
        } else {
            argumentNodes = new CALExpressionNode[0];
        }

        return new CALInvokeNode(functionNode, argumentNodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitBlockStatement(CALParser.BlockStatementContext ctx) {
        // TODO First resolve ExpressionVisitor#visitProcExpression  => use unnamed proc closure as a block statement
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Block statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public StmtIfNode visitIfStatement(CALParser.IfStatementContext ctx) {
        CALExpressionNode conditionNode = ExpressionVisitor.getInstance().visit(ctx.condition);
        StmtBlockNode thenNode = visitStatements(ctx.then);

        CALStatementNode elseNode;
        if (ctx.elseIf != null) {
            elseNode = visitElseIfStatement(ctx.elseIf);
        } else if (ctx.elze != null) {
            elseNode = visitStatements(ctx.elze);
        } else {
            elseNode = null;
        }

        return new StmtIfNode(conditionNode, thenNode, elseNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public StmtIfNode visitElseIfStatement(CALParser.ElseIfStatementContext ctx) {
        CALExpressionNode conditionNode = ExpressionVisitor.getInstance().visit(ctx.condition);
        StmtBlockNode thenNode = visitStatements(ctx.then);

        CALStatementNode elseNode;
        if (ctx.elseIf != null) {
            elseNode = visitElseIfStatement(ctx.elseIf);
        } else if (ctx.elze != null) {
            elseNode = visitStatements(ctx.elze);
        } else {
            elseNode = null;
        }

        return new StmtIfNode(conditionNode, thenNode, elseNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitWhileStatement(CALParser.WhileStatementContext ctx) {
        // TODO Create While statement node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "While statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitForeachStatement(CALParser.ForeachStatementContext ctx) {
        CALExpressionNode variableNode = null;
        CALExpressionNode collectionNode = null;

        if (ctx.foreachGenerators().foreachGenerator().size() > 1) {
            // TODO Add support for multiple generators
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx.foreachGenerators(), "Multiple foreach generators are not yet supported");
        }
        for (CALParser.ForeachGeneratorContext generatorCtx: ctx.foreachGenerators().foreachGenerator()) {
            if (generatorCtx.generatorBody().variables.size() > 1) {
                // TODO Add support for multiple variables in a generator
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Multiple variables in a foreach generator are not yet supported");
            }
            for (Token variable: generatorCtx.generatorBody().variables) {
                variableNode = ScopeEnvironment.getInstance().createWriteNode(variable.getText(), new NullLiteralNode());

                if (!(variableNode instanceof CALWriteLocalVariableNode)) {
                    throw new CALParseError(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Variable name re-use in a foreach generator is not yet supported");
                }
            }
            List<CALExpressionNode> expressionNodes = ((ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitExpressions(generatorCtx.generatorBody().expressions()));
            collectionNode = expressionNodes.get(0);

            if (expressionNodes.size() > 1) {
                // TODO Add support for generator filters
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Foreach generator filters are not yet supported");
            }
        }

        return ForeacheNodeGen.create(StatementVisitor.getInstance().visit(ctx.body), (CALWriteLocalVariableNode) variableNode, collectionNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitChooseStatement(CALParser.ChooseStatementContext ctx) {
        // TODO Create Choose statement node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Choose statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitCaseStatement(CALParser.CaseStatementContext ctx) {
        // TODO Create Case statement node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Case statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitAlternativeStatement(CALParser.AlternativeStatementContext ctx) {
        // TODO First resolve #visitCaseStatement
        // Note: Unreachable for now
        return super.visitAlternativeStatement(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitReadStatement(CALParser.ReadStatementContext ctx) {
        // TODO Create Read statement node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Read statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitWriteStatement(CALParser.WriteStatementContext ctx) {
        // TODO Create Write statement node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Write statement is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitActionSelectionStatement(CALParser.ActionSelectionStatementContext ctx) {
        // TODO Create Action Selection statement node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Action Selection statement is not yet supported");
    }
}