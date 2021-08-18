package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFIFOSizeNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALReadFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALWriteFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListRangeInitNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
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
        if (ctx.delay != null) {
            // TODO Add support for action delay
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Action delay is not yet supported");
        }

        ScopeEnvironment.getInstance().pushScope();

        String actionName;
        if (ctx.actionTag() != null) {
            actionName = CollectionVisitor.qualifiedIdToString(visitActionTag(ctx.actionTag()));
        } else {
            actionName = "unnamed action"; // Note: Compliant with corresponding transformer, but probably unnecessary
        }

        List<CALStatementNode> actionStatements = new ArrayList<>();
        if (ctx.inputPatterns() != null) {
            actionStatements.addAll(CollectionVisitor.getInstance().visitInputPatterns(ctx.inputPatterns()));
        }
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

        // Firing condition = Sufficient number of input tokens + Guards
        List<CALExpressionNode> firingConditions = new LinkedList<>();
        if (ctx.inputPatterns() != null) {
            for (CALParser.InputPatternContext patternCtx: ctx.inputPatterns().inputPattern()) {
                // Note: All necessary checks are performed during input patterns visit (previously called)
                CALExpressionNode portQueue = ScopeEnvironment.getInstance().createReadNode(patternCtx.port.getText());
                CALExpressionNode minimumQueueSizeNode;
                if (patternCtx.repeat == null) {
                    minimumQueueSizeNode = new LongLiteralNode(1);
                } else {
                    minimumQueueSizeNode = ExpressionVisitor.getInstance().visit(patternCtx.repeat);
                }

                firingConditions.add(CALBinaryGreaterOrEqualNodeGen.create(new CALFIFOSizeNode(portQueue), minimumQueueSizeNode));
            }
        }
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

        ActionNode actionNode = new ActionNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                actionBodyNode,
                firingCondition,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                actionName
        );

        ScopeEnvironment.getInstance().popScope();

        return actionNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitInputPattern(CALParser.InputPatternContext ctx) {
        if (ctx.port == null) {
            // TODO Add support for implicit input pattern
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Implicit input pattern is not yet supported");
        }
        CALExpressionNode portQueue = ScopeEnvironment.getInstance().createReadNode(ctx.port.getText());

        if (ctx.patterns() == null) {
            return null;
        } else if (ctx.patterns().pattern().size() > 1) {
            // TODO Add support for multiple patterns in an input pattern
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Input pattern with multiple patterns is not yet supported");
        } else if (!(ctx.patterns().pattern().get(0) instanceof CALParser.SimplePatternContext)) {
            // TODO Add support for complex patterns in an input pattern
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Input pattern with multiple patterns is not yet supported");
        }
        String patternVariableName = ((CALParser.SimplePatternContext) ctx.patterns().pattern().get(0)).variable().name.getText();

        if (ctx.channelSelector() != null) {
            // TODO Add support for channel selector
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Channel selector in an input pattern is not yet supported");
        }

        CALExpressionNode valueNode;
        if (ctx.repeat == null) {
            // Simple input pattern
            valueNode = new CALReadFIFONode(portQueue);
        } else {
            CALExpressionNode repeatValueNode = ExpressionVisitor.getInstance().visit(ctx.repeat);

            /**
             * Repeated input pattern is translated into a block of statements:
             *      $tempList = [];
             *      $comprehensionCounter = 0;
             *      for (<0..Repeat Expression - 1>) {
             *          $tempList[$comprehensionCounter] = <Port Queue>.remove(0);
             *          $comprehensionCounter++;
             *      }
             *      <Pattern Variable> = $tempList;
             */
            CALStatementNode[] inputPatternStatementNodes = new CALStatementNode[3];
            inputPatternStatementNodes[0] = ScopeEnvironment.getInstance().createWriteNode("$tempList", new UnknownSizeListInitNode()); // $tempList = [];
            inputPatternStatementNodes[1] = ScopeEnvironment.getInstance().createWriteNode("$comprehensionCounter", new LongLiteralNode(0)); // $comprehensionCounter = 0;
            CALExpressionNode rangeListNode = ListRangeInitNodeGen.create(new LongLiteralNode(0), CALBinarySubNodeGen.create(repeatValueNode, new LongLiteralNode(1))); // <0..Repeat Expression - 1>

            CALStatementNode[] loopStatementNodes = new CALStatementNode[2];
            loopStatementNodes[0] = ListWriteNodeGen.create(
                    ScopeEnvironment.getInstance().createReadNode("$tempList"),
                    ScopeEnvironment.getInstance().createReadNode("$comprehensionCounter"),
                    new CALReadFIFONode(portQueue)
            ); // <Port Queue>.add(<Token Expression>[$comprehensionCounter]);
            loopStatementNodes[1] = ScopeEnvironment.getInstance().createWriteNode(
                    "$comprehensionCounter",
                    CALBinaryAddNodeGen.create(ScopeEnvironment.getInstance().createReadNode("$comprehensionCounter"), new LongLiteralNode(1))
            ); // $comprehensionCounter++;
            CALStatementNode loopBodyNode = new StmtBlockNode(loopStatementNodes);

            inputPatternStatementNodes[2] = ForeacheNodeGen.create(loopBodyNode, null, rangeListNode); // for (<0..Repeat Expression - 1>) { ... }

            valueNode = new ReturnsLastBodyNode(new StmtBlockNode(inputPatternStatementNodes), ScopeEnvironment.getInstance().createReadNode("$tempList")); // ... return $tempList;
        }

        return ScopeEnvironment.getInstance().createWriteNode(patternVariableName, valueNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitChannelSelector(CALParser.ChannelSelectorContext ctx) {
        // TODO First resolve #visitInputPattern and #visitOutputExpression
        // Note: Unreachable for now
        return super.visitChannelSelector(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitSimplePattern(CALParser.SimplePatternContext ctx) {
        // Note: Unreachable for now, only children directly accessed in #visitInputPattern
        return super.visitSimplePattern(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitComplexPattern(CALParser.ComplexPatternContext ctx) {
        // TODO First resolve #visitInputPattern
        // Note: Unreachable for now
        return super.visitComplexPattern(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitSubPattern(CALParser.SubPatternContext ctx) {
        // TODO First resolve #visitInputPattern
        // Note: Unreachable for now
        return super.visitSubPattern(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPatternExpression(CALParser.PatternExpressionContext ctx) {
        // TODO First resolve #visitInputPattern
        // Note: Unreachable for now
        return super.visitPatternExpression(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitOutputExpression(CALParser.OutputExpressionContext ctx) {
        if (ctx.port == null) {
            // TODO Add support for implicit output expressions
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Implicit output expression is not yet supported");
        }
        CALExpressionNode portQueue = ScopeEnvironment.getInstance().createReadNode(ctx.port.getText());


        if (ctx.expressions().expression().size() > 1) {
            // TODO Add support for multiple token expressions in an output expression
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Output expression with multiple tokens is not yet supported");
        }
        List<CALExpressionNode> tokenExpressions = (ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitExpressions(ctx.expressions());
        CALExpressionNode tokenExpression = tokenExpressions.get(0);

        if (ctx.channelSelector() != null) {
            // TODO Add support for channel selector
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Channel selector in an output expression is not yet supported");
        }

        if (ctx.repeat == null) {
            // Simple output expression
            return new CALWriteFIFONode(portQueue, tokenExpression);
        } else {
            CALExpressionNode repeatValueNode = ExpressionVisitor.getInstance().visit(ctx.repeat);

            /**
             * Repeated output expression is translated into a block of statements:
             *      $comprehensionCounter = 0;
             *      for (<0..Repeat Expression - 1>) {
             *          <Port Queue>.add(<Token Expression>[$comprehensionCounter]);
             *          $comprehensionCounter++;
             *      }
             */
            CALStatementNode[] outputExpressionStatementNodes = new CALStatementNode[2];
            outputExpressionStatementNodes[0] = ScopeEnvironment.getInstance().createWriteNode("$comprehensionCounter", new LongLiteralNode(0)); // $comprehensionCounter = 0;
            CALExpressionNode rangeListNode = ListRangeInitNodeGen.create(new LongLiteralNode(0), CALBinarySubNodeGen.create(repeatValueNode, new LongLiteralNode(1))); // <0..Repeat Expression - 1>

            CALStatementNode[] loopStatementNodes = new CALStatementNode[2];
            loopStatementNodes[0] = new CALWriteFIFONode(
                    portQueue,
                    ListReadNodeGen.create(tokenExpression, ScopeEnvironment.getInstance().createReadNode("$comprehensionCounter"))
            ); // <Port Queue>.add(<Token Expression>[$comprehensionCounter]);
            loopStatementNodes[1] = ScopeEnvironment.getInstance().createWriteNode(
                    "$comprehensionCounter",
                    CALBinaryAddNodeGen.create(ScopeEnvironment.getInstance().createReadNode("$comprehensionCounter"), new LongLiteralNode(1))
            ); // $comprehensionCounter++;
            CALStatementNode loopBodyNode = new StmtBlockNode(loopStatementNodes);

            outputExpressionStatementNodes[1] = ForeacheNodeGen.create(loopBodyNode, null, rangeListNode); // for (<0..Repeat Expression - 1>) { ... }

            return new StmtBlockNode(outputExpressionStatementNodes);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitInitializationActionDefinition(CALParser.InitializationActionDefinitionContext ctx) {
        // TODO First resolve ActorVisitor#visitActorDeclaration => implement similar to #visitActionDefinition (w/o input patterns)
        // Note: Unreachable for now
        return super.visitInitializationActionDefinition(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<Token> visitActionTag(CALParser.ActionTagContext ctx) {
        return CollectionVisitor.getInstance().visitQualifiedID(ctx.qualifiedID());
    }

}