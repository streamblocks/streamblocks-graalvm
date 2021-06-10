package ch.epfl.vlsc.truffle.cal.ast;

import java.util.HashMap;
import java.util.Map;

public class LexicalScopeRW implements LexicalScope {
    private final LexicalScope outer;
    private final Map<String, FrameSlotAndDepth> locals;

    LexicalScopeRW(LexicalScope outer) {
        this.outer = outer;
        this.locals = new HashMap<>();
        if (outer != null) {
            locals.putAll(outer.getLocals());
        }
    }

    @Override
    public Map<String, FrameSlotAndDepth> getLocals() {
        return locals;
    }

    @Override
    public boolean containsKey(String name) {
        return locals.containsKey(name);
    }
    
    @Override
    public FrameSlotAndDepth get(String name) {
        return locals.get(name);
    }

    @Override
    public FrameSlotAndDepth put(String name, FrameSlotAndDepth value) {
        return locals.put(name, value);
    }
}
