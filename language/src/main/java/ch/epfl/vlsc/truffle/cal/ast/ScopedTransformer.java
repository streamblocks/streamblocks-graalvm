package ch.epfl.vlsc.truffle.cal.ast;

import java.math.BigInteger;

import com.oracle.truffle.api.frame.FrameDescriptor;
import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ExprIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryBiggerOrEqualNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryBiggerThanNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryDivNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLessOrEqualNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLessThanNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLogicalAndNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryMulNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinarySubNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryBitNotNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryLogicalNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryMinusNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.unary.CALUnaryMinusNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListInitNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListRangeInitNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import se.lth.cs.tycho.ir.expr.ExprApplication;
import se.lth.cs.tycho.ir.expr.ExprBinaryOp;
import se.lth.cs.tycho.ir.expr.ExprComprehension;
import se.lth.cs.tycho.ir.expr.ExprIf;
import se.lth.cs.tycho.ir.expr.ExprIndexer;
import se.lth.cs.tycho.ir.expr.ExprLambda;
import se.lth.cs.tycho.ir.expr.ExprLet;
import se.lth.cs.tycho.ir.expr.ExprList;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprUnaryOp;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.expr.ExprLiteral.Kind;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;

public abstract class ScopedTransformer<T> extends Transformer<T> {
    protected int depth;
    protected FrameDescriptor frameDescriptor;
    protected LexicalScope lexicalScope;
    protected TransformContext context;

    public ScopedTransformer(CALLanguage language, Source source, LexicalScope parentScope,
            FrameDescriptor frameDescriptor, int depth, TransformContext context) {
        super(language, source);
        this.frameDescriptor = frameDescriptor;// new FrameDescriptor();
        // lexical scope must include parent scope
        lexicalScope = new LexicalScopeRW(parentScope);
        this.depth = depth + 1;
        this.context = context;

    }

    // TODO move in a new EntityTransformer
    // TODO merge with transformVarDecl
    protected CALStatementNode transformPortDecl(PortDecl port, Integer position) {
        // We create a frame slot for this argument,
        // give the rw verion to the assigning node
        // and keep the ro view for the lexicalScope as arguments can't
        // be modified
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(port.getName(), FrameSlotKind.Illegal);
        FrameSlotAndDepthRW frameSlotAndDepthRW = new FrameSlotAndDepthRW(frameSlot, depth);
        lexicalScope.put(port.getName(), new FrameSlotAndDepthRO(frameSlotAndDepthRW));
        return new InitializeArgNode(frameSlot, position);
    }

    // Arguments
    //
    public CALStatementNode transformArgument(VarDecl varDecl, Integer position) {
        // We create a frame slot for this argument,
        // give the rw verion to the assigning node
        // and keep the ro view for the lexicalScope as arguments can't
        // be modified
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(varDecl.getName(), FrameSlotKind.Illegal);
        FrameSlotAndDepthRW frameSlotAndDepthRW = new FrameSlotAndDepthRW(frameSlot, depth);
        lexicalScope.put(varDecl.getName(), new FrameSlotAndDepthRO(frameSlotAndDepthRW));
        return new InitializeArgNode(frameSlot, position);
    }

    // Local variables declared in header
    public CALExpressionNode transformVarDecl(VarDecl varDecl) {
        // TODO handle type with varDecl.getType
        String name = varDecl.getName();
        Expression value = varDecl.getValue();

        return createAssignment(name, value);
    }

    protected CALExpressionNode createAssignment(String name, Expression value) {
        CALExpressionNode valueNode = transformExpr(value);
        return createAssignment(name, valueNode);
    }

    protected CALExpressionNode createAssignment(String name, CALExpressionNode valueNode) {
        // FIXME this definitely doesnt work, we need to get the slot form lexicalscope
        // first
        // FIXME we have to handle the case where actor declare a state and an action
        // redaclare a variable with the same name
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(name, FrameSlotKind.Illegal);
        boolean newVariable = false;
        FrameSlotAndDepth slot;
        if (!lexicalScope.containsKey(name)) {
            newVariable = true;
            slot = new FrameSlotAndDepthRW(frameSlot, depth);
            lexicalScope.put(name, slot);
        } else {
            slot = lexicalScope.get(name);
        }
        CALExpressionNode nameNode = new StringLiteralNode(name);

        final CALExpressionNode result = slot.createWriteNode(valueNode, nameNode, newVariable, depth);

        if (valueNode.hasSource()) {
            final int start = nameNode.getSourceCharIndex();
            final int length = valueNode.getSourceEndIndex() - start;
            result.setSourceSection(start, length);
        }
        // result.addExpressionTag();

        return result;

    }

    public CALExpressionNode getReadNode(String name) {
        final CALExpressionNode result;
        final FrameSlotAndDepth frameSlot = lexicalScope.get(name);
        if (frameSlot != null) {
            /* Read of a local variable. */
            result = frameSlot.createReadNode(depth);
        } else {
            /*
             * Read of a global name. In our language, the only global names are functions.
             */
            result = new FunctionLiteralNode(name);
        }
        // result.setSourceSection(nameNode.getSourceCharIndex(),
        // nameNode.getSourceLength());
        result.addExpressionTag();
        return result;
    }

    public CALExpressionNode transformExpr(Expression expr) {
        if (expr == null) {
            return new NullLiteralNode();
        } else if (expr instanceof ExprLiteral) {
            return transformExprLiteral((ExprLiteral) expr);
        } else if (expr instanceof ExprVariable) {
            ExprVariable v = (ExprVariable) expr;
            // For now we assume that we only read variables names,
            // we have to implement local variables and scopes
            String name = v.getVariable().getName();
            return getReadNode(name);
        } else if (expr instanceof ExprBinaryOp) {
            return transformBinaryExpr((ExprBinaryOp) expr);
        } else if (expr instanceof ExprUnaryOp) {
            return transformUnaryExpr((ExprUnaryOp) expr);
        } else if (expr instanceof ExprLambda) {
            return (new LambdaTransformer(language, source, lexicalScope, (ExprLambda) expr, frameDescriptor, depth,
                    context)).transform();
        } else if (expr instanceof ExprLet) {
            return (new LetExprTransformer(language, source, lexicalScope, (ExprLet) expr, frameDescriptor, depth,
                    context)).transform();
        } else if (expr instanceof ExprApplication) {
            return transformExprApplication((ExprApplication) expr);
        } else if (expr instanceof ExprList) {
            return transformExprList((ExprList) expr);
        } else if (expr instanceof ExprIndexer) {
            return transformExprIndexer((ExprIndexer) expr);
        } else if (expr instanceof ExprComprehension) {
            return transformExprComprehension((ExprComprehension) expr);
        } else if (expr instanceof ExprIf) {
            return transformExprIf((ExprIf) expr);
        } else {
            throw new TransformException("unknown expr " + expr.getClass().getName(), source, expr);
        }
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
            throw new TransformException("unknown expr litteral " + expr.getKind().name(), source, expr);
        }
    }

    private CALExpressionNode transformExprComprehension(ExprComprehension comprehension) {
        if (comprehension.getFilters().size() > 0)
            throw new TransformException("filters not implemented", source, comprehension);
        Generator generator = comprehension.getGenerator();
        // FIXME filters
        ImmutableList<Expression> filters = comprehension.getFilters();
        if (filters.size() > 0)
            throw new TransformException("Filters not supported", source, comprehension);
        if (!(comprehension.getCollection() instanceof ExprList))
            throw new TransformException("for comp should have a collection", source, comprehension);
        // [ f(x) : for x in originalList ]
        ExprList collection = (ExprList) comprehension.getCollection();

        CALStatementNode[] init = new CALStatementNode[3];
        // tempList=[]
        init[0] = createAssignment("$tempList", new UnknownSizeListInitNode());
        // i=0
        init[1] = createAssignment("$comprehensionCounter", new BigIntegerLiteralNode(new BigInteger("0")));

        // resolve originalList
        CALExpressionNode list = transformExpr(generator.getCollection());
        if (generator.getVarDecls().size() != 1) {
            throw new TransformException("unsupported multiple var decls in for loop", source, generator);
        }
        // x = originalList[i]
        CALExpressionNode write = transformVarDecl(generator.getVarDecls().get(0));
        if (collection.getElements().size() > 1)
            throw new TransformException("unsupported more than 1 element for for-comp", source, generator);
        CALStatementNode[] bodyNodes = new CALStatementNode[2];
        // tempList[i] = f(x)
        bodyNodes[0] = ListWriteNodeGen.create(getReadNode("$tempList"), getReadNode("$comprehensionCounter"),
                transformExpr(collection.getElements().get(0)));
        // i= i + 1
        bodyNodes[1] = createAssignment("$comprehensionCounter", CALBinaryAddNodeGen
                .create(getReadNode("$comprehensionCounter"), new BigIntegerLiteralNode(new BigInteger("1"))));

        CALStatementNode body = new StmtBlockNode(bodyNodes);

        if (write instanceof CALWriteLocalVariableNode) {
            init[2] = ForeacheNodeGen.create(body, (CALWriteLocalVariableNode) write, list);
            return new ReturnsLastBodyNode(new StmtBlockNode(init), getReadNode("$tempList"));
        } else {
            throw new TransformException("Node write error", source, comprehension);
        }
        // Transform the for loop and return the temporary list
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

    public CALInvokeNode transformExprApplication(ExprApplication expr) {
        CALExpressionNode[] args = new CALExpressionNode[expr.getArgs().size()];
        int i = 0;
        for (Expression arg : expr.getArgs()) {
            args[i] = transformExpr(arg);
            i++;
        }
        CALExpressionNode functionNode = transformExpr(expr.getFunction());
        return new CALInvokeNode(functionNode, args);
    }

    public CALExpressionNode transformUnaryExpr(ExprUnaryOp expr) {
        CALExpressionNode result;
        CALExpressionNode valueNode = transformExpr(expr.getOperand());
        switch (expr.getOperation()) {
        case "-":
            result = CALUnaryMinusNodeGen.create(valueNode);
            break;
        case "not":
            result = CALUnaryLogicalNodeGen.create(valueNode);
            break;
        default:
            throw new Error("unimplemented unary op " + expr.getOperation());
        }
        return result;
    }

    public CALExpressionNode transformBinaryExpr(ExprBinaryOp expr) {
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
        case "..":
            result = ListRangeInitNodeGen.create(left, right);
            break;
        case "and":
            result = new CALBinaryLogicalAndNode(left, right);
            break;
        default:
            throw new Error("unimplemented bin op " + opeString);
        }
        return result;
    }
}
