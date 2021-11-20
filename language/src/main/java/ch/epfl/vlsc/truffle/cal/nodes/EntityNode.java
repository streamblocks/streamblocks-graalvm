package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;

public abstract class EntityNode extends CALRootNode{
    public EntityNode(CALLanguage language, FrameDescriptor frameDescriptor, CALExpressionNode bodyNode, SourceSection sourceSection, String name) {
        super(language, frameDescriptor, bodyNode, sourceSection, name);
    }
}
