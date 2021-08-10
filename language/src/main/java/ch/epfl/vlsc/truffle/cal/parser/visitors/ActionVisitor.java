package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLogicalAndNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALWriteFIFONode;
import ch.epfl.vlsc.truffle.cal.parser.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Singleton class that provides an implementation for an action sub-tree.
 */
public class ActionVisitor extends CALParserBaseVisitor<Object> {

    private static ActionVisitor instance;

    private ActionVisitor() {}

    public static ActionVisitor getInstance() {
        if (instance == null) {
            instance = new ActionVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public ActionNode visitActionDefinition(CALParser.ActionDefinitionContext ctx) {
        // TODO Add support for action tags
        // TODO Add support for action delay

        ScopeEnvironment environment = ScopeEnvironment.getInstance();
        environment.pushScope();

        List<CALStatementNode> actionStatements = new ArrayList<>();

        if (ctx.localVariables != null) {
            actionStatements.addAll(CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables));
        }
        if (ctx.body != null) {
            actionStatements.addAll(StatementVisitor.getInstance().visitStatements(ctx.body).getStatements());
        }
        if (ctx.outputExpressions() != null) {
            actionStatements.addAll(CollectionVisitor.getInstance().visitOutputExpressions(ctx.outputExpressions()));
        }

        StmtBlockNode bodyNode = new StmtBlockNode(actionStatements.toArray(new CALStatementNode[0]));
        ActionBodyNode actionBodyNode = new ActionBodyNode(bodyNode);

        // Firing condition = Sufficient input tokens + Guards
        List<CALExpressionNode> firingConditions = new LinkedList<>();
        // FIXME Implement conditions for input tokens
        if (ctx.guards != null) {
            firingConditions.addAll(CollectionVisitor.getInstance().visitExpressions(ctx.guards));
        }

        CALExpressionNode firingCondition;
        if (firingConditions.size() > 0) {
            firingCondition = firingConditions.remove(0);
            for (CALExpressionNode condition : firingConditions)
                firingCondition = new CALBinaryLogicalAndNode(firingCondition, condition);
        } else {
            firingCondition = new BooleanLiteralNode(true);
        }

        ActionNode result = new ActionNode(environment.getLanguage(), environment.getCurrentScope().getFrame(), actionBodyNode, firingCondition, null, "unnamed action");

        environment.popScope();

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitInputPatterns(CALParser.InputPatternsContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitInputPattern(CALParser.InputPatternContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitChannelSelector(CALParser.ChannelSelectorContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPatterns(CALParser.PatternsContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPattern(CALParser.PatternContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitSubPatterns(CALParser.SubPatternsContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitSubPattern(CALParser.SubPatternContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPatternExpression(CALParser.PatternExpressionContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALWriteFIFONode visitOutputExpression(CALParser.OutputExpressionContext ctx) {
        // TODO Add support for channel selector
        // TODO Add support for multiple output expressions

        CALExpressionNode portQueue = ScopeEnvironment.getInstance().createReadNode(ctx.port.getText());
        List<CALExpressionNode> tokenExpressions = (ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitExpressions(ctx.expressions());

        // FIXME Implement repeated expressions

        return new CALWriteFIFONode(portQueue, tokenExpressions.get(0));
    }

    /**
     * {@inheritDoc}
     */
    @Override public ActionNode visitInitializationActionDefinition(CALParser.InitializationActionDefinitionContext ctx) {
        // TODO Add support for action tags
        // TODO Add support for action delay

        ScopeEnvironment environment = ScopeEnvironment.getInstance();
        environment.pushScope();

        List<CALStatementNode> actionStatements = new ArrayList<>();

        if (ctx.localVariables != null) {
            actionStatements.addAll(CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables));
        }
        if (ctx.body != null) {
            actionStatements.addAll(StatementVisitor.getInstance().visitStatements(ctx.body).getStatements());
        }
        if (ctx.outputExpressions() != null) {
            actionStatements.addAll(CollectionVisitor.getInstance().visitOutputExpressions(ctx.outputExpressions()));
        }

        StmtBlockNode bodyNode = new StmtBlockNode(actionStatements.toArray(new CALStatementNode[0]));
        ActionBodyNode actionBodyNode = new ActionBodyNode(bodyNode);

        // Firing condition = Sufficient input tokens + Guards
        List<CALExpressionNode> firingConditions = new LinkedList<>();
        // FIXME Implement conditions for input tokens
        if (ctx.guards != null) {
            firingConditions.addAll(CollectionVisitor.getInstance().visitExpressions(ctx.guards));
        }

        CALExpressionNode firingCondition;
        if (firingConditions.size() > 0) {
            firingCondition = firingConditions.remove(0);
            for (CALExpressionNode condition : firingConditions)
                firingCondition = new CALBinaryLogicalAndNode(firingCondition, condition);
        } else {
            firingCondition = new BooleanLiteralNode(true);
        }

        ActionNode result = new ActionNode(environment.getLanguage(), environment.getCurrentScope().getFrame(), actionBodyNode, firingCondition, null, "unnamed action");

        environment.popScope();

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<Token> visitActionTag(CALParser.ActionTagContext ctx) {
        return CollectionVisitor.getInstance().visitQualifiedID(ctx.qualifiedID());
    }

}