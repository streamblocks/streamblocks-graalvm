package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
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
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.*;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import com.oracle.truffle.api.source.SourceSection;
import org.antlr.v4.runtime.Token;
import se.lth.cs.tycho.ir.QID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton class that provides an implementation for an action sub-tree.
 */
public class ActionVisitor extends CALParserBaseVisitor<Object> {

    private static ActionVisitor instance;
    public final static String UNNAMED_ACTION = "unnamed action";

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
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Action delay is not yet supported");
            }
        }

        ScopeEnvironment.getInstance().pushScope();

        QualifiedID actionName;
        boolean isQIDTagged;
        if (ctx.actionTag() != null) {
            actionName = CollectionVisitor.qualifiedIdCreator(visitActionTag(ctx.actionTag()));
            isQIDTagged = true;
        } else {
            actionName = QualifiedID.of(UNNAMED_ACTION); // Note: Compliant with corresponding transformer, but probably unnecessary
            isQIDTagged = false;
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
        bodyNode.setUnavailableSourceSection();
        bodyNode.addStatementTag();

        ActionBodyNode actionBodyNode = new ActionBodyNode(bodyNode);
        actionBodyNode.setUnavailableSourceSection();
        actionBodyNode.addStatementTag();

        // Firing condition = Sufficient number of input tokens + Guards
        List<CALExpressionNode> firingConditions = new LinkedList<>();
        if (ctx.inputPatterns() != null) {
            for (CALParser.InputPatternContext patternCtx: ctx.inputPatterns().inputPattern()) {
                // Note: All necessary checks are performed during input patterns visit (previously called)
                CALExpressionNode portQueue = ScopeEnvironment.getInstance().createReadNode(patternCtx.port.getText(), ScopeEnvironment.getInstance().createSourceSection(patternCtx));
                CALExpressionNode minimumQueueSizeNode;
                if (patternCtx.repeat == null) {
                    minimumQueueSizeNode = new LongLiteralNode(patternCtx.patterns() != null ? patternCtx.patterns().pattern().size() : 1);
                    minimumQueueSizeNode.setUnavailableSourceSection();
                    minimumQueueSizeNode.addExpressionTag();
                } else {
                    minimumQueueSizeNode = CALBinaryMulNodeGen.create(ExpressionVisitor.getInstance().visit(patternCtx.repeat), new LongLiteralNode(patternCtx.patterns() != null ? patternCtx.patterns().pattern().size() : 1));
                }

                CALFIFOSizeNode portQueueSizeNode = new CALFIFOSizeNode(portQueue);
                portQueueSizeNode.setUnavailableSourceSection();
                portQueueSizeNode.addExpressionTag();

                CALBinaryGreaterOrEqualNode firingCondition = CALBinaryGreaterOrEqualNodeGen.create(portQueueSizeNode, minimumQueueSizeNode);
                firingCondition.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(patternCtx));
                firingCondition.addExpressionTag();

                firingConditions.add(firingCondition);
            }
        }
        if (ctx.guards != null) {
            firingConditions.addAll(CollectionVisitor.getInstance().visitExpressions(ctx.guards));
        }

        CALExpressionNode firingCondition;
        if (firingConditions.size() > 0) {
            firingCondition = firingConditions.remove(0);
            for (CALExpressionNode condition : firingConditions) {
                SourceSection newSourceSection = ScopeEnvironment.getInstance().getSource().createSection(
                        firingCondition.getSourceSection().getStartLine(),
                        firingCondition.getSourceSection().getStartColumn(),
                        condition.getSourceSection().getEndLine(),
                        condition.getSourceSection().getEndColumn()
                );
                firingCondition = new CALBinaryLogicalAndNode(firingCondition, condition);
                firingCondition.setSourceSection(newSourceSection);
                firingCondition.addExpressionTag();
            }
        } else {
            firingCondition = new BooleanLiteralNode(true);
            firingCondition.setUnavailableSourceSection();
            firingCondition.addExpressionTag();
        }

        ActionNode actionNode = new ActionNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                actionBodyNode,
                firingCondition,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                actionName,
                isQIDTagged
        );
        // TODO Add RootTag / CallTag for actionNode

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

        if (ctx.channelSelector() != null) {
            // TODO Add support for channel selector
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Channel selector in an input pattern is not yet supported");
        }

        // Note: Custom source section to precisely specify a port token
        CALExpressionNode portQueue = ScopeEnvironment.getInstance().createReadNode(
                ctx.port.getText(),
                ScopeEnvironment.getInstance().getSource().createSection(ctx.port.getLine(), ctx.port.getCharPositionInLine() + 1, ctx.port.getText().length())
        );

        if (ctx.patterns() == null || ctx.patterns().pattern().size() == 0) {
            return null;
        } else if (ctx.patterns().pattern().stream().allMatch(x -> x instanceof CALParser.SimplePatternContext)) {
            if (ctx.repeat == null) {
                // Simple input pattern
                StmtBlockNode readBlock = new StmtBlockNode( ctx.patterns().pattern().stream().map(x -> {
                    CALExpressionNode valueNode = new CALReadFIFONode(portQueue);
                    valueNode.setUnavailableSourceSection();
                    valueNode.addExpressionTag();
                    return ScopeEnvironment.getInstance().createNewVariableWriteNode(((CALParser.SimplePatternContext) x).variable().name.getText(), valueNode, ScopeEnvironment.getInstance().createSourceSection(x));
                }).toArray(CALStatementNode[]::new));
                ReturnsLastBodyNode returnVal = new ReturnsLastBodyNode(readBlock);
                returnVal.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
                returnVal.addStatementTag();
                return returnVal;
            } else {
                CALExpressionNode repeatValueNode = ExpressionVisitor.getInstance().visit(ctx.repeat);

                /**
                 * Repeated input pattern is translated into a block of statements:
                 *      $list = [];
                 *      $counter = 0;
                 *      for (<0..Repeat Expression - 1>) {
                 *          $list[$counter] = <Port Queue>.remove(0);
                 *          $counter++;
                 *      }
                 *      <Pattern Variable> = $list;
                 */
                String counterVariableName = ScopeEnvironment.generateVariableName();

                CALStatementNode[] inputPatternStatementNodes = new CALStatementNode[3];

                // inputPatterns.forEach(varName: varName = [])
                inputPatternStatementNodes[0] = new StmtBlockNode(ctx.patterns().pattern().stream().map(x ->
                    ScopeEnvironment.getInstance().createNewVariableWriteNode(
                            ((CALParser.SimplePatternContext) x).variable().name.getText(),
                            new UnknownSizeListInitNode(),
                            ScopeEnvironment.getInstance().getSource().createUnavailableSection()
                    )
                ).toArray(CALStatementNode[]::new));


                inputPatternStatementNodes[1] = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                        counterVariableName,
                        new LongLiteralNode(0),
                        ScopeEnvironment.getInstance().getSource().createUnavailableSection()
                ); // $counter = 0;

                CALExpressionNode rangeListNode = ListRangeInitNodeGen.create(new LongLiteralNode(0), CALBinarySubNodeGen.create(repeatValueNode, new LongLiteralNode(1))); // <0..Repeat Expression - 1>
                rangeListNode.setUnavailableSourceSection();
                rangeListNode.addExpressionTag();

                CALStatementNode[] loopStatementNodes = new CALStatementNode[2];

                CALReadFIFONode portQueueNode = new CALReadFIFONode(portQueue); // <Port Queue>.remove(0);
                portQueueNode.setSourceSection(portQueue.getSourceSection());
                portQueueNode.addExpressionTag();

                loopStatementNodes[0] = new StmtBlockNode(ctx.patterns().pattern().stream().map(x ->
                    ListWriteNodeGen.create(
                        ScopeEnvironment.getInstance().createReadNode(((CALParser.SimplePatternContext) x).variable().name.getText(), ScopeEnvironment.getInstance().getSource().createUnavailableSection()),
                        ScopeEnvironment.getInstance().createReadNode(counterVariableName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()),
                        portQueueNode
                    )
                ).toArray(CALStatementNode[]::new)); // $list[$counter] = <Port Queue>.remove(0);

                loopStatementNodes[0].setUnavailableSourceSection();
                loopStatementNodes[0].addStatementTag();

                LongLiteralNode oneLiteralNode = new LongLiteralNode(1);
                oneLiteralNode.setUnavailableSourceSection();
                oneLiteralNode.addExpressionTag();

                CALBinaryAddNode incrementCounterNode = CALBinaryAddNodeGen.create(
                        ScopeEnvironment.getInstance().createReadNode(counterVariableName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()),
                        oneLiteralNode
                );
                incrementCounterNode.setUnavailableSourceSection();
                incrementCounterNode.addExpressionTag();

                loopStatementNodes[1] = ScopeEnvironment.getInstance().createExistingVariableWriteNode(
                        counterVariableName,
                        incrementCounterNode,
                        ScopeEnvironment.getInstance().getSource().createUnavailableSection()
                ); // $counter++;

                CALStatementNode loopBodyNode = new StmtBlockNode(loopStatementNodes);
                loopBodyNode.setUnavailableSourceSection();
                loopBodyNode.addStatementTag();

                inputPatternStatementNodes[2] = ForeacheNodeGen.create(loopBodyNode, null, rangeListNode); // for (<0..Repeat Expression - 1>) { ... }
                inputPatternStatementNodes[2].setUnavailableSourceSection();
                inputPatternStatementNodes[2].addStatementTag();

                CALStatementNode inputPatternBodyNode = new StmtBlockNode(inputPatternStatementNodes);
                inputPatternBodyNode.setUnavailableSourceSection();
                inputPatternBodyNode.addStatementTag();

                CALExpressionNode inputPatternReturnNode = new ReturnsLastBodyNode(inputPatternBodyNode);
                inputPatternReturnNode.addExpressionTag();
                inputPatternReturnNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
                return inputPatternReturnNode;
            }
        } else {
            // TODO Add support for complex patterns in an input pattern
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Input pattern with multiple patterns is not yet supported");
        }
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
        // Note: Custom source section to precisely specify a port token
        CALExpressionNode portQueue = ScopeEnvironment.getInstance().createReadNode(
                ctx.port.getText(),
                ScopeEnvironment.getInstance().getSource().createSection(ctx.port.getLine(), ctx.port.getCharPositionInLine() + 1, ctx.port.getText().length())
        );

        List<CALExpressionNode> tokenExpressions = (ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitExpressions(ctx.expressions());
        // CALExpressionNode tokenExpression = tokenExpressions.get(0);

        if (ctx.channelSelector() != null) {
            // TODO Add support for channel selector
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Channel selector in an output expression is not yet supported");
        }

        if (ctx.repeat == null) {
            // Simple output expression
            StmtBlockNode simpleOutputExpressionNode = new StmtBlockNode(tokenExpressions.stream().map(expr -> new CALWriteFIFONode(portQueue, expr)).toArray(CALWriteFIFONode[]::new));
            simpleOutputExpressionNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            simpleOutputExpressionNode.addStatementTag();

            return simpleOutputExpressionNode;
        } else {
            CALExpressionNode repeatValueNode = ExpressionVisitor.getInstance().visit(ctx.repeat);

            /**
             * Repeated output expression is translated into a block of statements:
             *      $counter = 0;
             *      for (<0..Repeat Expression - 1>) {
             *          <Port Queue>.add(<Token Expression>[$counter]);
             *          $counter++;
             *      }
             */
            String counterVariableName = ScopeEnvironment.generateVariableName();

            CALStatementNode[] outputExpressionStatementNodes = new CALStatementNode[2];
            outputExpressionStatementNodes[0] = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                    counterVariableName,
                    new LongLiteralNode(0),
                    ScopeEnvironment.getInstance().getSource().createUnavailableSection()
            ); // $counter = 0;

            CALExpressionNode rangeListNode = ListRangeInitNodeGen.create(new LongLiteralNode(0), CALBinarySubNodeGen.create(repeatValueNode, new LongLiteralNode(1))); // <0..Repeat Expression - 1>
            rangeListNode.setUnavailableSourceSection();
            rangeListNode.addExpressionTag();

            CALStatementNode[] loopStatementNodes = new CALStatementNode[2];

            List<ListReadNode> tokenExpressionElementNodes = tokenExpressions.stream().map(expr -> {
                ListReadNode listReadNode = ListReadNodeGen.create(
                        expr,
                        ScopeEnvironment.getInstance().createReadNode(counterVariableName, ScopeEnvironment.getInstance().getSource().createUnavailableSection())
                );
                listReadNode.setSourceSection(portQueue.getSourceSection());
                listReadNode.addExpressionTag();
                return listReadNode;
            }).collect(Collectors.toList());


            loopStatementNodes[0] = new StmtBlockNode(tokenExpressionElementNodes.stream().map(expr -> new CALWriteFIFONode(portQueue, expr)).toArray(CALWriteFIFONode[]::new)); // <Port Queue>.add(<Token Expression>[$counter]);
            loopStatementNodes[0].setUnavailableSourceSection();
            loopStatementNodes[0].addStatementTag();

            LongLiteralNode oneLiteralNode = new LongLiteralNode(1);
            oneLiteralNode.setUnavailableSourceSection();
            oneLiteralNode.addExpressionTag();

            CALBinaryAddNode incrementCounterNode = CALBinaryAddNodeGen.create(
                    ScopeEnvironment.getInstance().createReadNode(counterVariableName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()),
                    oneLiteralNode
            );
            incrementCounterNode.setUnavailableSourceSection();
            incrementCounterNode.addExpressionTag();

            loopStatementNodes[1] = ScopeEnvironment.getInstance().createExistingVariableWriteNode(
                    counterVariableName,
                    incrementCounterNode,
                    ScopeEnvironment.getInstance().getSource().createUnavailableSection()
            ); // $counter++;
            CALStatementNode loopBodyNode = new StmtBlockNode(loopStatementNodes);

            outputExpressionStatementNodes[1] = ForeacheNodeGen.create(loopBodyNode, null, rangeListNode); // for (<0..Repeat Expression - 1>) { ... }
            outputExpressionStatementNodes[1].setUnavailableSourceSection();
            outputExpressionStatementNodes[1].addStatementTag();

            StmtBlockNode repeatedOutputExpressionNode = new StmtBlockNode(outputExpressionStatementNodes);
            repeatedOutputExpressionNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            repeatedOutputExpressionNode.addStatementTag();

            return repeatedOutputExpressionNode;
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