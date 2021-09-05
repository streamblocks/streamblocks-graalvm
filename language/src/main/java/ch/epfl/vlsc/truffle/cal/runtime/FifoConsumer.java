package ch.epfl.vlsc.truffle.cal.runtime;

public interface FifoConsumer {
    public void add(Object val);
    public CALFifoFanout getFanout();
    public void setFanout(CALFifoFanout fanout);
}
