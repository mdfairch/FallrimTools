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
package resaver.ess;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes a savegame's list of plugins.
 *
 * @author Mark Fairchild
 */
final public class PluginInfo implements Element {

    /**
     * Make an absolute formID for a plugin. Any existing plugin info in the
     * formID will be discarded.
     *
     * @param plugin
     * @param id
     * @return
     */
    static public int makeFormID(Plugin plugin, int id) {
        if (plugin.LIGHTWEIGHT) {
            return 0xFE000000 | (plugin.INDEX << 12) | (id & 0xfff);
        } else {
            return (plugin.INDEX << 24) | (id & 0xffffff);
        }
    }

    /**
     * Splits a formID into a plugin and a local formID.
     *
     * @param formId
     * @return
     */
    public mf.Pair<Plugin, Integer> splitFormID(int formId) {
        if (formId>>24 == 0xFE) {
            int pluginIndex = (formId >> 12) & 0xFFF;
            int formIndex = formId & 0xFFF;
            
            if (pluginIndex < this.PLUGINS_LITE.size()) {
                return mf.Pair.of(this.PLUGINS_LITE.get(pluginIndex), formIndex);
            } else {
                return null;
            }
        } else {
             int pluginIndex = (formId >> 24) & 0xFF;
             int formIndex = formId & 0xFFFFFF;
             
            if (pluginIndex < this.PLUGINS_FULL.size()) {
                return mf.Pair.of(this.PLUGINS_FULL.get(pluginIndex), formIndex);
            } else {
                return null;
            }
        }        
    }

    /**
     * Creates a new <code>PluginInfo</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param supportsESL Whether to load a lightweight plugins table.
     * @throws IOException
     */
    public PluginInfo(ByteBuffer input, boolean supportsESL) throws IOException {
        Objects.requireNonNull(input);

        int pluginInfoSize = input.getInt();
        int numberOfFull = Byte.toUnsignedInt(input.get());
        if (numberOfFull < 0 || numberOfFull >= 256) {
            throw new IllegalArgumentException("Invalid full plugin count: " + numberOfFull);
        }

        this.PLUGINS_FULL = new java.util.ArrayList<>(numberOfFull);

        for (int i = 0; i < numberOfFull; i++) {
            Plugin p = Plugin.readFullPlugin(input, i);
            this.PLUGINS_FULL.add(p);
        }

        if (supportsESL) {
            int numberOfLite = input.getShort();
            if (numberOfLite < 0 || numberOfLite >= 4096) {
                throw new IllegalArgumentException("Invalid lite plugin count: " + numberOfLite);
            }

            this.PLUGINS_LITE = new ArrayList<>(numberOfLite);

            for (int i = 0; i < numberOfLite; i++) {
                Plugin p = Plugin.readLitePlugin(input, i);
                this.PLUGINS_LITE.add(p);
            }
            
        } else {
            this.PLUGINS_LITE = null;
        }

        this.PATHS = this.stream().collect(Collectors.toMap(p -> Paths.get(p.NAME), p -> p, (a,b) -> a));
        if (pluginInfoSize + 4 != this.calculateSize()) {
            throw new IllegalStateException(String.format("PluginInfoSize = %d, but read %d", pluginInfoSize, this.calculateSize()));
        }
    }

    /**
     * @see resaver.ess.Element#write(ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.calculateSize() - 4);
        output.put((byte) this.PLUGINS_FULL.size());

        this.PLUGINS_FULL.forEach(p -> p.write(output));

        if (this.hasLite()) {
            output.putShort((short) this.PLUGINS_LITE.size());
            this.PLUGINS_LITE.forEach(p -> p.write(output));
        }
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 4;
        sum += 1;
        sum += this.PLUGINS_FULL.stream().mapToInt(plugin -> plugin.calculateSize()).sum();

        if (this.hasLite()) {
            sum += 2;
            sum += this.PLUGINS_LITE.stream().mapToInt(plugin -> plugin.calculateSize()).sum();
        }
        return sum;
    }

    /**
     * @return The list of all plugins.
     */
    public List<Plugin> getAllPlugins() {
        return this.stream().collect(Collectors.toList());
    }

    /**
     * @return The list of all plugins.
     */
    public Stream<Plugin> stream() {
        return Stream.concat(this.getFullPlugins().stream(), this.getLitePlugins().stream());
    }

    /**
     * @return A flag indicating whether there is a lightweight plugin table.
     */
    public boolean hasLite() {
        return null != this.PLUGINS_LITE;
    }

    /**
     * Searches for a <code>Plugin</code> by name.
     * @param name The name to search for.
     * @return The <code>Plugin</code> with the matching name or <code>null</code>.
     */
    public Plugin find(String name) {
        return stream()
                .filter(p -> p.NAME.equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }
    
    /**
     * @return The list of plugins.
     */
    public List<Plugin> getFullPlugins() {
        return null == this.PLUGINS_FULL
                ? Collections.emptyList()
                : Collections.unmodifiableList(this.PLUGINS_FULL);
    }

    /**
     * @return The list of lightweight plugins.
     */
    public List<Plugin> getLitePlugins() {
        return this.hasLite()
                ? Collections.unmodifiableList(this.PLUGINS_LITE)
                : Collections.emptyList();
    }

    /**
     * @return A <code>Map</code> for matching a <code>Path</code> to a
     * corresponding <code>Plugin</code>.
     */
    public Map<Path, Plugin> getPaths() {
        return this.PATHS;
    }

    /**
     * @return The total number of lite and full plugins.
     */
    public int getSize() {
        return this.getFullPlugins().size() + this.getLitePlugins().size();
    }

    /**
     * Creates a string representation of the <code>PluginInfo</code>.
     *
     * @see Object#toString()
     * @return
     */
    @Override
    public String toString() {
        if (null == this.PLUGINS_FULL) {
            return "NOT INITIALIZED";
        }

        final StringBuilder BUF = new StringBuilder();
        BUF.append(String.format("%d plugins", this.getFullPlugins().size()));

        if (this.hasLite()) {
            BUF.append(String.format(", %d lightweight plugins", this.getLitePlugins().size()));
        }

        return BUF.toString();
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.PLUGINS_FULL);
        hash = 89 * hash + Objects.hashCode(this.PLUGINS_LITE);
        return hash;
    }

    /**
     * @see Object#equals(java.lang.Object)
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PluginInfo other = (PluginInfo) obj;
        return Objects.deepEquals(this.PLUGINS_FULL, other.PLUGINS_FULL)
                && Objects.deepEquals(this.PLUGINS_LITE, other.PLUGINS_LITE);
    }

    final private List<Plugin> PLUGINS_FULL;
    final private List<Plugin> PLUGINS_LITE;
    final private Map<Path, Plugin> PATHS;

}
