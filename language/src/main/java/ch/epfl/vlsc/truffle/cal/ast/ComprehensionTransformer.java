package ch.epfl.vlsc.truffle.cal.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.expr.ExprComprehension;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.util.ImmutableList;

class ComprehensionTransformer extends ScopedTransformer<ForeacheNode> {

    ExprComprehension comprehension;

    public ComprehensionTransformer(CALLanguage language, Source source, LexicalScope parentScope,
            ExprComprehension comprehension, FrameDescriptor frameDescriptor, int depth, TransformContext context) {
        super(language, source, new ROParentLexicalScope(parentScope), frameDescriptor, depth, context);

        this.comprehension = comprehension;
    }

    public ForeacheNode transform() {
        return null;
    }

}
