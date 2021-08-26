package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.utils.ActorNodeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

        String actorName = ctx.name.getText();

        List<CALStatementNode> headStatementNodes = new ArrayList<>();
        int startIndex = 0;
        if (ctx.formalParameters() != null) {
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headStatementNodes.addAll(formalParameterNodes);
            startIndex += ctx.formalParameters().formalParameter().size();
        }
        if (ctx.inputPorts != null) {
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<InitializeArgNode> inputPortNodes = CollectionVisitor.getInstance().visitPortDeclarations(ctx.inputPorts);
            headStatementNodes.addAll(inputPortNodes);
            startIndex += ctx.inputPorts.portDeclaration().size();
        }
        if (ctx.outputPorts != null) {
            VariableVisitor.setPortDeclarationStartIndex(startIndex);
            Collection<InitializeArgNode> outputPortNodes = CollectionVisitor.getInstance().visitPortDeclarations(ctx.outputPorts);
            headStatementNodes.addAll(outputPortNodes);
            //startIndex += ctx.outputPorts.portDeclaration().size();
        }
        if (ctx.time != null) {
            // TODO Add support for actor time
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(CALLanguage.showWarnings)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Actor time is not yet supported");
            }
        }
        if (ctx.localVariableDeclaration() != null) {
            for (CALParser.LocalVariableDeclarationContext localVariableCtx: ctx.localVariableDeclaration()) {
                headStatementNodes.add(VariableVisitor.getInstance().visitLocalVariableDeclaration(localVariableCtx));
            }
        }
        StmtBlockNode headNode = new StmtBlockNode(headStatementNodes.toArray(new CALStatementNode[0]));
        headNode.setUnavailableSourceSection();
        headNode.addStatementTag();

        List<ActionNode> actionNodes = new ArrayList<>();
        if (ctx.actionDefinition() != null) {
            for (CALParser.ActionDefinitionContext actionCtx: ctx.actionDefinition()) {
                actionNodes.add(ActionVisitor.getInstance().visitActionDefinition(actionCtx));
            }
        }

        if (ctx.initializationActionDefinition().size() > 0) {
            // TODO Add support for initialization actions
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(CALLanguage.showWarnings)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Initialization action is not yet supported");
            }
        }

        if (ctx.priorityOrder().size() > 0) {
            // Order the actions by priorities
            actionNodes = ActorNodeUtils.topologicalSortByPriorities(actionNodes, ctx.priorityOrder().stream().map(po -> visitPriorityOrder(po)).collect(Collectors.toList()));
        }

        if (ctx.actionSchedule().size() > 0) {
            // TODO Add support for action schedules
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(CALLanguage.showWarnings)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Action schedule is not yet supported");
            }
        }

        if (ctx.processDescription().size() > 0) {
            // TODO Add support for process descriptions
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(CALLanguage.showWarnings)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Process description is not yet supported");
            }
        }

        ActorNode actorNode = new ActorNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                actionNodes.toArray(new ActionNode[0]),
                headNode,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                actorName
        );
        // TODO Add RootTag / CallTag for actorNode

        ScopeEnvironment.getInstance().popScope();

        return actorNode;
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
    @Override public List<QualifiedID> visitPriorityOrder(CALParser.PriorityOrderContext ctx) {
        if (ctx.priorityInequality().size() == 1) {
            return visitPriorityInequality(ctx.priorityInequality(0));
        } else if (ctx.priorityInequality().size() == 0) {
            return Collections.emptyList();
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unexpected number of Priority Inequalities in Priorities");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public List<QualifiedID> visitPriorityInequality(CALParser.PriorityInequalityContext ctx) {
        return ctx.actionTag().stream().map(tag -> visitActionTag(tag)).collect(Collectors.toList());
    }

    @Override
    public QualifiedID visitActionTag(CALParser.ActionTagContext ctx) {
        return CollectionVisitor.qualifiedIdCreator(ActionVisitor.getInstance().visitActionTag(ctx));
    }
}