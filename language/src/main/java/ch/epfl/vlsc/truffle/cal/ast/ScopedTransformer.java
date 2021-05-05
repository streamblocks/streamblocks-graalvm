package ch.epfl.vlsc.truffle.cal.ast;

import java.math.BigInteger;

import com.oracle.truffle.api.frame.FrameDescriptor;
import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLessOrEqualNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLessThanNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryMulNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinarySubNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListInitNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import se.lth.cs.tycho.ir.expr.ExprApplication;
import se.lth.cs.tycho.ir.expr.ExprBinaryOp;
import se.lth.cs.tycho.ir.expr.ExprIndexer;
import se.lth.cs.tycho.ir.expr.ExprLambda;
import se.lth.cs.tycho.ir.expr.ExprList;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;

public abstract class ScopedTransformer<T> extends Transformer<T> {
    protected int depth;
    protected FrameDescriptor frameDescriptor;
    protected LexicalScope lexicalScope;

    public ScopedTransformer(CALLanguage language, Source source, LexicalScope parentScope,
            FrameDescriptor frameDescriptor, int depth) {
        super(language, source);
        this.frameDescriptor = frameDescriptor;// new FrameDescriptor();
        // lexical scope must include parent scope
        lexicalScope = new LexicalScopeRW(parentScope);
        this.depth = depth + 1;
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
    public CALExpressionNode transformVarDecl(LocalVarDecl varDecl) {
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
        if (expr instanceof ExprLiteral) {
            if (((ExprLiteral) expr).getKind() == ExprLiteral.Kind.String)
                return new StringLiteralNode(((ExprLiteral) expr).getText());
            else if (((ExprLiteral) expr).getKind() == ExprLiteral.Kind.Integer)
                return new BigIntegerLiteralNode(new BigInteger(((ExprLiteral) expr).getText()));
            else
                throw new Error("unknown expr litteral " + expr.getClass().getName());
        } else if (expr instanceof ExprVariable) {
            ExprVariable v = (ExprVariable) expr;
            // For now we assume that we only read variables names,
            // we have to implement local variables and scopes
            String name = v.getVariable().getName();
            return getReadNode(name);
        } else if (expr instanceof ExprBinaryOp) {
            return transformBinaryExpr((ExprBinaryOp) expr);
        } else if (expr instanceof ExprLambda) {
            return (new LambdaTransformer(language, source, lexicalScope, (ExprLambda) expr, frameDescriptor, depth))
                    .transform();
        } else if (expr instanceof ExprApplication) {
            return transformExprApplication((ExprApplication) expr);
        } else if (expr instanceof ExprList) {
            return transformExprList((ExprList) expr);
        } else if (expr instanceof ExprIndexer) {
            return transformExprIndexer((ExprIndexer) expr);
        } else {
            throw new Error("unknown expr " + expr.getClass().getName());
        }
    }

    private CALExpressionNode transformExprIndexer(ExprIndexer exprIndexer) {
        return ListReadNodeGen.create(transformExpr(exprIndexer.getStructure()),
                transformExpr(exprIndexer.getIndex()));
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
        }
        CALExpressionNode functionNode = transformExpr(expr.getFunction());
        return new CALInvokeNode(functionNode, args);
    }

    public CALExpressionNode transformBinaryExpr(ExprBinaryOp expr) {
        assert expr.getOperations().size() == 1;
        String opeString = expr.getOperations().get(0);
        CALBinaryNode result;

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
        case "<=":
            result = CALBinaryLessOrEqualNodeGen.create(left, right);
            break;
        case "<":
            result = CALBinaryLessThanNodeGen.create(left, right);
            break;
        default:
            throw new Error("unimplemented bin op " + opeString);
        }
        return result;
    }
}
