package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
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

    private CompilationUnitVisitor() {}

    public static CompilationUnitVisitor getInstance() {
        if (instance == null) {
            instance = new CompilationUnitVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Map<String, RootCallTarget> visitNamespaceCompilationUnit(CALParser.NamespaceCompilationUnitContext ctx) {
        return (Map<String, RootCallTarget>) visit(ctx.namespaceDeclaration());
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
    @Override public Map<String, RootCallTarget> visitUnnamedNamespaceDeclaration(CALParser.UnnamedNamespaceDeclarationContext ctx) {
        return visitNamespaceBody(ctx.body);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Map<String, RootCallTarget> visitNamedNamespaceDeclaration(CALParser.NamedNamespaceDeclarationContext ctx) {
        String name = CollectionVisitor.qualifiedIdToString(CollectionVisitor.getInstance().visitQualifiedID(ctx.name));
        ScopeEnvironment.getInstance().setName(name);

        Map<String, RootCallTarget> entities = visitNamespaceBody(ctx.body);

        return entities.entrySet().stream().collect(Collectors.toMap(e -> name + "." + e.getKey(), Map.Entry::getValue));
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
    @Override public Map<String, RootCallTarget> visitNamespaceBody(CALParser.NamespaceBodyContext ctx) {
        for (CALParser.ImportDeclarationContext importCtx: ctx.importDeclaration()) {
            if (importCtx instanceof CALParser.SingleImportDeclarationContext) {
                ScopeEnvironment.getInstance().addImport(visitSingleImportDeclaration((CALParser.SingleImportDeclarationContext) importCtx));
            }
        }

        Map<String, RootCallTarget> entities = new HashMap<>();
        if (ctx.typeDefinition().size() > 0) {
            // TODO Add support for type definitions
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Type definition is not yet supported");
        }
        if (ctx.globalVariableDeclaration().size() > 0) {
            // TODO Add support for global variables
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Global variable is not yet supported");
        }
        for (CALParser.ActorDeclarationContext actorCtx: ctx.actorDeclaration()) {
            CALRootNode entityNode = ActorVisitor.getInstance().visitActorDeclaration(actorCtx);
            entities.put(entityNode.getName(), Truffle.getRuntime().createCallTarget(entityNode));
        }
        for (CALParser.NetworkDeclarationContext networkCtx: ctx.networkDeclaration()) {
            CALRootNode entityNode = NetworkVisitor.getInstance().visitNetworkDeclaration(networkCtx);
            entities.put(entityNode.getName(), Truffle.getRuntime().createCallTarget(entityNode));
        }

        return entities;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitAnnotation(CALParser.AnnotationContext ctx) {
        // TODO Add support for annotations
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
    @Override public Object visitGroupImportDeclaration(CALParser.GroupImportDeclarationContext ctx) {
        return visitGroupImport(ctx.groupImport());
    }

    /**
     * {@inheritDoc}
     */
    @Override public Pair<String, String> visitSingleImport(CALParser.SingleImportContext ctx) {
        if (ctx.kind != null) {
            // TODO Add support for explicit import kind
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Explicit import kind is not yet supported");
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
    @Override public Object visitGroupImport(CALParser.GroupImportContext ctx) {
        // TODO Add support for group imports
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Group import is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitImportKind(CALParser.ImportKindContext ctx) {
        // Note: Unreachable for now, only token is directly accessed
        return super.visitImportKind(ctx);
    }

}