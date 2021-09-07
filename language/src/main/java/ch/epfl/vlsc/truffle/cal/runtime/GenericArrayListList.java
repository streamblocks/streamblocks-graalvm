package ch.epfl.vlsc.truffle.cal.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(ListLibrary.class)
public final class GenericArrayListList {
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

    @Override
    public String toString() {
        return "[" + String.join(", ", list.stream().map(x -> x.toString()).collect(Collectors.toList())) + "]";
    }
}
