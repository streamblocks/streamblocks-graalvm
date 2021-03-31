package ch.epfl.vlsc.truffle.cal.ast;

import java.util.Map;

interface LexicalScope {
    boolean containsKey(String name);
    Map<String, FrameSlotAndDepth> getLocals();
    FrameSlotAndDepth get(String name);
    FrameSlotAndDepth put(String name, FrameSlotAndDepth value);
}
