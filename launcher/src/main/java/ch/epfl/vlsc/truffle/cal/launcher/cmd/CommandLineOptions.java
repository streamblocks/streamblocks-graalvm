/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007-2011 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package ch.epfl.vlsc.truffle.cal.launcher.cmd;


import ch.epfl.vlsc.truffle.cal.shared.options.CalOptionTypes;
import ch.epfl.vlsc.truffle.cal.shared.options.StringArrayOptionType;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandLineOptions {
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    // Options which are only meaningful when using the TruffleCal launcher (not when using the Context API)
    public boolean showVersion = false;
    public boolean showCopyright = false;
    public ShowHelp showHelp = ShowHelp.NONE;
    /**
     * What should be done after context is created
     */
    public ExecutionAction executionAction = ExecutionAction.NONE;
    /**
     * What should be done when no action is set
     */
    public DefaultExecutionAction defaultExecutionAction = DefaultExecutionAction.NONE;
    /**
     * A thing to be executed: a file, inline script, etc. Used by executionAction when applicable.
     */
    public String toExecute = "";

    private final Map<String, String> options;
    private String[] arguments;
    private final List<String> unknownArguments;
    private Boolean gemOrBundle = null;

    public CommandLineOptions() {
        this.options = new HashMap<>();
        this.arguments = EMPTY_STRING_ARRAY;
        this.unknownArguments = new ArrayList<>();
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public <T> void setOption(OptionDescriptor descriptor, T value) {
        setOptionRaw(descriptor, CalOptionTypes.valueToString(value));
    }

    public <T> T getOption(OptionDescriptor descriptor) {
        return CalOptionTypes.parseValue(descriptor, getOptionRaw(descriptor));
    }

    public <T> void appendOptionValue(OptionDescriptor descriptor, String newValue) {
        final OptionType<?> type = descriptor.getKey().getType();

        final String appended;

        if (type == OptionType.defaultType(String.class)) {
            appended = getOptionRaw(descriptor) + newValue;
        } else if (type == StringArrayOptionType.INSTANCE) {
            appended = StringArrayOptionType.append(getOptionRaw(descriptor), newValue);
        } else {
            throw new UnsupportedOperationException();
        }

        setOptionRaw(descriptor, appended);
    }

    private void setOptionRaw(OptionDescriptor descriptor, String value) {
        options.put(descriptor.getName(), value);
    }

    private <T> String getOptionRaw(OptionDescriptor descriptor) {
        return options.getOrDefault(
                descriptor.getName(),
                CalOptionTypes.valueToString(descriptor.getKey().getDefaultValue()));
    }

    public String[] getArguments() {
        return arguments;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    public List<String> getUnknownArguments() {
        return unknownArguments;
    }

    private boolean contains(String[] array, String element) {
        for (String e : array) {
            if (e.equals(element)) {
                return true;
            }
        }
        return false;
    }
}

