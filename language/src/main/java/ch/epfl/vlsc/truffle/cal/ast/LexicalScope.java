package ch.epfl.vlsc.truffle.cal.ast;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.frame.FrameSlot;

class LexicalScope {
    protected final LexicalScope outer;
    protected final Map<String, FrameSlot> locals;

    LexicalScope(LexicalScope outer) {
        this.outer = outer;
        this.locals = new HashMap();
        if (outer != null) {
            locals.putAll(outer.locals);
        }
    }
}
