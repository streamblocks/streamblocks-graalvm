package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.parser.error.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;

/**
 * Singleton class that provides an implementation for a generator sub-tree.
 */
public class TypeVisitor extends CALParserBaseVisitor<Object> {

    private static TypeVisitor instance;

    private TypeVisitor() {}

    public static TypeVisitor getInstance() {
        if (instance == null) {
            instance = new TypeVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitTypeDefinition(CALParser.TypeDefinitionContext ctx) {
        // TODO First resolve CompilationUnitVisitor#visitNamespaceBody
        // Note: Unreachable for now
        return super.visitTypeDefinition(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitTaggedTuple(CALParser.TaggedTupleContext ctx) {
        // TODO First resolve #visitTypeDefinition
        // Note: Unreachable for now
        return super.visitTaggedTuple(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitTuple(CALParser.TupleContext ctx) {
        // TODO First resolve #visitTypeDefinition
        // Note: Unreachable for now
        return super.visitTuple(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitType(CALParser.TypeContext ctx) {
        // TODO First resolve VariableVisitor#visitPortDeclaration
        // TODO First resolve VariableVisitor#visitExplicitVariableDeclaration
        // TODO First resolve VariableVisitor#visitFunctionVariableDeclaration
        // TODO First resolve ExpressionVisitor#visitLambdaExpression
        // Note: Unreachable for now
        return super.visitType(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitTypeParameter(CALParser.TypeParameterContext ctx) {
        // TODO First resolve #visitType
        // Note: Unreachable for now
        return super.visitTypeParameter(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitTypeAttribute(CALParser.TypeAttributeContext ctx) {
        // TODO First resolve #visitType
        // Note: Unreachable for now
        return super.visitTypeAttribute(ctx);
    }

}