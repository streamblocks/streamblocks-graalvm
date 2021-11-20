package ch.epfl.vlsc.truffle.cal.nodes.fifo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALEntityInstance;
import ch.epfl.vlsc.truffle.cal.runtime.FifoConsumer;
import com.oracle.truffle.api.frame.VirtualFrame;

public class CALEntityAddOutputPortNode extends CALStatementNode {
    @Child private CALExpressionNode entity;
    @Child private CALExpressionNode fifo;
    private String portname;

    public CALEntityAddOutputPortNode(String portname, CALExpressionNode entity, CALExpressionNode fifo) {
        this.portname = portname;
        this.entity = entity;
        this.fifo = fifo;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        CALEntityInstance entityInstance = (CALEntityInstance) entity.executeGeneric(frame);
        FifoConsumer fifoConsumer = (FifoConsumer) fifo.executeGeneric(frame);
        entityInstance.addOutputPort(portname, fifoConsumer);
    }
}
