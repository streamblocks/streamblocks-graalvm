package ch.epfl.vlsc.truffle.cal;

import ch.epfl.vlsc.truffle.cal.builtins.CALBuiltinNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALContext;
import ch.epfl.vlsc.truffle.cal.runtime.CALObject;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.parsing.cal.CalParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@TruffleLanguage.Registration(id = CALLanguage.ID, name = "SL", defaultMimeType = CALLanguage.MIME_TYPE, characterMimeTypes = CALLanguage.MIME_TYPE, contextPolicy = TruffleLanguage.ContextPolicy.SHARED, fileTypeDetectors = CALFileDetector.class)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class, StandardTags.RootBodyTag.class, StandardTags.ExpressionTag.class, DebuggerTags.AlwaysHalt.class,
        StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class})
public class CALLanguage extends TruffleLanguage<CALContext> {

    public static volatile int counter;

    public static final String ID = "cal";
    public static final String MIME_TYPE = "application/x-cal";

    private final Shape rootShape;

    public CALLanguage() {
        counter++;
        this.rootShape = Shape.newBuilder().layout(CALObject.class).build();
    }

    @Override
    protected CALContext createContext(Env env) {
        return new CALContext(this, env, new ArrayList<>(EXTERNAL_BUILTINS));
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        Path path = Paths.get(source.getURI());
        CalParser parser = new CalParser(Files.newBufferedReader(path));
        NamespaceDecl decl = parser.CompilationUnit();
        Map<String, RootCallTarget> functions;
        return null;
    }

    /**
     * Allocate an empty object. All new objects initially have no properties. Properties are added
     * when they are first stored, i.e., the store triggers a shape change of the object.
     */
    public CALObject createObject(AllocationReporter reporter) {
        reporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        CALObject object = new CALObject(rootShape);
        reporter.onReturnValue(object, 0, AllocationReporter.SIZE_UNKNOWN);
        return object;
    }

    public static CALContext getCurrentContext() {
        return getCurrentContext(CALLanguage.class);
    }

    private static final List<NodeFactory<? extends CALBuiltinNode>> EXTERNAL_BUILTINS = Collections.synchronizedList(new ArrayList<>());

    public static void installBuiltin(NodeFactory<? extends CALBuiltinNode> builtin) {
        EXTERNAL_BUILTINS.add(builtin);
    }


}
