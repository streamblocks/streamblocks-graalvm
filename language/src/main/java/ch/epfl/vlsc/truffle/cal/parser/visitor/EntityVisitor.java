package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.ActorLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import com.oracle.truffle.api.source.SourceSection;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Singleton class that provides an implementation for an entity sub-tree.
 */
public class EntityVisitor extends CALParserBaseVisitor<Object> {

    public static class EntityInstance {

        public static class EntityParameter {
            public String name;

            public CALExpressionNode valueNode;

            public EntityParameter(String name, CALExpressionNode valueNode) {
                this.name = name;
                this.valueNode = valueNode;
            }
        }

        public String name;

        public String actor;

        public List<EntityParameter> parameters;

        public List<Pair<String, CALExpressionNode>> inputs;

        public List<Pair<String, CALExpressionNode>> outputs;

        public SourceSection sourceSection;

        public EntityInstance(String actor, List<EntityParameter> parameters, SourceSection sourceSection) {
            this.actor = actor;
            this.parameters = parameters;
            this.sourceSection = sourceSection;
            this.inputs = new LinkedList<>();
            this.outputs = new LinkedList<>();
        }

    }

    private static EntityVisitor instance;

    private EntityVisitor() {}

    public static EntityVisitor getInstance() {
        if (instance == null) {
            instance = new EntityVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Pair<CALStatementNode, CALExpressionNode> visitEntityDeclaration(CALParser.EntityDeclarationContext ctx) {
        // TODO Change after implementing complex expressions (if/list)
        EntityInstance instance = (EntityInstance) visitEntityExpression(ctx.entityExpression());
        instance.name = ctx.name.getText();

        CALExpressionNode actorLiteralNode = new ActorLiteralNode(ScopeEnvironment.getInstance().getEntityFullName(instance.actor));
        actorLiteralNode.setSourceSection(instance.sourceSection);
        actorLiteralNode.addExpressionTag();

        List<CALExpressionNode> argumentNodes = new ArrayList<>();
        // TODO Add support for named parameters
        argumentNodes.addAll(instance.parameters.stream().map(parameter -> parameter.valueNode).collect(Collectors.toList()));

        CALExpressionNode valueNode = new CALInvokeNode(actorLiteralNode, argumentNodes.toArray(new CALExpressionNode[0]));
        valueNode.setSourceSection(instance.sourceSection);
        valueNode.addExpressionTag();

        CALExpressionNode instanceNode = ScopeEnvironment.getInstance().createNewVariableWriteNode(
                instance.name,
                valueNode,
                DefaultValueCastNodeCreator.getInstance(),
                instance.sourceSection);

        ScopeEnvironment.getInstance().getCurrentScope().increaseDepth();
        CALExpressionNode entityNode = ScopeEnvironment.getInstance().createReadNode(instance.name, instance.sourceSection);

        CALInvokeNode entityInvokeNode = new CALInvokeNode(entityNode, new CALExpressionNode[0]);
        entityInvokeNode.setSourceSection(instance.sourceSection);
        entityInvokeNode.addStatementTag();
        ScopeEnvironment.getInstance().getCurrentScope().decreaseDepth();

        return Pair.of(instanceNode, entityInvokeNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitEntityExpression(CALParser.EntityExpressionContext ctx) {
        if (ctx.entityInstanceExpression() != null) {
            return visitEntityInstanceExpression(ctx.entityInstanceExpression());
        } else if (ctx.entityIfExpression() != null) {
            return visitEntityIfExpression(ctx.entityIfExpression());
        } else if (ctx.entityListExpression() != null) {
            return visitEntityListExpression(ctx.entityListExpression());
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized entity expression type"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public EntityInstance visitEntityInstanceExpression(CALParser.EntityInstanceExpressionContext ctx) {
        String actor = ctx.actor.getText();

        List<EntityInstance.EntityParameter> parameters = new ArrayList<>();
        if (ctx.entityParameters() != null) {
            for (CALParser.EntityParameterContext parameterCtx: ctx.entityParameters().entityParameter()) {
                String name = parameterCtx.name.getText();
                CALExpressionNode valueNode = ExpressionVisitor.getInstance().visit(parameterCtx.value);
                parameters.add(new EntityInstance.EntityParameter(name, valueNode));
            }
        }

        if (ctx.attributeSection() != null) {
            // TODO Add support for attribute section
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Entity attributes are not yet supported");
            }
        }

        return new EntityInstance(actor, parameters, ScopeEnvironment.getInstance().createSourceSection(ctx));
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitEntityIfExpression(CALParser.EntityIfExpressionContext ctx) {
        // TODO Create Entity If expression
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Entity conditional expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitEntityListExpression(CALParser.EntityListExpressionContext ctx) {
        // TODO Create Entity List expression
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Entity list expression is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitEntityParameter(CALParser.EntityParameterContext ctx) {
        // Note: Unreachable for now, only tokens directly accessed in #visitEntityInstanceExpression
        return super.visitEntityParameter(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitAttributeDeclaration(CALParser.AttributeDeclarationContext ctx) {
        // TODO First resolve #visitEntityInstanceExpression
        // Note: Unreachable for now
        return super.visitAttributeDeclaration(ctx);
    }

}