package ch.epfl.vlsc.truffle.cal.nodes.contorlflow;

import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;

@NodeInfo(shortName = "debugger", description = "The node implementing a debugger statement")
public class CALDebuggerNode extends CALStatementNode {
    @Override
    public void executeVoid(VirtualFrame frame) {
        // No op.
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == DebuggerTags.AlwaysHalt.class) {
            return true;
        }
        return super.hasTag(tag);
    }
}
