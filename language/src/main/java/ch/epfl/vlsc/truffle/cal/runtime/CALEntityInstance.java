package ch.epfl.vlsc.truffle.cal.runtime;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.nodes.EntityNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

public abstract class CALEntityInstance extends CALValue{
    public @CompilerDirectives.CompilationFinal MaterializedFrame frameDecl;

    public abstract void addInputPort(String portName, FifoConsumer fifo);

    public abstract void addOutputPort(String portName, FifoConsumer fifo);
}
