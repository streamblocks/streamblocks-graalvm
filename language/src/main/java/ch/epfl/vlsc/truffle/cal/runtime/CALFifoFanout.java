package ch.epfl.vlsc.truffle.cal.runtime;

import java.util.LinkedList;
import java.util.List;

public class CALFifoFanout implements com.oracle.truffle.api.interop.TruffleObject, FifoConsumer{
    private List<FifoConsumer> connectedFifos;
    private CALFifoFanout fanout;

    public CALFifoFanout(){
        this.connectedFifos = new LinkedList<FifoConsumer>();
    }

    public void add(Object val) {
        for(FifoConsumer connectedNode: connectedFifos){
            connectedNode.add(val);
        }
    }

    @Override
    public CALFifoFanout getFanout() {
        return fanout;
    }

    @Override
    public void setFanout(CALFifoFanout fanout) {
        this.fanout = fanout;
    }

    public void addFifo(FifoConsumer fifo){
        connectedFifos.add(fifo);
    }
}
