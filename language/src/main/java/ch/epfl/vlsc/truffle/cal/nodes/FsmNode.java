package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;

public class FsmNode extends CALRootNode{
    public FsmNode(CALLanguage language, FrameDescriptor frameDescriptor, CALExpressionNode bodyNode, SourceSection sourceSection, String name) {
        super(language, frameDescriptor, bodyNode, sourceSection, name);
    }
}
