package ch.epfl.vlsc.truffle.cal.ast;

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
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteFrameSlotNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
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

    public BlockTransformer(CALLanguage language, Source source) {
        this.source = source;
        this.language = language;
    }

    public Map<String, RootCallTarget> transformActors(NamespaceDecl namespace) {
        Map<String, RootCallTarget> nsFunctions = new HashMap<>();
        for (GlobalEntityDecl entity : namespace.getEntityDecls()) {
            if (entity.getEntity() instanceof CalActor) {
                // FIXME getname
                ActorNode actor = transformActor((CalActor) entity.getEntity(), namespace.getQID().toString());
                nsFunctions.put(actor.getName(), Truffle.getRuntime().createCallTarget(actor));
            }
        }
        
        // TODO try to instantiate here an actor
        return nsFunctions;
    }

    public ActorNode transformActor(CalActor actor, String name) {
        // start a block and a rootcalltarget
        // new lexical scope
        // FIXME
        return (new ActorTransformer(language, source, actor, name, 0)).transform();
    }


}
