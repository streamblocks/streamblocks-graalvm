package ch.epfl.vlsc.truffle.cal.runtime;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.Source;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.builtins.CALBuiltinNode;
import ch.epfl.vlsc.truffle.cal.builtins.CALLshiftBuiltinFactory;
import ch.epfl.vlsc.truffle.cal.builtins.CALPrintlnBuiltinFactory;
import ch.epfl.vlsc.truffle.cal.builtins.CALRshiftBuiltinFactory;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadArgumentNode;

public class CALContext {

    private static final Source BUILTIN_SOURCE = Source.newBuilder(CALLanguage.ID, "", "SL builtin").build();


    private final TruffleLanguage.Env env;
    private final BufferedReader input;
    private final PrintWriter output;
    private final CALFunctionRegistry functionRegistry;
    private final CALActorRegistry actorRegistry;
    private final CALLanguage language;
    private final AllocationReporter allocationReporter;


    public CALContext(CALLanguage language, TruffleLanguage.Env env, List<NodeFactory<? extends CALBuiltinNode>> externalBuiltins) {
        this.env = env;
        this.input = new BufferedReader(new InputStreamReader(env.in()));
        this.output = new PrintWriter(env.out(), true);
        this.language = language;
        this.allocationReporter = env.lookup(AllocationReporter.class);
        this.functionRegistry = new CALFunctionRegistry(language);
        this.actorRegistry = new CALActorRegistry(language);
        installBuiltins();
        for (NodeFactory<? extends CALBuiltinNode> builtin : externalBuiltins) {
            installBuiltin(builtin);
        }
    }


    public TruffleLanguage.Env getEnv() {
        return env;
    }


    public BufferedReader getInput() {
        return input;
    }

    public PrintWriter getOutput() {
        return output;
    }

    /**
     * Returns the registry of all functions that are currently defined.
     */
    public CALFunctionRegistry getFunctionRegistry() {
        return functionRegistry;
    }
    /**
     * Returns the registry of all actors that are currently defined.
     */
    // TODO rename getEntityRegistry
    public CALActorRegistry getActorRegistry() {
        return actorRegistry;
    }

    private void installBuiltins() {
        installBuiltin(CALRshiftBuiltinFactory.getInstance());
        installBuiltin(CALLshiftBuiltinFactory.getInstance());
        installBuiltin(CALPrintlnBuiltinFactory.getInstance());
    }

    public void installBuiltin(NodeFactory<? extends CALBuiltinNode> factory) {
        /* Register the builtin function in our function registry. */
        RootCallTarget target = language.lookupBuiltin(factory);
        String rootName = target.getRootNode().getName();

        int arity = factory.getNodeSignatures().get(0).size();
        CALStatementNode[] readArguments = new CALStatementNode[arity];
			for (int i = 0; i < readArguments.length; i++) {
				readArguments[i] = new CALReadArgumentNode(i);
			}
        CALExpressionNode body = ((CALRootNode) target.getRootNode()).getBodyNode();
        CALExpressionNode node = new ReturnsLastBodyNode(new StmtBlockNode(readArguments), body);
        CALRootNode rootNode = new CALRootNode(language, new FrameDescriptor(), node, BUILTIN_SOURCE.createUnavailableSection(), rootName);
        RootCallTarget newTarget = Truffle.getRuntime().createCallTarget(rootNode);
        getFunctionRegistry().register(rootName, newTarget);
    }

    public static NodeInfo lookupNodeInfo(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        NodeInfo info = clazz.getAnnotation(NodeInfo.class);
        if (info != null) {
            return info;
        } else {
            return lookupNodeInfo(clazz.getSuperclass());
        }
    }

    /*
     * Methods for object creation / object property access.
     */
    public AllocationReporter getAllocationReporter() {
        return allocationReporter;
    }

    /*
     * Methods for language interoperability.
     */

    public static Object fromForeignValue(Object a) {
        if (a instanceof Long || a instanceof CALBigNumber || a instanceof String || a instanceof Boolean) {
            return a;
        } else if (a instanceof Character) {
            return fromForeignCharacter((Character) a);
        } else if (a instanceof Number) {
            return fromForeignNumber(a);
        } else if (a instanceof TruffleObject) {
            return a;
        } else if (a instanceof CALContext) {
            return a;
        }
        throw shouldNotReachHere("Value is not a truffle value.");
    }

    @CompilerDirectives.TruffleBoundary
    private static long fromForeignNumber(Object a) {
        return ((Number) a).longValue();
    }

    @CompilerDirectives.TruffleBoundary
    private static String fromForeignCharacter(char c) {
        return String.valueOf(c);
    }

    public CallTarget parse(Source source) {
        return env.parsePublic(source);
    }

    /**
     * Returns an object that contains bindings that were exported across all used languages. To
     * read or write from this object the {@link TruffleObject interop} API can be used.
     */
    public TruffleObject getPolyglotBindings() {
        return (TruffleObject) env.getPolyglotBindings();
    }

    public static CALContext getCurrent() {
        return CALLanguage.getCurrentContext();
    }

}
