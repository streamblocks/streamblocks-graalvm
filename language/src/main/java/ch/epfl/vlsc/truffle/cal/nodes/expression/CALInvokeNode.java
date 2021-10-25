package ch.epfl.vlsc.truffle.cal.nodes.expression;

import ch.epfl.vlsc.truffle.cal.runtime.DFSIterator;
import ch.epfl.vlsc.truffle.cal.runtime.GenericBufferList;
import ch.epfl.vlsc.truffle.cal.runtime.ListLibrary;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALUndefinedNameException;
import com.oracle.truffle.dsl.processor.library.LibraryGenerator;

import java.util.Iterator;

@NodeInfo(shortName = "call")
@NodeChild(type = CALExpressionNode.class, value="function")
public abstract class CALInvokeNode extends CALExpressionNode {
    @Node.Children
    private final CALExpressionNode[] argumentNodes;
    @Node.Child
    private InteropLibrary library;

    public CALInvokeNode(CALExpressionNode[] argumentNodes) {
        this.argumentNodes = argumentNodes;
        this.library = InteropLibrary.getFactory().createDispatched(3);
    }

    @Specialization(guards = "lists.isList(function)", limit="3")
    @ExplodeLoop
    public Object doList(VirtualFrame frame, Object function, @CachedLibrary("function") ListLibrary lists) {
        /*
         * The number of arguments is constant for one invoke node. During compilation, the loop is
         * unrolled and the execute methods of all arguments are inlined. This is triggered by the
         * ExplodeLoop annotation on the method. The compiler assertion below illustrates that the
         * array length is really constant.
         */
        CompilerAsserts.compilationConstant(argumentNodes.length);

        Object[] argumentValues = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            argumentValues[i] = argumentNodes[i].executeGeneric(frame);
        }

        boolean actorExecuted = false;
        for(DFSIterator it = lists.iterator(function); it.hasNext();) {
            try {
                actorExecuted = actorExecuted | (boolean) library.execute(it.next(), argumentValues);
            } catch (ArityException | UnsupportedTypeException | UnsupportedMessageException e) {
                /* Execute was not successful. */
                throw CALUndefinedNameException.undefinedFunction(this, function);
            }
        }
        return actorExecuted;
    }

    @Specialization
    @ExplodeLoop
    public Object doDefault(VirtualFrame frame, Object function) {

        /*
         * The number of arguments is constant for one invoke node. During compilation, the loop is
         * unrolled and the execute methods of all arguments are inlined. This is triggered by the
         * ExplodeLoop annotation on the method. The compiler assertion below illustrates that the
         * array length is really constant.
         */
        CompilerAsserts.compilationConstant(argumentNodes.length);

        Object[] argumentValues = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            argumentValues[i] = argumentNodes[i].executeGeneric(frame);
        }

        try {
            if (function instanceof GenericBufferList) {
                GenericBufferList entityList = (GenericBufferList) function;
                return false;
            } else
                return library.execute(function, argumentValues);
        } catch (ArityException | UnsupportedTypeException | UnsupportedMessageException e) {
            /* Execute was not successful. */
            throw CALUndefinedNameException.undefinedFunction(this, function);
        }
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.CallTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

}
