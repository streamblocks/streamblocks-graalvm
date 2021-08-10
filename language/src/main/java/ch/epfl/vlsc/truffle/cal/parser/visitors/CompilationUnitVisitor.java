package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.parser.ScopeEnvironment;
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
        return null;
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
        Map<String, RootCallTarget> entities = visitNamespaceBody(ctx.body);

        return entities.entrySet().stream().collect(Collectors.toMap(e -> name + "." + e.getKey(), Map.Entry::getValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPackageNamespaceDeclaration(CALParser.PackageNamespaceDeclarationContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Map<String, RootCallTarget> visitNamespaceBody(CALParser.NamespaceBodyContext ctx) {
        // TODO Add support for type definitions
        // TODO Add support for global variables

        for (CALParser.ImportDeclarationContext context: ctx.importDeclaration()) {
            ScopeEnvironment.getInstance().addImport((Pair<String, String>) visit(context));
        }

        Map<String, RootCallTarget> entities = new HashMap<>();
        for (CALParser.ActorDeclarationContext context: ctx.actorDeclaration()) {
            CALRootNode entityNode = ActorVisitor.getInstance().visitActorDeclaration(context);
            entities.put(entityNode.getName(), Truffle.getRuntime().createCallTarget(entityNode));
        }

        // FIXME Add network entities

        return entities;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitUnitDeclaration(CALParser.UnitDeclarationContext ctx) {
        return null;
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Pair<String, String> visitSingleImport(CALParser.SingleImportContext ctx) {
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitVariableImportKind(CALParser.VariableImportKindContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitTypeImportKind(CALParser.TypeImportKindContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitEntityImportKind(CALParser.EntityImportKindContext ctx) {
        return null;
    }

}