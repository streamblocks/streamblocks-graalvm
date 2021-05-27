package ch.epfl.vlsc.truffle.cal.runtime;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;

import com.oracle.truffle.api.frame.VirtualFrame;

public class CALUndefinedFunctionRootNode extends CALRootNode {
    public CALUndefinedFunctionRootNode(CALLanguage language, String name) {
        super(language, null, null, null, name);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw CALUndefinedNameException.undefinedFunction(null, getName());
    }
}
