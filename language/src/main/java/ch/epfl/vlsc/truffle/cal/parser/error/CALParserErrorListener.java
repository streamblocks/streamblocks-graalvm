package ch.epfl.vlsc.truffle.cal.parser.error;

import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class CALParserErrorListener extends BaseErrorListener {
    private final Source source;

    public CALParserErrorListener(Source source) {
        this.source = source;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        int col = charPositionInLine + 1;
        String location = "-- line " + line + " col " + col + ": ";

        Token token = (Token) offendingSymbol;
        int length = token == null ? 1 : Math.max(token.getStopIndex() - token.getStartIndex(), 0);

        throw new CALParserError(source, line, col, length, String.format("Error(s) parsing script:%n" + location + msg));
    }
}
