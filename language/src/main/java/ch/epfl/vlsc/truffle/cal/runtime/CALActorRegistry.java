package ch.epfl.vlsc.truffle.cal.runtime;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;

import java.util.*;

public class CALActorRegistry {
    private final CALLanguage language;
    private final ActorsObject actorsObject = new ActorsObject();

    public CALActorRegistry(CALLanguage language) {
        this.language = language;
    }

    /**
     * Returns the canonical {@link CALActor} object for the given name. If it does not exist yet,
     * it is created.
     */
    public CALActor lookup(String name, boolean createIfNotPresent) {
        CALActor result = actorsObject.actors.get(name);
        if (result == null && createIfNotPresent) {
            result = new CALActor(language, name);
            actorsObject.actors.put(name, result);
        }
        return result;
    }

    /**
     * Associates the {@link CALActor} with the given name with the given implementation root
     * node. If the function did not exist before, it defines the function. If the function existed
     * before, it redefines the function and the old implementation is discarded.
     */
    public CALActor register(String name, RootCallTarget callTarget) {
        CALActor function = lookup(name, true);
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

    public CALActor getFunction(String name) {
        return actorsObject.actors.get(name);
    }

    /**
     * Returns the sorted list of all functions, for printing purposes only.
     */
    public List<CALActor> getFunctions() {
        List<CALActor> result = new ArrayList<>(actorsObject.actors.values());
        Collections.sort(result, new Comparator<CALActor>() {
            public int compare(CALActor f1, CALActor f2) {
                return f1.toString().compareTo(f2.toString());
            }
        });
        return result;
    }

    public TruffleObject getFunctionsObject() {
        return actorsObject;
    }
}
