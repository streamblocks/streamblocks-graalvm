package ch.epfl.vlsc.truffle.cal.ast;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.*;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.*;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListRangeInitNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.entity.cal.InputPattern;
import se.lth.cs.tycho.ir.entity.cal.OutputExpression;
import se.lth.cs.tycho.ir.expr.pattern.Pattern;
import se.lth.cs.tycho.ir.expr.pattern.PatternBinding;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.type.TypeExpr;

public class ActionTransformer extends ScopedTransformer<ActionNode> {

    public static final String UNNAMED_ACTION = "unnammed action";
    Action action;

    // FIXME we should use different frameDescriptor as the one used in actor is
    // persistent and this one should not
    public ActionTransformer(Action action, TransformContext context) {
        super(context);
        this.action = action;
    }

    public CALExpressionNode batchReadInput(InputPattern input) {
        CALStatementNode[] init = new CALStatementNode[3];
        // Create tempList
        // TODO in fact type might be known and size is probably also known
        init[0] = createAssignment("$tempList", new UnknownSizeListInitNode());
        // FIXME
        init[1] = createAssignment("$comprehensionCounter", new BigIntegerLiteralNode(new BigInteger("0")));

        // Create assignment to variable
        CALExpressionNode list = ListRangeInitNodeGen.create(new BigIntegerLiteralNode(new BigInteger("0")),
                CALBinarySubNodeGen.create(transformExpr(input.getRepeatExpr()), new LongLiteralNode(1)));
        //
        CALStatementNode[] bodyNodes = new CALStatementNode[2];
        bodyNodes[0] = ListWriteNodeGen.create(getReadNode("$tempList"), getReadNode("$comprehensionCounter"),
                new CALReadFIFONode(getReadNode(input.getPort().getName())));
        bodyNodes[1] = createAssignment("$comprehensionCounter", CALBinaryAddNodeGen
                .create(getReadNode("$comprehensionCounter"), new BigIntegerLiteralNode(new BigInteger("1"))));

        CALStatementNode body = new StmtBlockNode(bodyNodes);

        init[2] = ForeacheNodeGen.create(body, null, list);
        return new ReturnsLastBodyNode(new StmtBlockNode(init), getReadNode("$tempList"));
    }

    public CALExpressionNode batchWriteOutput(OutputExpression output) {
        CALStatementNode[] init = new CALStatementNode[3];
        // Create tempList
        // TODO in fact type might be known and size is probably also known
        init[0] = createAssignment("$tempList", new UnknownSizeListInitNode());
        // FIXME
        init[1] = createAssignment("$comprehensionCounter", new BigIntegerLiteralNode(new BigInteger("0")));

        // Create assignment to variable
        // FIXME
        CALExpressionNode list = ListRangeInitNodeGen.create(new BigIntegerLiteralNode(new BigInteger("0")),
                CALBinarySubNodeGen.create(transformExpr(output.getRepeatExpr()), new LongLiteralNode(1)));
        //
        CALStatementNode[] bodyNodes = new CALStatementNode[2];
        bodyNodes[0] = new CALWriteFIFONode(
                getReadNode(output.getPort().getName()), 
                ListReadNodeGen.create(
                    transformExpr(output.getExpressions().get(0)), 
                getReadNode(("$comprehensionCounter"))));
        bodyNodes[1] = createAssignment("$comprehensionCounter", CALBinaryAddNodeGen
                .create(getReadNode("$comprehensionCounter"), new BigIntegerLiteralNode(new BigInteger("1"))));

        CALStatementNode body = new StmtBlockNode(bodyNodes);

        init[2] = ForeacheNodeGen.create(body, null, list);
        return new ReturnsLastBodyNode(new StmtBlockNode(init), getReadNode("$tempList"));
    }

    public ActionNode transform() {
        ArrayList<CALStatementNode> bodyArr = new ArrayList<CALStatementNode>();
        bodyArr.ensureCapacity(action.getInputPatterns().size() + action.getOutputExpressions().size() + action.getBody().size() + action.getVarDecls().size());

        // Firing conditions
        List<CALExpressionNode> firingConditions = new LinkedList<>();
        List<CALStatementNode> inputTokenBindings = new LinkedList<>();
        List<CALFifoTransactionCommit> transactionCommits = new LinkedList<>();
        List<CALFifoTransactionRollback> transactionRollbacks = new LinkedList<>();

        for(InputPattern input: action.getInputPatterns()) {
            inputTokenBindings.add(new CALFifoTransactionInit(getReadNode(input.getPort().getName())));
            if (input.getRepeatExpr() == null) {
                for (int j = 0; j < input.getMatches().size(); ++j) {
                    Pattern pat = input.getMatches().get(j).getExpression().getAlternatives().get(0).getPattern();
                    String name;
                    TypeExpr type;
                    if (pat instanceof PatternBinding){
                        name = ((PatternBinding) pat).getDeclaration().getName();
                        type = ((PatternBinding) pat).getDeclaration().getType();
                    }
                    else
                        throw new TransformException("Pattern not implemented", context.getSource(), pat);
                    inputTokenBindings.add(createAssignment(name, type, new CALReadFIFONode(getReadNode(input.getPort().getName()))));
                }
                firingConditions.add(CALBinaryLessOrEqualNodeGen.create(new LongLiteralNode(input.getMatches().size()),
                        new CALFIFOSizeNode(getReadNode(input.getPort().getName()))));
            } else {
                ArrayList<String> inputExprNames = new ArrayList<String>();
                ArrayList<TypeExpr> types = new ArrayList<TypeExpr>();
                inputExprNames.ensureCapacity(input.getMatches().size());
                for (int j = 0; j < input.getMatches().size(); ++j) {
                    Pattern pat = input.getMatches().get(j).getExpression().getAlternatives().get(0).getPattern();
                    String name;
                    if (pat instanceof PatternBinding) {
                        name = ((PatternBinding) pat).getDeclaration().getName();
                        inputExprNames.add(name);
                        types.add(((PatternBinding) pat).getDeclaration().getType());
                    } else
                        throw new TransformException("Pattern not implemented", context.getSource(), pat);
                }
                List<CALExpressionNode> listReadNodes = createRepeatBatchInput(inputTokenBindings, inputExprNames, types, input.getPort().getName(), transformExpr(input.getRepeatExpr()));
                for (int j = 0; j < listReadNodes.size(); ++j) {
                    inputTokenBindings.add(createAssignment(inputExprNames.get(j), types.get(j), listReadNodes.get(j)));
                }
                firingConditions.add(CALBinaryLessOrEqualNodeGen.create(CALBinaryMulNodeGen.create(transformExpr(input.getRepeatExpr()), new LongLiteralNode(input.getMatches().size())),
                        new CALFIFOSizeNode(getReadNode(input.getPort().getName()))));
            }
            transactionCommits.add(new CALFifoTransactionCommit(getReadNode(input.getPort().getName())));
            transactionRollbacks.add(new CALFifoTransactionRollback(getReadNode(input.getPort().getName())));
        }

        CALExpressionNode guardCondition;
        // Parse guards
        if(action.getGuards().size() > 0){
            int i = 0;
            guardCondition = transformExpr(action.getGuards().get(i++));
            while(i < action.getGuards().size()){
                guardCondition = new CALBinaryLogicalAndNode(guardCondition, transformExpr(action.getGuards().get(i++)));
            }
        } else {
            guardCondition = new BooleanLiteralNode(true);
        }

        firingConditions.add(new ReturnsLastBodyNode(new StmtBlockNode(inputTokenBindings.toArray(new CALStatementNode[inputTokenBindings.size()])), guardCondition));

        // Build the boolean expression to determine whether an action is fireable
        CALExpressionNode firingCondition;
        if (firingConditions.size() > 0) {
            firingCondition = firingConditions.remove(0);
            for (CALExpressionNode cond : firingConditions)
                firingCondition = new CALBinaryLogicalAndNode(firingCondition, cond);
        } else {
            firingCondition = new BooleanLiteralNode(true);
        }

        // Prepend local variable declarations to the body nodes
        for (LocalVarDecl varDecl : action.getVarDecls()) {
            bodyArr.add(transformVarDecl(varDecl));
        }
        for (Statement statement : action.getBody()) {
            bodyArr.add(transformStatement(statement));
        }

        // get the variables name linked to the output and add a write to the FIFO
        // in the tail of the action
        for (OutputExpression output : action.getOutputExpressions()) {
            CALExpressionNode fifo = getReadNode(output.getPort().getName());
            if (output.getRepeatExpr() == null) {
                bodyArr.addAll(output.getExpressions()
                        .map(
                            expr -> transformExpr(expr)
                        ).map(
                            value -> new CALWriteFIFONode(fifo, value)
                        ));
            } else {
                if (output.getExpressions().size() > 1)
                    throw new TransformException("More than one output expr not supported", context.getSource(), output);
                bodyArr.add(batchWriteOutput(output));
            }
        }

        CALStatementNode[] body = bodyArr.toArray(new CALStatementNode[bodyArr.size()]);

        StmtBlockNode block = new StmtBlockNode(body);
        ActionBodyNode bodyNode = new ActionBodyNode(block);
        SourceSection actionSrc = getSourceSection(action);

        QualifiedID name;
        boolean isQidTagged;
        if (action.getTag() != null) {
            name = new QualifiedID(action.getTag().parts());
            isQidTagged = true;
        }else {
            name = QualifiedID.of(UNNAMED_ACTION);
            isQidTagged = false;
        }
        return new ActionNode(context.getLanguage(), context.getFrameDescriptor(), bodyNode, firingCondition, actionSrc, name, isQidTagged, transactionCommits.toArray(new CALStatementNode[transactionCommits.size()]), transactionRollbacks.toArray(new CALStatementNode[transactionRollbacks.size()]));
    }

    private List<CALExpressionNode> createRepeatBatchInput(List<CALStatementNode> bodyArr, ArrayList<String> inputExprNames, ArrayList<TypeExpr> types, String portName, CALExpressionNode repeatExpr) {
        LinkedList<CALStatementNode> init = new LinkedList<CALStatementNode>();
        List<String> tempListNames = inputExprNames.stream().map(exprName -> "$tempList" + exprName.hashCode()).collect(Collectors.toList());
        List<String> comprehensionCounterVarNames = inputExprNames.stream().map(exprName -> "$comprehensionCounter" + exprName.hashCode()).collect(Collectors.toList());
        for(int i = 0; i < inputExprNames.size(); ++i){
            init.add(createAssignment(tempListNames.get(i), new UnknownSizeListInitNode()));
            init.add(createAssignment(comprehensionCounterVarNames.get(i), new BigIntegerLiteralNode(new BigInteger("0"))));
        }

        // Create assignment to variable
        CALExpressionNode list = ListRangeInitNodeGen.create(new BigIntegerLiteralNode(new BigInteger("0")),
                CALBinarySubNodeGen.create(repeatExpr, new LongLiteralNode(1)));
        //
        ArrayList<CALStatementNode> bodyNodes = new ArrayList<CALStatementNode>();
        for(int i = 0; i < inputExprNames.size(); ++i){
            bodyNodes.add(ListWriteNodeGen.create(getReadNode(tempListNames.get(i)), getReadNode(comprehensionCounterVarNames.get(i)),
                    getTypeInfo(types.get(i)).create(new CALReadFIFONode(getReadNode(portName)))));
            bodyNodes.add(createAssignment(comprehensionCounterVarNames.get(i), CALBinaryAddNodeGen
                    .create(getReadNode(comprehensionCounterVarNames.get(i)), new BigIntegerLiteralNode(new BigInteger("1")))));
        }
        CALStatementNode body = new StmtBlockNode(bodyNodes.toArray(new CALStatementNode[bodyNodes.size()]));

        init.add(ForeacheNodeGen.create(body, null, list));
        bodyArr.addAll(init);
        return tempListNames.stream().map(listName -> getReadNode(listName)).collect(Collectors.toList()); //new ReturnsLastBodyNode(new StmtBlockNode(init.toArray(new CALStatementNode[])), getReadNode("$tempList"));
    }
}
