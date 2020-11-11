package ch.epfl.vlsc.truffle.cal.runtime;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;

import java.util.*;

public class CALFunctionRegistry {
    private final CALLanguage language;
    private final FunctionsObject functionsObject = new FunctionsObject();

    public CALFunctionRegistry(CALLanguage language) {
        this.language = language;
    }

    /**
     * Returns the canonical {@link CALFunction} object for the given name. If it does not exist yet,
     * it is created.
     */
    public CALFunction lookup(String name, boolean createIfNotPresent) {
        CALFunction result = functionsObject.functions.get(name);
        if (result == null && createIfNotPresent) {
            result = new CALFunction(language, name);
            functionsObject.functions.put(name, result);
        }
        return result;
    }

    /**
     * Associates the {@link CALFunction} with the given name with the given implementation root
     * node. If the function did not exist before, it defines the function. If the function existed
     * before, it redefines the function and the old implementation is discarded.
     */
    public CALFunction register(String name, RootCallTarget callTarget) {
        CALFunction function = lookup(name, true);
        function.setCallTarget(callTarget);
        return function;
    }

    public void register(Map<String, RootCallTarget> newFunctions) {
        for (Map.Entry<String, RootCallTarget> entry : newFunctions.entrySet()) {
            register(entry.getKey(), entry.getValue());
        }
    }

    public void register(Source newFunctions) {
        //register(SimpleLanguageParser.parseSL(language, newFunctions));
    }

    public CALFunction getFunction(String name) {
        return functionsObject.functions.get(name);
    }

    /**
     * Returns the sorted list of all functions, for printing purposes only.
     */
    public List<CALFunction> getFunctions() {
        List<CALFunction> result = new ArrayList<>(functionsObject.functions.values());
        Collections.sort(result, new Comparator<CALFunction>() {
            public int compare(CALFunction f1, CALFunction f2) {
                return f1.toString().compareTo(f2.toString());
            }
        });
        return result;
    }

    public TruffleObject getFunctionsObject() {
        return functionsObject;
    }
}
