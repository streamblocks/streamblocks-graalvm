package ch.epfl.vlsc.truffle.cal.nodes.expression.literals;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALFunction;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class FunctionLiteralNode extends CALExpressionNode {

    /**
     * The name of the function.
     */
    private final String functionName;


    @CompilerDirectives.CompilationFinal
    private CALFunction cachedFunction;

    public FunctionLiteralNode(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public CALFunction executeGeneric(VirtualFrame frame) {
        if (cachedFunction == null) {
            /* We are about to change a @CompilationFinal field. */
            CompilerDirectives.transferToInterpreterAndInvalidate();
            /* First execution of the node: lookup the function in the function registry. */
            cachedFunction = lookupContextReference(CALLanguage.class).get().getFunctionRegistry().lookup(functionName, true);
        }
        return cachedFunction;
    }

}