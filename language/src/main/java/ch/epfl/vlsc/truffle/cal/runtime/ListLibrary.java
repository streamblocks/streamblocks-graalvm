package ch.epfl.vlsc.truffle.cal.runtime;

import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;

import java.util.Iterator;

@GenerateLibrary
public abstract class ListLibrary extends Library{
    public boolean isList(Object receiver) {
        return false;
    }

    public abstract int size(Object receiver);
    public abstract Object read(Object receiver, int index);
    public abstract void write(Object receiver, int index, Object value);
    public abstract DFSIterator iterator(Object receiver);
}
