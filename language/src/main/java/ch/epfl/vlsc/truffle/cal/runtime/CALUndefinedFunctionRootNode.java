package ch.epfl.vlsc.truffle.cal.runtime;

import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;

public class CALUndefinedFunctionRootNode extends CALRootNode {
    public CALUndefinedFunctionRootNode(CALLanguage language, String name) {
        super(language, null, null, null, name);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw CALUndefinedNameException.undefinedFunction(null, getName());
    }
}
