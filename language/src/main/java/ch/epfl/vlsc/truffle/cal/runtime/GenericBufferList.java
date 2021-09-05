package ch.epfl.vlsc.truffle.cal.runtime;

import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

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
}
