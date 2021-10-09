package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;

import java.util.*;

public class ActionBodyOldReferenceFindVisitor extends CALParserBaseVisitor<List<String>>{
    private static ActionBodyOldReferenceFindVisitor instance;

    public static ActionBodyOldReferenceFindVisitor getInstance() {
        if (instance == null) {
            instance = new ActionBodyOldReferenceFindVisitor();
        }

        return instance;
    }

    @Override
    public List<String> visitVariableExpression(CALParser.VariableExpressionContext ctx) {
        if (ctx.isOld != null) return Arrays.asList(ctx.variable().name.getText());
        else return Collections.emptyList();
    }

    @Override
    protected List<String> aggregateResult(List<String> aggregate, List<String> nextResult) {
        if (aggregate == null && nextResult == null) return Collections.emptyList();
        else if (aggregate == null) return nextResult;
        else if (nextResult == null) return aggregate;
        else {
            LinkedList<String> res = new LinkedList<String>();
            res.addAll(aggregate);
            res.addAll(nextResult);
            return res;
        }
    }
}