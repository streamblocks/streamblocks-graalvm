package ch.epfl.vlsc.truffle.cal.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.ExprUnboxNodeGen;

public class ExprIfNode extends CALExpressionNode {

    /**
     * The condition of the {@code if}. This in a {@link ExprNode} because we require a
     * result value. We do not have a node type that can only return a {@code boolean} value, so
     * {@link #evaluateCondition executing the condition} can lead to a type error.
     */
    @Node.Child
    private CALExpressionNode conditionNode;

    @Node.Child
    private CALExpressionNode thenPartNode;

    @Node.Child
    private CALExpressionNode elsePartNode;


    private final ConditionProfile condition = ConditionProfile.createCountingProfile();

    public ExprIfNode(CALExpressionNode conditionNode, CALExpressionNode thenPartNode, CALExpressionNode elsePartNode) {
        this.conditionNode = ExprUnboxNodeGen.create(conditionNode);
        this.thenPartNode = thenPartNode;
        this.elsePartNode = elsePartNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        /*
         * In the interpreter, record profiling information that the condition was executed and with
         * which outcome.
         */
        if (condition.profile(evaluateCondition(frame))) {
            /* Execute the then-branch. */
            return thenPartNode.executeGeneric(frame);
        } else {
            /* Execute the else-branch (which is optional according to the CAL syntax). */
            return elsePartNode.executeGeneric(frame);
        }
    }

    private boolean evaluateCondition(VirtualFrame frame) {
        try {
            /*
             * The condition must evaluate to a boolean value, so we call the boolean-specialized
             * execute method.
             */
            return conditionNode.executeBoolean(frame);
        } catch (UnexpectedResultException ex) {
            /*
             * The condition evaluated to a non-boolean result. This is a type error in the SL
             * program.
             */
            throw CALException.typeError(this, ex.getResult());
        }
    }
}
