package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.nodes.local.CALScopedNode;

@NodeInfo(language = "SL", description = "The abstract base node for all SL statements")
@GenerateWrapper
public abstract class CALStatementNode extends CALScopedNode implements InstrumentableNode {
    private static final int NO_SOURCE = -1;
    private static final int UNAVAILABLE_SOURCE = -2;

    private SourceSection sourceSection;

    private boolean hasStatementTag;
    private boolean hasRootTag;

    /*
     * The creation of source section can be implemented lazily by looking up the root node source
     * and then creating the source section object using the indices stored in the node. This avoids
     * the eager creation of source section objects during parsing and creates them only when they
     * are needed. Alternatively, if the language uses source sections to implement language
     * semantics, then it might be more efficient to eagerly create source sections and store it in
     * the AST.
     *
     * For more details see {@link InstrumentableNode}.
     */
    @Override
    @CompilerDirectives.TruffleBoundary
    public final SourceSection getSourceSection() {
        return sourceSection;
    }

    public final boolean hasSource() {
        return sourceSection != null;
    }
    
    @Override
    public final boolean isInstrumentable() {
        return hasSource();
    }

    // invoked by the parser to set the source
    public final void setSourceSection(SourceSection sourceSection) {
        this.sourceSection = sourceSection;
    }

    public final void setUnavailableSourceSection() {
        sourceSection = null;
    }

    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return hasStatementTag;
        } else if (tag == StandardTags.RootTag.class || tag == StandardTags.RootBodyTag.class) {
            return hasRootTag;
        }
        return false;
    }

    public WrapperNode createWrapper(ProbeNode probe) {
        return new CALStatementNodeWrapper(this, probe);
    }

    /**
     * Execute this node as as statement, where no return value is necessary.
     */
    public abstract void executeVoid(VirtualFrame frame);

    /**
     * Marks this node as being a {@link StandardTags.StatementTag} for instrumentation purposes.
     */
    public final void addStatementTag() {
        hasStatementTag = true;
    }

    /**
     * Marks this node as being a {@link StandardTags.RootTag} and {@link StandardTags.RootBodyTag}
     * for instrumentation purposes.
     */
    public final void addRootTag() {
        hasRootTag = true;
    }

    @Override
    public String toString() {
        return formatSourceSection(this);
    }

    /**
     * Formats a source section of a node in human readable form. If no source section could be
     * found it looks up the parent hierarchy until it finds a source section. Nodes where this was
     * required append a <code>'~'</code> at the end.
     *
     * @param node the node to format.
     * @return a formatted source section string
     */
    public static String formatSourceSection(Node node) {
        if (node == null) {
            return "<unknown>";
        }
        SourceSection section = node.getSourceSection();
        boolean estimated = false;
        if (section == null) {
            section = node.getEncapsulatingSourceSection();
            estimated = true;
        }

        if (section == null || section.getSource() == null) {
            return "<unknown source>";
        } else {
            String sourceName = section.getSource().getName();
            int startLine = section.getStartLine();
            return String.format("%s:%d%s", sourceName, startLine, estimated ? "~" : "");
        }
    }
}
