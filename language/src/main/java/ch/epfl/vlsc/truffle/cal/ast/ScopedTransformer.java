package ch.epfl.vlsc.truffle.cal.ast;

import static java.util.Map.entry;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtWhileNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryListSizeNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.*;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.nodes.util.IntCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.nodes.util.ValueCastNodeCreator;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryLogicalNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryMinusNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.expr.*;
import se.lth.cs.tycho.ir.expr.ExprLiteral.Kind;
import se.lth.cs.tycho.ir.stmt.*;
import se.lth.cs.tycho.ir.stmt.lvalue.LValue;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueIndexer;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import se.lth.cs.tycho.ir.type.NominalTypeExpr;
import se.lth.cs.tycho.ir.type.TypeExpr;
import se.lth.cs.tycho.ir.util.ImmutableList;

public abstract class ScopedTransformer<T> extends Transformer<T> {
    protected TransformContext context;

    public ScopedTransformer(TransformContext context) {
        super(context);
        this.context = context;
    }

    /*
    * Truffle Frameslots offer the facility to store an object(called info) with each slot to store metadata.
    * We use the info slot to store a casting node, which can coerce/trim values to fit in the slot datatype.
    * For example, if an assignment of a 16-bit integer is made to a 8-bit slot, the value should be trimmed
    * to fit the slot, which can be achieved using the subclass IntCastNode of ValueCastNode.
    */
    protected ValueCastNodeCreator getTypeInfo(TypeExpr type) {
        if(type instanceof NominalTypeExpr){
            NominalTypeExpr nomType = (NominalTypeExpr) type;
            List<Expression> s = nomType.getValueParameters().stream().filter(x -> x.getName().equals("size")).map(x -> x.getValue()).collect(Collectors.toList());
            CALExpressionNode intsize = s.size() > 0 ? transformExpr(s.get(0)) : new LongLiteralNode(CALLanguage.getCurrentContext().getEnv().getOptions().get(CALLanguage.bitlength));
            switch (nomType.getName()){
                case "int":
                    return new IntCastNodeCreator(intsize, true);
                case "uint":
                    return new IntCastNodeCreator(intsize, false);
                case "List":
                    // TODO: handleList();
                    break;
                case "bool":
                    // TODO: handleBool();
                    break;
                default:
                    throw new TransformException("Unknown type: " + type, context.getSource(), type);
            }
            return new DefaultValueCastNodeCreator();
        } else if (type == null)
            return new DefaultValueCastNodeCreator();
        else
            throw new TransformException("Unknown type class: " + type, context.getSource(), type);
    }

    // TODO: This will be used when implementing types for optimizations
    private FrameSlotKind getFrameSlotKind(TypeExpr type) {
        if(type instanceof NominalTypeExpr){
            NominalTypeExpr nomType = (NominalTypeExpr) type;
            switch (nomType.getName()){
                case "int":
                    break;
                case "uint":
                    // handleUint();
                    break;
                case "List":
                    // handleList();
                    break;
                case "bool":
                    // handleBool();
                    break;
                default:
                    throw new TransformException("Unknown type: " + type, context.getSource(), type);
            }
            return FrameSlotKind.Illegal;
        } else
            return FrameSlotKind.Illegal;
    }

    // TODO move in a new EntityTransformer
    // TODO merge with transformVarDecl
    protected CALStatementNode transformPortDecl(PortDecl port, Integer position) {
        // We create a frame slot for this argument,
        // give the rw verion to the assigning node
        // and keep the ro view for the context.getLexicalScope() as arguments can't
        // be modified
        FrameSlot frameSlot = context.getFrameDescriptor().findOrAddFrameSlot(port.getName(), getTypeInfo(port.getType()), getFrameSlotKind(port.getType()));
        FrameSlotAndDepthRW frameSlotAndDepthRW = new FrameSlotAndDepthRW(frameSlot, context.getDepth());
        context.getLexicalScope().put(port.getName(), new FrameSlotAndDepthRO(frameSlotAndDepthRW));
        return new InitializeArgNode(frameSlot, position);
    }

    // Arguments
    //
    public CALStatementNode transformArgument(VarDecl varDecl, Integer position) {
        // We create a frame slot for this argument,
        // give the rw verion to the assigning node
        // and keep the ro view for the context.getLexicalScope() as arguments can't
        // be modified
        FrameSlot frameSlot = context.getFrameDescriptor().findOrAddFrameSlot(varDecl.getName(), getTypeInfo(varDecl.getType()), getFrameSlotKind(varDecl.getType()));
        FrameSlotAndDepthRW frameSlotAndDepthRW = new FrameSlotAndDepthRW(frameSlot, context.getDepth());
        context.getLexicalScope().put(varDecl.getName(), new FrameSlotAndDepthRO(frameSlotAndDepthRW));
        return new InitializeArgNode(frameSlot, position);
    }

    // Local variables declared in header
    public CALExpressionNode transformVarDecl(VarDecl varDecl) {
        // TODO handle type with varDecl.getType
        String name = varDecl.getName();
        Expression value = varDecl.getValue();
        if(value == null && varDecl.getType() instanceof NominalTypeExpr){
            NominalTypeExpr varDeclType = (NominalTypeExpr) varDecl.getType();
            return createAssignment(name, getTypeInfo(varDecl.getType()), fetchDefaultValue(varDeclType));
        } else
            return createAssignment(name, varDecl.getType(), value);
    }

    private CALExpressionNode fetchDefaultValue(NominalTypeExpr varDeclType) {
        if(varDeclType.getName().equals("List")){
            List<Expression> l = varDeclType.getValueParameters().stream().filter(x -> x.getName().equals("size")).map(x -> x.getValue()).collect(Collectors.toList());
            List<NominalTypeExpr> t = varDeclType.getTypeParameters().stream().filter(x -> x.getName().equals("type")).map(x -> (NominalTypeExpr) x.getValue()).collect(Collectors.toList());
            if(l.size() > 0){
                if(t.size() > 0){
                    return new ListInitNodeSizeExpression(transformExpr(l.get(0)), fetchDefaultValue(t.get(0)));
                }else
                    return new ListInitNodeSizeExpression(transformExpr(l.get(0)), new NullLiteralNode());
            }else{
                return new UnknownSizeListInitNode();
            }
        } else if (varDeclType.getName().equals("int")){
            return new LongLiteralNode(0);
        } else if (varDeclType.getName().equals("uint")) {
            return new LongLiteralNode(0);
        } else
            throw new TransformException("No default value for type " + varDeclType.getName(), context.getSource(), varDeclType);
    }

    protected CALExpressionNode createAssignment(String name, TypeExpr type, Expression value) {
        CALExpressionNode valueNode = transformExpr(value);
        return createAssignment(name, getTypeInfo(type), valueNode);
    }

    protected CALExpressionNode createAssignment(String name, ValueCastNodeCreator caster, Expression value) {
        CALExpressionNode valueNode = transformExpr(value);
        return createAssignment(name, caster, valueNode);
    }

    protected CALExpressionNode createAssignment(String name, CALExpressionNode value) {
        return createAssignment(name, new DefaultValueCastNodeCreator(), value);
    }

    protected CALExpressionNode createAssignment(String name, TypeExpr slotInfo, CALExpressionNode valueNode) {
        return createAssignment(name, getTypeInfo(slotInfo), valueNode);
    }

    protected CALExpressionNode createAssignment(String name, ValueCastNodeCreator slotInfo, CALExpressionNode valueNode) {
        // FIXME we have to handle the case where actor declare a state and an action
        // redaclare a variable with the same name
        FrameSlot frameSlot = context.getFrameDescriptor().findOrAddFrameSlot(name, slotInfo, FrameSlotKind.Illegal);
        boolean newVariable = false;
        FrameSlotAndDepth slot;
        if (!context.getLexicalScope().containsKey(name)) {
            newVariable = true;
            slot = new FrameSlotAndDepthRW(frameSlot, context.getDepth());
            context.getLexicalScope().put(name, slot);
        } else {
            slot = context.getLexicalScope().get(name);
        }
        CALExpressionNode nameNode = new StringLiteralNode(name);

        final CALExpressionNode result = slot.createWriteNode(valueNode, nameNode, newVariable, context.getDepth());

        // result.addExpressionTag();

        return result;
    }

    public CALExpressionNode getReadNode(String name) {
        final CALExpressionNode result;
        final FrameSlotAndDepth frameSlot = context.getLexicalScope().get(name);
        if (frameSlot != null) {
            /* Read of a local variable. */
            result = frameSlot.createReadNode(context.getDepth());
        } else {
            /*
             * Read of a global name. In our context.getLanguage(), the only global names are functions.
             */
            result = new FunctionLiteralNode(name);
        }
        // result.setSourceSection(nameNode.getSourceCharIndex(),
        // nameNode.getSourceLength());
        result.addExpressionTag();
        return result;
    }

    public CALExpressionNode transformExpr(Expression expr) {
    	CALExpressionNode output;
        if (expr == null) {
        	output = new NullLiteralNode();
        } else if (expr instanceof ExprLiteral) {
        	output = transformExprLiteral((ExprLiteral) expr);
        } else if (expr instanceof ExprVariable) {
        	output = transformExprVariable((ExprVariable) expr);
        } else if (expr instanceof ExprBinaryOp) {
        	output = transformBinaryExpr((ExprBinaryOp) expr);
        } else if (expr instanceof ExprUnaryOp) {
        	output = transformUnaryExpr((ExprUnaryOp) expr);
        } else if (expr instanceof ExprLambda) {
        	output = (new LambdaTransformer((ExprLambda) expr, context.deeper(true))).transform();
        } else if (expr instanceof ExprLet) {
        	// Readonly, but not deeper
        	TransformContext exprCtx = context.deeper(true);
        	exprCtx.setDepth(exprCtx.getDepth()-1);
        	output = (new LetExprTransformer((ExprLet) expr, exprCtx)).transform();
        } else if (expr instanceof ExprApplication) {
        	output = transformExprApplication((ExprApplication) expr);
        } else if (expr instanceof ExprList) {
        	output = transformExprList((ExprList) expr);
        } else if (expr instanceof ExprIndexer) {
        	output = transformExprIndexer((ExprIndexer) expr);
        } else if (expr instanceof ExprComprehension) {
        	output = transformExprComprehension((ExprComprehension) expr);
        } else if (expr instanceof ExprIf) {
        	output = transformExprIf((ExprIf) expr);
        } else if (expr instanceof ExprProc) {
            output = transformExprProc(((ExprProc) expr));
        } else {
            throw new TransformException("unknown expr " + expr.getClass().getName(), context.getSource(), expr);
        }
        return withSourceSection(output, expr);
    }

    private CALExpressionNode transformExprProc(ExprProc expr) {
        return (new ProcExprTransformer((ExprProc) expr, context.deeper(false))).transform();
    }

    private CALExpressionNode transformExprVariable(ExprVariable expr) {
            ExprVariable v = (ExprVariable) expr;
            // For now we assume that we only read variables names,
            // we have to implement local variables and scopes
            String name = v.getVariable().getName();
            return getReadNode(name);
    }
    private CALExpressionNode transformExprIf(ExprIf expr) {
        return new ExprIfNode(transformExpr(expr.getCondition()), transformExpr(expr.getThenExpr()),
                transformExpr(expr.getElseExpr()));
    }

    private CALExpressionNode transformExprLiteral(ExprLiteral expr) {
        switch (expr.getKind()) {
        case String:
            return new StringLiteralNode(expr.getText());
        case Integer:
            return new BigIntegerLiteralNode(new BigInteger(expr.getText()));
        case True:
        case False:
            return new BooleanLiteralNode(expr.getKind() == Kind.True);
        default:
            throw new TransformException("unknown expr litteral " + expr.getKind().name(), context.getSource(), expr);
        }
    }

    private CALExpressionNode transformExprComprehension(ExprComprehension comprehension) {
        CALStatementNode[] init = new CALStatementNode[3];
        // tempList=[]
        String tempListName = "$tempList" + String.valueOf(comprehension.hashCode());
        init[0] = createAssignment(tempListName, new DefaultValueCastNodeCreator(), new UnknownSizeListInitNode());

        // i=0
        String listIndexVarName = "$comprehensionCounter" + String.valueOf(comprehension.hashCode());
        init[1] = createAssignment(listIndexVarName, new DefaultValueCastNodeCreator(), new BigIntegerLiteralNode(new BigInteger("0")));

        // The Comprehension nodes will generate the content into the above list
        init[2] = (new ExprComprehensionTransformer(comprehension, tempListName, listIndexVarName, context)).transform();

        return new ReturnsLastBodyNode(new StmtBlockNode(init), getReadNode(tempListName));
    }

    private CALExpressionNode transformExprIndexer(ExprIndexer exprIndexer) {
        return ListReadNodeGen.create(transformExpr(exprIndexer.getStructure()), transformExpr(exprIndexer.getIndex()));
    }

    private CALExpressionNode transformExprList(ExprList exprList) {
        CALExpressionNode[] values = new CALExpressionNode[exprList.getElements().size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = transformExpr(exprList.getElements().get(i));
        }
        return new ListInitNode(values);
    }

    private CALInvokeNode transformExprApplication(ExprApplication expr) {
        CALExpressionNode[] args = new CALExpressionNode[expr.getArgs().size()];
        int i = 0;
        for (Expression arg : expr.getArgs()) {
            args[i] = transformExpr(arg);
            i++;
        }
        CALExpressionNode functionNode = transformExpr(expr.getFunction());
        return new CALInvokeNode(functionNode, args);
    }

    private CALExpressionNode transformUnaryExpr(ExprUnaryOp expr) {
        CALExpressionNode result;
        CALExpressionNode valueNode = transformExpr(expr.getOperand());
        switch (expr.getOperation()) {
        case "-":
            result = CALUnaryMinusNodeGen.create(valueNode);
            break;
        case "not":
            result = CALUnaryLogicalNodeGen.create(valueNode);
            break;
        case "#":
            result = CALUnaryListSizeNodeGen.create(valueNode);
            break;
        default:
            throw new Error("unimplemented unary op " + expr.getOperation());
        }
        return result;
    }

    private ExprBinaryOp prioritieBinaryOperations(ExprBinaryOp expr) {
        ImmutableList<String> operations = expr.getOperations();
        ImmutableList<Expression> operands = expr.getOperands();
        if (operations.size() < 2)
            return expr;

        // TODO: Get the precendence corrected
        Map<String, Integer> precedence = Map.ofEntries(
                entry("..", 1),
                entry("||", 7),
                entry("or", 7),
                entry("&&", 8),
                entry("and", 8),
                entry("|", 9),
                entry("^", 10),
                entry("&", 11),
                entry("==", 12),
                entry("=", 12),
                entry("!=", 12),
                entry(">=", 13),
                entry(">", 13),
                entry("<=", 13),
                entry("<", 13),
                entry("<<", 14),
                entry(">>", 14),
                entry("in", 14),
                entry("-", 15),
                entry("+", 15),
                entry("/", 16),
                entry("*", 16),
                entry("div", 16),
                entry("%", 16),
                entry("mod", 16),
                entry("**", 18)
        );

        int lower = Integer.MAX_VALUE;
        int operationIndex = 0;
        int i = 0;
        List<String> reverseOperations = new ArrayList<>(operations);
        Collections.reverse(reverseOperations);
        for( String opeString : reverseOperations) {
            if ( precedence.get(opeString) < lower) {
                lower = precedence.get(opeString);
                operationIndex = i;
            }
            i++;
        }
        operationIndex = operations.size() - 1 - operationIndex;
        ExprBinaryOp left = expr.copy(
                ImmutableList.from(operations.subList(0, operationIndex)),
                ImmutableList.from(operands.subList(0, operationIndex+1)));
        ExprBinaryOp right = expr.copy(
                ImmutableList.from(operations.subList(operationIndex+1, operations.size())),
                ImmutableList.from(operands.subList(operationIndex+1, operands.size())));

        return expr.copy(
                ImmutableList.of(operations.get(operationIndex)),
                ImmutableList.of(prioritieBinaryOperations(left), prioritieBinaryOperations(right)));
    }
    private CALExpressionNode transformBinaryExpr(ExprBinaryOp expr) {
        expr = prioritieBinaryOperations(expr);
        if (expr.getOperations().size() == 0)
            return transformExpr(expr.getOperands().get(0));
        assert expr.getOperations().size() == 1;
        String opeString = expr.getOperations().get(0);
        CALExpressionNode result;

        assert expr.getOperands().size() == 2;
        CALExpressionNode left = transformExpr(expr.getOperands().get(0));
        CALExpressionNode right = transformExpr(expr.getOperands().get(1));

        switch (opeString) {
        case "+":
            result = CALBinaryAddNodeGen.create(left, right);
            break;
        case "-":
            result = CALBinarySubNodeGen.create(left, right);
            break;
        case "*":
            result = CALBinaryMulNodeGen.create(left, right);
            break;
        case "/":
            result = CALBinaryDivNodeGen.create(left, right);
            break;
        case "mod":
            result = CALBinaryModNodeGen.create(left, right);
            break;
        case "<=":
            result = CALBinaryLessOrEqualNodeGen.create(left, right);
            break;
        case "<":
            result = CALBinaryLessThanNodeGen.create(left, right);
            break;
        case ">=":
            result = CALBinaryBiggerOrEqualNodeGen.create(left, right);
            break;
        case ">":
            result = CALBinaryBiggerThanNodeGen.create(left, right);
            break;
        case "=":
            result = CALBinaryEqualNodeGen.create(left, right);
            break;
        case "!=":
            result = CALBinaryNotEqualNodeGen.create(left, right);
            break;
        case "..":
            result = ListRangeInitNodeGen.create(left, right);
            break;
        case "and":
            result = new CALBinaryLogicalAndNode(left, right);
            break;
        case "or":
            result = new CALBinaryLogicalOrNode(left, right);
            break;
        case ">>":
            result = CALBinaryShiftRightNodeGen.create(left, right);
            break;
        case "<<":
            result = CALBinaryShiftLeftNodeGen.create(left, right);
            break;
        case "&":
            result = CALBinaryBitAndNodeGen.create(left, right);
            break;
        case "|":
            result = CALBinaryBitOrNodeGen.create(left, right);
            break;
        default:
            throw new Error("unimplemented bin op " + opeString);
        }
        return result;
    }
    public CALStatementNode transformStatement(Statement statement) {
    	CALStatementNode output;
        if (statement instanceof StmtCall) {
        	output = transformStmtCall((StmtCall) statement);
        } else if (statement instanceof StmtAssignment) {
        	output = transformStmtAssignment((StmtAssignment) statement);
        } else if (statement instanceof StmtForeach) {
        	output = transformStmtForeach((StmtForeach) statement);
        } else if (statement instanceof StmtIf) {
        	output = transformStmtIf((StmtIf) statement);
        } else if (statement instanceof StmtBlock) {
            output = transformStmtBlock((StmtBlock) statement);
        } else if (statement instanceof StmtWhile) {
            output = transformStmtWhile((StmtWhile) statement);
        } else {
            throw new Error("unknown statement " + statement.getClass().getName());
        }
        return withSourceSection(output, statement);
    }

    private StmtWhileNode transformStmtWhile(StmtWhile statement) {
        return new StmtWhileNode(transformExpr(statement.getCondition()), transformStatementsList(statement.getBody()));
    }

    private StmtBlockNode transformStmtBlock(StmtBlock statement) {
        CALStatementNode[] body = new CALStatementNode[statement.getVarDecls().size() + statement.getStatements().size()];
        int i = 0;

        for (LocalVarDecl varDecl : statement.getVarDecls()) {
            body[i] = transformVarDecl(varDecl);
            i++;
        }
        for(Statement stmt: statement.getStatements()){
            body[i] = transformStatement(stmt);
            i++;
        }
        return new StmtBlockNode(body);
    }

    private CALStatementNode transformStatementsList(List<Statement> statements) {
        CALStatementNode[] statementNodes = new CALStatementNode[statements.size()];
        for (int i = 0; i < statements.size(); i++) {
            statementNodes[i] = transformStatement(statements.get(i));
        }
        return new StmtBlockNode(statementNodes);
    }

    private CALStatementNode transformStmtIf(StmtIf stmtIf) {
        CALStatementNode elze = null;
        if (stmtIf.getElseBranch() != null)
            elze = transformStatementsList(stmtIf.getElseBranch());
        return new StmtIfNode(transformExpr(stmtIf.getCondition()), transformStatementsList(stmtIf.getThenBranch()),
                elze);
    }

    private CALStatementNode transformStmtForeach(StmtForeach foreach) {
        Generator generator = foreach.getGenerator();
        CALExpressionNode list = transformExpr(generator.getCollection());
        if (generator.getVarDecls().size() != 1) {
            throw new Error("unsupported multiple var decls in for loop");
        }
        // transformVarDecl NEEDS to be called before body nodes transformations
        // in order to get the read of the variable right
        CALExpressionNode write = transformVarDecl(generator.getVarDecls().get(0));
        CALStatementNode[] bodyNodes = new CALStatementNode[foreach.getBody().size()];
        for (int i = 0; i < foreach.getBody().size(); i++) {
            bodyNodes[i] = transformStatement(foreach.getBody().get(i));
        }

        CALStatementNode statement = new StmtBlockNode(bodyNodes);
        if (write instanceof CALWriteLocalVariableNode) {
            ForeacheNode f = ForeacheNodeGen.create(statement, (CALWriteLocalVariableNode) write, list);
            return f;
        } else {
            // FIXME once createAssignment is fixed
            throw new Error("unsupported variable name reuse");
        }
    }

    private CALStatementNode transformStmtAssignment(StmtAssignment stmtAssignment) {
        if (stmtAssignment.getLValue() instanceof LValueVariable) {
            LValueVariable lvalue = (LValueVariable) stmtAssignment.getLValue();
            String name = lvalue.getVariable().getName();

            return createAssignment(name, new DefaultValueCastNodeCreator(), stmtAssignment.getExpression());
        } else if (stmtAssignment.getLValue() instanceof LValueIndexer) {
            LValueIndexer lvalue = (LValueIndexer) stmtAssignment.getLValue();
            return ListWriteNodeGen.create(getReadNode(lvalue.getStructure()), transformExpr(lvalue.getIndex()),
                    transformExpr(stmtAssignment.getExpression()));
        } else {
            throw new Error("unknown lvalue " + stmtAssignment.getLValue().getClass().getName());
        }
    }

    private CALExpressionNode getReadNode(LValue structure) {
        if(structure instanceof LValueVariable){
            return getReadNode(((LValueVariable) structure).getVariable().getName());
        }else if(structure instanceof  LValueIndexer){
            return ListReadNodeGen.create(getReadNode(((LValueIndexer) structure).getStructure()), transformExpr(((LValueIndexer) structure).getIndex()));
        }else{
            throw new TransformException("Unknown LValue structure for ReadNode: " + structure.toString(), context.getSource());
        }
    }

    private CALInvokeNode transformStmtCall(StmtCall stmtCall) {

        return new CALInvokeNode(transformExpr(stmtCall.getProcedure()),
                stmtCall.getArgs().map(new Function<Expression, CALExpressionNode>() {
                    public CALExpressionNode apply(Expression expr) {
                        return transformExpr(expr);
                    }
                }).toArray(new CALExpressionNode[0]));
    }
}
