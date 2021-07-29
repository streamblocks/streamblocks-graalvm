package ch.epfl.vlsc.truffle.cal.parser;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import org.graalvm.collections.Pair;

import java.util.HashMap;
import java.util.Map;

public class CALNodeFactoryContext {

	private CALLanguage language;

	private Source source;

	private Map<String, String> imports;

	private LexicalScope globalScope;

    private LexicalScope currentScope;

    public CALNodeFactoryContext(CALLanguage language, Source source) {
		this.language = language;
		this.source = source;
		imports = new HashMap<>();
		globalScope = new LexicalScope(null, null,0, LexicalScope.ScopeKind.RW);
		currentScope = globalScope;
    }

	public CALLanguage getLanguage() {
		return language;
	}

	public Source getSource() {
		return source;
	}

	public void addImport(Pair<String, String> importEntity) {
		imports.put(importEntity.getLeft(), importEntity.getRight());
	}

    public LexicalScope getCurrentScope() {
    	return currentScope;
	}

	public LexicalScope getGlobalScope() {
		return globalScope;
	}

	public void pushScope() {
		pushScope(false);
	}

	public void pushScope(boolean isReadOnly) {
		pushScope(isReadOnly, true);
	}
	public void pushScope(boolean isReadOnly, boolean isDeeper) {
    	FrameDescriptor frame;
    	if (currentScope == globalScope) {
    		// Only create new frame for top-level entities (Actors & Networks)
    		frame = new FrameDescriptor();
		} else {
    		frame = currentScope.getFrame();
		}

    	int depth = currentScope.getDepth();
    	if (isDeeper) {
    		// Always increase scope depth (except for Let closure!)
			depth++;
		}

    	LexicalScope.ScopeKind kind;
    	if (isReadOnly) {
    		kind = LexicalScope.ScopeKind.PARENT_RO;
		} else {
    		kind = LexicalScope.ScopeKind.RW;
		}

		currentScope = new LexicalScope(currentScope, frame, depth, kind);
	}

	public void popScope() {
		currentScope = currentScope.getOuterScope();
	}

	public CALExpressionNode createReadNode(String name) {
		CALExpressionNode variableExpression;
		DepthFrameSlot slot = currentScope.get(name);
		if (slot != null) {
			variableExpression = slot.createReadNode(currentScope.getDepth());
		} else {
			variableExpression = new FunctionLiteralNode(name);
		}
		variableExpression.addExpressionTag();

		return variableExpression;
	}

	public CALExpressionNode createWriteNode(String name, CALExpressionNode valueNode) {
		CALExpressionNode nameNode = new StringLiteralNode(name);
		FrameSlot frameSlot = currentScope.getFrame().findOrAddFrameSlot(name, FrameSlotKind.Illegal);

		DepthFrameSlot slot;
		if (!currentScope.containsKey(name)) {
			slot = new DepthFrameSlot(frameSlot, currentScope.getDepth());
			currentScope.put(name, slot);

			return slot.createWriteNode(valueNode, nameNode, true, currentScope.getDepth());
		} else {
			slot = currentScope.get(name);

			return slot.createWriteNode(valueNode, nameNode, false, currentScope.getDepth());
		}
	}
}
