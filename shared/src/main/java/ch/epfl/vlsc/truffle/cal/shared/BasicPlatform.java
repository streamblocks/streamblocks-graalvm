package ch.epfl.vlsc.truffle.cal.shared;

import java.util.Locale;

public class BasicPlatform {
    public enum OS_TYPE {
        LINUX("linux"),
        DARWIN("darwin"),
        SOLARIS("solaris"),
        WINDOWS("mswin32");

        private final String calName;

        OS_TYPE(String calName) {
            this.calName = calName;
        }
    }

    public enum ARCH {
        AMD64("x86_64"),
        ARM64("arm64"),
        AARCH64("aarch64"),
        UNKNOWN("unknown");

        private final String calName;

        ARCH(String name) {
            this.calName = name;
        }
    }

    public static final OS_TYPE OS = determineOS();
    public static final ARCH ARCHITECTURE = determineArchitecture();

    public static String getOSName() {
        return OS.calName;
    }

    public static String getArchName() {
        return ARCHITECTURE.calName;
    }

    public static OS_TYPE determineOS() {
        final String osName = System.getProperty("os.name");

        final String lowerOSName = osName.toLowerCase(Locale.ENGLISH);
        if (lowerOSName.contains("windows")) {
            return OS_TYPE.WINDOWS;
        }

        if (lowerOSName.startsWith("mac") || lowerOSName.startsWith("darwin")) {
            return OS_TYPE.DARWIN;
        } else if (lowerOSName.startsWith("sunos") || lowerOSName.startsWith("solaris")) {
            return OS_TYPE.SOLARIS;
        }

        final String upperOSName = osName.toUpperCase(Locale.ENGLISH);
        for (OS_TYPE os : OS_TYPE.values()) {
            if (upperOSName.startsWith(os.name())) {
                return os;
            }
        }

        throw new UnsupportedOperationException("Unknown platform: " + osName);
    }

    private static ARCH determineArchitecture() {
        String architecture = System.getProperty("os.arch");

        switch (architecture) {
            case "amd64":
            case "x86_64":
                return ARCH.AMD64;
            case "arm64":
                return ARCH.ARM64;
            case "aarch64":
                return ARCH.AARCH64;
            default:
                return ARCH.UNKNOWN;
        }
    }

    public static String getKernelMajorVersion() {
        if (OS == OS_TYPE.DARWIN) {
            // FIXME: return BuildInformationImpl.INSTANCE.getKernelMajorVersion();
            return "";
        } else {
            return "";
        }
    }
}
