package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.LambdaNode;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;

import java.math.BigInteger;
import java.util.function.Function;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.LetExprNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import se.lth.cs.tycho.ir.Variable;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.expr.ExprBinaryOp;
import se.lth.cs.tycho.ir.expr.ExprLambda;
import se.lth.cs.tycho.ir.expr.ExprLet;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
public class LetExprTransformer extends ScopedTransformer<LetExprNode> {

    ExprLet letExpr;

    public LetExprTransformer(CALLanguage language, Source source, LexicalScope parentScope, ExprLet letExpr,
            FrameDescriptor frameDescriptor, int depth, TransformContext context) {
        // let expressions are side-effect-free
        // kind of an exception here as we have a new lexical scope but we don't go deeper as we don't have any materialized frame
        super(language, source, new ROParentLexicalScope(parentScope), frameDescriptor, depth-1, context);
        this.letExpr = letExpr;
    }

    public LetExprNode transform() {
        CALStatementNode[] varDecls = new CALStatementNode[letExpr.getVarDecls().size()];
        int i = 0;

        // Prepend arguments so they are specialized the same way as in the body
        for (VarDecl varDecl : letExpr.getVarDecls()) {
            varDecls[i] = transformVarDecl(varDecl);
            i++;
        }
        StmtBlockNode varDeclsNode = new StmtBlockNode(varDecls);
        CALExpressionNode bodyNode = transformExpr(letExpr.getBody());
        // TODO fix CAL parser
        /*SourceSection lambdaSrc = source.createSection(lambda.getFromLineNumber(), lambda.getFromColumnNumber(),
                lambda.getToLineNumber());*/
        return new LetExprNode(varDeclsNode, bodyNode);
    }

}
