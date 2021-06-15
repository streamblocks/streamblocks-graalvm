package ch.epfl.vlsc.truffle.cal.ast;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.ActionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ForeacheNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLessOrEqualNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLogicalAndNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinarySubNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALFIFOSizeNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALReadFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALWriteFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListRangeInitNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListReadNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListWriteNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.entity.cal.InputPattern;
import se.lth.cs.tycho.ir.entity.cal.OutputExpression;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.expr.pattern.Pattern;
import se.lth.cs.tycho.ir.expr.pattern.PatternBinding;
import se.lth.cs.tycho.ir.stmt.Statement;

public class ActionTransformer extends ScopedTransformer<ActionNode> {

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
        CALStatementNode[] body = new CALStatementNode[action.getInputPatterns().size()
                + action.getOutputExpressions().size() + action.getBody().size() + action.getVarDecls().size()];
        int i = 0;

        for (InputPattern input : action.getInputPatterns()) {
            // TODO implement patterns
            // FIXME
            Pattern pat = input.getMatches().get(0).getExpression().getAlternatives().get(0).getPattern();
            String name;
            if (pat instanceof PatternBinding)
                name = ((PatternBinding) pat).getDeclaration().getName();
            else
                throw new TransformException("Pattern not implemented", context.getSource(), pat);

            if (input.getRepeatExpr() != null)
                body[i] = createAssignment(name, batchReadInput(input));
            else
                body[i] = createAssignment(name, new CALReadFIFONode(getReadNode(input.getPort().getName())));
            i++;
        }
        // Prepend local variable declarations to the body nodes
        for (LocalVarDecl varDecl : action.getVarDecls()) {
            body[i] = transformVarDecl(varDecl);
            i++;
        }
        for (Statement statement : action.getBody()) {
            body[i] = transformSatement(statement);
            i++;
        }
        // get the variables name linked to the output and add a write to the FIFO
        // in the tail of the action
        for (OutputExpression output : action.getOutputExpressions()) {
            CALExpressionNode fifo = getReadNode(output.getPort().getName());
            if (output.getExpressions().size() > 1)
                throw new TransformException("More than one output expr not supported", context.getSource(), output);
            if (output.getRepeatExpr() == null) {
                CALExpressionNode value = transformExpr(output.getExpressions().get(0));
                body[i] = new CALWriteFIFONode(fifo, value);
            } else {
                body[i] = batchWriteOutput(output);
            }
            i++;
        }

        // Firing conditions
        List<CALExpressionNode> firingConditions = new LinkedList<>();
        // check there is enough tokens to bind
        for (InputPattern input : action.getInputPatterns()) {
            // TODO implement patterns
            Pattern pat = input.getMatches().get(0).getExpression().getAlternatives().get(0).getPattern();
            String name;
            if (pat instanceof PatternBinding)
                name = ((PatternBinding) pat).getDeclaration().getName();
            else
                throw new TransformException("Pattern not implemented", context.getSource(), pat);
            if (input.getRepeatExpr() == null)
                firingConditions.add(CALBinaryLessOrEqualNodeGen.create(new LongLiteralNode(1),
                        new CALFIFOSizeNode(getReadNode(input.getPort().getName()))));
            else
                firingConditions.add(CALBinaryLessOrEqualNodeGen.create(transformExpr(input.getRepeatExpr()),
                        new CALFIFOSizeNode(getReadNode(input.getPort().getName()))));

                
        }
        // Parse guards
        for (Expression guard : action.getGuards()) {
            firingConditions.add(transformExpr(guard));
        }
        // Build the boolean expression to determine whether an action is fireable
        CALExpressionNode firingCondition;
        if (firingConditions.size() > 0) {
            firingCondition = firingConditions.remove(0);
            for (CALExpressionNode cond : firingConditions)
                firingCondition = new CALBinaryLogicalAndNode(firingCondition, cond);
        } else {
            firingCondition = new BooleanLiteralNode(true);
        }

        StmtBlockNode block = new StmtBlockNode(body);
        ActionBodyNode bodyNode = new ActionBodyNode(block);
        SourceSection actionSrc = getSourceSection(action);
        String name;
        if (action.getTag() != null)
        	name = action.getTag().toString();
        else
        	name = "unnammed action";
        return new ActionNode(context.getLanguage(), context.getFrameDescriptor(), bodyNode, firingCondition, actionSrc, name);
    }
}
