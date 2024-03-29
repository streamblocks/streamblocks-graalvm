package ch.epfl.vlsc.truffle.cal.nodes.local;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInterface;

public interface WriteNode extends NodeInterface {

	public void write(VirtualFrame topFrame, Object value);

}