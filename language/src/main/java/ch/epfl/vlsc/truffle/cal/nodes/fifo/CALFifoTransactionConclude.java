package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;

public abstract class CALFifoTransactionConclude extends CALStatementNode {
    protected final CALExpressionNode fifo;
    public CALFifoTransactionConclude(CALExpressionNode readNode){
        this.fifo = readNode;
    }
}
