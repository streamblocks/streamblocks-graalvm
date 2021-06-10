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
import ch.epfl.vlsc.truffle.cal.nodes.LambdaNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
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
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
public class LambdaTransformer extends ScopedTransformer<LambdaNode> {

    ExprLambda lambda;

    // FIXME we should use different frameDescriptor as the one used in actor is
    // persistent and this one should not
    public LambdaTransformer(ExprLambda lambda, TransformContext context) {
        // lambda are side-effect-free
        super(context);
        this.lambda = lambda;
    }

    public LambdaNode transform() {
        // FIXME lambda doesn't have var decl ?!
        
        CALStatementNode[] varDecls = new CALStatementNode[lambda.getValueParameters().size() /*lambda.getVarDecls().size()*/];
        int i = 0;
        // Prepend local variable declarations to the body nodes
        /*
        for (LocalVarDecl varDecl : lambda.getVarDecls()) {
            body[i] = transformVarDecl(varDecl);
            i++;
        }*/

        // Prepend arguments so they are specialized the same way as in the body
        for (VarDecl varDecl : lambda.getValueParameters()) {
            varDecls[i] = transformArgument(varDecl, i);
            i++;
        }
        StmtBlockNode varDeclsNode = new StmtBlockNode(varDecls);
        ReturnsLastBodyNode bodyNode = new ReturnsLastBodyNode(varDeclsNode, transformExpr(lambda.getBody()));
        // TODO fix CAL parser
        /*SourceSection lambdaSrc = source.createSection(lambda.getFromLineNumber(), lambda.getFromColumnNumber(),
                lambda.getToLineNumber());*/
        SourceSection lambdaSrc = context.getSource().createUnavailableSection();
        // FIXME name
        CALRootNode bodyRootNode = new CALRootNode(context.getLanguage(), context.getFrameDescriptor(), bodyNode, lambdaSrc, "lambda-1");
        return new LambdaNode(bodyRootNode);
    }

}
