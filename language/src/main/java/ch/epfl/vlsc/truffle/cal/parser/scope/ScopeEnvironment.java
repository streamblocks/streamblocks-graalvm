package ch.epfl.vlsc.truffle.cal.parser.scope;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.nodes.util.ValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
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

	private static int VARIABLE_ID;

    private static int LAMBDA_ID;

	private static int FIFO_ID;

	private static int FANOUT_ID;

    private ScopeEnvironment(CALLanguage language, Source source) {
		this.language = language;
		this.source = source;
		imports = new HashMap<>();
		globalScope = new Scope(null, null,0, Scope.ScopeKind.RW);
		currentScope = globalScope;
    }

	public static void createInstance(CALLanguage language, Source source) {
		instance = new ScopeEnvironment(language, source);
		VARIABLE_ID = 1;
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

	public void addImportMultiple(Map<String, String> importEntities) {
		imports.putAll(importEntities);
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
    	FrameDescriptor frame = currentScope.getFrame();
    	int depth = currentScope.getDepth();
    	if (isDeeper) {
    		// Always create new frame and increase scope depth (except for Let closure!)
			frame = new FrameDescriptor();
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

	public CALExpressionNode createReadNode(String name, SourceSection sourceSection) {
		CALExpressionNode variableExpression;
		DepthFrameSlot slot = currentScope.get(name);
		if (slot != null) {
			variableExpression = slot.createReadNode(currentScope.getDepth(), sourceSection);
		} else {
			variableExpression = new FunctionLiteralNode(name);
		}
		variableExpression.addExpressionTag();

		return variableExpression;
	}

	public void createFrameSlot(String name, ValueCastNodeCreator valueCastNodeCreator) {
		if (!currentScope.containsKey(name)) {
			FrameSlot frameSlot = currentScope.getFrame().addFrameSlot(name, valueCastNodeCreator, FrameSlotKind.Illegal);
			DepthFrameSlot slot = new DepthFrameSlot(frameSlot, currentScope.getDepth());
			currentScope.put(name, slot);
		}
	}

	public void createReadOnlyFrameSlot(String name) {
		createFrameSlot(name, DefaultValueCastNodeCreator.getInstance());
		DepthFrameSlot readOnlySlot = new DepthFrameSlot(currentScope.get(name));
		currentScope.put(name, readOnlySlot);
	}

	public CALExpressionNode createNewVariableWriteNode(String name, CALExpressionNode valueNode, ValueCastNodeCreator valueCastNodeCreator, SourceSection sourceSection) {
		if (currentScope.containsKey(name)) {
			throw new CALParseError(source, sourceSection.getStartLine(), sourceSection.getStartColumn(), sourceSection.getCharLength(), "Variable " + name + " has already been defined");
		}

		return createWriteNode(name, valueNode, valueCastNodeCreator, true, sourceSection);
	}

	public CALExpressionNode createExistingVariableWriteNode(String name, CALExpressionNode valueNode, SourceSection sourceSection) {
		if (!currentScope.containsKey(name)) {
			throw new CALParseError(source, sourceSection.getStartLine(), sourceSection.getStartColumn(), sourceSection.getCharLength(), "Cannot write to undefined variable " + name);
		}

		CALExpressionNode nameNode = new StringLiteralNode(name);
		DepthFrameSlot slot = currentScope.get(name);

		return slot.createWriteNode(nameNode, valueNode, false, currentScope.getDepth(), sourceSection);
	}

	public CALExpressionNode createWriteNode(String name, CALExpressionNode valueNode, ValueCastNodeCreator valueCastNodeCreator, boolean isNewVariable, SourceSection sourceSection) {
		createFrameSlot(name, valueCastNodeCreator);

		CALExpressionNode nameNode = new StringLiteralNode(name);
		DepthFrameSlot slot = currentScope.get(name);

		return slot.createWriteNode(nameNode, valueNode, isNewVariable, currentScope.getDepth(), sourceSection);
	}

	public static String generateVariableName() {
		String variableName = "$temp" + VARIABLE_ID;
		VARIABLE_ID++;

		return variableName;
	}

	public static String generateLambdaName() {
    	String lambdaName = "$lambda" + LAMBDA_ID;
		LAMBDA_ID++;

		return lambdaName;
	}

	public static String generateFIFOName() {
		String fifoName = "$fifo" + FIFO_ID;
		FIFO_ID++;

		return fifoName;
	}

	public static String generateFifoFanoutName() {
		String fanoutName = "$fifoFanout" + FANOUT_ID;
		FANOUT_ID++;

		return fanoutName;
	}

	// FIXME: TODO: This method is STRICTLY a hack and needs to be removed
	// The visitors should not be directly holding references to frameslots, to ensure lexical scoping
	public FrameSlot findFrameSlot(String actionIndexSlotName) {
    	return getCurrentScope().get(actionIndexSlotName).getSlot();
	}
}
