/*
 * Copyright 2017 Mark Fairchild.
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
package resaver;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import resaver.esp.PluginData;
import resaver.esp.RecordCode;
import resaver.esp.StringTable;
import resaver.ess.Plugin;
import resaver.ess.PluginInfo;

/**
 * Combines the results of script analysis and ESP analysis.
 *
 * @author Mark Fairchild
 */
final public class Analysis extends Mod.Analysis { //implements java.io.Serializable {

    /**
     * Creates a new <code>Analysis</code>.
     *
     * @param plugins The <code>Plugininfo</code> structure of the savefile.
     * @param profileAnalysis 
     * @param espInfos
     * @param strings
     * @param hasModInfo 
     */
    public Analysis(PluginInfo plugins, Mod.Analysis profileAnalysis, Map<Plugin, PluginData> espInfos, StringTable strings, boolean hasModInfo) {
        this.PLUGINS = plugins;
        this.ESP_INFOS = Objects.requireNonNull(espInfos);
        this.STRINGS = Objects.requireNonNull(strings);
        this.MODS.addAll(profileAnalysis.MODS);
        this.SCRIPTS.putAll(profileAnalysis.SCRIPTS);
        this.ESPS.putAll(profileAnalysis.ESPS);
        this.SCRIPT_ORIGINS.putAll(profileAnalysis.SCRIPT_ORIGINS);
        this.HAS_MOD_INFO = hasModInfo;
    }

    public String getName(Plugin plugin, int formID) {
        return this.ESP_INFOS.containsKey(plugin)
                ? this.ESP_INFOS.get(plugin).getName(formID, this.STRINGS)
                : null;
    }

    public RecordCode getType(Plugin plugin, int formID) {
        return this.ESP_INFOS.containsKey(plugin)
                ? this.ESP_INFOS.get(plugin).getType(formID)
                : null;
    }

    public Set<Integer> find(String searchTerm) {
        return this.ESP_INFOS.values().stream()
                .map(v -> v.getID(searchTerm, this.STRINGS))
                .filter(Objects::nonNull)
                .flatMap(r -> r.stream())
                .collect(Collectors.toSet());
    }

    public long getScriptDataSize() {
        return this.ESP_INFOS.values().stream().mapToLong(i -> i.getScriptDataSize()).sum();
    }

    final public PluginInfo PLUGINS;
    final public Map<Plugin, PluginData> ESP_INFOS;
    final public StringTable STRINGS;
    final public boolean HAS_MOD_INFO;
    
}
