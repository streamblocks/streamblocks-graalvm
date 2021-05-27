package ch.epfl.vlsc.truffle.cal.ast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.decl.Import;
import se.lth.cs.tycho.ir.decl.SingleImport;

public class TransformContext {
    public QID namespace;
    public Map<String, QID> entities;

    public TransformContext(NamespaceDecl namespace) {
        Map<String, QID> globalContext = new HashMap<>();
        for (Import imp : namespace.getImports()) {
            if (imp instanceof SingleImport)
                globalContext.put(((SingleImport) imp).getLocalName(), ((SingleImport) imp).getGlobalName());
            else
                throw new Error("Unsupported import type");
        }

        this.entities = globalContext;
        this.namespace = namespace.getQID();
    }

    public QID getEntityQID(String name) {
        // Either use an imported or default to local
        return entities.getOrDefault(name, namespace.concat(QID.parse(name)));
    }
}
