package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ExprIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.LetExprNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryLogicalNotNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryMinusNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.*;
import ch.epfl.vlsc.truffle.cal.parser.error.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import org.antlr.v4.runtime.Token;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Singleton class that provides an implementation for a expression sub-tree.
 */
public class ExpressionVisitor extends CALParserBaseVisitor<CALExpressionNode> {

    private static ExpressionVisitor instance;

    private ExpressionVisitor() {}

    public static ExpressionVisitor getInstance() {
        if (instance == null) {
            instance = new ExpressionVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitListComprehensionExprExpression(CALParser.ListComprehensionExprExpressionContext ctx) {
        return visitListComprehension(ctx.listComprehension());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitExprExpression(CALParser.ExprExpressionContext ctx) {
        return visit(ctx.expression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitFieldSelectorExpression(CALParser.FieldSelectorExpressionContext ctx) {
        // TODO Create Field Selector expression node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Field Selector expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitVariableExprExpression(CALParser.VariableExprExpressionContext ctx) {
        return visitVariableExpression(ctx.variableExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitMapComprehensionExprExpression(CALParser.MapComprehensionExprExpressionContext ctx) {
        return visitMapComprehension(ctx.mapComprehension());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitUnaryOperationExpression(CALParser.UnaryOperationExpressionContext ctx) {
        CALExpressionNode operand = (CALExpressionNode) visit(ctx.operand);
        String operator = ctx.operator.getText();

        switch (operator) {
            case "-":
                return CALUnaryMinusNodeGen.create(operand);
            case "!":
                // TODO: Change to CALUnaryBitNotNode
            case "not":
                return CALUnaryLogicalNotNodeGen.create(operand);
            case "rng":
                // TODO: Create CALUnaryMapRangeNode
            case "dom":
                // TODO: Create CALUnaryMapDomainNode
            case "#":
                // TODO: Create CALUnaryCollectionSizeNode
            default:
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unary operator \"" + operator + "\" is not yet supported");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitProcExprExpression(CALParser.ProcExprExpressionContext ctx) {
        return visitProcExpression(ctx.procExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitSymbolReferenceExprExpression(CALParser.SymbolReferenceExprExpressionContext ctx) {
        return visitSymbolReferenceExpression(ctx.symbolReferenceExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitLiteralExprExpression(CALParser.LiteralExprExpressionContext ctx) {
        return visit(ctx.literalExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitIndexerExpression(CALParser.IndexerExpressionContext ctx) {
        CALExpressionNode resultNode = visit(ctx.composite);
        Collection<CALExpressionNode> indices = CollectionVisitor.getInstance().visitExpressions(ctx.indices);
        for (CALExpressionNode indexNode: indices) {
            resultNode = ListReadNodeGen.create(resultNode, indexNode);
        }

        return resultNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitSetComprehensionExprExpression(CALParser.SetComprehensionExprExpressionContext ctx) {
        return visitSetComprehension(ctx.setComprehension());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitCaseExprExpression(CALParser.CaseExprExpressionContext ctx) {
        return visitCaseExpression(ctx.caseExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitBinaryOperationExpression(CALParser.BinaryOperationExpressionContext ctx) {
        CALExpressionNode operand1 = visit(ctx.operand1);
        String operator = ctx.operator.getText();
        CALExpressionNode operand2 = visit(ctx.operand2);

        switch (operator) {
            case "^":
                // TODO: Change to Power expression node
                return CALBinaryXorNodeGen.create(operand1, operand2);
            case "..":
                return ListRangeInitNodeGen.create(operand1, operand2);
            case "+":
                return CALBinaryAddNodeGen.create(operand1, operand2);
            case "-":
                return CALBinarySubNodeGen.create(operand1, operand2);
            case "*":
                return CALBinaryMulNodeGen.create(operand1, operand2);
            case "div":
                // TODO: Change to Integer Division expression node
            case "/":
                return CALBinaryDivNodeGen.create(operand1, operand2);
            case "%":
            case "mod":
                return CALBinaryModNodeGen.create(operand1, operand2);
            case "<<":
                return CALBinaryShiftLeftNodeGen.create(operand1, operand2);
            case ">>":
                return CALBinaryShiftRightNodeGen.create(operand1, operand2);
            case "<":
                return CALBinaryLessThanNodeGen.create(operand1, operand2);
            case "<=":
                return CALBinaryLessOrEqualNodeGen.create(operand1, operand2);
            case ">":
                return CALBinaryGreaterThanNodeGen.create(operand1, operand2);
            case ">=":
                return CALBinaryGreaterOrEqualNodeGen.create(operand1, operand2);
            case "==":
            case "=":
                return CALBinaryEqualNodeGen.create(operand1, operand2);
            case "!=":
                return CALBinaryNotEqualNodeGen.create(operand1, operand2);
            case "&":
                return CALBinaryBitAndNodeGen.create(operand1, operand2);
            case "|":
                return CALBinaryBitOrNodeGen.create(operand1, operand2);
            case "and":
                return new CALBinaryLogicalAndNode(operand1, operand2);
            case "or":
                return new CALBinaryLogicalOrNode(operand1, operand2);
            default:
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Binary operator \"" + operator + "\" is not yet supported");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitLambdaExprExpression(CALParser.LambdaExprExpressionContext ctx) {
        return visitLambdaExpression(ctx.lambdaExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitTypeAssertionExprExpression(CALParser.TypeAssertionExprExpressionContext ctx) {
        return visitTypeAssertionExpression(ctx.typeAssertionExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitIfExprExpression(CALParser.IfExprExpressionContext ctx) {
        return visitIfExpression(ctx.ifExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitLetExprExpression(CALParser.LetExprExpressionContext ctx) {
        return visitLetExpression(ctx.letExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitCallExprExpression(CALParser.CallExprExpressionContext ctx) {
        return visitCallExpression(ctx.callExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitIntegerLiteralExpression(CALParser.IntegerLiteralExpressionContext ctx) {
        try {
            long value = Long.parseLong(ctx.IntegerLiteral().getText());
            return new LongLiteralNode(value);
        } catch (NumberFormatException e) {
            return new BigIntegerLiteralNode(new BigInteger(ctx.IntegerLiteral().getText()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitFloatingPointLiteralExpression(CALParser.FloatingPointLiteralExpressionContext ctx) {
        // TODO Create Float Literal node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Float literal expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public BooleanLiteralNode visitBooleanLiteralExpression(CALParser.BooleanLiteralExpressionContext ctx) {
        return new BooleanLiteralNode(Boolean.parseBoolean(ctx.BooleanLiteral().getText()));
    }

    /**
     * {@inheritDoc}
     */
    @Override public StringLiteralNode visitCharacterLiteralExpression(CALParser.CharacterLiteralExpressionContext ctx) {
        // TODO Create Char Literal node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Char literal expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public StringLiteralNode visitStringLiteralExpression(CALParser.StringLiteralExpressionContext ctx) {
        return new StringLiteralNode(ctx.StringLiteral().getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override public NullLiteralNode visitNullLiteralExpression(CALParser.NullLiteralExpressionContext ctx) {
        return new NullLiteralNode();
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitVariableExpression(CALParser.VariableExpressionContext ctx) {
        if (ctx.isOld != null) {
            // TODO Add support for old variable
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Old variable expression is not yet supported");
        }
        return ScopeEnvironment.getInstance().createReadNode(ctx.variable().getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitSymbolReferenceExpression(CALParser.SymbolReferenceExpressionContext ctx) {
        // TODO Create Symbol Reference expression node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Symbol Reference expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public ExprIfNode visitIfExpression(CALParser.IfExpressionContext ctx) {
        CALExpressionNode conditionNode = visit(ctx.condition);
        CALExpressionNode thenNode = visit(ctx.then);

        CALExpressionNode elseNode;
        if (ctx.elseIf != null) {
            elseNode = visitElseIfExpression(ctx.elseIf);
        } else {
            elseNode = visit(ctx.elze);
        }

        return new ExprIfNode(conditionNode, thenNode, elseNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public ExprIfNode visitElseIfExpression(CALParser.ElseIfExpressionContext ctx) {
        CALExpressionNode conditionNode = visit(ctx.condition);
        CALExpressionNode thenNode = visit(ctx.then);

        CALExpressionNode elseNode;
        if (ctx.elseIf != null) {
            elseNode = visitElseIfExpression(ctx.elseIf);
        } else {
            elseNode = visit(ctx.elze);
        }

        return new ExprIfNode(conditionNode, thenNode, elseNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public LetExprNode visitLetExpression(CALParser.LetExpressionContext ctx) {
        ScopeEnvironment.getInstance().pushScope(true, false);

        List<CALExpressionNode> localVariableNodes = (ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables);
        StmtBlockNode headNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
        CALExpressionNode bodyNode = visit(ctx.body);
        LetExprNode letNode = new LetExprNode(headNode, bodyNode);

        ScopeEnvironment.getInstance().popScope();

        return letNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public LambdaNode visitLambdaExpression(CALParser.LambdaExpressionContext ctx) {
        StmtBlockNode headNode;
        CALExpressionNode bodyNode;

        if (ctx.isConst != null) {
            // TODO Add support for constant lambda
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Constant lambda expression is not yet supported");
        }
        if (ctx.type() != null) {
            // TODO Add support for lambda return type
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Lambda expression's return type is not yet supported");
        }

        ScopeEnvironment.getInstance().pushScope(true);

        if (ctx.formalParameters() != null) {
            List<CALStatementNode> formalParameterNodes = (ArrayList<CALStatementNode>) CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headNode = new StmtBlockNode(formalParameterNodes.toArray(new CALStatementNode[0]));
        } else {
            headNode = null;
        }

        if (ctx.localVariables != null) {
            ScopeEnvironment.getInstance().pushScope(true, false);

            List<CALExpressionNode> localVariableNodes = (ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables);
            StmtBlockNode letHeadNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
            CALExpressionNode letBodyNode = visit(ctx.body);

            bodyNode = new LetExprNode(letHeadNode, letBodyNode);

            ScopeEnvironment.getInstance().popScope();
        } else {
            bodyNode = visit(ctx.body);
        }

        ReturnsLastBodyNode lambdaBodyNode = new ReturnsLastBodyNode(headNode, bodyNode);
        CALRootNode lambdaBodyRootNode = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                lambdaBodyNode,
                null,
                ScopeEnvironment.generateLambdaName()
        );
        LambdaNode lambdaNode = new LambdaNode(lambdaBodyRootNode);

        ScopeEnvironment.getInstance().popScope();

        return lambdaNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitProcExpression(CALParser.ProcExpressionContext ctx) {
        // TODO Create Proc expression node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Proc expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitSetComprehension(CALParser.SetComprehensionContext ctx) {
        // TODO Create Set Init node and Unknown Size Set Init node => see #visitListComprehension
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Set comprehension expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitListComprehension(CALParser.ListComprehensionContext ctx) {
        if (ctx.generators() == null) {
            // Simple list collection expression
            CALExpressionNode[] valueNodes;
            if (ctx.computations != null) {
                valueNodes = CollectionVisitor.getInstance().visitExpressions(ctx.computations).toArray(new CALExpressionNode[0]);
            } else {
                valueNodes = new CALExpressionNode[0];
            }

            return new ListInitNode(valueNodes);
        } else {
            // Comprehensions w/ generators
            CALExpressionNode variableNode = null;
            CALExpressionNode collectionNode = null;

            if (ctx.generators().generator().size() > 1) {
                // TODO Add support for multiple generators
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx.generators(), "Multiple comprehension generators are not yet supported");
            }
            for (CALParser.GeneratorContext generatorCtx: ctx.generators().generator()) {
                if (generatorCtx.generatorBody().variables.size() > 1) {
                    // TODO Add support for multiple variables in a generator
                    throw new CALParseError(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Multiple variables in a comprehension generator are not yet supported");
                }
                for (Token variable: generatorCtx.generatorBody().variables) {
                    variableNode = ScopeEnvironment.getInstance().createWriteNode(variable.getText(), null);

                    if (!(variableNode instanceof CALWriteLocalVariableNode)) {
                        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Variable name re-use in a comprehension generator is not yet supported");
                    }
                }
                List<CALExpressionNode> expressionNodes = ((ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitExpressions(generatorCtx.generatorBody().expressions()));
                collectionNode = expressionNodes.get(0);

                if (expressionNodes.size() > 1) {
                    // TODO Add support for generator filters
                    throw new CALParseError(ScopeEnvironment.getInstance().getSource(), generatorCtx.generatorBody(), "Comprehension generator filters are not yet supported");
                }
            }

            List<CALExpressionNode> computationExpressions = (ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitExpressions(ctx.computations);
            if (computationExpressions.size() > 1) {
                // TODO Add support for multiple computation expressions
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx.computations, "Multiple comprehension computation expressions are not yet supported");
            }
            CALExpressionNode computationExpression = computationExpressions.get(0);

            /**
             * Comprehension expression is translated into a block of statements:
             *      $tempList = [];
             *      $comprehensionCounter = 0;
             *      foreach (<Variable> in <Collection>) {
             *          $tempList[$comprehensionCounter] = <Computation Expression>; // Note: Computation Expression use Variable
             *          $comprehensionCounter++;
             *      }
             *      return $tempList;
             */
            CALStatementNode[] comprehensionStatementNodes = new CALStatementNode[3];
            comprehensionStatementNodes[0] = ScopeEnvironment.getInstance().createWriteNode("$tempList", new UnknownSizeListInitNode()); // $tempList = [];
            comprehensionStatementNodes[1] = ScopeEnvironment.getInstance().createWriteNode("$comprehensionCounter", new LongLiteralNode(0)); // $comprehensionCounter = 0;

            CALStatementNode[] loopStatementNodes = new CALStatementNode[2];
            loopStatementNodes[0] = ListWriteNodeGen.create(
                    ScopeEnvironment.getInstance().createReadNode("$tempList"),
                    ScopeEnvironment.getInstance().createReadNode("$comprehensionCounter"),
                    computationExpression
            ); // $tempList[$comprehensionCounter] = <Computation Expression>;
            loopStatementNodes[1] = ScopeEnvironment.getInstance().createWriteNode(
                    "$comprehensionCounter",
                    CALBinaryAddNodeGen.create(ScopeEnvironment.getInstance().createReadNode("$comprehensionCounter"), new LongLiteralNode(1))
            ); // $comprehensionCounter++;
            CALStatementNode loopBodyNode = new StmtBlockNode(loopStatementNodes);

            comprehensionStatementNodes[2] = ForeacheNodeGen.create(loopBodyNode, (CALWriteLocalVariableNode) variableNode, collectionNode); // foreach (<Variable> in <Collection>) { ... }
            CALStatementNode comprehensionBodyNode = new StmtBlockNode(comprehensionStatementNodes);

            return new ReturnsLastBodyNode(comprehensionBodyNode, ScopeEnvironment.getInstance().createReadNode("$tempList")); // ... return $tempList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitMapComprehension(CALParser.MapComprehensionContext ctx) {
        // TODO Create Map Init node and Unknown Size Map Init node => see #visitListComprehension
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Map comprehension expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitMappings(CALParser.MappingsContext ctx) {
        // TODO First resolve #visitMapComprehension
        // Note: Unreachable for now
        return super.visitMappings(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitMapping(CALParser.MappingContext ctx) {
        // TODO First resolve #visitMappings
        // Note: Unreachable for now
        return super.visitMapping(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitTypeAssertionExpression(CALParser.TypeAssertionExpressionContext ctx) {
        // TODO Create Type Assertion expression node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Type Assertion expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitCaseExpression(CALParser.CaseExpressionContext ctx) {
        // TODO Create Case expression node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Case expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitAlternativeExpression(CALParser.AlternativeExpressionContext ctx) {
        // TODO First resolve #visitCaseExpression
        // Note: Unreachable for now
        return super.visitAlternativeExpression(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitCallExpression(CALParser.CallExpressionContext ctx) {
        CALExpressionNode functionNode = ExpressionVisitor.getInstance().visitVariableExpression(ctx.function);

        CALExpressionNode[] argumentNodes;
        if (ctx.arguments != null) {
            argumentNodes = CollectionVisitor.getInstance().visitExpressions(ctx.arguments).toArray(new CALExpressionNode[0]);
        } else {
            argumentNodes = new CALExpressionNode[0];
        }

        return new CALInvokeNode(functionNode, argumentNodes);
    }
}