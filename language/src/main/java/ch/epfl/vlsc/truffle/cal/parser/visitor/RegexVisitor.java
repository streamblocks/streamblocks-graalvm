package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.tuple.Pair;

public class RegexVisitor extends CALParserBaseVisitor<Pair<BidiMap<QualifiedID, Character>, String>> {
    char curr = 884;
    BidiMap<QualifiedID, Character> qual2c = new DualHashBidiMap<>();

    @Override
    public Pair<BidiMap<QualifiedID, Character>, String> visitRegExp(CALParser.RegExpContext ctx) {
        if(ctx.actionTag() != null) {
            QualifiedID qid = ActorVisitor.getInstance().visitActionTag(ctx.actionTag());
            char qidc;
            if (!qual2c.containsKey(qid)) {
                qidc = curr++;
                qual2c.put(qid, qidc);
            } else
                qidc = qual2c.get(qid);
            return Pair.of(qual2c, "(" + (qidc) + ")");
        } else if (ctx.regExpGroup() != null) {
            return visitRegExpGroup(ctx.regExpGroup());
        } else if (ctx.regExpOptional() != null) {
            return visitRegExpOptional(ctx.regExpOptional());
        } else if (ctx.regExp().size() == 1 && ctx.stop.getText().equals("*")) {
            Pair<BidiMap<QualifiedID, Character>, String> p = visitRegExp(ctx.regExp(0));
            return Pair.of(qual2c, "(" + p.getRight() + "*" + ")");
        } else if (ctx.regExp().size() == 2 && ctx.getChildCount() == 2) {
            // Regex Pair
            Pair<BidiMap<QualifiedID, Character>, String> p1 = visitRegExp(ctx.regExp(0));
            Pair<BidiMap<QualifiedID, Character>, String> p2 = visitRegExp(ctx.regExp(1));
            return Pair.of(qual2c, "(" + p1.getRight() + p2.getRight() + ")");
        } else if (ctx.regExp().size() == 2 && ctx.getChildCount() == 3 && ctx.getChild(1).getText().equals("|")) {
            Pair<BidiMap<QualifiedID, Character>, String> p1 = visitRegExp(ctx.regExp(0));
            Pair<BidiMap<QualifiedID, Character>, String> p2 = visitRegExp(ctx.regExp(1));
            return Pair.of(qual2c, "(" + p1.getRight() + "|" + p2.getRight() + ")");
        } else
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unexpected regex type found");
    }

    @Override
    public Pair<BidiMap<QualifiedID, Character>, String> visitRegExpOptional(CALParser.RegExpOptionalContext ctx) {
        Pair<BidiMap<QualifiedID, Character>, String> p = visitRegExp(ctx.regExp());
        return Pair.of(qual2c, "(" + p.getRight() + "?" + ")");
    }

    @Override
    public Pair<BidiMap<QualifiedID, Character>, String> visitRegExpGroup(CALParser.RegExpGroupContext ctx) {
        Pair<BidiMap<QualifiedID, Character>, String> p = visitRegExp(ctx.regExp());
        return Pair.of(qual2c, "(" + p.getRight() + ")");
    }
}
