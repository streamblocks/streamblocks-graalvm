package ch.epfl.vlsc.truffle.cal.ast;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.decl.Import;
import se.lth.cs.tycho.ir.decl.SingleImport;

public class TransformContext implements Cloneable {
    private QID namespace;
    public Map<String, QID> entities;
    protected LexicalScope lexicalScope;
    private int depth;
    private FrameDescriptor frameDescriptor;
    private CALLanguage language;
    private Source source;

    
    public TransformContext(CALLanguage language, Source source, NamespaceDecl namespace) {
        Map<String, QID> globalContext = new HashMap<>();
        for (Import imp : namespace.getImports()) {
            if (imp instanceof SingleImport)
                globalContext.put(((SingleImport) imp).getLocalName(), ((SingleImport) imp).getGlobalName());
            else
                throw new Error("Unsupported import type");
        }

        this.entities = globalContext;
        this.namespace = namespace.getQID();
        this.language = language;
        this.source = source;
    }

    private TransformContext() {
		// TODO Auto-generated constructor stub
	}

	public QID getEntityQID(String name) {
        // Either use an imported or default to local
        return entities.getOrDefault(name, namespace.concat(QID.parse(name)));
    }
	public QID getNamespace() {
		return namespace;
	}
	public int getDepth() {
		return depth;
	}
	public void setDepth(int depth) {
		this.depth = depth;
	}
    public LexicalScope getLexicalScope() {
    	return lexicalScope;
    }
    public void clearLexicalScopeAndFrame() {
    	this.lexicalScope = new LexicalScopeRW(null);
    	this.frameDescriptor = new FrameDescriptor();
    }
    public Source getSource() {
    	return source;
    }
    public FrameDescriptor getFrameDescriptor() {
    	return frameDescriptor;
    }
    @Override
    public TransformContext clone() {
    	TransformContext copy = new TransformContext();
    	copy.entities = entities;
    	copy.namespace = namespace;
    	copy.language = this.language;
    	copy.source = this.source;
    	copy.depth = this.depth;
    	copy.lexicalScope = this.lexicalScope;
    	copy.frameDescriptor = this.frameDescriptor;
    	return copy;
    }
    public TransformContext deeper(boolean readonly) {
    	TransformContext copy = this.clone();
    	
        // lexical scope must include parent scope
    	if (readonly)
    		//copy.lexicalScope
    		copy.lexicalScope = new ROParentLexicalScope(this.lexicalScope);
		else
    		copy.lexicalScope = new LexicalScopeRW(this.lexicalScope);
    	copy.depth = this.depth + 1;
    	return copy;
    }
    public TransformContext readonly() {
    	TransformContext copy = new TransformContext();
    	copy.lexicalScope = new ROParentLexicalScope(lexicalScope);
    	return copy;
    }

	public CALLanguage getLanguage() {
		return language;
	}
}
