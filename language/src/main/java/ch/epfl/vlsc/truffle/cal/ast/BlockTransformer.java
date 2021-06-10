package ch.epfl.vlsc.truffle.cal.ast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;

import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import ch.epfl.vlsc.truffle.cal.nodes.NetworkNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.entity.nl.NlNetwork;

public class BlockTransformer {

    TransformContext context;

    public BlockTransformer(TransformContext context) {
        this.context = context;
    }

    private QID entityQID(String name) {
        return context.getNamespace().concat(new QID(Arrays.asList(name)));
    }
    public Map<QID, RootCallTarget> transformActors(NamespaceDecl namespace) {
        Map<QID, RootCallTarget> nsFunctions = new HashMap<>();
        for (GlobalEntityDecl entity : namespace.getEntityDecls()) {
            QID entityName = entityQID(entity.getName());
            if (entity.getEntity() instanceof CalActor) {
                // FIXME getname
                ActorNode actor = transformActor((CalActor) entity.getEntity(), entityName);
                nsFunctions.put(entityName, Truffle.getRuntime().createCallTarget(actor));
            } else if (entity.getEntity() instanceof NlNetwork) {
                NetworkNode nlNetwork = transformNetork((NlNetwork) entity.getEntity(), entityName);
                nsFunctions.put(entityName, Truffle.getRuntime().createCallTarget(nlNetwork));
            } else {
                throw new UnsupportedOperationException("Unsupported global entity", null);
            }
        }
        return nsFunctions;
    }

    public NetworkNode transformNetork(NlNetwork nlNetwork, QID name) {
        // start a block and a rootcalltarget
        // new lexical scope
        return (new NetworkTransformer(nlNetwork, name, context.deeper(false))).transform();
    }

    public ActorNode transformActor(CalActor actor, QID name) {
        // start a block and a rootcalltarget
        // new lexical scope
        return (new ActorTransformer(actor, name, context.deeper(false))).transform();
    }

}
