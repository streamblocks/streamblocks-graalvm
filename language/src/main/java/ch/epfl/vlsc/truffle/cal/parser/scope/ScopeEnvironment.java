package ch.epfl.vlsc.truffle.cal.parser.scope;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.antlr.v4.runtime.ParserRuleContext;
import org.graalvm.collections.Pair;

import java.util.HashMap;
import java.util.Map;

public class ScopeEnvironment {

	private static ScopeEnvironment instance;

	private CALLanguage language;

	private Source source;

	private String name;

	private Map<String, String> imports;

	private Scope globalScope;

    private Scope currentScope;

    private static int LAMBDA_ID;

	private static int FIFO_ID;

    private ScopeEnvironment(CALLanguage language, Source source) {
		this.language = language;
		this.source = source;
		imports = new HashMap<>();
		globalScope = new Scope(null, null,0, Scope.ScopeKind.RW);
		currentScope = globalScope;
    }

	public static void createInstance(CALLanguage language, Source source) {
		instance = new ScopeEnvironment(language, source);
		LAMBDA_ID = 1;
		FIFO_ID = 1;
	}

    public static ScopeEnvironment getInstance() {
    	if (instance == null) {
    		throw new Error("Scope Environment: Cannot get the instance, must first be created with 'createInstance'.");
		}

		return instance;
	}

	public CALLanguage getLanguage() {
		return language;
	}

	public Source getSource() {
		return source;
	}

	public SourceSection createSourceSection(ParserRuleContext context) {
		SourceSection section;
		try {
			section = source.createSection(
					context.getStart().getLine(),
					context.getStart().getCharPositionInLine() + 1,
					context.getStop().getLine(),
					context.getStart().getCharPositionInLine() + 1
			);
		} catch (Exception e) {
			section = source.createUnavailableSection();
		}

		return section;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addImport(Pair<String, String> importEntity) {
		imports.put(importEntity.getLeft(), importEntity.getRight());
	}

	public String getEntityFullName(String name) {
		if (imports.containsKey(name)) {
			return imports.get(name);
		} else if (this.name != null) {
			return this.name + "." + name;
		} else {
			return name;
		}
	}

    public Scope getCurrentScope() {
    	return currentScope;
	}

	public Scope getGlobalScope() {
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

    	Scope.ScopeKind kind;
    	if (isReadOnly) {
    		kind = Scope.ScopeKind.PARENT_RO;
		} else {
    		kind = Scope.ScopeKind.RW;
		}

		currentScope = new Scope(currentScope, frame, depth, kind);
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

	public static String generateLambdaName() {
    	String lambdaName = "lambda" + LAMBDA_ID;
		LAMBDA_ID++;

		return lambdaName;
	}

	public static String generateFIFOName() {
		String fifoName = "fifo" + FIFO_ID;
		FIFO_ID++;

		return fifoName;
	}
}
