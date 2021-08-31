package ch.epfl.vlsc.truffle.cal.launcher;

import ch.epfl.vlsc.truffle.cal.launcher.cmd.CommandLineException;
import ch.epfl.vlsc.truffle.cal.launcher.cmd.CommandLineOptions;
import ch.epfl.vlsc.truffle.cal.launcher.cmd.CommandLineParser;
import ch.epfl.vlsc.truffle.cal.launcher.cmd.ExecutionAction;
import ch.epfl.vlsc.truffle.cal.shared.Metrics;
import ch.epfl.vlsc.truffle.cal.shared.TruffleCal;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.nativeimage.ProcessProperties;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class CalLauncher extends AbstractLanguageLauncher {

    private CommandLineOptions config;
    private String implementationName = null;


    public static void main(String[] args) {
        new CalLauncher().launch(args);
    }


    @Override
    protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
        // Set default options for the launcher which don't match the OptionKey's default.
        // These options can still be overridden if set explicitly.
        polyglotOptions.put(OptionsCatalog.EMBEDDED.getName(), "false");
        if (isAOT()) {
            final String launcher = ProcessProperties.getExecutableName();
            polyglotOptions.put(OptionsCatalog.LAUNCHER.getName(), launcher);
        }

        // Instrumentation should not swallow exceptions, especially exceptions from Truffle safepoints (GR-32154)
        polyglotOptions.put("engine.InstrumentExceptionsAreThrown", "true");
        // TruffleCal is never distributed without the GraalVM compiler, so this warning is not necessary
        polyglotOptions.put("engine.WarnInterpreterOnly", "false");

        config = new CommandLineOptions();

        try {
            config.executionAction = ExecutionAction.UNSET;

            final CommandLineParser argumentCommandLineParser = new CommandLineParser(arguments, config, true, false);
            argumentCommandLineParser.processArguments();


        } catch (CommandLineException commandLineException) {
            System.err.println("cal: " + commandLineException.getMessage());
            if (commandLineException.isUsageError()) {
                printHelp(System.err);
            }
            System.exit(1);
        }

        return config.getUnknownArguments();
    }

    @Override
    protected AbortException abortUnrecognizedArgument(String argument) {
        throw abortInvalidArgument(
                argument,
                "cal: invalid option " + argument + "  (Use --help for usage instructions.)");
    }

    @Override
    protected void launch(Context.Builder contextBuilder) {
        Metrics.begin();
        printPreRunInformation(config);
        final int exitValue = runCalMain(contextBuilder, config);
        Metrics.end();
        System.exit(exitValue);
    }

    private int runCalMain(Context.Builder contextBuilder, CommandLineOptions config) {

        if (config.executionAction == ExecutionAction.NONE) {
            return 0;
        }

        try (Context context = createContext(contextBuilder, config)) {
            System.out.println("== running on " + context.getEngine());
            Metrics.printTime("before-run");

            File input = new File(config.toExecute);
            if (input.exists()) {
                final Source source = Source.newBuilder(
                        TruffleCal.LANGUAGE_ID,
                        new File(config.toExecute)).build();

                final String kind = config.executionAction.name();
                Value value = context.eval(source);
                Metrics.printTime("after-run");
                return 0;
            } else {
                System.err.println("cal: no input files");
                return 1;
            }
        } catch (PolyglotException e) {
            if (e.isHostException()) {
                System.err.println("cal: a host exception reached the top level:");
            } else {
                System.err.println(
                        "cal: an exception escaped out of the interpreter - this is an implementation bug");
            }
            e.printStackTrace();
            return 1;
        } catch (IOException e) {
            System.err.println(
                    "cal: error with the given input file : " + e);
            return 1;
        }

    }

    private Context createContext(Context.Builder builder, CommandLineOptions config) {
        builder.options(config.getOptions());

        builder.arguments(TruffleCal.LANGUAGE_ID, config.getArguments());

        return builder.build();
    }

    @Override
    protected String getLanguageId() {
        return TruffleCal.LANGUAGE_ID;
    }

    @Override
    protected String getMainClass() {
        return CalLauncher.class.getName();
    }

    @Override
    protected void printVersion() {
        System.out.println(TruffleCal.getVersionString(getImplementationNameFromEngine()));
        System.out.println();
        printPolyglotVersions();
    }

    private String getImplementationNameFromEngine() {
        if (implementationName == null) {
            try (Engine engine = Engine.create()) {
                implementationName = engine.getImplementationName();
            }
        }

        return implementationName;
    }

    private void printPreRunInformation(CommandLineOptions config) {
        if (config.showVersion) {
            System.out.println(TruffleCal.getVersionString(getImplementationNameFromEngine()));
        }

        if (config.showCopyright) {
            System.out.println(TruffleCal.CAL_COPYRIGHT);
        }

        switch (config.showHelp) {
            case NONE:
                break;
            case SHORT:
                printShortHelp(System.out);
                break;
            case LONG:
                printHelp(System.out);
                break;
        }
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
        printHelp(System.out);
    }

    private static void printHelp(PrintStream out) {
        out.println("Usage: cal [switches] [--] [programfile] [arguments]");
        out.println("  -e              top entity qualified id to be executed");
        out.println("  -i              number of iterations");
        out.println("  -l              directory look up");
        out.println("  --verbose       turn on verbose mode and disable script from stdin");
        out.println("  --version       print the version number, then exit");
        out.println("  --help          show this message, -h for short message");
    }

    private static void printShortHelp(PrintStream out) {
        out.println("Usage: cal [switches] [--] [programfile] [arguments]");
        out.println("  -h              show this message, --help for more info");
    }


}
