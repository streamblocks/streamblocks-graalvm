package ch.epfl.vlsc.truffle.cal.ast;

public abstract class Transformer<R> {
    protected Transformer(TransformContext context) {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                throw new TransformException(e.getMessage(), context.getSource());
            }
        });
    }

    public abstract R transform();

}
