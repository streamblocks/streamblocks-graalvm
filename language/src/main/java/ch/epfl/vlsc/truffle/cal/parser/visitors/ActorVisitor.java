package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.ActorNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.parser.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;

import java.util.ArrayList;
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
        // FIXME Finish implementing actor head statements
        // TODO Add support for initialization actions
        // TODO Add support for action priorities
        // TODO Add support for action schedules

        ScopeEnvironment environment = ScopeEnvironment.getInstance();
        environment.pushScope();

        List<CALStatementNode> headStatements = new ArrayList<>();

        if (ctx.localVariableDeclaration() != null) {
            for (CALParser.LocalVariableDeclarationContext context: ctx.localVariableDeclaration()) {
                headStatements.add(VariableVisitor.getInstance().visitLocalVariableDeclaration(context));
            }
        }
        StmtBlockNode head = new StmtBlockNode(headStatements.toArray(new CALStatementNode[0]));

        List<ActionNode> actions = new ArrayList<>();
        if (ctx.actionDefinition() != null) {
            for (CALParser.ActionDefinitionContext context: ctx.actionDefinition()) {
                actions.add(ActionVisitor.getInstance().visitActionDefinition(context));
            }
        }

        ActorNode result = new ActorNode(environment.getLanguage(), environment.getCurrentScope().getFrame(), actions.toArray(new ActionNode[0]), head, null, ctx.name.getText());

        environment.popScope();

        return result;

    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPortDeclarations(CALParser.PortDeclarationsContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPortDeclaration(CALParser.PortDeclarationContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitProcessDescription(CALParser.ProcessDescriptionContext ctx) {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Object visitActionSchedule(CALParser.ActionScheduleContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitScheduleFSM(CALParser.ScheduleFSMContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitStateTransition(CALParser.StateTransitionContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitScheduleRegExp(CALParser.ScheduleRegExpContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitRegExp(CALParser.RegExpContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPriorityOrder(CALParser.PriorityOrderContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitPriorityInequality(CALParser.PriorityInequalityContext ctx) {
        return null;
    }

}