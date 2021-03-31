package ch.epfl.vlsc.truffle.cal.nodes;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtFunctionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadArgumentNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteFrameSlotNode;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

@NodeInfo(language = "CAL", description = "The root of all CAL execution trees")
public class CALRootNode extends RootNode {
    /** The function body that is executed, and specialized during execution. */
    @Child private CALExpressionNode bodyNode;

    /** The name of the function, for printing purposes only. */
    private final String name;

    private boolean isCloningAllowed;

    private final SourceSection sourceSection;
    
    @CompilerDirectives.CompilationFinal(dimensions = 1) private volatile CALWriteFrameSlotNode[] argumentNodesCache;
    // TODO FIXME bodyNode should maybe be expression
    public CALRootNode(CALLanguage language, FrameDescriptor frameDescriptor, CALExpressionNode bodyNode, SourceSection sourceSection, String name) {
        super(language, frameDescriptor);
        this.bodyNode = bodyNode;
        this.name = name;
        this.sourceSection = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        assert lookupContextReference(CALLanguage.class).get() != null;
        return bodyNode.executeGeneric(frame);
    }

    public CALExpressionNode getBodyNode() {
        return bodyNode;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setCloningAllowed(boolean isCloningAllowed) {
        this.isCloningAllowed = isCloningAllowed;
    }

    @Override
    public boolean isCloningAllowed() {
        return isCloningAllowed;
    }

    @Override
    public String toString() {
        return "root " + name;
    }

    public final CALWriteFrameSlotNode[] getDeclaredArguments() {
        CALWriteFrameSlotNode[] argumentNodes = argumentNodesCache;
        if (argumentNodes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            argumentNodesCache = argumentNodes = findArgumentNodes();
        }
        return argumentNodes;
    }

    private CALWriteFrameSlotNode[] findArgumentNodes() {
        List<CALWriteFrameSlotNode> writeArgNodes = new ArrayList<>(4);
        NodeUtil.forEachChild(this.getBodyNode(), new NodeVisitor() {

            private CALWriteFrameSlotNode wn; // The current write node containing a CALot

            @Override
            public boolean visit(Node node) {
                // When there is a write node, search for CALReadArgumentNode among its children:
                if (node instanceof InstrumentableNode.WrapperNode) {
                    return NodeUtil.forEachChild(node, this);
                }
                if (node instanceof CALWriteFrameSlotNode) {
                    wn = (CALWriteFrameSlotNode) node;
                    boolean all = NodeUtil.forEachChild(node, this);
                    wn = null;
                    return all;
                } else if (wn != null && (node instanceof CALReadArgumentNode)) {
                    writeArgNodes.add(wn);
                    return true;
                } else if (wn == null && (node instanceof CALStatementNode && !(node instanceof StmtBlockNode || node instanceof StmtFunctionBodyNode))) {
                    // A different CAL node - we're done.
                    return false;
                } else {
                    return NodeUtil.forEachChild(node, this);
                }
            }
        });
        return writeArgNodes.toArray(new CALWriteFrameSlotNode[writeArgNodes.size()]);
    }
}
