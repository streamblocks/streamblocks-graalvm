package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALComprehensionRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import se.lth.cs.tycho.ir.expr.ExprComprehension;

import java.math.BigInteger;

public class ExprComprehensionRootTransformer extends ScopedTransformer<CALComprehensionRootNode> {
    ExprComprehension comprehension;
    public ExprComprehensionRootTransformer(ExprComprehension comprehension, TransformContext context) {
        super(context);
        this.comprehension = comprehension;
    }

    @Override
    public CALComprehensionRootNode transform() {
        CALStatementNode[] init = new CALStatementNode[3];
        // tempList=[]
        init[0] = createAssignment("$tempList", new UnknownSizeListInitNode());
        // i=0
        init[1] = createAssignment("$comprehensionCounter", new BigIntegerLiteralNode(new BigInteger("0")));
        init[2] = (new ExprComprehensionTransformer(comprehension, context.deeper(false))).transform();
        return new CALComprehensionRootNode(new StmtBlockNode(init), getReadNode("$tempList"));
    }
}
