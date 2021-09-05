package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NamespaceEntitiesMapVisitor extends CALParserBaseVisitor<Map<List<String>, List<QualifiedID>>> {

    private static NamespaceEntitiesMapVisitor instance;

    public static NamespaceEntitiesMapVisitor getInstance() {
        if (instance == null) {
            instance = new NamespaceEntitiesMapVisitor();
        }

        return instance;
    }

    @Override
    public Map<List<String>, List<QualifiedID>> visitUnnamedNamespaceDeclaration(CALParser.UnnamedNamespaceDeclarationContext ctx) {
        Map<List<String>, List<QualifiedID>> namespaceEntities = new HashMap<>();
        namespaceEntities.put(List.of(), new LinkedList<QualifiedID>());
        for(CALParser.ActorDeclarationContext actor: ctx.namespaceBody().actorDeclaration()) {
            namespaceEntities.get(List.of()).add(QualifiedID.parse(actor.name.getText()));
        }

        for(CALParser.NetworkDeclarationContext network: ctx.namespaceBody().networkDeclaration()) {
            namespaceEntities.get(List.of()).add(QualifiedID.parse(network.name.getText()));
        }
        return namespaceEntities;
    }

    @Override
    public Map<List<String>, List<QualifiedID>> visitNamedNamespaceDeclaration(CALParser.NamedNamespaceDeclarationContext ctx) {
        Map<List<String>, List<QualifiedID>> namespaceEntities = new HashMap<>();
        LinkedList<QualifiedID> entities = new LinkedList<QualifiedID>();
        for(CALParser.ActorDeclarationContext actor: ctx.namespaceBody().actorDeclaration()) {
            entities.add(QualifiedID.parse(actor.name.getText()));
        }

        for(CALParser.NetworkDeclarationContext network: ctx.namespaceBody().networkDeclaration()) {
            entities.add(QualifiedID.parse(network.name.getText()));
        }

        namespaceEntities.put(QualifiedID.parse(ctx.name.getText()).parts(), entities);

        return namespaceEntities;
    }

    @Override
    protected Map<List<String>, List<QualifiedID>> aggregateResult(Map<List<String>, List<QualifiedID>> aggregate, Map<List<String>, List<QualifiedID>> nextResult) {
        Map<List<String>, List<QualifiedID>> result = new HashMap<>();

        if (aggregate != null)
            for(List<String> namespaceParts: aggregate.keySet()) {
                if(!result.containsKey(namespaceParts)) result.put(namespaceParts, new LinkedList<>());
                result.get(namespaceParts).addAll(aggregate.get(namespaceParts));
            }

        if (nextResult != null)
            for(List<String> namespaceParts: nextResult.keySet()) {
                if(!result.containsKey(namespaceParts)) result.put(namespaceParts, new LinkedList<>());
                result.get(namespaceParts).addAll(nextResult.get(namespaceParts));
            }
        return result;
    }
}
