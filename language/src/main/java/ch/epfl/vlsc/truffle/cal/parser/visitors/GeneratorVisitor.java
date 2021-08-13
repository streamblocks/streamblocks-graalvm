package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
/**
 * Singleton class that provides an implementation for a generator sub-tree.
 */
public class GeneratorVisitor extends CALParserBaseVisitor<Object> {

    private static GeneratorVisitor instance;

    private GeneratorVisitor() {}

    public static GeneratorVisitor getInstance() {
        if (instance == null) {
            instance = new GeneratorVisitor();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitGenerator(CALParser.GeneratorContext ctx) {
        // Note: Unreachable for now, only children directly accessed in ExpressionVisitor#visitListComprehension
        return super.visitGenerator(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitForeachGenerator(CALParser.ForeachGeneratorContext ctx) {
        // Note: Unreachable for now, only children directly accessed in StatementVisitor#visitForeachStatement
        return super.visitForeachGenerator(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitChooseGenerator(CALParser.ChooseGeneratorContext ctx) {
        // Note: Unreachable for now, StatementVisitor#visitChooseStatement is not yet implemented
        return super.visitChooseGenerator(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object visitGeneratorBody(CALParser.GeneratorBodyContext ctx) {
        // Note: Unreachable for now, only children directly accessed in both StatementVisitor#visitForeachStatement and ExpressionVisitor#visitListComprehension
        return super.visitGeneratorBody(ctx);
    }

}