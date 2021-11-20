package ch.epfl.vlsc.truffle.cal.runtime;

import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Collectors;

@ExportLibrary(ListLibrary.class)
public final class GenericBufferList implements com.oracle.truffle.api.interop.TruffleObject {
    private int length;
    private Object[] buffer;

    public GenericBufferList(int length) {
        this.length = length;
        this.buffer = new Object[length];
    }
    public GenericBufferList(Object[] buffer) {
        this.length = buffer.length;
        this.buffer = buffer;
    }

    @ExportMessage boolean isList() {
        return true;
    }
    @ExportMessage int size() {
        return length;
    }
    @ExportMessage Object read(int index) {
        if (index < length)
            return buffer[index];
        else
            throw new IndexOutOfBoundsException(); // TODO custom exception
    }
    @ExportMessage void write(int index, Object value) {
        if (index < length)
            buffer[index] = value;
        else
            throw new IndexOutOfBoundsException(); // TODO custom exception
    }

    @ExportMessage public DFSIterator iterator() {
        DFSIterator it = new DFSIterator() {
            int i = 0;
            DFSIterator it = null;
            @Override
            public boolean hasNext() {
                while(i < buffer.length) {
                    Object curr = buffer[i];
                    if(curr instanceof GenericBufferList) {
                        it = ((GenericBufferList) curr).iterator();
                        if(it.hasNext()) return true;
                        else ++i;
                    } else {
                        it = null;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Object next() {
                if (it != null) return it.next();
                else return buffer[i++];
            }
        };
        return it;
    }

    @Override
    public String toString() {
        return "[" + String.join(", ", Arrays.asList(buffer).stream().map(x -> x.toString()).collect(Collectors.toList())) + "]";
    }
}
