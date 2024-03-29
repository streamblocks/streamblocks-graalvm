package ch.epfl.vlsc.truffle.cal;

import ch.epfl.vlsc.truffle.cal.ast.FrameSlotAndDepthRW;
import ch.epfl.vlsc.truffle.cal.builtins.CALBuiltinNode;
import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtWhileNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryAddNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.binary.CALBinaryLessThanNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.ActorLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALReadArgumentNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.utils.NamespaceElementsToCallTarget;
import ch.epfl.vlsc.truffle.cal.runtime.CALContext;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import com.oracle.truffle.api.*;
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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;

import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        CALExpressionNode call = CALInvokeNodeGen.create(new CALExpressionNode[0], actor);
        // Assign
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        String actorInstanceName = "testActorInstance";

        // The second argument, slotInfo is used here to store a factory of type cast/coerce nodes.
        // This is used, for example, to trim a 16-bit number to fit in 8-bit slot, in order to simulate
        // overflow/underflow of types.
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
        if(iterations >= 0) {
            bodyNodes = new CALStatementNode[3];
            bodyNodes[0] = result;
            String iterationNumVarName = "$" + actorInstanceName + "iterationNum" + String.valueOf(source.hashCode());
            FrameSlot iterationNumFrameSlot = frameDescriptor.findOrAddFrameSlot(iterationNumVarName, DefaultValueCastNodeCreator.getInstance(), FrameSlotKind.Object);
            FrameSlotAndDepthRW iterationNumExistingSlot = new FrameSlotAndDepthRW(iterationNumFrameSlot, 0);
            // Create variable to store number of iterations done
            bodyNodes[1] = iterationNumExistingSlot.createWriteNode(new BigIntegerLiteralNode(new BigInteger("0")), new StringLiteralNode(iterationNumVarName), true, 0);
            // Loop until the number of iterations done is less than number of targetted iterations
            bodyNodes[2] = new StmtWhileNode(
                    CALBinaryLessThanNodeGen.create(iterationNumExistingSlot.createReadNode(0), new BigIntegerLiteralNode(new BigInteger(String.valueOf(iterations)))),
                    new StmtBlockNode(new CALStatementNode[]{
                        CALInvokeNodeGen.create(new CALExpressionNode[0], instance),
                        iterationNumExistingSlot.createWriteNode(CALBinaryAddNodeGen.create(iterationNumExistingSlot.createReadNode(0), new BigIntegerLiteralNode(BigInteger.ONE)), new StringLiteralNode(iterationNumVarName), false, 0)
                    }));
        } else {
            bodyNodes = new CALStatementNode[3];
            bodyNodes[0] = result;

            // The network/actor returns a boolean indicating whether it executed or not.
            // Through the following, we loop till the network/actor executes at least 1 action.
            // If during an iteration, there is no progress, then we stop.
            String executionStatusVarName = ScopeEnvironment.generateVariableName();
            FrameSlot executionStatusFrameSlot = frameDescriptor.findOrAddFrameSlot(executionStatusVarName, DefaultValueCastNodeCreator.getInstance(), FrameSlotKind.Boolean);
            FrameSlotAndDepthRW executionStatusExistingSlot = new FrameSlotAndDepthRW(executionStatusFrameSlot, 0);
            // Execute once and store execution result to a variable
            bodyNodes[1] = executionStatusExistingSlot.createWriteNode(CALInvokeNodeGen.create(new CALExpressionNode[0], instance), new StringLiteralNode(executionStatusVarName), true, 0);
            // Loop until the execution status variable is true
            bodyNodes[2] = new StmtWhileNode(executionStatusExistingSlot.createReadNode(0), executionStatusExistingSlot.createWriteNode(CALInvokeNodeGen.create(new CALExpressionNode[0], instance), new StringLiteralNode(executionStatusVarName), false, 0));
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
        Map<String, RootCallTarget> functions = new HashMap<>();
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
            NamespaceElementsToCallTarget namespaceElementsToCallTarget = CALParser.parseCAL(this, p.getRight(), p.getLeft(), namespaceEntities);
            entities.putAll(namespaceElementsToCallTarget.entities);
            functions.putAll(namespaceElementsToCallTarget.functions);
        }

        return Truffle.getRuntime().createCallTarget(new CALEvalRootNode(this, getRootCall(entities, source), functions, entities));
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
