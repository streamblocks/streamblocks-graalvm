package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALComprehensionRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLogicalAndNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import com.mxgraph.util.svg.ExtendedGeneralPath;
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.expr.ExprComprehension;
import se.lth.cs.tycho.ir.expr.ExprList;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.util.ImmutableList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class ExprComprehensionTransformer extends ScopedTransformer<CALComprehensionNode> {
    private final ExprComprehension comprehension;

    public ExprComprehensionTransformer(ExprComprehension comprehension, TransformContext context) {
        super(context);
        this.comprehension = comprehension;
    }

    @Override
    public CALComprehensionNode transform() {
        Generator generator = comprehension.getGenerator();
        // Resolve original list
        CALExpressionNode generatorList = transformExpr(generator.getCollection());
        if (generator.getVarDecls().size() != 1) {
            throw new TransformException("unsupported multiple var decls in for loop", context.getSource(), generator);
        }
        // x = originalList[i]
//        CALExpressionNode[] write = (CALExpressionNode[]) generator.getVarDecls().map(varDecl -> transformVarDecl(varDecl)).toArray();
        CALExpressionNode write = transformVarDecl(generator.getVarDecls().get(0));

        ImmutableList<Expression> filters = comprehension.getFilters();
        CALExpressionNode filter;
        if(filters.size() == 0){
            filter = new BooleanLiteralNode(true);
        }else if(filters.size() == 1){
            filter = transformExpr(filters.get(0));
        }else{
            filter = transformExpr(filters.get(0));
            for(int i = 1; i < filters.size(); ++i){
                filter = new CALBinaryLogicalAndNode(filter, transformExpr(filters.get(i)));
            }
        }

        Expression collection = comprehension.getCollection();
        CALStatementNode[] bodyNodes;
        if(collection instanceof ExprList){
            ExprList collectionList = (ExprList) collection;
            ArrayList<CALStatementNode> stmts = new ArrayList<CALStatementNode>();
            for(int i = 0; i < collectionList.getElements().size(); ++i){
                stmts.add(ListWriteNodeGen.create(getReadNode("$tempList"), getReadNode("$comprehensionCounter"),
                        transformExpr(collectionList.getElements().get(i))));
                stmts.add(createAssignment("$comprehensionCounter", CALBinaryAddNodeGen
                        .create(getReadNode("$comprehensionCounter"), new BigIntegerLiteralNode(new BigInteger("1")))));
            }
            bodyNodes = (CALStatementNode[]) stmts.toArray(new CALStatementNode[stmts.size()]);
        }else if(collection instanceof ExprComprehension){
            bodyNodes = new CALStatementNode[]{(new ExprComprehensionTransformer((ExprComprehension) collection, context.deeper(false))).transform()};
        }else{
            throw new TransformException("Unexpected collection type in for-comprehension", context.getSource(), comprehension);
        }
        CALStatementNode body = new StmtBlockNode(bodyNodes);

        if (write instanceof CALWriteLocalVariableNode) {
            return CALComprehensionNodeGen.create((CALWriteLocalVariableNode) write, filter, body, generatorList);
        }else{
            throw new TransformException("Node write error", context.getSource(), comprehension);
        }
    }
}
