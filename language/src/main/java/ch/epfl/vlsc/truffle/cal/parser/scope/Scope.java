package ch.epfl.vlsc.truffle.cal.parser.scope;

import com.oracle.truffle.api.frame.FrameDescriptor;

import java.util.HashMap;
import java.util.Map;

public class Scope {

    public enum ScopeKind {
        RW,
        PARENT_RO
    }

    private final Scope outerScope;

    private final Map<String, DepthFrameSlot> localVariables;

    private final Map<String, DepthFrameSlot> allVariables;

    private final FrameDescriptor frame;

    private int depth;

    private final ScopeKind kind;

    public Scope(Scope outerScope, FrameDescriptor frame, int depth, ScopeKind kind) {
        this.outerScope = outerScope;
        this.frame = frame;
        this.depth = depth;
        this.kind = kind;

        this.localVariables = new HashMap<>();

        this.allVariables = new HashMap<>();
        if (outerScope != null) {
            allVariables.putAll(outerScope.allVariables);
        }
    }

    public Scope getOuterScope() {
        return outerScope;
    }

    public FrameDescriptor getFrame() {
        return frame;
    }

    public int getDepth() {
        return depth;
    }

    public void increaseDepth() {
        this.depth++;
    }

    public void decreaseDepth() {
        this.depth--;
    }

    public ScopeKind getKind() {
        return kind;
    }

    public Map<String, DepthFrameSlot> getLocalVariables() {
        return localVariables;
    }

    public Map<String, DepthFrameSlot> getAllVariables() {
        return allVariables;
    }

    public boolean containsKey(String name) {
        return allVariables.containsKey(name);
    }

    public DepthFrameSlot get(String name) {
        switch (kind) {
            case PARENT_RO:
                DepthFrameSlot slot = localVariables.get(name);
                if (slot != null) {
                    return slot;
                }

                DepthFrameSlot parentSlot = outerScope.allVariables.get(name);
                if (parentSlot != null) {
                    return new DepthFrameSlot(parentSlot);
                }

                return null;
            case RW:
                return allVariables.get(name);
            default:
                return null;
        }
    }

    public void put(String name, DepthFrameSlot slot) {
        localVariables.put(name, slot);
        allVariables.put(name, slot);
    }

}
