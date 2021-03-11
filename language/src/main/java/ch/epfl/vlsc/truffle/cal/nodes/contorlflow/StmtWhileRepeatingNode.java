package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ExprUnboxNodeGen;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;

public final class StmtWhileRepeatingNode extends Node implements RepeatingNode {


    @Child
    private CALExpressionNode conditionNode;

    @Child
    private CALStatementNode bodyNode;

    /**
     * Profiling information, collected by the interpreter, capturing whether a {@code continue}
     * statement was used in this loop. This allows the compiler to generate better code for loops
     * without a {@code continue}.
     */
    private final BranchProfile continueTaken = BranchProfile.create();
    private final BranchProfile breakTaken = BranchProfile.create();

    public StmtWhileRepeatingNode(CALExpressionNode conditionNode, CALStatementNode bodyNode) {
        this.conditionNode = ExprUnboxNodeGen.create(conditionNode);
        this.bodyNode = bodyNode;
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
        if (!evaluateCondition(frame)) {
            /* Normal exit of the loop when loop condition is false. */
            return false;
        }

        try {
            /* Execute the loop body. */
            bodyNode.executeVoid(frame);
            /* Continue with next loop iteration. */
            return true;

        } catch (ContinueException ex) {
            /* In the interpreter, record profiling information that the loop uses continue. */
            continueTaken.enter();
            /* Continue with next loop iteration. */
            return true;

        } catch (BreakException ex) {
            /* In the interpreter, record profiling information that the loop uses break. */
            breakTaken.enter();
            /* Break out of the loop. */
            return false;
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
             * program. We report it with the same exception that Truffle DSL generated nodes use to
             * report type errors.
             */
            throw new UnsupportedSpecializationException(this, new Node[]{conditionNode}, ex.getResult());
        }
    }

    @Override
    public String toString() {
        return CALStatementNode.formatSourceSection(this);
    }

}
