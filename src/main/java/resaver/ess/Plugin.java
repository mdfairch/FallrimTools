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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import resaver.Analysis;
import resaver.ess.papyrus.ScriptInstance;

/**
 * Abstraction for plugins.
 *
 * @author Mark Fairchild
 */
final public class Plugin implements AnalyzableElement, Linkable, Comparable<Plugin>, java.io.Serializable {

    static public Plugin PROTOTYPE = new Plugin("Unofficial Skyrim Legendary Edition Patch", -1, false);

    /**
     * Creates a new <code>Plugin</code> by reading from an input stream.
     *
     * @param input The input stream.
     * @param index The index of the plugin.
     * @return The new Plugin.
     */
    static public Plugin readFullPlugin(ByteBuffer input, int index) {
        Objects.requireNonNull(input);
        if (index < 0 || index > 255) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }

        String name = mf.BufferUtil.getWString(input);
        return new Plugin(name, index, false);

    }

    /**
     * Creates a new <code>Plugin</code> by reading from an input stream.
     *
     * @param input The input stream.
     * @param index The index of the plugin.
     * @return The new Plugin.
     * @throws IOException
     */
    static public Plugin readLitePlugin(ByteBuffer input, int index) throws IOException {
        if (index < 0 || index >= 4096) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }

        String name = mf.BufferUtil.getWString(input);
        return new Plugin(name, index, true);

    }

    /**
     * Creates a new <code>Plugin</code> that is not part of a savefile.
     *
     * @param name The name for the unloaded plugin.
     * @return The new Plugin.
     */
    static public Plugin makeUnloadedPlugin(String name) {
        Objects.requireNonNull(name);
        return new Plugin(name, -1, false);
    }

    /**
     * Creates a new <code>Plugin</code>.
     *
     * @param name The name of the plugin.
     * @param index The index of the plugin.
     * @param lightweight A flag indicating that it is a lightweight plugin.
     * @throws IOException
     */
    private Plugin(String name, int index, boolean lightweight) {
        Objects.requireNonNull(name);
        this.NAME = name;
        this.INDEX = index;
        this.LIGHTWEIGHT = lightweight;
    }

    /**
     * @see resaver.ess.Element#write(ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        mf.BufferUtil.putWString(output, this.NAME);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 2 + this.NAME.getBytes(StandardCharsets.ISO_8859_1).length;
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        return Linkable.makeLink("plugin", this.NAME, this.toString());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return this.NAME;
    }

    /**
     * @return String representation.
     */
    public String indexName() {
        return this.LIGHTWEIGHT
                ? String.format("FE%03x: %s", this.INDEX, this.NAME)
                : String.format("%02x: %s", this.INDEX, this.NAME);
    }

    /**
     * Finds all of the changeforms associated with this Plugin.
     *
     * @param save The savefile to search.
     * @return A set of changeforms.
     */
    public Set<ChangeForm> getChangeForms(ESS save) {
        final Set<ChangeForm> FORMS = save.getChangeForms().stream()
                .filter(form -> Objects.equals(this, form.getRefID().PLUGIN))
                .collect(Collectors.toSet());

        return FORMS;
    }

    /**
     * Finds all of the scriptinstances associated with this Plugin.
     *
     * @param save The savefile to search.
     * @return A set of scriptinstances.
     */
    public Set<ScriptInstance> getInstances(ESS save) {
        final Set<ScriptInstance> INSTANCES = save.getPapyrus().getScriptInstances().values().stream()
                .filter(instance -> instance.getRefID() != null)
                .filter(instance -> instance.getRefID().PLUGIN == this)
                .collect(Collectors.toSet());

        return INSTANCES;
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(resaver.Analysis analysis, ESS save) {
        final StringBuilder BUILDER = new StringBuilder();

        if (this.LIGHTWEIGHT) {
            BUILDER.append("<html><h3>LITE PLUGIN</h3>");
            BUILDER.append("<p>Name: ").append(this.NAME).append("</p>");
            BUILDER.append("<p>Index: FE:").append(this.INDEX).append("</p>");
        } else {
            BUILDER.append("<html><h3>FULL PLUGIN</h3>");
            BUILDER.append("<p>Name: ").append(this.NAME).append("</p>");
            BUILDER.append("<p>Index: ").append(this.INDEX).append("</p>");
        }

        final Set<ChangeForm> FORMS = this.getChangeForms(save);
        final Set<ScriptInstance> INSTANCES = this.getInstances(save);

        BUILDER.append("<p>").append(FORMS.size()).append(" ChangeForms.</p>");
        if (FORMS.size() < 100) {
            BUILDER.append("<ul>");
            FORMS.forEach(form -> BUILDER.append("<li>").append(form.toHTML(null)));
            BUILDER.append("</ul>");
        }

        BUILDER.append("<p>").append(INSTANCES.size()).append(" ScriptInstances.</p>");
        if (INSTANCES.size() < 100) {
            BUILDER.append("<ul>");
            INSTANCES.forEach(instance -> BUILDER.append("<li>").append(instance.toHTML(null)));
            BUILDER.append("</ul>");
        }

        if (null != analysis) {
            final List<String> PROVIDERS = new ArrayList<>();

            Predicate<String> espFilter = esp -> esp.equalsIgnoreCase(NAME);
            analysis.ESPS.forEach((mod, esps) -> {
                esps.stream().filter(espFilter).forEach(esp -> PROVIDERS.add(mod));
            });

            if (!PROVIDERS.isEmpty()) {
                String probableProvider = PROVIDERS.get(PROVIDERS.size() - 1);

                Predicate<SortedSet<String>> modFilter = e -> e.contains(probableProvider);
                int numScripts = (int) analysis.SCRIPT_ORIGINS.values().stream().filter(modFilter).count();

                BUILDER.append(String.format("<p>%d scripts.</p>", numScripts));
                BUILDER.append(String.format("<p>The plugin probably came from mod \"%s\".</p>", probableProvider));

                if (PROVIDERS.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    PROVIDERS.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }
            }
        }

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Analysis analysis, String mod) {
        Objects.requireNonNull(analysis);
        Objects.requireNonNull(mod);

        Predicate<String> filter = esp -> esp.equalsIgnoreCase(NAME);
        final List<String> PROVIDERS = new ArrayList<>();

        analysis.ESPS.forEach((m, esps) -> {
            esps.stream().filter(filter).forEach(esp -> PROVIDERS.add(m));
        });

        return PROVIDERS.contains(mod);
    }

    /**
     * The name field.
     */
    final public String NAME;

    /**
     * The index field.
     */
    final public int INDEX;

    /**
     *
     */
    final public boolean LIGHTWEIGHT;

    /**
     * @see Comparable#compareTo(java.lang.Object)
     * @param o
     * @return
     */
    @Override
    public int compareTo(Plugin o) {
        if (null == o) {
            return 1;
        }

        return Objects.compare(this.NAME, o.NAME, String::compareToIgnoreCase);
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.NAME.toLowerCase());
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
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        } else {
            final Plugin other = (Plugin) obj;
            return this.NAME.equalsIgnoreCase(other.NAME);
        }
    }

}
