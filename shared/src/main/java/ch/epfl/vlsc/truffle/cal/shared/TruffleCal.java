package ch.epfl.vlsc.truffle.cal.shared;

import org.graalvm.nativeimage.ImageInfo;

public class TruffleCal {

    public static final String FORMAL_NAME = "TruffleCal";
    public static final String LANGUAGE_ID = "cal";
    public static final String EXTENSION = ".cal";
    public static final String ENGINE_ID = "trufflecal";
    public static final String LANGUAGE_VERSION = "1.0.0";
    public static final String LANGUAGE_REVISION = BuildInformationImpl.INSTANCE.getFullRevision();
    public static final String BOOT_SOURCE_NAME = "main_boot_source";
    public static final String CAL_COPYRIGHT = "trufflecal - Copyright (c) 2021-" +
            BuildInformationImpl.INSTANCE.getCopyrightYear() + " EPFL VLSC";

    public static String getVersionString(String implementationName) {
        final String buildName = BuildInformationImpl.INSTANCE.getBuildName();
        final String nameExtra;

        if (buildName == null) {
            nameExtra = "";
        } else {
            nameExtra = String.format(" (%s)", BuildInformationImpl.INSTANCE.getBuildName());
        }

        return String.format(
                "%s%s %s, like cal %s, %s %s [%s-%s]",
                ENGINE_ID,
                nameExtra,
                getEngineVersion(),
                LANGUAGE_VERSION,
                implementationName,
                ImageInfo.inImageCode() ? "Native" : "JVM",
                BasicPlatform.getArchName(),
                BasicPlatform.getOSName());
    }

    public static String getEngineVersion() {
        // The property cannot be read in a static initializer, it's set later
        final String systemVersion = System.getProperty("org.graalvm.version");

        // No version information, or just "dev" - use 0.0-commit
        if (systemVersion == null || systemVersion.equals("dev")) {
            return "0.0-" + BuildInformationImpl.INSTANCE.getShortRevision();
        }

        // A "-dev" version number - append the commit as well
        if (systemVersion.endsWith("-dev")) {
            return systemVersion + "-" + BuildInformationImpl.INSTANCE.getShortRevision();
        }

        return systemVersion;
    }


}
