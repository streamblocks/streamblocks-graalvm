package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ProcNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALNull;
import com.oracle.truffle.api.source.SourceSection;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.ExprLambda;
import se.lth.cs.tycho.ir.expr.ExprProc;
import se.lth.cs.tycho.ir.stmt.Statement;

public class ProcExprTransformer extends ScopedTransformer<ProcNode> {
    // FIXME This class has been copied and modified from LambdaTransformer, so all FIXME in LambdaTransformer apply
    ExprProc proc;

    public ProcExprTransformer(ExprProc procArg, TransformContext context) {
        // lambda are side-effect-free
        super(context);
        this.proc = procArg;
    }

    public ProcNode transform() {
        CALStatementNode[] body = new CALStatementNode[proc.getValueParameters().size() + proc.getBody().size()];
        int i = 0;

        // Prepend arguments so they are specialized the same way as in the body
        for (VarDecl varDecl : proc.getValueParameters()) {
            body[i] = transformArgument(varDecl, i);
            i++;
        }
        for(Statement stmt: proc.getBody()){
            body[i] = transformSatement(stmt);
            i++;
        }
        StmtBlockNode bodyStmt = new StmtBlockNode(body);
        ReturnsLastBodyNode bodyNode = new ReturnsLastBodyNode(bodyStmt, new BooleanLiteralNode(true));

        SourceSection procSrc = getSourceSection(proc);

        // FIXME name
        CALRootNode bodyRootNode = new CALRootNode(context.getLanguage(), context.getFrameDescriptor(), bodyNode, procSrc, "proc-1");
        return new ProcNode(bodyRootNode);
    }
}
