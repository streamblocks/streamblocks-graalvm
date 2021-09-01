package ch.epfl.vlsc.truffle.cal.shared;

import ch.epfl.vlsc.truffle.cal.annotations.PopulateBuildInformation;

@PopulateBuildInformation
public interface BuildInformation {
    String getBuildName();

    String getShortRevision();

    String getFullRevision();

    String getCopyrightYear();

    String getCompileDate();

    String getKernelMajorVersion();
}
