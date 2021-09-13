package ch.epfl.vlsc.truffle.cal.runtime;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.truffle.api.interop.TruffleObject;

public class CALFifo implements TruffleObject, FifoConsumer {
    private List<Object> content = new LinkedList<>();
    private boolean transactionActive = false;
    private int transactionIndex = 0;
    private CALFifoFanout fanout;

    public int size() {
        return content.size();
    }
    public void add(Object val) {
        content.add(val);
    }

    public Object removeFirst() {
        if(transactionActive){
            Object to_ret = content.get(transactionIndex);
            transactionIndex++;
            return to_ret;
        }else
            return content.remove(0);
    }

    public void startTransaction(){
        transactionActive = true;
        transactionIndex = 0;
    }

    public void commit(){
        transactionActive = false;
        for(int i = 0; i < transactionIndex; ++i){
            content.remove(0);
        }
        transactionIndex = 0;
    }

    public void rollback(){
        transactionActive = false;
        transactionIndex = 0;
    }

    public void setFanout(CALFifoFanout fanout) {
        this.fanout = fanout;
    }

    public CALFifoFanout getFanout() {
        return fanout;
    }

    @Override
    public String toString() {
        return "[" + String.join(", ", content.stream().map(x -> x.toString()).collect(Collectors.toList())) + "]";
    }
}

