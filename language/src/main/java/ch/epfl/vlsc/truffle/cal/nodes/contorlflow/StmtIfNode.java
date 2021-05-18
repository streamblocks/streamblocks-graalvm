package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.ExprUnboxNodeGen;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

public class StmtIfNode extends CALStatementNode {

    /**
     * The condition of the {@code if}. This in a {@link ExprNode} because we require a
     * result value. We do not have a node type that can only return a {@code boolean} value, so
     * {@link #evaluateCondition executing the condition} can lead to a type error.
     */
    @Node.Child
    private CALExpressionNode conditionNode;

    @Node.Child
    private CALStatementNode thenPartNode;

    @Node.Child
    private CALStatementNode elsePartNode;


    private final ConditionProfile condition = ConditionProfile.createCountingProfile();

    public StmtIfNode(CALExpressionNode conditionNode, CALStatementNode thenPartNode, CALStatementNode elsePartNode) {
        this.conditionNode = ExprUnboxNodeGen.create(conditionNode);
        this.thenPartNode = thenPartNode;
        this.elsePartNode = elsePartNode;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        /*
         * In the interpreter, record profiling information that the condition was executed and with
         * which outcome.
         */
        if (condition.profile(evaluateCondition(frame))) {
            /* Execute the then-branch. */
            thenPartNode.executeVoid(frame);
        } else {
            /* Execute the else-branch (which is optional according to the CAL syntax). */
            if (elsePartNode != null) {
                elsePartNode.executeVoid(frame);
            }
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
