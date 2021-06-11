package ch.epfl.vlsc.truffle.cal.ast;

import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import se.lth.cs.tycho.ir.IRNode;

public abstract class Transformer<R> {
	private TransformContext context;
    protected Transformer(TransformContext context) {
    	this.context = context;
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                throw new TransformException(e.getMessage(), context.getSource());
            }
        });
    }
    
    protected SourceSection getSourceSection(IRNode originalNode) {
    	try {
    		SourceSection section = context.getSource().createSection(
    				originalNode.getFromLineNumber(), 
    				originalNode.getFromColumnNumber(), 
    				originalNode.getToLineNumber(),
    				originalNode.getToColumnNumber());
    		return section;
    	} catch (Exception e) {
    		return context.getSource().createUnavailableSection();
    	}
    }
    protected <NT extends CALStatementNode> NT withSourceSection(NT output, IRNode originalNode) {
    	try {
    		output.setSourceSection(getSourceSection(originalNode));
    		return output;
    	} catch (Exception e) {
    		return output;
    	}
    }

    public abstract R transform();

}
