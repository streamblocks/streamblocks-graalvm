package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtFunctionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryListSizeNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryLogicalNotNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryMinusNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.*;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.nodes.util.ValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;

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
        CALExpressionNode operand = visit(ctx.operand);
        String operator = ctx.operator.getText();

        CALExpressionNode unaryOperationNode;
        switch (operator) {
            case "-":
                unaryOperationNode = CALUnaryMinusNodeGen.create(operand);
                break;
            case "!":
                // TODO: Change to CALUnaryBitNotNode
            case "not":
                unaryOperationNode = CALUnaryLogicalNotNodeGen.create(operand);
                break;
            case "rng":
                // TODO: Create CALUnaryMapRangeNode
            case "dom":
                // TODO: Create CALUnaryMapDomainNode
            case "#":
                unaryOperationNode = CALUnaryListSizeNodeGen.create(operand);
                break;
            default:
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unary operator \"" + operator + "\" is not yet supported");
        }

        unaryOperationNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        unaryOperationNode.addExpressionTag();

        return unaryOperationNode;
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
        return TypeCastVisitor.getInstance().visitType(ctx.type()).create(ExpressionVisitor.getInstance().visit(ctx.expression()));
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
        List<CALExpressionNode> indices = (ArrayList<CALExpressionNode>) CollectionVisitor.getInstance().visitExpressions(ctx.indices);
        for (CALExpressionNode indexNode: indices) {
            resultNode = ListReadNodeGen.create(resultNode, indexNode);
            // Note: Custom source section to precisely specify a part of indexer expression
            resultNode.setSourceSection(ScopeEnvironment.getInstance().getSource().createSection(
                    ctx.composite.getStart().getLine(),
                    ctx.composite.getStart().getCharPositionInLine() + 1,
                    ctx.indices.expression(indices.indexOf(indexNode)).getStop().getLine(),
                    ctx.indices.expression(indices.indexOf(indexNode)).getStop().getCharPositionInLine() + 1
            ));
            resultNode.addExpressionTag();
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

        CALExpressionNode binaryOperationNode;
        switch (operator) {
            case "^":
                // TODO: Change to Power expression node
                binaryOperationNode = CALBinaryXorNodeGen.create(operand1, operand2);
                break;
            case "..":
                binaryOperationNode = ListRangeInitNodeGen.create(operand1, operand2);
                break;
            case "+":
                binaryOperationNode = CALBinaryAddNodeGen.create(operand1, operand2);
                break;
            case "-":
                binaryOperationNode = CALBinarySubNodeGen.create(operand1, operand2);
                break;
            case "*":
                binaryOperationNode = CALBinaryMulNodeGen.create(operand1, operand2);
                break;
            case "div":
                // TODO: Change to Integer Division expression node
            case "/":
                binaryOperationNode = CALBinaryDivNodeGen.create(operand1, operand2);
                break;
            case "%":
            case "mod":
                binaryOperationNode = CALBinaryModNodeGen.create(operand1, operand2);
                break;
            case "<<":
                binaryOperationNode = CALBinaryShiftLeftNodeGen.create(operand1, operand2);
                break;
            case ">>":
                binaryOperationNode = CALBinaryShiftRightNodeGen.create(operand1, operand2);
                break;
            case "<":
                binaryOperationNode = CALBinaryLessThanNodeGen.create(operand1, operand2);
                break;
            case "<=":
                binaryOperationNode = CALBinaryLessOrEqualNodeGen.create(operand1, operand2);
                break;
            case ">":
                binaryOperationNode = CALBinaryGreaterThanNodeGen.create(operand1, operand2);
                break;
            case ">=":
                binaryOperationNode = CALBinaryGreaterOrEqualNodeGen.create(operand1, operand2);
                break;
            case "==":
            case "=":
                binaryOperationNode = CALBinaryEqualNodeGen.create(operand1, operand2);
                break;
            case "!=":
                binaryOperationNode = CALBinaryNotEqualNodeGen.create(operand1, operand2);
                break;
            case "&":
                binaryOperationNode = CALBinaryBitAndNodeGen.create(operand1, operand2);
                break;
            case "|":
                binaryOperationNode = CALBinaryBitOrNodeGen.create(operand1, operand2);
                break;
            case "and":
                binaryOperationNode = new CALBinaryLogicalAndNode(operand1, operand2);
                break;
            case "or":
                binaryOperationNode = new CALBinaryLogicalOrNode(operand1, operand2);
                break;
            default:
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Binary operator \"" + operator + "\" is not yet supported");
        }

        binaryOperationNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        binaryOperationNode.addExpressionTag();

        return binaryOperationNode;
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
            LongLiteralNode literalNode = new LongLiteralNode(value);
            literalNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            literalNode.addExpressionTag();

            return literalNode;
        } catch (NumberFormatException e) {
            BigIntegerLiteralNode literalNode = new BigIntegerLiteralNode(new BigInteger(ctx.IntegerLiteral().getText()));
            literalNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            literalNode.addExpressionTag();

            return literalNode;
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
        BooleanLiteralNode literalNode = new BooleanLiteralNode(Boolean.parseBoolean(ctx.BooleanLiteral().getText()));
        literalNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        literalNode.addExpressionTag();

        return literalNode;
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
        StringLiteralNode literalNode = new StringLiteralNode(ctx.StringLiteral().getText());
        literalNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        literalNode.addExpressionTag();

        return literalNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public NullLiteralNode visitNullLiteralExpression(CALParser.NullLiteralExpressionContext ctx) {
        NullLiteralNode literalNode = new NullLiteralNode();
        literalNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        literalNode.addExpressionTag();

        return literalNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitVariableExpression(CALParser.VariableExpressionContext ctx) {
        if (ctx.isOld != null) {
            // TODO Add support for old variable
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Old variable expression is not yet supported");
            }
        }

        return ScopeEnvironment.getInstance().createReadNode(ctx.variable().getText(), ScopeEnvironment.getInstance().createSourceSection(ctx));
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

        ExprIfNode ifNode = new ExprIfNode(conditionNode, thenNode, elseNode);
        ifNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        ifNode.addExpressionTag();

        return ifNode;
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

        ExprIfNode elseIfNode = new ExprIfNode(conditionNode, thenNode, elseNode);
        elseIfNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        elseIfNode.addExpressionTag();

        return elseIfNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public LetExprNode visitLetExpression(CALParser.LetExpressionContext ctx) {
        ScopeEnvironment.getInstance().pushScope(true, false);

        Collection<CALExpressionNode> localVariableNodes = CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables);
        StmtBlockNode headNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
        headNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.localVariables));
        headNode.addStatementTag();

        CALExpressionNode bodyNode = visit(ctx.body);

        LetExprNode letNode = new LetExprNode(headNode, bodyNode);
        letNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        letNode.addExpressionTag();

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
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Constant lambda expression is not yet supported");
            }
        }
        if (ctx.type() != null) {
            // TODO Add support for lambda return type
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Lambda expression return type is not yet supported");
            }
        }

        ScopeEnvironment.getInstance().pushScope(true);

        if (ctx.formalParameters() != null) {
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headNode = new StmtBlockNode(formalParameterNodes.toArray(new CALStatementNode[0]));
            headNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.formalParameters()));
            headNode.addStatementTag();
        } else {
            headNode = null;
        }

        if (ctx.localVariables != null) {
            ScopeEnvironment.getInstance().pushScope(true, false);

            Collection<CALExpressionNode> localVariableNodes = CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables);
            StmtBlockNode letHeadNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
            letHeadNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.localVariables));
            letHeadNode.addStatementTag();

            CALExpressionNode letBodyNode = visit(ctx.body);

            bodyNode = new LetExprNode(letHeadNode, letBodyNode);
            bodyNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            bodyNode.addExpressionTag();

            ScopeEnvironment.getInstance().popScope();
        } else {
            bodyNode = visit(ctx.body);
        }

        ReturnsLastBodyNode lambdaBodyNode = new ReturnsLastBodyNode(headNode, bodyNode);
        lambdaBodyNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        lambdaBodyNode.addExpressionTag();

        CALRootNode lambdaBodyRootNode = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                lambdaBodyNode,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                ScopeEnvironment.generateLambdaName()
        );
        // TODO Add RootTag / CallTag for lambdaBodyRootNode

        LambdaNode lambdaNode = new LambdaNode(lambdaBodyRootNode);
        lambdaNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        lambdaNode.addExpressionTag();

        ScopeEnvironment.getInstance().popScope();

        return lambdaNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitProcExpression(CALParser.ProcExpressionContext ctx) {
        // FIXME: Copied over from visitLambdaExpression
        StmtBlockNode headNode;
        CALExpressionNode bodyNode;

        ScopeEnvironment.getInstance().pushScope(false);

        if (ctx.formalParameters() != null) {
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headNode = new StmtBlockNode(formalParameterNodes.toArray(new CALStatementNode[0]));
            headNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.formalParameters()));
            headNode.addStatementTag();
        } else {
            headNode = new StmtBlockNode(new CALStatementNode[0]);
        }

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

        ReturnsLastBodyNode procBodyNode = new ReturnsLastBodyNode(headNode, bodyNode);
        procBodyNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        procBodyNode.addStatementTag();

        CALRootNode procBodyRootNode = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                procBodyNode,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                ScopeEnvironment.generateLambdaName()
        );
        // TODO Add RootTag / CallTag for procBodyRootNode

        ProcNode procNode = new ProcNode(procBodyRootNode);
        procNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        procNode.addStatementTag();

        ScopeEnvironment.getInstance().popScope();

        return procNode;
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
        if (ctx.tail != null) {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx.tail, "List Comprehension tails are not supported yet");
        }
        if (ctx.generators() == null)
            return visitSimpleListComprehension(ctx);
        else
            return visitListComprehensionWithGenerators(ctx);
    }

    private CALExpressionNode visitSimpleListComprehension(CALParser.ListComprehensionContext ctx) {
        // Simple list collection expression
        CALExpressionNode[] valueNodes;
        if (ctx.computations != null) {
            valueNodes = CollectionVisitor.getInstance().visitExpressions(ctx.computations).toArray(new CALExpressionNode[0]);
        } else {
            valueNodes = new CALExpressionNode[0];
        }

        ListInitNode simpleComprehensionNode = new ListInitNode(valueNodes);
        simpleComprehensionNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        simpleComprehensionNode.addExpressionTag();

        return simpleComprehensionNode;
    }

    private CALExpressionNode visitListComprehensionWithGenerators(CALParser.ListComprehensionContext ctx) {
        CALStatementNode[] init = new CALStatementNode[3];
        // tempList=[]
        String tempListName = ScopeEnvironment.generateVariableName();
        init[0] = ScopeEnvironment.getInstance().createNewVariableWriteNode(tempListName, new UnknownSizeListInitNode(), DefaultValueCastNodeCreator.getInstance(), ScopeEnvironment.getInstance().getSource().createUnavailableSection());

        // i=0
        String listIndexVarName = ScopeEnvironment.generateVariableName();
        init[1] = ScopeEnvironment.getInstance().createNewVariableWriteNode(listIndexVarName, new BigIntegerLiteralNode(new BigInteger("0")), DefaultValueCastNodeCreator.getInstance(), ScopeEnvironment.getInstance().getSource().createUnavailableSection());

        // The Comprehension nodes will generate the content into the above list
        init[2] = (new ListComprehensionVisitor(tempListName, listIndexVarName)).visitListComprehension(ctx);
        ReturnsLastBodyNode list = new ReturnsLastBodyNode(
                new StmtBlockNode(init),
                ScopeEnvironment.getInstance().createReadNode(tempListName, ScopeEnvironment.getInstance().getSource().createUnavailableSection()));

        list.addExpressionTag();
        list.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));

        return list;
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

        CALInvokeNode callNode = new CALInvokeNode(functionNode, argumentNodes);
        callNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        callNode.addExpressionTag();

        return callNode;
    }
}