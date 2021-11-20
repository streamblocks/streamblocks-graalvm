package ch.epfl.vlsc.truffle.cal.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(ListLibrary.class)
public final class GenericArrayListList implements TruffleObject {
    private List<Object> list;

    public GenericArrayListList(int length) {
        this.list = new ArrayList<>(length);
    }

    public GenericArrayListList(Object[] buffer) {
        this.list = new ArrayList<>(Arrays.asList(buffer));
    }

    @ExportMessage
    boolean isList() {
        return true;
    }

    @ExportMessage
    int size() {
        return list.size();
    }

    @ExportMessage
    Object read(int index) {
        if (index < list.size())
            return list.get(index);
        else
            throw new IndexOutOfBoundsException(); // TODO custom exception
    }

    @ExportMessage
    void write(int index, Object value) {
        if (index < list.size())
            list.set(index, value);
        else if (index == list.size())
            list.add(value);
        else
            throw new IndexOutOfBoundsException(); // TODO custom exception
    }

    @ExportMessage public DFSIterator iterator() {
        DFSIterator it = new DFSIterator() {
            int i = 0;
            DFSIterator it = null;
            @Override
            public boolean hasNext() {
                if (it != null) {
                    if (it.hasNext()) return true;
                    else ++i;
                }
                while(i < list.size()) {
                    Object curr = list.get(i);
                    if(curr instanceof GenericArrayListList) {
                        it = ((GenericArrayListList) curr).iterator();
                        if(it.hasNext()) return true;
                        else ++i;
                    } else{
                        it = null;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Object next() {
                if (it != null) return it.next();
                else return list.get(i++);
            }
        };
        return it;
    }

    @Override
    public String toString() {
        return "[" + String.join(", ", list.stream().map(x -> x.toString()).collect(Collectors.toList())) + "]";
    }
}
