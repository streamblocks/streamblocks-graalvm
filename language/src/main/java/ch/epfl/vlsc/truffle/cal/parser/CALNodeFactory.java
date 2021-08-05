package ch.epfl.vlsc.truffle.cal.parser;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALWriteFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.Node;
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

    private ScopeEnvironment context;

    public CALNodeFactory(CALLanguage language, Source source) {
        this.context = new ScopeEnvironment(language, source);
    }

    // Namespace Declaration
    public Map<String, RootCallTarget> createNamespace(List<Token> name, Map<String, RootCallTarget> body) {
        return body.entrySet().stream().collect(Collectors.toMap(e -> (name != null ? CALNodeFactory.qualifiedIdToString(name) + "."  : "") + e.getKey(), Map.Entry::getValue));
    }

    // Namespace Body
    public void addNamespaceBodyImport(Pair<String, String> bodyImport) {
        context.addImport(bodyImport);
    }

    public Map<String, RootCallTarget> createNamespaceBody(List<CALRootNode> entities) {
        return entities.stream().collect(Collectors.toMap(CALRootNode::getName, entity -> Truffle.getRuntime().createCallTarget(entity)));
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

    public ActorNode createActor(Token name, List<CALExpressionNode> localVariables, List<ActionNode> initializers, List<ActionNode> actions) {
        // FIXME Finish implementing actor head statements
        // TODO Add support for initialization actions
        // TODO Add support for action priorities
        // TODO Add support for action schedules
        List<CALStatementNode> headStatements = new ArrayList<>();

        if (localVariables != null) {
            headStatements.addAll(localVariables);
        }
        StmtBlockNode head = new StmtBlockNode(headStatements.toArray(new CALStatementNode[0]));

        ActorNode result = new ActorNode(context.getLanguage(), context.getCurrentScope().getFrame(), actions.toArray(new ActionNode[0]), head, null, name.getText());

        context.popScope();

        return result;
    }

    // Action Declaration
    public void createActionScope() {
        context.pushScope();
    }

    public ActionNode createAction(List<CALWriteFIFONode> outputExpressions, List<CALExpressionNode> guards, List<CALExpressionNode> localVariables, StmtBlockNode body) {
        // TODO Add support for action tags
        // TODO Add support for action delay
        List<CALStatementNode> actionStatements = new ArrayList<>();

        if (localVariables != null) {
            actionStatements.addAll(localVariables);
        }
        if (body != null) {
            actionStatements.addAll(body.getStatements());
        }
        if (outputExpressions != null) {
            actionStatements.addAll(outputExpressions);
        }

        StmtBlockNode bodyNode = new StmtBlockNode(actionStatements.toArray(new CALStatementNode[0]));
        ActionBodyNode actionBodyNode = new ActionBodyNode(bodyNode);

        // Firing condition = Sufficient input tokens + Guards
        List<CALExpressionNode> firingConditions = new LinkedList<>();
        // FIXME Implement conditions for input tokens
        firingConditions.addAll(guards);

        CALExpressionNode firingCondition;
        if (firingConditions.size() > 0) {
            firingCondition = firingConditions.remove(0);
            for (CALExpressionNode cond : firingConditions)
                firingCondition = new CALBinaryLogicalAndNode(firingCondition, cond);
        } else {
            firingCondition = new BooleanLiteralNode(true);
        }

        ActionNode result = new ActionNode(context.getLanguage(), context.getCurrentScope().getFrame(), actionBodyNode, firingCondition, null, "unnamed action");

        context.popScope();

        return result;
    }

    // Output Expressions
    public CALWriteFIFONode createOutputExpression(Token port, List<CALExpressionNode> expressions, CALExpressionNode repeatExpression) {
        // FIXME Implement repeated expressions
        // TODO Add support for channel selector
        CALExpressionNode portQueue = context.createReadNode(port.getText());

        return new CALWriteFIFONode(portQueue, expressions.get(0));
    }

    // Local Variable Declaration
    // TODO Add support for external variables ('external')

    // Explicit Variable Declaration
    public CALExpressionNode createExplicitVariable(Token name, CALExpressionNode value) {
        // TODO Add support for variable type
        // TODO Add support for constant variables ('=')
        // TODO Add support for mutable variables ('mutable')

        return context.createWriteNode(name.getText(), value);
    }

    // Function Variable Declaration
    public void createFunctionVariableScope() {
        createLambdaExpressionScope();
    }



    public CALExpressionNode createFunctionVariable(Token name, List<InitializeArgNode> formalParameters, List<CALExpressionNode> localVariables, CALExpressionNode body) {
        // TODO Add support for constant functions
        // TODO Add support for function return type

        return context.createWriteNode(name.getText(), createLambdaExpression(formalParameters, localVariables, body));
    }

    // Formal Parameter
    public InitializeArgNode createFormalParameter(CALExpressionNode explicitVariable, int position) {
        if (explicitVariable instanceof CALWriteLocalVariableNodeGen) {
            StringLiteralNode variableNameNode = (StringLiteralNode) ((CALWriteLocalVariableNodeGen) explicitVariable).getNameNode();
            String variableName = variableNameNode.getValue();
            DepthFrameSlot readOnlySlot = new DepthFrameSlot(context.getCurrentScope().get(variableName));
            context.getCurrentScope().put(variableName, readOnlySlot);

            return new InitializeArgNode(context.getCurrentScope().get(variableName).getSlot(), position);
        }

        return null;
    }

    // Procedure Variable Declaration
    // TODO Use named proc closure

    // Generator Body
    public Pair<List<Token>, List<CALExpressionNode>> createGeneratorBody(List<Token> variables, List<CALExpressionNode> expressions) {
        return Pair.create(variables, expressions);
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

    public LambdaNode createLambdaExpression(List<InitializeArgNode> formalParameters, List<CALExpressionNode> localVariables, CALExpressionNode body) {
        // TODO Add support for constant lambdas
        // TODO Add support for lambda return type

        StmtBlockNode head = null;
        if (formalParameters != null) {
            head = new StmtBlockNode(formalParameters.toArray(new CALStatementNode[0]));
        }

        if (localVariables != null) {
            body = createLetExpression(localVariables, body);
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
    public CALExpressionNode createComprehension(List<CALExpressionNode> expressions, List<Pair<List<Token>, List<CALExpressionNode>>> generators) {
        // TODO Add support for multiple computation expressions
        // TODO Add support for multiple variables in a generator
        // TODO Add support for multiple generators
        // TODO Add support for generator filters
        if (generators == null) {
            // Simple collection expression
            return new ListInitNode(expressions.toArray(new CALExpressionNode[0]));
        }

        // Comprehensions w/ generators
        Pair<List<Token>, List<CALExpressionNode>> generator = generators.get(0);

        CALExpressionNode computationExpression = expressions.get(0);
        Token variable = generator.getLeft().get(0);
        CALExpressionNode variableNode = context.createWriteNode(variable.getText(), null);
        fixComprehensionComputationExpression(variable, computationExpression);
        CALExpressionNode collection = generator.getRight().get(0);
        List<CALExpressionNode> filters;
        if (generator.getRight().size() > 1) {
            filters = generator.getRight().subList(1, generator.getRight().size() - 1);
        }

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
        }
    }

    private void fixComprehensionComputationExpression(Token variable, CALExpressionNode expression) {
        // FIXME Finish fixing previously unknown loop variable
        if (expression instanceof FunctionLiteralNode && ((FunctionLiteralNode) expression).getFunctionName().equals(variable.getText())) {
            CALExpressionNode correctExpression = context.createReadNode(variable.getText());
            expression.replace(correctExpression);
        }

        for (Node child: expression.getChildren()) {
            fixComprehensionComputationExpression(variable, (CALExpressionNode) child);
        }
    }

    // Case Expression
    // TODO Create CALCaseExpressionNode

    // Type Assertion Expression
    // TODO Create CALTypeAssertionExpressionNode

    // Call Expression
    public CALInvokeNode createCallExpression(CALExpressionNode function, List<CALExpressionNode> arguments) {
        return new CALInvokeNode(function, arguments != null ? arguments.toArray(new CALExpressionNode[0]) : new CALExpressionNode[0]);
    }

    // Lvalue
    public Pair<Token, List<CALExpressionNode>> createLvalue(Token variable, List<CALExpressionNode> indices) {
        // TODO Add support for fields
        return Pair.create(variable, indices);
    }

    // Statements
    public StmtBlockNode createStatements(List<CALStatementNode> statements) {
        return new StmtBlockNode(statements.toArray(new CALStatementNode[0]));
    }

    // Assignment Statement
    public CALStatementNode createAssignmentStatement(Pair<Token, List<CALExpressionNode>> lvalue, CALExpressionNode value) {
        Token variable = lvalue.getLeft();
        List<CALExpressionNode> indices = lvalue.getRight();
        if (indices.size() > 0) {
            CALStatementNode expression = context.createReadNode(variable.getText());
            for (CALExpressionNode index : indices) {
                if (indices.indexOf(index) < indices.size() - 1) {
                    expression = ListReadNodeGen.create((CALExpressionNode) expression, index);
                } else {
                    expression = ListWriteNodeGen.create((CALExpressionNode) expression, index, value);
                }
            }
            return expression;
        } else {
            return context.createWriteNode(variable.getText(), value);
        }
    }

    // Call Statement
    public CALInvokeNode createCallStatement(CALExpressionNode function, List<CALExpressionNode> arguments) {
        return new CALInvokeNode(function, arguments != null ? arguments.toArray(new CALExpressionNode[0]) : new CALExpressionNode[0]);
    }

    // Statement block
    // TODO Use unnamed proc closure

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
    public List<List<CALExpressionNode>> createForeachStatementGeneratorVariables(List<Pair<List<Token>, List<CALExpressionNode>>> generators) {
        // Needs to be called before evaluating loop body so that the loop variables can be accessed properly
        // TODO Add support for multiple variables in a generator
        // TODO Add support for multiple generators
        Pair<List<Token>, List<CALExpressionNode>> generator = generators.get(0);
        Token variable = generator.getLeft().get(0);
        CALExpressionNode variableNode = context.createWriteNode(variable.getText(), null);

        return new ArrayList<>(List.of(List.of(variableNode)));
    }

    public List<CALExpressionNode> getForeachStatementGeneratorCollections(List<Pair<List<Token>, List<CALExpressionNode>>> generators) {
        // TODO Add support for multiple generators
        Pair<List<Token>, List<CALExpressionNode>> generator = generators.get(0);
        CALExpressionNode collection = generator.getRight().get(0);

        return new ArrayList<>(List.of(collection));
    }

    public List<List<CALExpressionNode>> getForeachStatementGeneratorFilters(List<Pair<List<Token>, List<CALExpressionNode>>> generators) {
        // TODO Add support for multiple generators
        Pair<List<Token>, List<CALExpressionNode>> generator = generators.get(0);
        List<CALExpressionNode> filters = new ArrayList<>();
        if (generator.getRight().size() > 1) {
            filters = generator.getRight().subList(1, generator.getRight().size() - 1);
        }

        return new ArrayList<>(List.of(filters));
    }

    public ForeacheNode createForeachStatement(List<List<CALExpressionNode>> variables, List<CALExpressionNode> collections, List<List<CALExpressionNode>> filters, List<CALExpressionNode> localVariables, StmtBlockNode body) {
        // TODO Add support for multiple variables in a generator
        // TODO Add support for multiple generators
        // TODO Add support for generator filters?
        CALExpressionNode variable = variables.get(0).get(0);
        CALExpressionNode collection = collections.get(0);

        if (variable instanceof CALWriteLocalVariableNode) {
            return ForeacheNodeGen.create(body, (CALWriteLocalVariableNode) variable, collection);
        } else {
            throw new Error("Foreach Statement: Variable name re-use is unsupported.");
        }
    }

    // Choose Statement
    // TODO Create CALChooseStatementNode

    // Case Statement
    // TODO Create CALCaseStatementNode

    // Read Statement
    // TODO Create CALReadStatementNode

    // Write Statement
    // TODO Create CALWriteStatementNode

    // Action Selection Statement
    // TODO Create Action Selection statement
}