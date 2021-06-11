package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.LetExprNode;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.ExprLet;

public class LetExprTransformer extends ScopedTransformer<LetExprNode> {

    ExprLet letExpr;

    public LetExprTransformer(ExprLet letExpr, TransformContext context) {
        // let expressions are side-effect-free
        // kind of an exception here as we have a new lexical scope but we don't go
        // deeper as we don't have any materialized frame
        super(context);
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
        
        
        return withSourceSection(new LetExprNode(varDeclsNode, bodyNode), letExpr);
    }

}
