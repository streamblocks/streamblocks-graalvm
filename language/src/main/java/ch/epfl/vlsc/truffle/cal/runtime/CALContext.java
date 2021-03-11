package ch.epfl.vlsc.truffle.cal.runtime;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.builtins.CALBuiltinNode;
import ch.epfl.vlsc.truffle.cal.builtins.CALPrintlnBuiltin;
import ch.epfl.vlsc.truffle.cal.builtins.CALPrintlnBuiltinFactory;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadArgumentNode;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.Source;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

public class CALContext {

    private static final Source BUILTIN_SOURCE = Source.newBuilder(CALLanguage.ID, "", "SL builtin").build();


    private final TruffleLanguage.Env env;
    private final BufferedReader input;
    private final PrintWriter output;
    private final CALFunctionRegistry functionRegistry;
    private final CALLanguage language;
    private final AllocationReporter allocationReporter;
    private final Iterable<Scope> topScopes; // Cache the top scopes


    public CALContext(CALLanguage language, TruffleLanguage.Env env, List<NodeFactory<? extends CALBuiltinNode>> externalBuiltins) {
        this.env = env;
        this.input = new BufferedReader(new InputStreamReader(env.in()));
        this.output = new PrintWriter(env.out(), true);
        this.language = language;
        this.allocationReporter = env.lookup(AllocationReporter.class);
        this.functionRegistry = new CALFunctionRegistry(language);
        this.topScopes = Collections.singleton(Scope.newBuilder("global", functionRegistry.getFunctionsObject()).build());
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

    public Iterable<Scope> getTopScopes() {
        return topScopes;
    }

    private void installBuiltins() {
        installBuiltin(CALPrintlnBuiltinFactory.getInstance());
    }

    public void installBuiltin(NodeFactory<? extends CALBuiltinNode> factory) {
        /* Register the builtin function in our function registry. */
        RootCallTarget target = language.lookupBuiltin(factory);
        String rootName = target.getRootNode().getName();
        getFunctionRegistry().register(rootName, target);
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
