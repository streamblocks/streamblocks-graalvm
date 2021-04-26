package ch.epfl.vlsc.truffle.cal.runtime;

import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.interop.TruffleObject;

public class CALFifo implements TruffleObject {
    private List<Object> content = new LinkedList<>();

    public int size() {
        return content.size();
    }
    public void add(Object val) {
        content.add(val);
    }
    public Object removeFirst() {
        return content.remove(0);
    }
}

