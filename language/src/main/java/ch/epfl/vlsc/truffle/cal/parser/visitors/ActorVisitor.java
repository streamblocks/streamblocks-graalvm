package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.parser.error.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.DepthFrameSlot;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Singleton class that provides an implementation for an actor sub-tree.
 */
public class ActorVisitor extends CALParserBaseVisitor<Object> {

    private static ActorVisitor instance;

    private ActorVisitor() {}

    public static ActorVisitor getInstance() {
        if (instance == null) {
            instance = new ActorVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public ActorNode visitActorDeclaration(CALParser.ActorDeclarationContext ctx) {
        ScopeEnvironment.getInstance().pushScope();

        List<CALStatementNode> headStatementNodes = new ArrayList<>();
        if (ctx.formalParameters() != null) {
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headStatementNodes.addAll(formalParameterNodes);
        }
        if (ctx.inputPorts != null) {
            Collection<InitializeArgNode> inputPortNodes = CollectionVisitor.getInstance().visitPortDeclarations(ctx.inputPorts);
            headStatementNodes.addAll(inputPortNodes);
        }
        if (ctx.outputPorts != null) {
            Collection<InitializeArgNode> outputPortNodes = CollectionVisitor.getInstance().visitPortDeclarations(ctx.outputPorts);
            headStatementNodes.addAll(outputPortNodes);
        }
        if (ctx.time != null) {
            // TODO Add support for actor time
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Actor time is not yet supported");
        }
        if (ctx.localVariableDeclaration() != null) {
            for (CALParser.LocalVariableDeclarationContext context: ctx.localVariableDeclaration()) {
                headStatementNodes.add(VariableVisitor.getInstance().visitLocalVariableDeclaration(context));
            }
        }
        StmtBlockNode headNode = new StmtBlockNode(headStatementNodes.toArray(new CALStatementNode[0]));

        List<ActionNode> actionNodes = new ArrayList<>();
        if (ctx.actionDefinition() != null) {
            for (CALParser.ActionDefinitionContext context: ctx.actionDefinition()) {
                actionNodes.add(ActionVisitor.getInstance().visitActionDefinition(context));
            }
        }

        if (ctx.initializationActionDefinition().size() > 0) {
            // TODO Add support for initialization actions
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Initialization action is not yet supported");
        }

        if (ctx.priorityOrder().size() > 0) {
            // TODO Add support for action priorities
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Action priorities are not yet supported");
        }

        if (ctx.actionSchedule().size() > 0) {
            // TODO Add support for action schedules
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Action schedule is not yet supported");
        }

        if (ctx.processDescription().size() > 0) {
            // TODO Add support for process descriptions
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Process description is not yet supported");
        }

        ActorNode actorNode = new ActorNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                actionNodes.toArray(new ActionNode[0]),
                headNode,
                null,
                ctx.name.getText()
        );

        ScopeEnvironment.getInstance().popScope();

        return actorNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override public InitializeArgNode visitPortDeclaration(CALParser.PortDeclarationContext ctx) {
        if (ctx.isMulti != null) {
            // TODO Add support for multi-ports ('multi')
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Multi-port is not yet supported");
        }
        if (ctx.type() != null) {
            // TODO Add support for port type
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Port type is not yet supported");
        }

        String portName = ctx.name.getText();

        DepthFrameSlot existingSlot = ScopeEnvironment.getInstance().getCurrentScope().get(portName);
        DepthFrameSlot readOnlySlot = new DepthFrameSlot(existingSlot);

        ScopeEnvironment.getInstance().getCurrentScope().put(portName, readOnlySlot);

        ParserRuleContext parentCtx = ctx.getParent();
        if (parentCtx instanceof CALParser.PortDeclarationsContext) {
            return new InitializeArgNode(readOnlySlot.getSlot(), ((CALParser.PortDeclarationsContext) parentCtx).portDeclaration().indexOf(ctx));
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Port position cannot be determined"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitProcessDescription(CALParser.ProcessDescriptionContext ctx) {
        // TODO Create Process Description node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Process description is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitActionSchedule(CALParser.ActionScheduleContext ctx) {
        // TODO Create both FSM and RegExp schedule nodes
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Action schedules are not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitScheduleFSM(CALParser.ScheduleFSMContext ctx) {
        // TODO Create FSM schedule node
        // Note: Unreachable for now
        return super.visitScheduleFSM(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitStateTransition(CALParser.StateTransitionContext ctx) {
        // TODO First resolve #visitScheduleFSM
        // Note: Unreachable for now
        return super.visitStateTransition(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitScheduleRegExp(CALParser.ScheduleRegExpContext ctx) {
        // TODO Create RegExp schedule node
        // Note: Unreachable for now
        return super.visitScheduleRegExp(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitRegExp(CALParser.RegExpContext ctx) {
        // TODO First resolve #visitScheduleRegExp
        // Note: Unreachable for now
        return super.visitRegExp(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPriorityOrder(CALParser.PriorityOrderContext ctx) {
        // TODO Create Priority Order node
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Actor priorities are not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPriorityInequality(CALParser.PriorityInequalityContext ctx) {
        // TODO First resolve #visitPriorityOrder
        // Note: Unreachable for now
        return super.visitPriorityInequality(ctx);
    }

}