package ch.epfl.vlsc.truffle.cal.runtime;

import java.util.LinkedList;
import java.util.List;

public class CALFifo {
    private List<Object> content = new LinkedList<>();

    public void add(Object val) {
        content.add(val);
    }
    public Object removeFirst() {
        return content.remove(0);
    }
}

