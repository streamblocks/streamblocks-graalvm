package ch.epfl.vlsc.truffle.cal.parser.utils;

import com.oracle.truffle.api.RootCallTarget;

import java.util.Map;

public class NamespaceElementsToCallTarget {
    public Map<String, RootCallTarget> entities;
    public Map<String, RootCallTarget> functions;
}
