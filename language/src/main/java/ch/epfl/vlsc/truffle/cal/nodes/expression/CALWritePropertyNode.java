package ch.epfl.vlsc.truffle.cal.nodes.expression;

import ch.epfl.vlsc.truffle.cal.nodes.util.CALToMemberNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALUndefinedNameException;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = ".=")
@NodeChild("receiverNode")
@NodeChild("nameNode")
@NodeChild("valueNode")
public abstract class CALWritePropertyNode extends CALExpressionNode {

    static final int LIBRARY_LIMIT = 3;

    @Specialization(guards = "arrays.hasArrayElements(receiver)", limit = "LIBRARY_LIMIT")
    protected Object writeArray(Object receiver, Object index, Object value,
                                @CachedLibrary("receiver") InteropLibrary arrays,
                                @CachedLibrary("index") InteropLibrary numbers) {
        try {
            arrays.writeArrayElement(receiver, numbers.asLong(index), value);
        } catch (UnsupportedMessageException | UnsupportedTypeException | InvalidArrayIndexException e) {
            // read was not successful. In SL we only have basic support for errors.
            throw CALUndefinedNameException.undefinedProperty(this, index);
        }
        return value;
    }

    @Specialization(limit = "LIBRARY_LIMIT")
    protected Object writeObject(Object receiver, Object name, Object value,
                                 @CachedLibrary("receiver") InteropLibrary objectLibrary,
                                 @Cached CALToMemberNode asMember) {
        try {
            objectLibrary.writeMember(receiver, asMember.execute(name), value);
        } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException e) {
            // write was not successful. In SL we only have basic support for errors.
            throw CALUndefinedNameException.undefinedProperty(this, name);
        }
        return value;
    }

}

