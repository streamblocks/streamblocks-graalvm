package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtIfNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLogicalAndNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.decl.GeneratorVarDecl;
import se.lth.cs.tycho.ir.expr.ExprComprehension;
import se.lth.cs.tycho.ir.expr.ExprList;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.StmtIf;
import se.lth.cs.tycho.ir.util.ImmutableList;

import java.math.BigInteger;
import java.util.ArrayList;

public class ExprComprehensionTransformer extends ScopedTransformer<CALComprehensionNode> {
    private final ExprComprehension comprehension;
    private final String tempListName;
    private final String listIndexVarName;

    public ExprComprehensionTransformer(ExprComprehension comprehension, String tempListName, String listIndexVarName, TransformContext context) {
        super(context);
        this.comprehension = comprehension;
        this.tempListName = tempListName;
        this.listIndexVarName = listIndexVarName;
    }

    @Override
    public CALComprehensionNode transform() {
        // Resolve original list
        Generator generator = comprehension.getGenerator();
        CALExpressionNode generatorList = transformExpr(generator.getCollection());
        if (generator.getVarDecls().size() != 1) {
            throw new TransformException("Exactly 1 variable declaration in comprehension loops generator supported", context.getSource(), generator);
        }
        // x = originalList[i]
        // CALExpressionNode[] write = (CALExpressionNode[]) generator.getVarDecls().map(varDecl -> transformVarDecl(varDecl)).toArray();
        CALExpressionNode write = transformShadowVarDecl(generator.getVarDecls().get(0));

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
        Boolean lexScope;
        if(collection instanceof ExprList){
            lexScope = false;
            ExprList collectionList = (ExprList) collection;
            ArrayList<CALStatementNode> stmts = new ArrayList<CALStatementNode>();
            for(int i = 0; i < collectionList.getElements().size(); ++i){
                stmts.add(ListWriteNodeGen.create(getReadNode(tempListName), getReadNode(listIndexVarName),
                        transformExpr(collectionList.getElements().get(i))));
                stmts.add(createAssignment(listIndexVarName, CALBinaryAddNodeGen
                        .create(getReadNode(listIndexVarName), new BigIntegerLiteralNode(new BigInteger("1")))));
            }
            bodyNodes = (CALStatementNode[]) stmts.toArray(new CALStatementNode[stmts.size()]);
        }else if(collection instanceof ExprComprehension){
            lexScope = true;
            bodyNodes = new CALStatementNode[]{(new ExprComprehensionTransformer((ExprComprehension) collection, tempListName, listIndexVarName, context.deeper(false))).transform()};
        }else{
            throw new TransformException("Unexpected collection type in for-comprehension", context.getSource(), comprehension);
        }
        CALStatementNode bodyStatement = new StmtIfNode(filter, new StmtBlockNode(bodyNodes),null);
        CALExpressionNode bodyReturnsLast = new ReturnsLastBodyNode(bodyStatement, null);
        CALRootNode bodyRootNode = new CALRootNode(context.getLanguage(), context.getFrameDescriptor(), bodyReturnsLast, getSourceSection(collection), "comprehensionCollection");

        if (write instanceof CALWriteLocalVariableNode) {
            if(lexScope)
                return CALComprehensionContinueNodeGen.create((CALWriteLocalVariableNode) write, bodyRootNode, generatorList);
            else
                return CALComprehensionLeafNodeGen.create((CALWriteLocalVariableNode) write, bodyStatement, generatorList);
        }else{
            throw new TransformException("Node write error", context.getSource(), comprehension);
        }
    }

    private CALExpressionNode transformShadowVarDecl(GeneratorVarDecl varDecl) {
        String name = varDecl.getName();
        Expression value = varDecl.getValue();
        CALExpressionNode valueNode = transformExpr(value);
        FrameSlot frameSlot = context.getFrameDescriptor().findOrAddFrameSlot(name, getTypeInfo(varDecl.getType()), FrameSlotKind.Illegal);
        boolean newVariable = true;
        FrameSlotAndDepth slot;
        slot = new FrameSlotAndDepthRW(frameSlot, context.getDepth());
        context.getLexicalScope().put(name, slot);

        CALExpressionNode nameNode = new StringLiteralNode(name);

        final CALExpressionNode result = slot.createWriteNode(valueNode, nameNode, newVariable, context.getDepth());

        // result.addExpressionTag();

        return result;
    }
}
