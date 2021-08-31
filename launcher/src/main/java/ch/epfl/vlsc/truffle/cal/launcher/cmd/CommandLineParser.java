package ch.epfl.vlsc.truffle.cal.launcher.cmd;

import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import ch.epfl.vlsc.truffle.cal.shared.options.Verbosity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class CommandLineParser {
    private static final Logger LOGGER = createLogger();

    private final List<String> arguments;
    private int argumentIndex;
    private final boolean processArgv;
    private final boolean calOpts;
    final CommandLineOptions config;
    private int lastInterpreterArgumentIndex;
    private int characterIndex;

    public CommandLineParser(List<String> arguments, CommandLineOptions config, boolean processArgv, boolean calOpts) {
        this.argumentIndex = 0;
        this.characterIndex = 0;
        this.lastInterpreterArgumentIndex = -1;
        this.config = config;
        this.processArgv = processArgv;
        this.calOpts = calOpts;
        this.arguments = Collections.unmodifiableList(arguments);
    }


    public void processArguments() throws CommandLineException {
        while (argumentIndex < arguments.size() && isInterpreterArgument(getCurrentArgument())) {
            processArgument();
            argumentIndex++;
        }

        if (!endOfInterpreterArguments()) {
            lastInterpreterArgumentIndex = argumentIndex;
        }
        assert lastInterpreterArgumentIndex >= 0;

        if (config.executionAction == ExecutionAction.UNSET) {
            if (argumentIndex < arguments.size()) {
                config.executionAction = ExecutionAction.FILE;
                //consume the file name
                config.toExecute = getCurrentArgument();
                argumentIndex++;
            }
        }
    }

    private boolean endOfInterpreterArguments() {
        return lastInterpreterArgumentIndex != -1;
    }

        private boolean isInterpreterArgument(String argument) {
        return argument.length() > 0 && (argument.charAt(0) == '-' || argument.charAt(0) == '+') &&
                !endOfInterpreterArguments();
    }

    private String getArgumentError(String additionalError) {
        return "invalid argument\n" + additionalError + "\n";
    }

    private void processArgument() throws CommandLineException {
        String argument = getCurrentArgument();

        if (argument.length() == 1) {
            // sole "-" means read from stdin and pass remaining args as ARGV
            lastInterpreterArgumentIndex = argumentIndex;

            if (config.executionAction == ExecutionAction.UNSET) {
                config.executionAction = ExecutionAction.STDIN;
            } else {
                // if other action is set then ignore '-', just threat it as a first script argument,
                // and stop option parsing
            }

            return;
        }

        FOR: for (characterIndex = 1; characterIndex < argument.length(); characterIndex++) {
            switch (argument.charAt(characterIndex)) {
                case 'c':
                    config.setOption(OptionsCatalog.SYNTAX_CHECK, true);
                    break;
                case 'X':
                    final String dir = grabValue(
                            getArgumentError(
                                    " -" + argument.charAt(characterIndex) +
                                            " must be followed by a directory expression"));
                    config.setOption(OptionsCatalog.WORKING_DIRECTORY, dir);
                    break FOR;
                case 'd':
                    config.setOption(OptionsCatalog.DEBUG, true);
                    config.setOption(OptionsCatalog.VERBOSITY, Verbosity.TRUE);
                    break;
                case 'h':
                    config.showHelp = ShowHelp.SHORT;
                    // cancel other execution actions
                    config.executionAction = ExecutionAction.NONE;
                    break;
                case 'e':
                    String entity = grabValue(getArgumentError("Wrong Qualified ID for entity."));
                    config.setOption(OptionsCatalog.ENTITY_QID, entity);
                    break;
                case 'I':
                    String s = grabValue(
                            getArgumentError("-I must be followed by a directory name to add to lib path"));
                    for (String path : s.split(File.pathSeparator)) {
                        if (path.startsWith("~" + File.separator)) {
                            path = System.getProperty("user.home") + File.separator + path.substring(2);
                        }
                        config.appendOptionValue(OptionsCatalog.IMPORT_PATHS, path);
                    }
                    break FOR;
                case 'i':
                    String iterations = grabValue(getArgumentError("Value must be an integer"));
                    config.setOption(OptionsCatalog.ITERATIONS, Integer.parseInt(iterations));
                    break FOR;
                case 'l':
                    config.setOption(OptionsCatalog.DIRECTORY_LOOKUP, true);
                    break FOR;
                case 'v':
                    config.setOption(OptionsCatalog.VERBOSITY, Verbosity.TRUE);
                    config.showVersion = true;
                    config.defaultExecutionAction = DefaultExecutionAction.NONE;
                    break;
                case 'w':
                    config.setOption(OptionsCatalog.VERBOSITY, Verbosity.TRUE);
                    setAllWarningCategories(true);
                    break;
                case 'W': {
                    String temp = grabOptionalValue();
                    if (temp == null) {
                        temp = "2";
                    }
                    if (temp.startsWith(":")) {
                        switch (temp) {
                            case ":deprecated":
                                config.setOption(OptionsCatalog.WARN_DEPRECATED, true);
                                break;
                            case ":no-deprecated":
                                config.setOption(OptionsCatalog.WARN_DEPRECATED, false);
                                break;
                            case ":experimental":
                                config.setOption(OptionsCatalog.WARN_EXPERIMENTAL, true);
                                break;
                            case ":no-experimental":
                                config.setOption(OptionsCatalog.WARN_EXPERIMENTAL, false);
                                break;
                            default:
                                LOGGER.warning("unknown warning category: `" + temp.substring(1) + "'");
                                break;
                        }
                    } else {
                        switch (temp) {
                            case "0":
                                config.setOption(OptionsCatalog.VERBOSITY, Verbosity.NIL);
                                setAllWarningCategories(false);
                                break;
                            case "2":
                                config.setOption(OptionsCatalog.VERBOSITY, Verbosity.TRUE);
                                setAllWarningCategories(true);
                                break;
                            case "1":
                            default:
                                config.setOption(OptionsCatalog.VERBOSITY, Verbosity.FALSE);
                                config.setOption(OptionsCatalog.WARN_DEPRECATED, false);
                                break;
                        }
                    }
                    break FOR;
                }
                case '-':
                    if (argument.equals("--copyright")) {
                        config.showCopyright = true;
                        // cancel other execution actions
                        config.executionAction = ExecutionAction.NONE;
                        break FOR;
                    } else if (calOpts && argument.equals("--help")) {
                        break FOR;
                    } else if (argument.equals("--version")) {
                        config.showVersion = true;
                        // cancel other execution actions
                        config.executionAction = ExecutionAction.NONE;
                        break FOR;
                    } else if (argument.startsWith("--dump=")) {
                        warnInternalDebugTool(argument);
                        break FOR;
                    } else {
                        if (argument.equals("--")) {
                            // cal interpreter compatibilty
                            // Usage: cal [switches] [--] [programfile] [arguments])
                            lastInterpreterArgumentIndex = argumentIndex;
                            break;
                        }
                    }
                    config.getUnknownArguments().add(argument);
                    break FOR;
                default:
                    config.getUnknownArguments().add(argument);
                    break FOR;
            }
        }
    }

    private void setAllWarningCategories(boolean value) {
        // place all warning for deprecated ar experimental options here
    }

    private static String[] valueListFor(String argument, String key) throws CommandLineException {
        int length = key.length() + 3; // 3 is from -- and = (e.g. --disable=)
        String[] values = argument.substring(length).split(",");

        if (values.length == 0) {
            errorMissingEquals(key);
        }

        return values;
    }


    private void warnInternalDebugTool(String option) {
        LOGGER.warning("the " + option + " switch is silently ignored as it is an internal development tool");
    }

    private static void errorMissingEquals(String label) throws CommandLineException {
        throw new CommandLineException("missing argument for --" + label + "\n", true);
    }

    /** Split string into (limited) sub-parts.
     *
     * @param str the string
     * @param sep the separator
     * @param lim has same effect as with {@link String#split(String, int)} */
    private static List<String> split(final String str, final char sep, final int lim) {
        final int len = str.length();
        if (len == 0) {
            return Collections.singletonList(str);
        }

        final ArrayList<String> result = new ArrayList<>(lim <= 0 ? 8 : lim);

        int e;
        int s = 0;
        int count = 0;
        while ((e = str.indexOf(sep, s)) != -1) {
            if (lim == ++count) { // limited (lim > 0) case
                result.add(str.substring(s));
                return result;
            }
            result.add(str.substring(s, e));
            s = e + 1;
        }
        if (s < len || (s == len && lim > 0)) {
            result.add(str.substring(s));
        }

        return result;
    }

    private String grabValue(String errorMessage) throws CommandLineException {
        return grabValue(errorMessage, true);
    }

    private String grabValue(String errorMessage, boolean usageError) throws CommandLineException {
        String optValue = grabOptionalValue();
        if (optValue != null) {
            return optValue;
        }
        argumentIndex++;
        if (argumentIndex < arguments.size()) {
            return getCurrentArgument();
        }
        throw new CommandLineException(errorMessage, usageError);
    }

    private String grabOptionalValue() {
        characterIndex++;
        String argValue = getCurrentArgument();
        if (characterIndex < argValue.length()) {
            return argValue.substring(characterIndex);
        }
        return null;
    }

    private String getCurrentArgument() {
        return arguments.get(argumentIndex);
    }

    private CommandLineException notImplemented(String option) {
        return new CommandLineException(String.format("the %s option is not implemented", option));
    }

    private static Logger createLogger() {
        final Logger logger = Logger.getLogger("cal-launcher");

        logger.setUseParentHandlers(false);

        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                System.err.printf("[cal] %s %s%n", record.getLevel().getName(), record.getMessage());
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {
            }

        });

        return logger;
    }

}
