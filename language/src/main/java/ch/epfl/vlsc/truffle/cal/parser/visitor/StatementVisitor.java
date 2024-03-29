package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtFunctionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtWhileNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.parser.CALLexer;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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
            statementNode.addStatementTag();
            statementNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(statementCtx));
            statementNodes.add(statementNode);
        }

        StmtBlockNode statementsNode = new StmtBlockNode(statementNodes.toArray(new CALStatementNode[0]));
        statementsNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        statementsNode.addStatementTag();

        return statementsNode;
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
            CALExpressionNode lvalueNode = ScopeEnvironment.getInstance().createReadNode(variableName, ScopeEnvironment.getInstance().createSourceSection(ctx.lvalue().variable()));
            for (ParseTree accessor : accessors.subList(0, accessors.size() - 1)) {
                if (accessor instanceof CALParser.FieldContext) {
                    // TODO Add support for accessing fields
                    throw new CALParseError(ScopeEnvironment.getInstance().getSource(), (CALParser.FieldContext) accessor, "Lvalue field access is not yet supported");
                } else if (accessor instanceof CALParser.ExpressionContext) {
                    CALExpressionNode indexNode = ExpressionVisitor.getInstance().visit(accessor);

                    lvalueNode = ListReadNodeGen.create(lvalueNode, indexNode);
                    // Note: Custom source section to precisely specify a part of lvalue
                    lvalueNode.setSourceSection(ScopeEnvironment.getInstance().getSource().createSection(
                            ctx.lvalue().variable().getStart().getLine(),
                            ctx.lvalue().variable().getStart().getCharPositionInLine() + 1,
                            ((CALParser.ExpressionContext) accessor).getStop().getLine(),
                            ((CALParser.ExpressionContext) accessor).getStop().getCharPositionInLine() + 1
                    ));
                    lvalueNode.addExpressionTag();
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

                ListWriteNode assignmentNode = ListWriteNodeGen.create(lvalueNode, lastIndexNode, valueNode);
                assignmentNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
                assignmentNode.addStatementTag();

                return assignmentNode;
            } else {
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized lvalue accessor type"); // Note: Unreachable case
            }
        } else {
            CALWriteVariableNode existingVariableWriteNode = ScopeEnvironment.getInstance().createExistingVariableWriteNode(variableName, valueNode, ScopeEnvironment.getInstance().createSourceSection(ctx));
            existingVariableWriteNode.addStatementTag();
            existingVariableWriteNode.setHasWriteVarTag();
            return existingVariableWriteNode;
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

        CALInvokeNode callNode = CALInvokeNodeGen.create(argumentNodes, functionNode);
        callNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        callNode.addStatementTag();

        return callNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitBlockStatement(CALParser.BlockStatementContext ctx) {
        // TODO First resolve ExpressionVisitor#visitProcExpression  => use unnamed proc closure as a block statement
        StmtBlockNode headNode;
        CALExpressionNode bodyNode;

        ScopeEnvironment.getInstance().pushScope(false);

        if (ctx.localVariables != null) {
            ScopeEnvironment.getInstance().pushScope(false, false);

            Collection<CALExpressionNode> localVariableNodes = CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables);
            StmtBlockNode letHeadNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
            letHeadNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.localVariables));
            letHeadNode.addStatementTag();

            CALExpressionNode letBodyNode = new StmtFunctionBodyNode(StatementVisitor.getInstance().visitStatements(ctx.statements()));

            bodyNode = new LetExprNode(letHeadNode, letBodyNode);
            bodyNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            bodyNode.addExpressionTag();

            ScopeEnvironment.getInstance().popScope();
        } else {
            bodyNode = new StmtFunctionBodyNode(StatementVisitor.getInstance().visit(ctx.statements()));
        }

        CALRootNode procBodyRootNode = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                bodyNode,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                ScopeEnvironment.generateLambdaName()
        );
        // TODO Add RootTag / CallTag for procBodyRootNode

        ProcNode procNode = new ProcNode(procBodyRootNode);
        procNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        procNode.addStatementTag();

        ScopeEnvironment.getInstance().popScope();

        return CALInvokeNodeGen.create(new CALExpressionNode[0], procNode);
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

        StmtIfNode ifNode = new StmtIfNode(conditionNode, thenNode, elseNode);
        ifNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        ifNode.addStatementTag();

        return ifNode;
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

        StmtIfNode elseIfNode = new StmtIfNode(conditionNode, thenNode, elseNode);
        elseIfNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        elseIfNode.addStatementTag();

        return elseIfNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitWhileStatement(CALParser.WhileStatementContext ctx) {
        if (ctx.blockVariableDeclarations() != null) {
            ScopeEnvironment.getInstance().pushScope(false, false);
            Collection<CALExpressionNode> localVariableNodes = CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.blockVariableDeclarations());
            StmtBlockNode letHeadNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
            letHeadNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.blockVariableDeclarations()));
            letHeadNode.addStatementTag();

            StmtWhileNode letBodyNode = new StmtWhileNode(ExpressionVisitor.getInstance().visit(ctx.expression()), StatementVisitor.getInstance().visit(ctx.statements()));
            letBodyNode.addStatementTag();
            StmtBlockNode block = new StmtBlockNode(new CALStatementNode[]{letHeadNode, letBodyNode});
            block.addStatementTag();
            block.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            ScopeEnvironment.getInstance().popScope();
            return block;
        } else {
            StmtWhileNode whileNode = new StmtWhileNode(ExpressionVisitor.getInstance().visit(ctx.expression()), StatementVisitor.getInstance().visit(ctx.statements()));
            whileNode.addStatementTag();
            whileNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            return whileNode;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitForeachStatement(CALParser.ForeachStatementContext ctx) {
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

        StmtBlockNode stmtBlockNode = visitStatements(ctx.body);
        stmtBlockNode.addStatementTag();
        ForeacheNode foreachNode = ForeacheNodeGen.create(stmtBlockNode, (CALWriteLocalVariableNode) variableNode, collectionNode);
        foreachNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        foreachNode.addStatementTag();

        return foreachNode;
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