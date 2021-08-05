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
import ch.epfl.vlsc.truffle.cal.parser.CALNodeFactoryContext;
import ch.epfl.vlsc.truffle.cal.parser.antlr.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.antlr.CALParserBaseVisitor;
import org.antlr.v4.runtime.Token;
import org.graalvm.collections.Pair;

import java.math.BigInteger;
import java.util.List;

/**
 * This class provides an implementation for a expression parser sub-tree.
 */
public class CALParserExpressionVisitor extends CALParserBaseVisitor<CALExpressionNode> {

    private CALNodeFactoryContext context;

    public CALParserExpressionVisitor(CALNodeFactoryContext context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitListComprehensionExprExpression(CALParser.ListComprehensionExprExpressionContext ctx) {
        return visit(ctx.listComprehension());
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
        // TODO Create CALFieldSelectorNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitVariableExprExpression(CALParser.VariableExprExpressionContext ctx) {
        return visit(ctx.variableExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitMapComprehensionExprExpression(CALParser.MapComprehensionExprExpressionContext ctx) {
        return visit(ctx.mapComprehension());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitUnaryOperationExpression(CALParser.UnaryOperationExpressionContext ctx) {
        CALExpressionNode operand = visit(ctx.operand);
        Token operator = ctx.operator;

        switch (operator.getText()) {
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
                throw new Error("Unary operator " + operator.getText() + " is not (yet) supported.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitProcExprExpression(CALParser.ProcExprExpressionContext ctx) {
        return visit(ctx.procExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitSymbolReferenceExprExpression(CALParser.SymbolReferenceExprExpressionContext ctx) {
        return visit(ctx.symbolReferenceExpression());
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
        CALExpressionNode expression = visit(ctx.composite);
        List<CALExpressionNode> indices = visit(ctx.indices);

        for (CALExpressionNode index: indices) {
            expression = ListReadNodeGen.create(expression, index);
        }

        return expression;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitSetComprehensionExprExpression(CALParser.SetComprehensionExprExpressionContext ctx) {
        return visit(ctx.setComprehension());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitCaseExprExpression(CALParser.CaseExprExpressionContext ctx) {
        return visit(ctx.caseExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitBinaryOperationExpression(CALParser.BinaryOperationExpressionContext ctx) {
        CALExpressionNode operand1 = visit(ctx.operand1);
        Token operator = ctx.operator;
        CALExpressionNode operand2 = visit(ctx.operand2);

        switch (operator.getText()) {
            case "^":
                // TODO: Change to CALBinaryPowerNode
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
                // TODO: Change to CALBinaryIntDivNode
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
                throw new Error("Binary operator " + operator.getText() + " is not (yet) supported.");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public CALExpressionNode visitLambdaExprExpression(CALParser.LambdaExprExpressionContext ctx) {
        return visit(ctx.lambdaExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitTypeAssertionExprExpression(CALParser.TypeAssertionExprExpressionContext ctx) {
        return visit(ctx.typeAssertionExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitIfExprExpression(CALParser.IfExprExpressionContext ctx) {
        return visit(ctx.ifExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitLetExprExpression(CALParser.LetExprExpressionContext ctx) {
        return visit(ctx.letExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitCallExprExpression(CALParser.CallExprExpressionContext ctx) {
        return visit(ctx.callExpression());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitIntegerLiteralExpression(CALParser.IntegerLiteralExpressionContext ctx) {
        try {
            long value = Long.parseLong(ctx.IntegerLiteral.getText());
            return new LongLiteralNode(value);
        } catch (NumberFormatException e) {
            return new BigIntegerLiteralNode(new BigInteger(ctx.IntegerLiteral.getText()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitFloatingPointLiteralExpression(CALParser.FloatingPointLiteralExpressionContext ctx) {
        // TODO Change to FloatLiteralNode

        try {
            long value = Long.parseLong(ctx.FloatingPointLiteral.getText());
            return new LongLiteralNode(value);
        } catch (NumberFormatException e) {
            return new BigIntegerLiteralNode(new BigInteger(ctx.FloatingPointLiteral.getText()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public BooleanLiteralNode visitBooleanLiteralExpression(CALParser.BooleanLiteralExpressionContext ctx) {
        return new BooleanLiteralNode(Boolean.parseBoolean(ctx.BooleanLiteral.getText()));
    }

    /**
     * {@inheritDoc}
     */
    @Override public StringLiteralNode visitCharacterLiteralExpression(CALParser.CharacterLiteralExpressionContext ctx) {
        // TODO Change to CharLiteralNode

        return new StringLiteralNode(ctx.CharacterLiteral.getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override public StringLiteralNode visitStringLiteralExpression(CALParser.StringLiteralExpressionContext ctx) {
        return new StringLiteralNode(ctx.StringLiteral.getText());
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
        // TODO Add support for old variables

        return context.createReadNode(ctx.variable().getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitSymbolReferenceExpression(CALParser.SymbolReferenceExpressionContext ctx) {
        // TODO Create CALSymbolReferenceNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public ExprIfNode visitIfExpression(CALParser.IfExpressionContext ctx) {
        return new ExprIfNode(visit(ctx.condition), visit(ctx.then), visit(ctx.elseIf != null ? ctx.elseIf : ctx.elze));
    }

    /**
     * {@inheritDoc}
     */
    @Override public ExprIfNode visitElseIfExpression(CALParser.ElseIfExpressionContext ctx) {
        return new ExprIfNode(visit(ctx.condition), visit(ctx.then), visit(ctx.elseIf != null ? ctx.elseIf : ctx.elze));
    }

    /**
     * {@inheritDoc}
     */
    @Override public LetExprNode visitLetExpression(CALParser.LetExpressionContext ctx) {
        context.pushScope(true, false);

        StmtBlockNode head = new StmtBlockNode(visit(ctx.localVariables).toArray(new CALStatementNode[0]));
        LetExprNode result = new LetExprNode(head, visit(ctx.body));

        context.popScope();

        return result;
    }

    static int LAMBDA_ID = 1;

    /**
     * {@inheritDoc}
     */
    @Override public LambdaNode visitLambdaExpression(CALParser.LambdaExpressionContext ctx) {
        // TODO Add support for constant lambdas
        // TODO Add support for lambda return type

        StmtBlockNode head;
        CALExpressionNode body;

        context.pushScope(true);

        if (ctx.formalParameters != null) {
            head = new StmtBlockNode(visit(ctx.formalParameters).toArray(new CALStatementNode[0]));
        } else {
            head = null;
        }

        if (ctx.localVariables != null) {
            context.pushScope(true, false);

            StmtBlockNode letHead = new StmtBlockNode(visit(ctx.localVariables).toArray(new CALStatementNode[0]));
            body = new LetExprNode(letHead, visit(ctx.body));

            context.popScope();
        } else {
            body = visit(ctx.body);
        }

        ReturnsLastBodyNode bodyNode = new ReturnsLastBodyNode(head, body);
        CALRootNode bodyRootNode = new CALRootNode(context.getLanguage(), context.getCurrentScope().getFrame(), bodyNode, null, "lambda-" + LAMBDA_ID);
        LambdaNode result = new LambdaNode(bodyRootNode);

        context.popScope();
        LAMBDA_ID++;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitProcExpression(CALParser.ProcExpressionContext ctx) {
        // TODO Create CALProcExpressionNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitSetComprehension(CALParser.SetComprehensionContext ctx) {
        // TODO Create SetInitNode + UnknownSizeSetInitNode => see {@link visitListComprehension}

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitListComprehension(CALParser.ListComprehensionContext ctx) {
        // TODO Add support for multiple computation expressions
        // TODO Add support for multiple variables in a generator
        // TODO Add support for multiple generators
        // TODO Add support for generator filters

        if (ctx.generators() == null) {
            // Simple collection expression
            if (ctx.computations == null) {
                return new ListInitNode(new CALExpressionNode[0]);
            }

            return new ListInitNode(visit(ctx.computations).toArray(new CALExpressionNode[0]));
        }

        // Comprehensions w/ generators
        /*Pair<List<Token>, List<CALExpressionNode>> generator = visit(ctx.generators()).get(0);

        Token variable = generator.getLeft().get(0);
        CALExpressionNode variableNode = context.createWriteNode(variable.getText(), null);
        CALExpressionNode collection = generator.getRight().get(0);
        CALExpressionNode computationExpression = expressions.get(0);

        CALStatementNode[] comprehensionBody = new CALStatementNode[3];
        // Initialization
        comprehensionBody[0] = context.createWriteNode("$tempList", new UnknownSizeListInitNode());
        comprehensionBody[1] = context.createWriteNode("$comprehensionCounter", new BigIntegerLiteralNode(new BigInteger("0")));
        // Loop iteration body
        CALStatementNode[] loopBody = new CALStatementNode[2];
        loopBody[0] = ListWriteNodeGen.create(context.createReadNode("$tempList"), context.createReadNode("$comprehensionCounter"), computationExpression);
        loopBody[1] = context.createWriteNode(
                "$comprehensionCounter",
                CALBinaryAddNodeGen.create(
                        context.createReadNode("$comprehensionCounter"),
                        new BigIntegerLiteralNode(new BigInteger("1"))
                )
        );
        CALStatementNode loopBodyNode = new StmtBlockNode(loopBody);

        if (variableNode instanceof CALWriteLocalVariableNode) {
            comprehensionBody[2] = ForeacheNodeGen.create(loopBodyNode, (CALWriteLocalVariableNode) variableNode, collection);
            CALStatementNode comprehensionBodyNode = new StmtBlockNode(comprehensionBody);

            return new ReturnsLastBodyNode(comprehensionBodyNode, context.createReadNode("$tempList"));
        } else {
            throw new Error("Comprehension Expression: Variable name re-use is unsupported.");
        }*/

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitMapComprehension(CALParser.MapComprehensionContext ctx) {
        // TODO Create MapInitNode + UnknownSizeMapInitNode => see {@link visitListComprehension}

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitMappings(CALParser.MappingsContext ctx) {
        // TODO First resolve {@link visitMapComprehension} dependencies

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitMapping(CALParser.MappingContext ctx) {
        // TODO First resolve {@link visitMapComprehension} dependencies

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitTypeAssertionExpression(CALParser.TypeAssertionExpressionContext ctx) {
        // TODO Create CALTypeAssertionExpressionNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitCaseExpression(CALParser.CaseExpressionContext ctx) {
        // TODO Create CALCaseExpressionNode

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitAlternativeExpression(CALParser.AlternativeExpressionContext ctx) {
        // TODO First resolve {@link visitCaseExpression} dependencies

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitCallExpression(CALParser.CallExpressionContext ctx) {
        return new CALInvokeNode(visit(ctx.function), ctx.arguments != null ? visit(ctx.arguments).toArray(new CALExpressionNode[0]) : new CALExpressionNode[0]);
    }
}