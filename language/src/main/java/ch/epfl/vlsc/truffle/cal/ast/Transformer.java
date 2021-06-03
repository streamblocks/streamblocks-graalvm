package ch.epfl.vlsc.truffle.cal.ast;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.source.Source;

public abstract class Transformer<R> {
    Source source;
    CALLanguage language;
    protected Transformer(CALLanguage language, Source source) {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                throw new TransformException(e.getMessage(), source);
            }
        });
        this.source = source;
        this.language = language;
    }

    public abstract R transform();

}
