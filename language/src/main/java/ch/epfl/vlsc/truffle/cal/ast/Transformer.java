package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.source.Source;

public abstract class Transformer<R> {
    Source source;
    CALLanguage language;
    protected Transformer(CALLanguage language, Source source) {
        this.source = source;
        this.language = language;
    }

    public abstract R transform();

}
