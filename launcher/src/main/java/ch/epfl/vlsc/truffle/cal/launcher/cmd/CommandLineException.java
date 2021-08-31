package ch.epfl.vlsc.truffle.cal.launcher.cmd;

public class CommandLineException extends Exception {

    private static final long serialVersionUID = -7893606455917078404L;

    private final boolean usageError;

    public CommandLineException(String message) {
        this(message, false);
    }

    public CommandLineException(String message, boolean usageError) {
        super(message);
        this.usageError = usageError;
    }

    public boolean isUsageError() {
        return usageError;
    }

}