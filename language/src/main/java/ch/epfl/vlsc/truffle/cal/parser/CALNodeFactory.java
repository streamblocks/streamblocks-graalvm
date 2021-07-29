package ch.epfl.vlsc.truffle.cal.parser;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.graalvm.collections.Pair;

import java.math.BigInteger;
import java.util.*;
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
    String namespaceName;
    Map<String, RootCallTarget> namespaceEntities;

    public void setNamespaceName(List<Token> name) {
        namespaceName = CALNodeFactory.qualifiedIdToString(name);
    }

    public void setNamespaceBody(Map<String, RootCallTarget> body) {
        namespaceEntities = new HashMap<>();
        for (Map.Entry<String, RootCallTarget> entry: body.entrySet()) {
            namespaceEntities.put((namespaceName != null ? namespaceName + "."  : "") + entry.getKey(), entry.getValue());
        }
    }

    public Map<String, RootCallTarget> createNamespace() {
        return namespaceEntities;
    }

    // Namespace Body
    Map<String, RootCallTarget> namespaceBodyEntities;

    public void initNamespaceBody() {
        namespaceBodyEntities = new HashMap<>();
    }

    public void addNamespaceBodyImport(Pair<String, String> bodyImport) {
        context.addImport(bodyImport);
    }

    public void addNamespaceBodyEntity(CALRootNode entity) {
        namespaceBodyEntities.put(entity.getName(), Truffle.getRuntime().createCallTarget(entity));
    }

    public Map<String, RootCallTarget> createNamespaceBody() {
        return namespaceBodyEntities;
    }

    // Qualified ID
    List<Token> qualifiedIdParts;

    public void initQualifiedId() {
        qualifiedIdParts = new ArrayList<>();
    }

    public void addQualifiedIdPart(Token part) {
        qualifiedIdParts.add(part);
    }

    public List<Token> createQualifiedId() {
        return qualifiedIdParts;
    }

    public static String qualifiedIdToString(List<Token> qualifiedId) {
        return qualifiedId.stream().map(Token::getText).collect(Collectors.joining("."));
    }

    // Single Import
    List<Token> singleImportGlobalName;
    String singleImportLocalName;

    public void setSingleImportGlobalName(List<Token> globalName) {
        singleImportGlobalName = globalName;
    }

    public void setSingleImportLocalName(Token localName) {
        singleImportLocalName = localName.getText();
    }

    public Pair<String, String> createSingleImport() {
        if (singleImportLocalName == null) {
            singleImportLocalName = singleImportGlobalName.get(singleImportGlobalName.size() - 1).getText();
        }

        return Pair.create(singleImportLocalName, CALNodeFactory.qualifiedIdToString(singleImportGlobalName));
    }

    // Actor Declaration
    String actorName;
    List<ActionNode> actorActions;

    public void initActor() {
        context.pushScope();

        actorActions = new ArrayList<>();
    }

    public void setActorName(Token name) {
        actorName = name.getText();
    }

    public void addActorAction(ActionNode action) {
        actorActions.add(action);
    }

    public ActorNode createActor() {
        ActorNode result = new ActorNode(
                context.getLanguage(),
                context.getCurrentScope().getFrame(),
                actorActions.toArray(new ActionNode[0]),
                new StmtBlockNode(new CALStatementNode[0]),
                null,
                actorName
        );

        context.popScope();

        return result;
    }

    // Action Declaration
    List<CALExpressionNode> actionLocalVariables;
    List<CALStatementNode> actionBody;

    public void initAction() {
        context.pushScope();
    }

    public void setActionLocalVariables(List<CALExpressionNode> localVariables) {
        actionLocalVariables = localVariables;
    }

    public void setActionBody(List<CALStatementNode> body) {
        actionBody = body;
    }

    public ActionNode createAction() {
        List<CALStatementNode> actionStatements = new ArrayList<>();

        if (actionLocalVariables != null) {
            actionStatements.addAll(actionLocalVariables);
        }
        if (actionBody != null) {
            actionStatements.addAll(actionBody);
        }

        ActionNode result = new ActionNode(
            context.getLanguage(),
            context.getCurrentScope().getFrame(),
            new ActionBodyNode(new StmtBlockNode(actionStatements.toArray(new CALStatementNode[0]))),
            new BooleanLiteralNode(true),
            null,
            "unnamed action"
        );

        context.popScope();

        return result;
    }

    // Block Variables
    List<CALExpressionNode> blockVariables;

    public void initBlockVariables() {
        blockVariables = new ArrayList<>();
    }

    public void addBlockVariable(CALExpressionNode blockVariable) {
        blockVariables.add(blockVariable);
    }

    public List<CALExpressionNode> createBlockVariables() {
        return blockVariables;
    }

    // Explicit Variable Declaration
    Token explicitVariableName;
    CALExpressionNode explicitVariableValue;

    public void setExplicitVariableName(Token name) {
        explicitVariableName = name;
    }

    public void setExplicitVariableValue(CALExpressionNode value) {
        explicitVariableValue = value;
    }

    public CALExpressionNode createExplicitVariable() {
        // TODO Support variable type & constant variables ('=')
        return context.createWriteNode(explicitVariableName.getText(), explicitVariableValue);
    }

    // Formal Parameters
    List<CALExpressionNode> formalParameters;

    public void initFormalParameters() {
        formalParameters = new ArrayList<>();
    }

    public void addFormalParameter(CALExpressionNode formalParameter) {
        formalParameters.add(formalParameter);
    }

    public List<CALExpressionNode> createFormalParameters() {
        return formalParameters;
    }

    // Expressions
    List<CALExpressionNode> expressions;

    public void initExpressions() {
        expressions = new ArrayList<>();
    }

    public void addExpression(CALExpressionNode expression) {
        expressions.add(expression);
    }

    public List<CALExpressionNode> createExpressions() {
        return expressions;
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
    boolean isOldVariableExpression;
    Token variableExpressionVariable;

    public void setVariableExpressionAsOld() {
        isOldVariableExpression = true;
    }

    public void setVariableExpressionVariable(Token variable) {
        variableExpressionVariable = variable;
    }

    public CALExpressionNode createVariableExpression() {
        // TODO Add support for old variables
        CALExpressionNode result = context.createReadNode(variableExpressionVariable.getText());

        isOldVariableExpression = false;

        return result;
    }

    // Symbol Reference Expression
    // TODO Create CALSymbolReferenceNode

    // Conditional Expression
    CALExpressionNode conditionalExpressionCondition;
    CALExpressionNode conditionalExpressionThenExpression;
    CALExpressionNode conditionalExpressionElseExpression;

    public void setConditionalExpressionCondition(CALExpressionNode condition) {
        conditionalExpressionCondition = condition;
    }

    public void setConditionalExpressionThenExpression(CALExpressionNode thenExpression) {
        conditionalExpressionThenExpression = thenExpression;
    }

    public void setConditionalExpressionElseExpression(CALExpressionNode elseExpression) {
        conditionalExpressionElseExpression = elseExpression;
    }

    public ExprIfNode createConditionalExpression() {
        return new ExprIfNode(conditionalExpressionCondition, conditionalExpressionThenExpression, conditionalExpressionElseExpression);
    }

    // Local scope (Let Expression)
    List<CALExpressionNode> letExpressionLocalVariables;
    CALExpressionNode letExpressionBody;

    public void initLetExpression() {
        context.pushScope(true, false);
    }

    public void setLetExpressionLocalVariables(List<CALExpressionNode> localVariables) {
        letExpressionLocalVariables = localVariables;
    }

    public void setLetExpressionBody(CALExpressionNode body) {
        letExpressionBody = body;
    }

    public LetExprNode createLetExpression() {
        LetExprNode result = new LetExprNode(
            new StmtBlockNode(letExpressionLocalVariables.toArray(new CALStatementNode[0])),
            letExpressionBody
        );

        context.popScope();

        return result;
    }

    // Function closure (Lambda Expression)
    static int LAMBDA_ID = 1;
    List<CALExpressionNode> lambdaExpressionFormalParameters;
    List<CALExpressionNode> lambdaExpressionLocalVariables;
    CALExpressionNode lambdaExpressionBody;

    public void initLambdaExpression() {
        context.pushScope(true, false);
    }

    public void setLambdaExpressionFormalParameters(List<CALExpressionNode> formalParameters) {
        lambdaExpressionFormalParameters = formalParameters;
    }

    public void setLambdaExpressionLocalVariables(List<CALExpressionNode> localVariables) {
        context.pushScope(true, false);
        lambdaExpressionLocalVariables = localVariables;
    }

    public void setLambdaExpressionBody(CALExpressionNode body) {
        lambdaExpressionBody = body;
    }

    public LambdaNode createLambdaExpression() {
        // TODO Add support for constant lambdas
        // TODO Add support for lambda return type

        StmtBlockNode lambdaExpressionHead = null;
        if (lambdaExpressionFormalParameters != null) {
            lambdaExpressionHead = new StmtBlockNode(lambdaExpressionFormalParameters.toArray(new CALStatementNode[0]));
        }

        if (lambdaExpressionLocalVariables != null) {
            lambdaExpressionBody = new LetExprNode(
                    new StmtBlockNode(lambdaExpressionLocalVariables.toArray(new CALStatementNode[0])),
                    lambdaExpressionBody
            );

            context.popScope();
        }

        LambdaNode result = new LambdaNode(
            new CALRootNode(
                context.getLanguage(),
                context.getCurrentScope().getFrame(),
                new ReturnsLastBodyNode(lambdaExpressionHead, lambdaExpressionBody),
                null,
                "lambda-" + LAMBDA_ID
            )
        );

        context.popScope();
        LAMBDA_ID++;
        lambdaExpressionFormalParameters = null;
        lambdaExpressionLocalVariables = null;

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
    CALExpressionNode callExpressionFunction;
    List<CALExpressionNode> callExpressionArguments;

    public void setCallExpressionFunction(CALExpressionNode variableExpression) {
        callExpressionFunction = variableExpression;
    }

    public void setCallExpressionArguments(List<CALExpressionNode> expressions) {
        callExpressionArguments = expressions;
    }

    public CALInvokeNode createCallExpression() {
        return new CALInvokeNode(callExpressionFunction, callExpressionArguments.toArray(new CALExpressionNode[0]));
    }

    // Lvalues
    List<Token> lvalues;

    public void initLvalues() {
        lvalues = new ArrayList<>();
    }

    public void addLvalue(Token lvalue) {
        lvalues.add(lvalue);
    }

    public List<Token> createLvalues() {
        return lvalues;
    }

    // Lvalue
    Token lvalueVariable;

    public void setLvalueVariable(Token variable) {
        lvalueVariable = variable;
    }

    public Token createLvalue() {
        return lvalueVariable;
    }

    // Statements
    List<CALStatementNode> statements;

    public void initStatements() {
        statements = new ArrayList<>();
    }

    public void addStatement(CALStatementNode statement) {
        statements.add(statement);
    }

    public List<CALStatementNode> createStatements() {
        return statements;
    }

    // Assignment Statement
    Token assignmentStatementVariable;
    CALExpressionNode assignmentStatementValue;

    public void setAssignmentStatementVariable(Token variable) {
        assignmentStatementVariable = variable;
    }

    public void setAssignmentStatementValue(CALExpressionNode value) {
        assignmentStatementValue = value;
    }

    public CALExpressionNode createAssignmentStatement() {
        return context.createWriteNode(assignmentStatementVariable.getText(), assignmentStatementValue);
    }

    // Call Statement
    CALExpressionNode callStatementFunction;
    List<CALExpressionNode> callStatementArguments;

    public void setCallStatementFunction(CALExpressionNode function) {
        callStatementFunction = function;
    }

    public void setCallStatementArguments(List<CALExpressionNode> arguments) {
        callStatementArguments = arguments;
    }

    public CALInvokeNode createCallStatement() {
        return new CALInvokeNode(callStatementFunction, callStatementArguments.toArray(new CALExpressionNode[0]));
    }

    // Statement block
    // TODO Use unnamed proc node

    // Conditional Statements
    CALExpressionNode conditionalStatementCondition;
    StmtBlockNode conditionalStatementThenStatements;
    StmtBlockNode conditionalStatementElseStatements;

    public void setConditionalStatementCondition(CALExpressionNode condition) {
        conditionalStatementCondition = condition;
    }

    public void setConditionalStatementThenStatements(List<CALStatementNode> thenStatements) {
        conditionalStatementThenStatements = new StmtBlockNode(thenStatements.toArray(new CALStatementNode[0]));
    }

    public void setConditionalStatementElseStatements(List<CALStatementNode> elseStatements) {
        conditionalStatementElseStatements = new StmtBlockNode(elseStatements.toArray(new CALStatementNode[0]));
    }

    public StmtIfNode createConditionalStatement() {
        // FIXME Fix circular dependency: Action Statements -> If Statement -> Then Statements
        if (conditionalStatementThenStatements != null) {
            statements.removeAll(conditionalStatementThenStatements.getStatements());
        }

        if (conditionalStatementElseStatements != null) {
            statements.removeAll(conditionalStatementElseStatements.getStatements());
        }

        StmtIfNode result = new StmtIfNode(conditionalStatementCondition, conditionalStatementThenStatements, conditionalStatementElseStatements);

        conditionalStatementElseStatements = null;

        return result;
    }
}