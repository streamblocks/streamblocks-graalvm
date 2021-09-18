package ch.epfl.vlsc.truffle.cal.nodes.local;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

public abstract class CALWriteVariableNode extends CALExpressionNode implements FrameSlotNode {
    private boolean hasWriteVarTag;

    public void setHasWriteVarTag() {
        this.hasWriteVarTag = true;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.WriteVariableTag.class) {
            return hasWriteVarTag;
        } else
            return super.hasTag(tag);
    }
}
