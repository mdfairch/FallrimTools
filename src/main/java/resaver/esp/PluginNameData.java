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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import resaver.ess.Plugin;

/**
 * Stores form name information.
 *
 * @author Mark Fairchild
 */
final public class PluginNameData implements PluginData {

    /**
     * Creates a new <code>ESPInfo</code>.
     * Not threadsafe.
     *
     * @param plugin The name of the ESP.
     */
    public PluginNameData(Plugin plugin) {
        this.MAP = new java.util.HashMap<>();
        this.ESPNAME = plugin;
        this.scriptDataSize = 0;
    }

    /**
     * Adds an entry for a record.
     *
     * @param formID The formId of the record.
     * @param code The record code.
     * @param fields The record's fields, which will be sampled for suitable
     * names.
     */
    @Override
    public void addRecord(int formID, RecordCode code, FieldList fields) {
        final Info INFO = new Info(code, fields);
        this.MAP.put(formID, INFO);
    }

    /**
     * Calculates the script data size and adds it.
     * @param script 
     */
    @Override
    public void addScriptData(Script script) {
        this.scriptDataSize += script.calculateSize();
    }
    
    /**
     * @return The size of the plugin's script data.
     */
    public long getScriptDataSize() {
        return this.scriptDataSize;
    }
    
    /**
     * @return The number of stored names.
     */
    public long getNameCount() {
        return this.MAP.size();
    }
    
    /**
     * Adds all of the entries from on <code>PluginNameData</code> to another.
     *
     * @param other
     */
    public void addAll(PluginNameData other) {
        this.MAP.putAll(other.MAP);
        this.scriptDataSize += other.scriptDataSize;
    }

    /**
     *
     * @param searchTerm
     * @param strings
     * @return
     */
    public Set<Integer> getID(String searchTerm, StringTable strings) {
        Set<Integer> matches = new TreeSet<>(this.MAP.keySet()
                .stream()
                .filter(id -> searchTerm.equalsIgnoreCase(this.getName(id, strings)))
                .collect(Collectors.toSet()));

        return matches;
    }

    /**
     * Tries to retrieve the RecordType for a formID.
     *
     * @param formID
     * @return
     */
    public RecordCode getType(int formID) {
        if (!this.MAP.containsKey(formID)) {
            return null;
        }

        return this.MAP.get(formID).CODE;
    }

    /**
     * Tries to retrieve a suitable name for a formID.
     *
     * @param formID
     * @param strings The StringTable.
     * @return
     */
    public String getName(int formID, StringTable strings) {
        if (!this.MAP.containsKey(formID)) {
            return null;
        }

        final Info INFO = this.MAP.get(formID);

        if (INFO.FULL != null) {
            if (INFO.FULL.hasString()) {
                return INFO.FULL.getString();
            } else if (INFO.FULL.hasIndex() && null != strings) {
                int index = INFO.FULL.getIndex();
                String lookup = strings.get(this.ESPNAME, index);
                if (lookup != null) {
                    return lookup;
                }
            }
        }

        if (INFO.NAME != null) {
            int baseID = INFO.NAME.getFormID();
            assert baseID != formID;
            String baseName = this.getName(baseID, strings);
            if (null != baseName) {
                return baseName;
            }
        }

        if (null != INFO.EDID) {
            return INFO.EDID.getValue();
        }

        return null;
    }

    /**
     * @see Object#toString()
     * @return
     */
    @Override
    public String toString() {
        return this.ESPNAME.toString();
    }

    /**
     * The actual mapping.
     */
    final private Map<Integer, Info> MAP;

    /**
     * The name of the map.
     */
    final private Plugin ESPNAME;

    /**
     * Size of the script data.
     */
    private long scriptDataSize;
    
    /**
     * Stores ID information.
     */
    static final public class Info implements java.io.Serializable {

        public Info(RecordCode code, FieldList fields) {
            
            FieldEDID edid = null;
            FieldName name = null;
            FieldFull full = null;
            
            for (Field field : fields) {
                if (field instanceof FieldFull) {
                    full = (FieldFull) field;
                } else if (field instanceof FieldName) {
                    name = (FieldName) field;
                } else if (field instanceof FieldEDID) {
                    edid = (FieldEDID) field;
                }
            }
            
            this.CODE = code;
            this.EDID = edid;
            this.NAME = name;
            this.FULL = full;
        }

        final public RecordCode CODE;
        final public FieldFull FULL;
        final public FieldName NAME;
        final public FieldEDID EDID;
    }
}
