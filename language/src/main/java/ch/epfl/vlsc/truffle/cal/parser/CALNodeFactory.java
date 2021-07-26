package ch.epfl.vlsc.truffle.cal.parser;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
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

    // Compilation Unit
    Map<String, RootCallTarget> compilationUnitEntities;

    public void initializeCompilationUnit() {
        compilationUnitEntities = new HashMap<>();
    }

    public void setCompilationUnitEntities(Map<String, RootCallTarget> entities) {
        compilationUnitEntities.putAll(entities);
    }

    public Map<String, RootCallTarget> finalizeCompilationUnit() {
        return compilationUnitEntities;
    }

    // Namespace Declaration
    String namespaceName;
    Map<String, RootCallTarget> namespaceEntities;

    public void initializeNamespace() {
        namespaceName = null;
        namespaceEntities = new HashMap<>();
    }

    public void setNamespaceName(List<Token> qualifiedId) {
        namespaceName = CALNodeFactory.qualifiedIdToString(qualifiedId);
    }

    public void setNamespaceEntities(Map<String, RootCallTarget> entities) {
        for (Map.Entry<String, RootCallTarget> entry: entities.entrySet()) {
            String entityFullName;
            if (namespaceName != null) {
                entityFullName = namespaceName + "." + entry.getKey();
            } else {
                entityFullName = entry.getKey();
            }
            namespaceEntities.put(entityFullName, entry.getValue());
        }
    }

    public Map<String, RootCallTarget> finalizeNamespace() {
        return namespaceEntities;
    }

    // Namespace Body
    Map<String, RootCallTarget> namespaceBodyEntities;

    public void initializeNamespaceBody() {
        namespaceBodyEntities = new HashMap<>();
    }

    public void addNamespaceBodyImport(Pair<String, String> importEntity) {
        context.addImport(importEntity);
    }

    public void addNamespaceBodyEntity(CALRootNode entity) {
        namespaceBodyEntities.put(entity.getName(), Truffle.getRuntime().createCallTarget(entity));
    }

    public Map<String, RootCallTarget> finalizeNamespaceBody() {
        return namespaceBodyEntities;
    }

    // Qualified ID
    List<Token> qualifiedIdParts;

    public void initializeQualifiedId() {
        qualifiedIdParts = new ArrayList<>();
    }

    public void addQualifiedIdPart(Token id) {
        qualifiedIdParts.add(id);
    }

    public List<Token> finalizeQualifiedId() {
        return qualifiedIdParts;
    }

    public static String qualifiedIdToString(List<Token> qualifiedId) {
        return String.join(".", qualifiedId.stream().map(x -> x.getText()).collect(Collectors.toList()));
    }

    // Annotation
    // TO-DO

    // Unit
    // TO-DO

    // Import Declaration
    Pair<String, String> importDeclaration;

    public void initializeImport() {
        importDeclaration = null;
    }

    public void setImportAsSingle(Pair<String, String> singleImport) {
        importDeclaration = singleImport;
    }

    public Pair<String, String> finalizeImport() {
        return importDeclaration;
    }

    // Single Import
    List<Token> singleImportGlobalName;
    String singleImportLocalName;

    public void initializeSingleImport() {
        singleImportGlobalName = null;
        singleImportLocalName = null;
    }

    public void setSingleImportGlobalName(List<Token> qualifiedId) {
        singleImportGlobalName = qualifiedId;
    }

    public void setSingleImportLocalName(Token id) {
        singleImportLocalName = id.getText();
    }

    public Pair<String, String> finalizeSingleImport() {
        if (singleImportLocalName == null) {
            singleImportLocalName = singleImportGlobalName.get(singleImportGlobalName.size() - 1).getText();
        }

        return Pair.create(singleImportLocalName, CALNodeFactory.qualifiedIdToString(singleImportGlobalName));
    }

    // Actor Declaration
    String actorName;
    List<ActionNode> actorActions;

    public void initializeActor() {
        this.context.pushScope(false);

        actorName = null;
        actorActions = new ArrayList<>();
    }

    public void setActorName(Token id) {
        actorName = id.getText();
    }

    public void addActorAction(ActionNode action) {
        actorActions.add(action);
    }

    public ActorNode finalizeActor() {

        ActorNode result = new ActorNode(
                context.getLanguage(),
                context.getCurrentScope().getFrame(),
                actorActions.toArray(new ActionNode[0]),
                new StmtBlockNode(new CALStatementNode[0]),
                null,
                actorName
        );

        this.context.popScope();

        return result;
    }

    // Action Declaration
    List<CALExpressionNode> actionLocalVariables;
    List<CALStatementNode> actionBodyStatements;

    public void initializeAction() {
        this.context.pushScope(false);

        actionLocalVariables = null;
        actionBodyStatements = null;
    }

    public void setActionLocalVariables(List<CALExpressionNode> blockVariables) {
        actionLocalVariables = blockVariables;
    }

    public void setActionBodyStatements(List<CALStatementNode> statements) {
        actionBodyStatements = statements;
    }

    public ActionNode finalizeAction() {
        List<CALStatementNode> actionBody = new ArrayList<>();

        actionBody.addAll(actionLocalVariables);
        actionBody.addAll(actionBodyStatements);

        ActionNode result = new ActionNode(
            context.getLanguage(),
            context.getCurrentScope().getFrame(),
            new ActionBodyNode(new StmtBlockNode(actionBody.toArray(new CALStatementNode[0]))),
            new BooleanLiteralNode(true),
            null,
            "unnamed action"
        );

        this.context.popScope();

        return result;
    }

    // Block Variables
    List<CALExpressionNode> blockVariables;

    public void initializeBlockVariables() {
        blockVariables = new ArrayList<>();
    }

    public void addBlockVariable(CALExpressionNode blockVariable) {
        blockVariables.add(blockVariable);
    }

    public List<CALExpressionNode> finalizeBlockVariables() {
        return blockVariables;
    }

    // Block Variable
    CALExpressionNode blockVariable;

    public void initializeBlockVariable() {
        blockVariable = null;
    }

    public void setExplicitBlockVariable(CALExpressionNode explicitVariable) {
        blockVariable = explicitVariable;
    }

    public CALExpressionNode finalizeBlockVariable() {
        return blockVariable;
    }

    // Explicit Variable Declaration
    Token explicitVariableName;
    CALExpressionNode explicitVariableExpression;

    public void initializeExplicitVariable() {
        explicitVariableName = null;
        explicitVariableExpression = null;
    }

    public void setExplicitVariableName(Token id) {
        explicitVariableName = id;
    }

    public void setExplicitVariableExpression(CALExpressionNode expression) {
        explicitVariableExpression = expression;
    }

    public CALExpressionNode finalizeExplicitVariable() {
        return context.createWriteNode(explicitVariableName.getText(), explicitVariableExpression);
    }

    // Expressions
    List<CALExpressionNode> expressions;

    public void initializeExpressions() {
        expressions = new ArrayList<>();
    }

    public void addExpression(CALExpressionNode expression) {
        expressions.add(expression);
    }

    public List<CALExpressionNode> finalizeExpressions() {
        return expressions;
    }

    // Expression
    CALExpressionNode expression;

    public void initializeExpression() {
        expression = null;
    }

    public void setUnaryOperationExpression(Token operator, CALExpressionNode operand) {
        switch (operator.getText()) {
            case "-":
                expression = CALUnaryMinusNodeGen.create(operand);
                break;
            case "!":
                // TODO: Change to CALUnaryBitNotNode
            case "not":
                expression = CALUnaryLogicalNotNodeGen.create(operand);
                break;
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

    public void setBinaryOperationExpression(CALExpressionNode operand1, Token operator, CALExpressionNode operand2) {
        switch (operator.getText()) {
            case "^":
                // TODO: Change to CALBinaryPowerNode
                expression = CALBinaryXorNodeGen.create(operand1, operand2);
                break;
            case "..":
                expression = ListRangeInitNodeGen.create(operand1, operand2);
                break;
            case "+":
                expression = CALBinaryAddNodeGen.create(operand1, operand2);
                break;
            case "-":
                expression = CALBinarySubNodeGen.create(operand1, operand2);
                break;
            case "*":
                expression = CALBinaryMulNodeGen.create(operand1, operand2);
                break;
            case "div":
                // TODO: Change to CALBinaryIntDivNode
            case "/":
                expression = CALBinaryDivNodeGen.create(operand1, operand2);
                break;
            case "%":
            case "mod":
                expression = CALBinaryModNodeGen.create(operand1, operand2);
                break;
            case "<<":
                expression = CALBinaryShiftLeftNodeGen.create(operand1, operand2);
                break;
            case ">>":
                expression = CALBinaryShiftRightNodeGen.create(operand1, operand2);
                break;
            case "<":
                expression = CALBinaryLessThanNodeGen.create(operand1, operand2);
                break;
            case "<=":
                expression = CALBinaryLessOrEqualNodeGen.create(operand1, operand2);
                break;
            case ">":
                expression = CALBinaryGreaterThanNodeGen.create(operand1, operand2);
                break;
            case ">=":
                expression = CALBinaryGreaterOrEqualNodeGen.create(operand1, operand2);
                break;
            case "==":
            case "=":
                expression = CALBinaryEqualNodeGen.create(operand1, operand2);
                break;
            case "!=":
                expression = new CALBinaryNotEqualNode(operand1, operand2);
                break;
            case "&":
                expression = CALBinaryBitAndNodeGen.create(operand1, operand2);
                break;
            case "|":
                expression = CALBinaryBitOrNodeGen.create(operand1, operand2);
                break;
            case "and":
                expression = new CALBinaryLogicalAndNode(operand1, operand2);
                break;
            case "or":
                expression = new CALBinaryLogicalOrNode(operand1, operand2);
                break;
            default:
                throw new Error("Binary operator " + operator.getText() + " is not (yet) supported.");
        }
    }

    public void setLiteralExpression(CALExpressionNode literalExpression) {
        expression = literalExpression;
    }

    public void setVariableExpression(CALExpressionNode variableExpression) {
        expression = variableExpression;
    }

    public void setCallExpression(CALInvokeNode callExpression) {
        expression = callExpression;
    }

    public CALExpressionNode finalizeExpression() {
        return expression;
    }

    // Literal Expression
    CALExpressionNode literalExpression;

    public void initializeLiteralExpression() {
        literalExpression = null;
    }

    public void setIntegerLiteralExpression(Token integerLiteral) {
        try {
            long value = Long.parseLong(integerLiteral.getText());
            literalExpression = new LongLiteralNode(value);
        } catch (NumberFormatException e) {
            literalExpression = new BigIntegerLiteralNode(new BigInteger(integerLiteral.getText()));
        }
    }

    public void setFloatLiteralExpression(Token floatLiteral) {
        // TODO: Change to FloatLiteralNode
        this.setIntegerLiteralExpression(floatLiteral);
    }

    public void setBooleanLiteralExpression(Token booleanLiteral) {
        literalExpression = new BooleanLiteralNode(Boolean.parseBoolean(booleanLiteral.getText()));
    }

    public void setCharLiteralExpression(Token charLiteral) {
        // TODO: Change to CharLiteralNode
        this.setStringLiteralExpression(charLiteral);

    }

    public void setStringLiteralExpression(Token stringLiteral) {
        literalExpression = new StringLiteralNode(stringLiteral.getText());
    }

    public void setNullLiteralExpression(Token nullLiteral) {
        literalExpression = new NullLiteralNode();
    }

    public CALExpressionNode finalizeLiteralExpression() {
        return literalExpression;
    }

    // Variable Expression
    Token variableExpressionVariable;

    public void initializeVariableExpression() {
        variableExpressionVariable = null;
    }

    public void setVariableExpressionVariable(Token variable) {
        variableExpressionVariable = variable;
    }

    public CALExpressionNode finalizeVariableExpression() {
        return context.createReadNode(variableExpressionVariable.getText());
    }

    // Call Expression
    CALExpressionNode callExpressionFunction;
    List<CALExpressionNode> callExpressionArguments;

    public void initializeCallExpression() {
        callExpressionFunction = null;
        callExpressionArguments = null;
    }

    public void setCallExpressionFunction(CALExpressionNode variableExpression) {
        callExpressionFunction = variableExpression;
    }

    public void setCallExpressionArguments(List<CALExpressionNode> expressions) {
        callExpressionArguments = expressions;
    }

    public CALInvokeNode finalizeCallExpression() {
        return new CALInvokeNode(callExpressionFunction, callExpressionArguments.toArray(new CALExpressionNode[0]));
    }

    // Lvalues
    List<Token> lvalues;

    public void initializeLvalues() {
        lvalues = new ArrayList<>();
    }

    public void addLvalue(Token lvalue) {
        lvalues.add(lvalue);
    }

    public List<Token> finalizeLvalues() {
        return lvalues;
    }

    // Lvalue
    Token lvalueVariable;

    public void initializeLvalue() {
        lvalueVariable = null;
    }

    public void setLvalueVariable(Token variable) {
        lvalueVariable = variable;
    }

    public Token finalizeLvalue() {
        return lvalueVariable;
    }

    // Variable
    Token variable;

    public void initializeVariable() {
        variable = null;
    }

    public void setVariable(Token id) {
        variable = id;
    }

    public Token finalizeVariable() {
        return variable;
    }

    // Statements
    List<CALStatementNode> statements;

    public void initializeStatements() {
        statements = new ArrayList<>();
    }

    public void addStatement(CALStatementNode statement) {
        statements.add(statement);
    }

    public List<CALStatementNode> finalizeStatements() {
        return statements;
    }

    // Statement
    CALStatementNode statement;

    public void initializeStatement() {
        statement = null;
    }

    public void setAssignmentStatement(CALExpressionNode assignmentStatement) {
        statement = assignmentStatement;
    }

    public void setCallStatement(CALInvokeNode callStatement) {
        statement = callStatement;
    }

    public CALStatementNode finalizeStatement() {
        return statement;
    }

    // Assignment Statement
    Token assignmentStatementLvalue;
    CALExpressionNode assignmentStatementExpression;

    public void initializeAssignmentStatement() {
        assignmentStatementLvalue = null;
        assignmentStatementExpression = null;
    }

    public void setAssignmentStatementLvalue(Token lvalue) {
        assignmentStatementLvalue = lvalue;
    }

    public void setAssignmentStatementExpression(CALExpressionNode expression) {
        assignmentStatementExpression = expression;
    }

    public CALExpressionNode finalizeAssignmentStatement() {
        return context.createWriteNode(assignmentStatementLvalue.getText(), assignmentStatementExpression);
    }

    // Call Statement
    CALExpressionNode callStatementFunction;
    List<CALExpressionNode> callStatementArguments;

    public void initializeCallStatement() {
        callStatementFunction = null;
        callStatementArguments = null;
    }

    public void setCallStatementFunction(CALExpressionNode variableExpression) {
        callStatementFunction = variableExpression;
    }

    public void setCallStatementArguments(List<CALExpressionNode> expressions) {
        callStatementArguments = expressions;
    }

    public CALInvokeNode finalizeCallStatement() {
        return new CALInvokeNode(callStatementFunction, callStatementArguments.toArray(new CALExpressionNode[0]));
    }

}