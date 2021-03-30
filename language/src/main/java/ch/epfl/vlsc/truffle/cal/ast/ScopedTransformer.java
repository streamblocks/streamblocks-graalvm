package ch.epfl.vlsc.truffle.cal.ast;
import java.math.BigInteger;

import com.oracle.truffle.api.frame.FrameDescriptor;
import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import se.lth.cs.tycho.ir.expr.ExprBinaryOp;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;

public abstract class ScopedTransformer<T> extends Transformer<T> {
    protected int depth;
    protected FrameDescriptor frameDescriptor;
    protected LexicalScope lexicalScope;
    public ScopedTransformer(CALLanguage language, Source source, LexicalScope parentScope, FrameDescriptor frameDescriptor, int depth) {
        super(language, source);
        this.frameDescriptor = frameDescriptor;// new FrameDescriptor();
        // lexical scope must include parent scope
        lexicalScope = new LexicalScope(parentScope);
        this.depth = depth + 1;
    }

    public CALExpressionNode transformVarDecl(LocalVarDecl varDecl) {
        // TODO handle type with varDecl.getType
        String name = varDecl.getName();
        Expression value = varDecl.getValue();

        return createAssignment(name, value);
    }

    protected CALExpressionNode createAssignment(String name, Expression value) {
        // FIXME this definitely doesnt work, we need to get the slot form lexicalscope
        // first
        // FIXME we have to handle the case where actor declare a state and an action
        // redaclare a variable with the same name
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(name, FrameSlotKind.Illegal);
        boolean newVariable = false;
        FrameSlotAndDepth slot;
        if (!lexicalScope.locals.containsKey(name)) {
            newVariable = true;
            slot = new FrameSlotAndDepth(frameSlot, depth);
            lexicalScope.locals.put(name, slot);
        } else {
            slot = lexicalScope.locals.get(name);
        }
        CALExpressionNode valueNode = transformExpr(value);
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
            final CALExpressionNode result;
            final FrameSlotAndDepth frameSlot = lexicalScope.locals.get(name);
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
        } else if (expr instanceof ExprBinaryOp) {
            return transformBinaryExpr((ExprBinaryOp) expr);
        } else {
            throw new Error("unknown expr " + expr.getClass().getName());
        }
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
        default:
            throw new Error("unimplemented bin op " + opeString);
        }
        return result;
    }
}
