package ch.epfl.vlsc.truffle.cal.shared.options;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;

public class OptionsCatalog {

    public static final OptionKey<String> ENTITY_QID_KEY = new OptionKey<>("");

    public static final OptionKey<Integer> ITERATIONS_KEY = new OptionKey<>(-1);

    public static final OptionKey<Integer> BITLENGTH_KEY = new OptionKey<>(32);

    public static final OptionKey<Boolean> DIRECTORY_LOOKUP_KEY = new OptionKey<>(false);

    public static final OptionKey<String[]> IMPORT_PATHS_KEY = new OptionKey<>(StringArrayOptionType.EMPTY_STRING_ARRAY, StringArrayOptionType.INSTANCE);

    public static final OptionKey<String> LAUNCHER_KEY = new OptionKey<>("");

    public static final OptionKey<Boolean> EMBEDDED_KEY = new OptionKey<>(true);

    public static final OptionKey<String> WORKING_DIRECTORY_KEY = new OptionKey<>("");

    public static final OptionKey<Boolean> DEBUG_KEY = new OptionKey<>(false);

    public static final OptionKey<Verbosity> VERBOSITY_KEY = new OptionKey<>(Verbosity.FALSE, EnumOptionType.optionTypeFor(Verbosity.class));

    public static final OptionKey<Boolean> SYNTAX_CHECK_KEY = new OptionKey<>(false);

    public static final OptionKey<Boolean> WARN_DEPRECATED_KEY = new OptionKey<>(false);

    public static final OptionKey<Boolean> WARN_SHOW_KEY = new OptionKey<>(false);

    public static final OptionKey<Boolean> WARN_EXPERIMENTAL_KEY = new OptionKey<>(true);

    public static final OptionDescriptor ENTITY_QID = OptionDescriptor
            .newBuilder(ENTITY_QID_KEY, "cal.entity-qid")
            .help("Top entity QID to be used for execution")
            .category(OptionCategory.USER)
            .stability(OptionStability.STABLE)
            .build();

    public static final OptionDescriptor ITERATIONS = OptionDescriptor
            .newBuilder(ITERATIONS_KEY, "cal.iterations")
            .help("Limit the number of iterations in action selection")
            .category(OptionCategory.USER)
            .stability(OptionStability.STABLE)
            .build();

    public static final OptionDescriptor BITLENGTH = OptionDescriptor
            .newBuilder(BITLENGTH_KEY, "cal.bitlength")
            .help("Sets the default bit length used for int/uint variables without size parameter specified. Default: 32")
            .category(OptionCategory.EXPERT)
            .stability(OptionStability.EXPERIMENTAL)
            .build();

    public static final OptionDescriptor DIRECTORY_LOOKUP = OptionDescriptor
            .newBuilder(DIRECTORY_LOOKUP_KEY, "cal.directory-lookup")
            .help("Load cal files in the same directory")
            .category(OptionCategory.USER)
            .stability(OptionStability.STABLE)
            .build();

    public static final OptionDescriptor IMPORT_PATHS = OptionDescriptor
            .newBuilder(IMPORT_PATHS_KEY, "cal.import-paths")
            .help("Load cal files in the given directories")
            .category(OptionCategory.USER)
            .stability(OptionStability.EXPERIMENTAL)
            .build();

    public static final OptionDescriptor LAUNCHER = OptionDescriptor
            .newBuilder(LAUNCHER_KEY, "cal.launcher")
            .help("The location of the TruffleCal launcher program")
            .category(OptionCategory.EXPERT)
            .stability(OptionStability.EXPERIMENTAL)
            .build();

    public static final OptionDescriptor EMBEDDED = OptionDescriptor
            .newBuilder(EMBEDDED_KEY, "cal.embedded")
            .help("Set default options for an embedded use of TruffleCal, rather than top-level use")
            .category(OptionCategory.EXPERT)
            .stability(OptionStability.EXPERIMENTAL)
            .build();

    public static final OptionDescriptor WORKING_DIRECTORY = OptionDescriptor
            .newBuilder(WORKING_DIRECTORY_KEY, "cal.working-directory")
            .help("Interpreter will switch to this directory")
            .category(OptionCategory.USER)
            .stability(OptionStability.STABLE)
            .build();

    public static final OptionDescriptor DEBUG = OptionDescriptor
            .newBuilder(DEBUG_KEY, "cal.debug")
            .help("Sets $DEBUG to this value")
            .category(OptionCategory.USER)
            .stability(OptionStability.STABLE)
            .build();

    public static final OptionDescriptor VERBOSITY = OptionDescriptor
            .newBuilder(VERBOSITY_KEY, "cal.verbose")
            .help("Sets $VERBOSE to this value")
            .category(OptionCategory.USER)
            .stability(OptionStability.STABLE)
            .build();

    public static final OptionDescriptor SYNTAX_CHECK = OptionDescriptor
            .newBuilder(SYNTAX_CHECK_KEY, "cal.syntax-check")
            .help("Do not execute just check syntax")
            .category(OptionCategory.INTERNAL)
            .stability(OptionStability.EXPERIMENTAL)
            .build();

    public static final OptionDescriptor WARN_SHOW = OptionDescriptor
            .newBuilder(WARN_SHOW_KEY, "cal.show-warnings")
            .help("Show warnings")
            .category(OptionCategory.EXPERT)
            .stability(OptionStability.EXPERIMENTAL)
            .build();

    public static final OptionDescriptor WARN_DEPRECATED = OptionDescriptor
            .newBuilder(WARN_DEPRECATED_KEY, "cal.warn-deprecated")
            .help("Sets deprecated Warning category")
            .category(OptionCategory.EXPERT)
            .stability(OptionStability.EXPERIMENTAL)
            .build();

    public static final OptionDescriptor WARN_EXPERIMENTAL = OptionDescriptor
            .newBuilder(WARN_EXPERIMENTAL_KEY, "cal.warn-experimental")
            .help("Sets experimental Warning category ")
            .category(OptionCategory.EXPERT)
            .stability(OptionStability.EXPERIMENTAL)
            .build();

    public static OptionDescriptor fromName(String name) {
        switch (name) {
            case "cal.embedded":
                return EMBEDDED;
            case "cal.entity-qid":
                return ENTITY_QID;
            case "cal.iterations":
                return ITERATIONS;
            case "cal.bitlength":
                return BITLENGTH;
            case "cal.directory-lookup":
                return DIRECTORY_LOOKUP;
            case "cal.import-paths":
                return IMPORT_PATHS;
            case "cal.launcher":
                return LAUNCHER;
            case "cal.working-directory":
                return WORKING_DIRECTORY;
            case "cal.debug":
                return DEBUG;
            case "cal.verbose":
                return VERBOSITY;
            case "cal.syntax-check":
                return SYNTAX_CHECK;
            case "cal.warn-deprecated":
                return WARN_DEPRECATED;
            case "cal.warn-experimental":
                return WARN_EXPERIMENTAL;
            default:
                return null;
        }
    }

    public static OptionDescriptor[] allDescriptors() {
        return new OptionDescriptor[]{
                EMBEDDED,
                ENTITY_QID,
                ITERATIONS,
                BITLENGTH,
                DIRECTORY_LOOKUP,
                IMPORT_PATHS,
                LAUNCHER,
                WORKING_DIRECTORY,
                DEBUG,
                VERBOSITY,
                SYNTAX_CHECK,
                WARN_SHOW
        };
    }

}
