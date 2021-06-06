package ch.epfl.vlsc.truffle.cal.ast;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.stmt.StmtForeach;

public class ForeachTransformer extends ScopedTransformer<CALStatementNode> {

    StmtForeach foreach;

    // FIXME we should use different frameDescriptor as the one used in actor is
    // persistent and this one should not
    public ForeachTransformer(CALLanguage language, Source source, LexicalScope parentScope, StmtForeach foreach,
            FrameDescriptor frameDescriptor, int depth, TransformContext context) {
        super(language, source, parentScope, frameDescriptor, depth, context);
        this.foreach = foreach;

    }

    public CALStatementNode transform() {
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
            bodyNodes[i] = transformSatement(foreach.getBody().get(i));
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
}
