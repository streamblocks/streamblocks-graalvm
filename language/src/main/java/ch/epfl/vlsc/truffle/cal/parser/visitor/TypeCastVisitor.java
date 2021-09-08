package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.BigIntegerLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.LongLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.ListInitNodeSizeExpression;
import ch.epfl.vlsc.truffle.cal.nodes.local.lists.UnknownSizeListInitNode;
import ch.epfl.vlsc.truffle.cal.nodes.util.DefaultValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.nodes.util.IntCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.nodes.util.ValueCastNodeCreator;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TypeCastVisitor extends CALParserBaseVisitor<ValueCastNodeCreator> {

    private static TypeCastVisitor instance;

    private TypeCastVisitor() {}

    public static TypeCastVisitor getInstance() {
        if (instance == null) {
            instance = new TypeCastVisitor();
        }

        return instance;
    }

    @Override
    public ValueCastNodeCreator visitType(CALParser.TypeContext ctx) {
        if (ctx == null) return DefaultValueCastNodeCreator.getInstance();

        if (ctx.name == null) {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Types without name not yet supported");
        }

        List<CALExpressionNode> sizeDecl =  ctx.typeAttributes() != null ? ctx.typeAttributes().typeAttribute().stream().filter(x -> x.exprAttributeName != null && x.exprAttributeName.getText().equals("size")).map(x -> ExpressionVisitor.getInstance().visit(x.expression())).collect(Collectors.toList()) : Collections.emptyList();

        if (ctx.name.getText().equals("List")) {
            return DefaultValueCastNodeCreator.getInstance();
        } else if (ctx.name.getText().equals("int")) {
            CALExpressionNode intsize = sizeDecl.size() > 0 ? sizeDecl.get(0) : new LongLiteralNode(CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.BITLENGTH_KEY));
            return new IntCastNodeCreator(intsize, true);
        } else if (ctx.name.getText().equals("uint")) {
            CALExpressionNode intsize = sizeDecl.size() > 0 ? sizeDecl.get(0) : new LongLiteralNode(CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.BITLENGTH_KEY));
            return new IntCastNodeCreator(intsize, false);
        } else if (ctx.name.getText().equals("bool")) {
            return DefaultValueCastNodeCreator.getInstance();
        } else
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "No default value for type unknows type " + ctx.name.getText());
    }
}
