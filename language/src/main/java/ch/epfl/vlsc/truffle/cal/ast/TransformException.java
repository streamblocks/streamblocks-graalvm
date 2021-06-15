package ch.epfl.vlsc.truffle.cal.ast;

import com.oracle.truffle.api.source.Source;

import se.lth.cs.tycho.ir.IRNode;

public class TransformException extends RuntimeException {
    public TransformException(String message, Source source) {
        super(String.format("%s at %s", message, source.getName()));
    }
    public TransformException(String message, Source source, IRNode node) {
        super(String.format("%s at %s:%d:%d", message, source.getName(), node.getFromLineNumber(), node.getFromColumnNumber()));
    }
}
