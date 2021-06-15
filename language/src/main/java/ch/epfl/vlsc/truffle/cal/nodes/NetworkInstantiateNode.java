package ch.epfl.vlsc.truffle.cal.nodes;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import ch.epfl.vlsc.truffle.cal.runtime.CALNetworkInstance;

/* Instantiate an network
 * Takes care of assigning the variable declaration and the network state
 */
class NetworkInstantiateNode extends CALExpressionNode {
    NetworkNode network;
    // Variable declaration now, but maybe other stuff too later
    @Child CALStatementNode head;
    @Child CALRootNode body;
    public NetworkInstantiateNode(NetworkNode network, CALStatementNode head, CALRootNode body) {
        this.head = head;
        this.body = body;
        this.network = network;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // create a new frame with the network's frame descriptor
        MaterializedFrame networkFrame = Truffle.getRuntime().createMaterializedFrame(frame.getArguments(), network.getFrameDescriptor());
        // TODO add arguments, create the actors
        // create and link the FIFOs
        head.executeVoid(networkFrame);
        return new CALNetworkInstance(network, networkFrame, body);
    }
}
