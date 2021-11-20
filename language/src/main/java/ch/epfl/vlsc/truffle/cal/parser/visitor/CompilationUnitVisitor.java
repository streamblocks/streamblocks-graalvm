package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.utils.NamespaceElementsToCallTarget;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import org.antlr.v4.runtime.Token;
import org.graalvm.collections.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Singleton class that provides an implementation for a compilation unit sub-tree.
 */
public class CompilationUnitVisitor extends CALParserBaseVisitor<Object> {

    private static CompilationUnitVisitor instance;
    private Map<List<String>, List<QualifiedID>> NamespaceEntitiesMap;

    private CompilationUnitVisitor() {}

    public static CompilationUnitVisitor getInstance() {
        if (instance == null) {
            instance = new CompilationUnitVisitor();
        }

        return instance;
    }

    public void setNamespaceEntitiesMap(Map<List<String>, List<QualifiedID>> NamespaceEntitiesMap) {
        this.NamespaceEntitiesMap = NamespaceEntitiesMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override public NamespaceElementsToCallTarget visitNamespaceCompilationUnit(CALParser.NamespaceCompilationUnitContext ctx) {
        return (NamespaceElementsToCallTarget) visit(ctx.namespaceDeclaration());
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitUnitCompilationUnit(CALParser.UnitCompilationUnitContext ctx) {
        return visitUnitDeclaration(ctx.unitDeclaration());
    }

    /**
     * {@inheritDoc}
     */
    @Override public NamespaceElementsToCallTarget visitUnnamedNamespaceDeclaration(CALParser.UnnamedNamespaceDeclarationContext ctx) {
        return visitNamespaceBody(ctx.body);
    }

    /**
     * {@inheritDoc}
     */
    @Override public NamespaceElementsToCallTarget visitNamedNamespaceDeclaration(CALParser.NamedNamespaceDeclarationContext ctx) {
        String name = CollectionVisitor.qualifiedIdToString(CollectionVisitor.getInstance().visitQualifiedID(ctx.name));
        ScopeEnvironment.getInstance().setName(name);

        NamespaceElementsToCallTarget namespaceElementsToCallTarget = visitNamespaceBody(ctx.body);

        NamespaceElementsToCallTarget completeQidElementsToCallTarget = new NamespaceElementsToCallTarget();
        completeQidElementsToCallTarget.entities = namespaceElementsToCallTarget.entities.entrySet().stream().collect(Collectors.toMap(e -> name + "." + e.getKey(), Map.Entry::getValue));
        completeQidElementsToCallTarget.functions = namespaceElementsToCallTarget.functions.entrySet().stream().collect(Collectors.toMap(e -> name + "." + e.getKey(), Map.Entry::getValue));
        return completeQidElementsToCallTarget;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Map<String, RootCallTarget> visitPackageNamespaceDeclaration(CALParser.PackageNamespaceDeclarationContext ctx) {
        // TODO Add support for packages
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Package declaration is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public NamespaceElementsToCallTarget visitNamespaceBody(CALParser.NamespaceBodyContext ctx) {
        for (CALParser.ImportDeclarationContext importCtx: ctx.importDeclaration()) {
            if (importCtx instanceof CALParser.SingleImportDeclarationContext) {
                ScopeEnvironment.getInstance().addImport(visitSingleImportDeclaration((CALParser.SingleImportDeclarationContext) importCtx));
            } else if (importCtx instanceof  CALParser.GroupImportDeclarationContext) {
                ScopeEnvironment.getInstance().addImportMultiple(visitGroupImportDeclaration((CALParser.GroupImportDeclarationContext) importCtx));
            }
        }

        Map<String, RootCallTarget> entities = new HashMap<>();
        Map<String, RootCallTarget> functions = new HashMap<>();
        if (ctx.typeDefinition().size() > 0) {
            // TODO Add support for type definitions
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Type definition is not yet supported");
            }
        }

        for(CALParser.GlobalVariableDeclarationContext varDecl: ctx.globalVariableDeclaration()) {
            if (varDecl.functionVariableDeclaration() != null) {
                CALRootNode entityNode = VariableVisitor.getInstance().getFunctionRootNode(varDecl.functionVariableDeclaration());
                functions.put(varDecl.functionVariableDeclaration().name.getText(), Truffle.getRuntime().createCallTarget(entityNode));
            } else {
                // TODO Add support for non-function global variable declarations
                throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Non function global variables are not supported yet");
            }
        }

        for (CALParser.ActorDeclarationContext actorCtx: ctx.actorDeclaration()) {
            CALRootNode entityNode = ActorVisitor.getInstance().visitActorDeclaration(actorCtx);
            entities.put(entityNode.getName(), Truffle.getRuntime().createCallTarget(entityNode));
        }
        for (CALParser.NetworkDeclarationContext networkCtx: ctx.networkDeclaration()) {
            CALRootNode entityNode = NetworkVisitor.getInstance().visitNetworkDeclaration(networkCtx);
            entities.put(entityNode.getName(), Truffle.getRuntime().createCallTarget(entityNode));
        }

        NamespaceElementsToCallTarget ret = new NamespaceElementsToCallTarget();
        ret.entities = entities;
        ret.functions = functions;
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitAnnotation(CALParser.AnnotationContext ctx) {
        // TODO Add support for annotations in parent rules
        // Note: Unreachable for now
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Annotation is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitNamedAnnotationParameter(CALParser.NamedAnnotationParameterContext ctx) {
        // TODO First resolve #visitAnnotation
        // Note: Unreachable for now
        return super.visitNamedAnnotationParameter(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitUnnamedAnnotationParameter(CALParser.UnnamedAnnotationParameterContext ctx) {
        // TODO First resolve #visitAnnotation
        // Note: Unreachable for now
        return super.visitUnnamedAnnotationParameter(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitUnitDeclaration(CALParser.UnitDeclarationContext ctx) {
        // TODO Add support for units
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unit is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Pair<String, String> visitSingleImportDeclaration(CALParser.SingleImportDeclarationContext ctx) {
        return visitSingleImport(ctx.singleImport());
    }

    /**
     * {@inheritDoc}
     */
    @Override public Map<String, String> visitGroupImportDeclaration(CALParser.GroupImportDeclarationContext ctx) {
        return visitGroupImport(ctx.groupImport());
    }

    /**
     * {@inheritDoc}
     */
    @Override public Pair<String, String> visitSingleImport(CALParser.SingleImportContext ctx) {
        if (ctx.kind != null) {
            // TODO Add support for explicit import kind
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Explicit import kind is not yet supported");
            }
        }

        List<Token> globalName = (ArrayList<Token>) CollectionVisitor.getInstance().visitQualifiedID(ctx.globalName);

        String localName;
        if (ctx.localName != null) {
            localName = ctx.localName.getText();
        } else {
            localName = globalName.get(globalName.size() - 1).getText();
        }

        return Pair.create(localName, CollectionVisitor.qualifiedIdToString(globalName));
    }

    /**
     * {@inheritDoc}
     */
    @Override public Map<String, String> visitGroupImport(CALParser.GroupImportContext ctx) {
        if (ctx.kind != null && !(ctx.kind.kind.getText().equals("entity"))) {
//            // TODO Add support for explicit import kind
//            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
//                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Explicit import kind is not yet supported");
//            }
            return new HashMap<>();
        }

        QualifiedID globalName = CollectionVisitor.qualifiedIdCreator(CollectionVisitor.getInstance().visitQualifiedID(ctx.globalName));

        Map<String, String> locNameToGlobName = new HashMap<>();
        for(QualifiedID qid: NamespaceEntitiesMap.get(globalName.parts())) {
            String localName = qid.getLast().toString();
            locNameToGlobName.put(localName, globalName.concat(qid).toString());
        }

        return locNameToGlobName;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitImportKind(CALParser.ImportKindContext ctx) {
        // Note: Unreachable for now, only token is directly accessed
        return super.visitImportKind(ctx);
    }

}