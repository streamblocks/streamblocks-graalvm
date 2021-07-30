package ch.epfl.vlsc.truffle.cal.parser;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.graalvm.collections.Pair;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.*;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.*;

/**
 * Helper class used by the CAL {@link Parser} to create nodes.
 * The code is factored out of the automatically generated parser to keep the attributed grammar of CAL small.
 */
public class CALNodeFactory {

    private CALNodeFactoryContext context;

    public CALNodeFactory(CALLanguage language, Source source) {
        this.context = new CALNodeFactoryContext(language, source);
    }

    // Namespace Declaration
    public Map<String, RootCallTarget> createNamespace(List<Token> name, Map<String, RootCallTarget> body) {
        return body.entrySet().stream().collect(Collectors.toMap(e -> (name != null ? CALNodeFactory.qualifiedIdToString(name) + "."  : "") + e.getKey(), Map.Entry::getValue));
    }

    // Namespace Body
    public void addNamespaceBodyImport(Pair<String, String> bodyImport) {
        context.addImport(bodyImport);
    }

    public Map<String, RootCallTarget> createNamespaceBody(Map<String, CALRootNode> entities) {
        return entities.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Truffle.getRuntime().createCallTarget(e.getValue())));
    }

    // Qualified ID
    public static String qualifiedIdToString(List<Token> qualifiedId) {
        return qualifiedId.stream().map(Token::getText).collect(Collectors.joining("."));
    }

    // Single Import
    public Pair<String, String> createSingleImport(List<Token> globalName, Token localName) {
        return Pair.create(
            localName != null ? localName.getText() : globalName.get(globalName.size() - 1).getText(),
            CALNodeFactory.qualifiedIdToString(globalName)
        );
    }

    // Actor Declaration
    public void createActorScope() {
        context.pushScope();
    }

    public ActorNode createActor(Token name, List<ActionNode> actions) {
        // TODO Finish actor head statements
        StmtBlockNode head = new StmtBlockNode(new CALStatementNode[0]);

        ActorNode result = new ActorNode(context.getLanguage(), context.getCurrentScope().getFrame(), actions.toArray(new ActionNode[0]), head, null, name.getText());

        context.popScope();

        return result;
    }

    // Action Declaration
    public void createActionScope() {
        context.pushScope();
    }

    public ActionNode createAction(List<CALExpressionNode> localVariables, StmtBlockNode body) {
        List<CALStatementNode> actionStatements = new ArrayList<>();

        if (localVariables != null) {
            actionStatements.addAll(localVariables);
        }
        if (body != null) {
            actionStatements.addAll(body.getStatements());
        }

        StmtBlockNode bodyNode = new StmtBlockNode(actionStatements.toArray(new CALStatementNode[0]));
        ActionBodyNode actionBodyNode = new ActionBodyNode(bodyNode);
        // TODO Finish action firing conditions
        CALExpressionNode firingCondition = new BooleanLiteralNode(true);
        ActionNode result = new ActionNode(context.getLanguage(), context.getCurrentScope().getFrame(), actionBodyNode, firingCondition, null, "unnamed action");

        context.popScope();

        return result;
    }

    // Explicit Variable Declaration
    public CALExpressionNode createExplicitVariable(Token name, CALExpressionNode value) {
        // TODO Add support for variable type
        // TODO Add support for constant variables ('=')
        // TODO Add support for mutable variables ('mutable')

        return context.createWriteNode(name.getText(), value);
    }

    // Generator Body
    public Pair<List<Token>, List<CALExpressionNode>> createGeneratorBody(List<Token> variables, List<CALExpressionNode> collections) {
        return Pair.create(variables, collections);
    }

    // Indexer Expression
    public CALExpressionNode createIndexerExpression(CALExpressionNode composite, List<CALExpressionNode> indices) {
        CALExpressionNode expression = composite;
        for (CALExpressionNode index: indices) {
            expression = ListReadNodeGen.create(expression, index);
        }

        return expression;
    }

    // Field Selector Expression
    // TODO Create CALFieldSelectorNode

    // Operation Expressions
    public CALExpressionNode createUnaryOperationExpression(Token operator, CALExpressionNode operand) {
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

    public CALExpressionNode createBinaryOperationExpression(CALExpressionNode operand1, Token operator, CALExpressionNode operand2) {
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

    // Literal Expressions
    public CALExpressionNode createIntegerLiteralExpression(Token integerLiteral) {
        try {
            long value = Long.parseLong(integerLiteral.getText());
            return new LongLiteralNode(value);
        } catch (NumberFormatException e) {
            return new BigIntegerLiteralNode(new BigInteger(integerLiteral.getText()));
        }
    }

    public CALExpressionNode createFloatLiteralExpression(Token floatLiteral) {
        // TODO: Change to FloatLiteralNode
        return createIntegerLiteralExpression(floatLiteral);
    }

    public CALExpressionNode createBooleanLiteralExpression(Token booleanLiteral) {
        return new BooleanLiteralNode(Boolean.parseBoolean(booleanLiteral.getText()));
    }

    public CALExpressionNode createCharLiteralExpression(Token charLiteral) {
        // TODO: Change to CharLiteralNode
        return createStringLiteralExpression(charLiteral);
    }

    public CALExpressionNode createStringLiteralExpression(Token stringLiteral) {
        return new StringLiteralNode(stringLiteral.getText());
    }

    public CALExpressionNode createNullLiteralExpression(Token nullLiteral) {
        return new NullLiteralNode();
    }

    // Variable Expression
    public CALExpressionNode createVariableExpression(Token variable) {
        // TODO Add support for old variables
        return context.createReadNode(variable.getText());
    }

    // Symbol Reference Expression
    // TODO Create CALSymbolReferenceNode

    // Conditional Expression
    public ExprIfNode createConditionalExpression(CALExpressionNode condition, CALExpressionNode thenExpression, CALExpressionNode elseExpression) {
        return new ExprIfNode(condition, thenExpression, elseExpression);
    }

    // Local scope (Let Expression)
    public void createLetExpressionScope() {
        context.pushScope(true, false);
    }

    public LetExprNode createLetExpression(List<CALExpressionNode> localVariables, CALExpressionNode body) {
        StmtBlockNode head = new StmtBlockNode(localVariables.toArray(new CALStatementNode[0]));
        LetExprNode result = new LetExprNode(head, body);
        context.popScope();

        return result;
    }

    // Function closure (Lambda Expression)
    static int LAMBDA_ID = 1;

    public void createLambdaExpressionScope() {
        context.pushScope(true);
    }

    public LambdaNode createLambdaExpression(List<CALExpressionNode> formalParameters, List<CALExpressionNode> localVariables, CALExpressionNode body) {
        // TODO Add support for constant lambdas
        // TODO Add support for lambda return type

        StmtBlockNode head = null;
        if (formalParameters != null) {
            head = new StmtBlockNode(formalParameters.toArray(new CALStatementNode[0]));
        }

        if (localVariables != null) {
            StmtBlockNode letHead = new StmtBlockNode(localVariables.toArray(new CALStatementNode[0]));
            body = new LetExprNode(letHead, body);
            context.popScope();
        }

        ReturnsLastBodyNode bodyNode = new ReturnsLastBodyNode(head, body);
        CALRootNode bodyRootNode = new CALRootNode(context.getLanguage(), context.getCurrentScope().getFrame(), bodyNode, null, "lambda-" + LAMBDA_ID);
        LambdaNode result = new LambdaNode(bodyRootNode);

        context.popScope();
        LAMBDA_ID++;

        return result;
    }

    // Procedure Closure (Proc Expression)
    // TODO Create CALProcExpressionNode

    // Comprehensions w/ generators

    // Case Expression
    // TODO Create CALCaseExpressionNode

    // Type Assertion Expression
    // TODO Create CALTypeAssertionExpressionNode

    // Call Expression
    public CALInvokeNode createCallExpression(CALExpressionNode function, List<CALExpressionNode> arguments) {
        return new CALInvokeNode(function, arguments != null ? arguments.toArray(new CALExpressionNode[0]) : new CALExpressionNode[0]);
    }

    // Lvalue
    public Token createLvalue(Token variable) {
        return variable;
    }

    // Statements
    public StmtBlockNode createStatements(List<CALStatementNode> statements) {
        return new StmtBlockNode(statements.toArray(new CALStatementNode[0]));
    }

    // Assignment Statement
    public CALExpressionNode createAssignmentStatement(Token lvalue, CALExpressionNode value) {
        return context.createWriteNode(lvalue.getText(), value);
    }

    // Call Statement
    public CALInvokeNode createCallStatement(CALExpressionNode function, List<CALExpressionNode> arguments) {
        return new CALInvokeNode(function, arguments != null ? arguments.toArray(new CALExpressionNode[0]) : new CALExpressionNode[0]);
    }

    // Statement block
    // TODO Use unnamed proc node

    // Conditional Statements
    public StmtIfNode createConditionalStatement(CALExpressionNode condition, StmtBlockNode thenStatements, CALStatementNode elseStatements) {
        if (!(elseStatements instanceof StmtBlockNode)) {
            // Wrap Elseif statement in a statement block
            elseStatements = new StmtBlockNode(new CALStatementNode[]{ elseStatements });
        }

        return new StmtIfNode(condition, thenStatements, elseStatements);
    }

    // While Statement
    // TODO Create CALWhileStatementNode

    // Foreach Statement
    public ForeacheNode createForeachStatement(List<Pair<List<Token>, List<CALExpressionNode>>> generators, List<CALExpressionNode> localVariables, StmtBlockNode body) {
        Pair<List<Token>, List<CALExpressionNode>> generator = generators.get(0);
        Token variable = generator.getLeft().get(0);
        // TODO Finish getting foreach variable (like in variable declaration)
        CALExpressionNode variableNode = null;
        CALExpressionNode collection = generator.getRight().get(0);

        return ForeacheNodeGen.create(body, (CALWriteLocalVariableNode) variableNode, collection);
    }
}