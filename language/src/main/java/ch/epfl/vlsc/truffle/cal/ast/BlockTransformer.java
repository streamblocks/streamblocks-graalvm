package ch.epfl.vlsc.truffle.cal.ast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.NetworkNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteFrameSlotNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.Import;
import se.lth.cs.tycho.ir.decl.SingleImport;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.entity.nl.NlNetwork;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;

public class BlockTransformer {

    Source source;
    CALLanguage language;
    TransformContext context;

    public BlockTransformer(CALLanguage language, Source source, TransformContext context) {
        this.source = source;
        this.language = language;
        this.context = context;
    }

    private QID entityQID(String name) {
        return context.namespace.concat(new QID(Arrays.asList(name)));
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
        return (new NetworkTransformer(language, source, nlNetwork, name, 0, context)).transform();
    }

    public ActorNode transformActor(CalActor actor, QID name) {
        // start a block and a rootcalltarget
        // new lexical scope
        return (new ActorTransformer(language, source, actor, name, 0, context)).transform();
    }

}
