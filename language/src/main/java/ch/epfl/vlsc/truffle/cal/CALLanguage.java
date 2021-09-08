package ch.epfl.vlsc.truffle.cal;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtWhileNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BooleanLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

import ch.epfl.vlsc.truffle.cal.ast.FrameSlotAndDepthRW;
import ch.epfl.vlsc.truffle.cal.builtins.CALBuiltinNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALEvalRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.ActorLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadArgumentNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALContext;

@TruffleLanguage.Registration(id = CALLanguage.ID, name = "CAL", defaultMimeType = CALLanguage.MIME_TYPE, characterMimeTypes = CALLanguage.MIME_TYPE, contextPolicy = TruffleLanguage.ContextPolicy.SHARED, fileTypeDetectors = CALFileDetector.class)
@ProvidedTags({ StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class,
        StandardTags.RootBodyTag.class, StandardTags.ExpressionTag.class, DebuggerTags.AlwaysHalt.class,
        StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class })
public class CALLanguage extends TruffleLanguage<CALContext> {

    public static volatile int counter;

    public static final String ID = "cal";
    public static final String MIME_TYPE = "application/x-cal";
    private static final Source BUILTIN_SOURCE = Source.newBuilder(CALLanguage.ID, "", "CAL builtin").build();

    private final Map<NodeFactory<? extends CALBuiltinNode>, RootCallTarget> builtinTargets = new ConcurrentHashMap<>();

    public CALLanguage() {
        counter++;
    }

    @Override
    protected CALContext createContext(Env env) {
        return new CALContext(this, env, new ArrayList<>(EXTERNAL_BUILTINS));
    }

    private RootCallTarget getRootCall(Map<String, RootCallTarget> actors, Source source) {
        String actorName = getCurrentContext().getEnv().getOptions().get(OptionsCatalog.ENTITY_QID_KEY);
        //RootCallTarget startNode = actors.get(QID.parse(actorName));
        RootCallTarget startNode = actors.get(actorName);
        assert startNode != null;
        CALExpressionNode actor = new ActorLiteralNode(actorName);
        CALExpressionNode call = new CALInvokeNode(actor, new CALExpressionNode[0]);
        // Assign
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        String actorInstanceName = "testActorInstance";
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(actorInstanceName, DefaultValueCastNodeCreator.getInstance(), FrameSlotKind.Illegal);
        FrameSlotAndDepthRW existingSlot = new FrameSlotAndDepthRW(frameSlot, 0);
        boolean newVariable = true;
        CALExpressionNode valueNode = call;
        CALExpressionNode nameNode = new StringLiteralNode(actorInstanceName);

        final CALExpressionNode result = existingSlot.createWriteNode(valueNode, nameNode, newVariable, 0);

        // Call actor instance
        final CALExpressionNode instance = existingSlot.createReadNode(0);

        int iterations = getCurrentContext().getEnv().getOptions().get(OptionsCatalog.ITERATIONS_KEY);
        CALStatementNode[] bodyNodes;
        if(iterations >= 0){
            bodyNodes = new CALStatementNode[1 + iterations];
            bodyNodes[0] = result;
            for (int i = 0; i < iterations; i++) {
                bodyNodes[i + 1] = new CALInvokeNode(instance, new CALExpressionNode[0]);
            }
        } else {
            bodyNodes = new CALStatementNode[3];
            bodyNodes[0] = result;

            // Assign
            String executionStatusVarName = ScopeEnvironment.generateVariableName();
            FrameSlot executionStatusFrameSlot = frameDescriptor.findOrAddFrameSlot(executionStatusVarName, DefaultValueCastNodeCreator.getInstance(), FrameSlotKind.Boolean);
            FrameSlotAndDepthRW executionStatusExistingSlot = new FrameSlotAndDepthRW(executionStatusFrameSlot, 0);
            bodyNodes[1] = executionStatusExistingSlot.createWriteNode(new CALInvokeNode(instance, new CALExpressionNode[0]), new StringLiteralNode(executionStatusVarName), true, 0);

            bodyNodes[2] = new StmtWhileNode(executionStatusExistingSlot.createReadNode(0), executionStatusExistingSlot.createWriteNode(new CALInvokeNode(instance, new CALExpressionNode[0]), new StringLiteralNode(executionStatusVarName), false, 0));
        }
        CALStatementNode body = new StmtBlockNode(bodyNodes);
        RootNode toyRoot = new CALRootNode(this, frameDescriptor, new ReturnsLastBodyNode(body),
                source.createUnavailableSection(), "program root");
        return Truffle.getRuntime().createCallTarget(toyRoot);
    }

    public static List<File> getFilesRecursively(File dir) {
        if (dir.isDirectory()) {
            List<File> ls = new LinkedList<>();
            for (File fObj : dir.listFiles()) {
                ls.addAll(getFilesRecursively(fObj));
            }
            return ls;
        } else if (dir.getName().endsWith(".cal"))
            return Arrays.asList(dir);
        else
            return Collections.emptyList();
    }

    // FIXME we need to build a graph to resolve imports in order to be able
    // to resolve "import all" statements
    // FIXME resolve global variable declarations
    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        File entry = Paths.get(source.getURI()).toFile();
        List<File> allFiles;
        if (getCurrentContext().getEnv().getOptions().get(OptionsCatalog.DIRECTORY_LOOKUP_KEY))
            allFiles = getFilesRecursively(entry.getParentFile());
        else
            allFiles = Arrays.asList(entry);

        Map<String, RootCallTarget> entities = new HashMap<>();
        Map<List<String>, List<QualifiedID>> namespaceEntities = new HashMap<>();
        List<ImmutablePair<Source, CALParser.CompilationUnitContext>> sourceParsers = new LinkedList<>();
        for (File file : allFiles) {
            Source iSource = Source.newBuilder(CALLanguage.ID, new FileReader(file), file.getName()).build();
            Pair<Map<List<String>, List<QualifiedID>>, CALParser.CompilationUnitContext> namespaceEntitiesAndParser = CALParser.getNamespaceEntitiesAndParser(iSource);
            for (List<String> namespaceParts : namespaceEntitiesAndParser.getLeft().keySet()) {
                if(!namespaceEntities.containsKey(namespaceParts)) namespaceEntities.put(namespaceParts, new LinkedList<>());
                namespaceEntities.get(namespaceParts).addAll(namespaceEntitiesAndParser.getLeft().get(namespaceParts));
            }
            sourceParsers.add(new ImmutablePair<>(iSource, namespaceEntitiesAndParser.getRight()));
        }

        for(ImmutablePair<Source, CALParser.CompilationUnitContext> p : sourceParsers){
            entities.putAll(CALParser.parseCAL(this, p.getRight(), p.getLeft(), namespaceEntities));
        }

        return Truffle.getRuntime().createCallTarget(new CALEvalRootNode(this, getRootCall(entities, source), new HashMap<>(), entities));

        /*Map<QID, RootCallTarget> entities = new HashMap<>();
        for (File file : allFiles) {
            CalParser parser = new CalParser(Files.newBufferedReader(file.toPath()));
            NamespaceDecl decl = parser.CompilationUnit();
            Source iSource = Source.newBuilder(CALLanguage.ID, new FileReader(file), file.getName()).build();
            BlockTransformer astTransformer = new BlockTransformer(new TransformContext(this, iSource, decl), decl);
            entities.putAll(astTransformer.transform());
        }
        Map<String, RootCallTarget> parsedEntities = new HashMap<>(entities.size());
        for (Entry<QID, RootCallTarget> e : entities.entrySet())
            parsedEntities.put(e.getKey().toString(), e.getValue());
        return Truffle.getRuntime()
                .createCallTarget(new CALEvalRootNode(this, getRootCall(entities, source), new HashMap<>(), parsedEntities));*/
    }

    public RootCallTarget lookupBuiltin(NodeFactory<? extends CALBuiltinNode> factory) {
        RootCallTarget target = builtinTargets.get(factory);
        if (target != null) {
            return target;
        }

        /*
         * The builtin node factory is a class that is automatically generated by the
         * Truffle DSL. The signature returned by the factory reflects the signature of
         * the @Specialization
         *
         * methods in the builtin classes.
         */
        int argumentCount = factory.getExecutionSignature().size();
        CALExpressionNode[] argumentNodes = new CALExpressionNode[argumentCount];
        /*
         * Builtin functions are like normal functions, i.e., the arguments are passed
         * in as an Object[] array encapsulated in SLArguments. A SLReadArgumentNode
         * extracts a parameter from this array.
         */
        for (int i = 0; i < argumentCount; i++) {
            argumentNodes[i] = new CALReadArgumentNode(i);
        }
        /* Instantiate the builtin node. This node performs the actual functionality. */
        CALBuiltinNode builtinBodyNode = factory.createNode((Object) argumentNodes);
        builtinBodyNode.addRootTag();
        /*
         * The name of the builtin function is specified via an annotation on the node
         * class.
         */
        String name = lookupNodeInfo(builtinBodyNode.getClass()).shortName();
        builtinBodyNode.setUnavailableSourceSection();

        /*
         * Wrap the builtin in a RootNode. Truffle requires all AST to start with a
         * RootNode.
         */
        CALRootNode rootNode = new CALRootNode(this, new FrameDescriptor(), builtinBodyNode,
                BUILTIN_SOURCE.createUnavailableSection(), name);

        /*
         * Register the builtin function in the builtin registry. Call targets for
         * builtins may be reused across multiple contexts.
         */
        RootCallTarget newTarget = Truffle.getRuntime().createCallTarget(rootNode);
        RootCallTarget oldTarget = builtinTargets.put(factory, newTarget);
        if (oldTarget != null) {
            return oldTarget;
        }
        return newTarget;
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

    public boolean isSingleContext() {
        return false;
    }

    public static CALContext getCurrentContext() {
        return getCurrentContext(CALLanguage.class);
    }

    private static final List<NodeFactory<? extends CALBuiltinNode>> EXTERNAL_BUILTINS = Collections
            .synchronizedList(new ArrayList<>());

    public static void installBuiltin(NodeFactory<? extends CALBuiltinNode> builtin) {
        EXTERNAL_BUILTINS.add(builtin);
    }

    @Override
    protected Object getScope(CALContext context) {
        return context.getActorRegistry().getFunctionsObject();
    }
    @Override
    protected OptionDescriptors getOptionDescriptors() {
        // this class is generated by the annotation processor
        return OptionDescriptors.create(Arrays.asList(OptionsCatalog.allDescriptors()));
    }

}
