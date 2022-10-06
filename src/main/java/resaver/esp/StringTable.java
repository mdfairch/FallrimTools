/*
 * Copyright 2016 Mark Fairchild.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package resaver.esp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import resaver.ess.Plugin;

/**
 * A StringTable stores reads and stores strings from the mod stringtables;
 * mostly just applies to Skyrim.esm and the DLCs.
 *
 * @author Mark Fairchild
 */
public class StringTable implements java.io.Serializable {

    public StringTable() {
        this.TABLE = new java.util.HashMap<>();
    }

    /**
     *
     * @param stringsFiles
     * @param plugin
     */
    public void populateFromFiles(Collection<StringsFile> stringsFiles, Plugin plugin) {
        Objects.requireNonNull(stringsFiles);

        final Map<Integer, String> SUBTABLE = this.TABLE.computeIfAbsent(plugin, p -> new HashMap<>());
        stringsFiles.stream().forEach(stringsFile -> SUBTABLE.putAll(stringsFile.TABLE));
    }

    /**
     * Retrieves a string using its formid.
     *
     * @param plugin
     * @param stringID
     * @return
     */
    public String get(Plugin plugin, int stringID) {
        return this.TABLE.getOrDefault(plugin, Collections.emptyMap()).get(stringID);
    }

    /**
     * The reference for accessing the stringtable.
     */
    final public Map<Plugin, Map<Integer, String>> TABLE;

    static private enum Type {
        STRINGS(Pattern.compile(".+\\.STRINGS$", Pattern.CASE_INSENSITIVE)),
        ILSTRINGS(Pattern.compile(".+\\.ILSTRINGS$", Pattern.CASE_INSENSITIVE)),
        DLSTRINGS(Pattern.compile(".+\\.DLSTRINGS$", Pattern.CASE_INSENSITIVE));

        static Type match(String filename) {
            if (STRINGS.REGEX.asPredicate().test(filename)) {
                return STRINGS;
            }
            if (ILSTRINGS.REGEX.asPredicate().test(filename)) {
                return ILSTRINGS;
            }
            if (DLSTRINGS.REGEX.asPredicate().test(filename)) {
                return DLSTRINGS;
            }
            return null;
        }

        private Type(java.util.regex.Pattern regex) {
            this.REGEX = Objects.requireNonNull(regex);
        }

        final public java.util.regex.Pattern REGEX;
    };

}
