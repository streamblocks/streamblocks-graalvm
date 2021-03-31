package ch.epfl.vlsc.truffle.cal.ast;

import java.util.HashMap;
import java.util.Map;

/* Define a lexical scope that can't write on
 * parent scopes 
 */
class ROParentLexicalScope implements LexicalScope {
    private final LexicalScope outer;
    private final Map<String, FrameSlotAndDepth> locals;

    ROParentLexicalScope(LexicalScope outer) {
        this.outer = outer;
        this.locals = new HashMap<>();
    }

    public boolean containsKey(String name) {
        return locals.containsKey(name) || outer.containsKey(name);
    }
    public FrameSlotAndDepth get(String name) {
        FrameSlotAndDepth val = locals.get(name);
        if (val == null)
            return new FrameSlotAndDepthRO(outer.get(name));
        else
            return val;
    }
    @Override
    public FrameSlotAndDepth put(String name, FrameSlotAndDepth value) {
        return locals.put(name, value);
    }

	@Override
	public Map<String, FrameSlotAndDepth> getLocals() {
		Map<String, FrameSlotAndDepth> all = new HashMap<>(outer.getLocals());
		all.putAll(locals);
		return all;
	}


}
