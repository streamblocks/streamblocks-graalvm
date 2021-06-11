package ch.epfl.vlsc.truffle.cal.ast;

import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.LambdaNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.ExprLambda;
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

        SourceSection lambdaSrc = getSourceSection(lambda);
        // FIXME name
        CALRootNode bodyRootNode = new CALRootNode(context.getLanguage(), context.getFrameDescriptor(), bodyNode, lambdaSrc, "lambda-1");
        return new LambdaNode(bodyRootNode);
    }

}
