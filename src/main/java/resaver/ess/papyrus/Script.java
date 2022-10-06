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
package resaver.ess.papyrus;

import resaver.ListException;
import resaver.ess.AnalyzableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.stream.Collectors;
import resaver.IString;
import java.nio.ByteBuffer;
import resaver.Analysis;
import resaver.ess.Element;
import resaver.ess.ESS;
import resaver.ess.Linkable;

/**
 * Describes a script in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class Script extends Definition {

    /**
     * Creates a new <code>Script</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     *
     */
    public Script(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException, PapyrusElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        this.NAME = context.readTString(input);
        this.TYPE = context.readTString(input);

        try {
            int count = input.getInt();
            this.MEMBERS = MemberDesc.readList(input, count, context);
        } catch (ListException ex) {
            throw new PapyrusElementException("Failed to read Script members.", ex, this);
        }
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);

        this.NAME.write(output);
        this.TYPE.write(output);
        output.putInt(this.MEMBERS.size());
        this.MEMBERS.forEach(member -> member.write(output));
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 4;
        sum += this.NAME.calculateSize();
        sum += this.TYPE.calculateSize();
        sum += this.MEMBERS.stream().mapToInt(member -> member.calculateSize()).sum();
        return sum;
    }

    /**
     * @return The name of the papyrus element.
     */
    @Override
    public TString getName() {
        return this.NAME;
    }

    /**
     * @return The type of the array.
     */
    public TString getType() {
        return this.TYPE;
    }

    /**
     * @return The list of <code>MemberDesc</code>.
     */
    @Override
    public List<MemberDesc> getMembers() {
        return java.util.Collections.unmodifiableList(this.MEMBERS);
    }

    /**
     * @return The list of <code>MemberDesc</code> prepended by the
     * <code>MemberDesc</code> objects of all superscripts.
     */
    public List<MemberDesc> getExtendedMembers() {
        if (null != this.parent) {
            final List<MemberDesc> EXTENDED = this.parent.getExtendedMembers();
            EXTENDED.addAll(this.MEMBERS);
            return EXTENDED;
        } else {
            final List<MemberDesc> EXTENDED = new ArrayList<>(this.MEMBERS);
            return EXTENDED;
        }
    }

    /**
     * @param scripts The ScriptMap.
     */
    public void resolveParent(ScriptMap scripts) {
        this.parent = scripts.get(this.TYPE);
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        if (null != target && target instanceof MemberDesc) {
            int i = this.getExtendedMembers().indexOf(target);
            if (i >= 0) {
                return Linkable.makeLink("script", this.NAME, i, this.NAME.toString());
            }
        }
        return Linkable.makeLink("script", this.NAME, this.NAME.toString());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        if (this.isUndefined()) {
            return "#" + this.NAME + " (" + this.getInstanceCount() + ")";
        }

        return this.NAME + " (" + this.getInstanceCount() + ")";
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
        BUILDER.append("<html>");

        if (this.TYPE.isEmpty()) {
            BUILDER.append(String.format("<h3>SCRIPT %s</h3>", this.NAME));
        } else if (null != this.parent) {
            BUILDER.append(String.format("<h3>SCRIPT %s extends %s</h3>", this.NAME, this.parent.toHTML(this)));
        } else {
            BUILDER.append(String.format("<h3>SCRIPT %s extends %s</h3>", this.NAME, this.TYPE));
        }

        if (this.isUndefined()) {
            BUILDER.append("<p>WARNING: SCRIPT MISSING!<br />Selecting \"Remove Undefined Instances\" will delete this.</p>");
        }

        if (null != analysis) {
            SortedSet<String> mods = analysis.SCRIPT_ORIGINS.get(this.NAME.toIString());

            if (null != mods) {
                if (mods.size() > 1) {
                    BUILDER.append("<p>WARNING: MORE THAN ONE MOD PROVIDES THIS SCRIPT!<br />Exercise caution when editing or deleting this script!</p>");
                }

                String probablyProvider = mods.last();
                BUILDER.append(String.format("<p>This script probably came from \"%s\".</p>", probablyProvider));
                BUILDER.append("<p>Full list of providers:</p>");
                BUILDER.append("<ul>");
                mods.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                BUILDER.append("</ul>");
            }
        }

        int inheritCount = 0;
        for (Script p = this.parent; p != null; p = p.parent) {
            inheritCount += p.MEMBERS.size();
        }
        BUILDER.append(String.format("<p>Contains %d member variables, %d were inherited.</p>", this.MEMBERS.size() + inheritCount, inheritCount));

        final List<ScriptInstance> INSTANCES = save.getPapyrus()
                .getScriptInstances()
                .values()
                .stream()
                .filter(instance -> instance.getScript() == this)
                .collect(Collectors.toList());

        final List<Reference> REFERENCES = save.getPapyrus()
                .getReferences()
                .values()
                .stream()
                .filter(ref -> ref.getScript() == this)
                .collect(Collectors.toList());

        BUILDER.append(String.format("<p>There are %d instances of this script.</p>", INSTANCES.size()));
        if (INSTANCES.size() < 20) {
            BUILDER.append("<ul>");
            INSTANCES.forEach(i -> {
                String s = String.format("<li>%s</a>", i.toHTML(null));
                BUILDER.append(s);
            });
            BUILDER.append("</ul>");
        }

        BUILDER.append(String.format("<p>There are %d references of this script.</p>", REFERENCES.size()));
        if (REFERENCES.size() < 20) {
            BUILDER.append("<ul>");
            REFERENCES.forEach(i -> {
                String s = String.format("<li>%s</a>", i.toHTML(null));
                BUILDER.append(s);
            });
            BUILDER.append("</ul>");
        }

        /*if (null != analysis && analysis.SCRIPTS.containsKey(this.NAME.toIString())) {
            final Path PEXFILE = analysis.SCRIPTS.get(this.NAME.toIString());
            BUILDER.append("");
            BUILDER.append(String.format("<hr /><p>Disassembled source code:<br />(from %s)</p>", PEXFILE));

            if (Files.exists(PEXFILE) && Files.isReadable(PEXFILE)) {
                try {
                    final resaver.pex.PexFile SCRIPT = resaver.pex.PexFile.readScript(PEXFILE);
                    final List<String> CODE = new LinkedList<>();
                    try {
                        SCRIPT.disassemble(CODE, AssemblyLevel.STRIPPED);
                    } catch (Exception ex) {
                        BUILDER.append("Error disassembling script: ").append(ex.getMessage());
                    }

                    BUILDER.append("<p<code><pre>");
                    CODE.forEach(s -> BUILDER.append(s).append('\n'));
                    BUILDER.append("</pre></code></p>");

                } catch (RuntimeException ex) {
                    BUILDER.append("<p><em>Error: disassembly failed.</em></p>");
                } catch (java.io.IOException ex) {
                    BUILDER.append("<p><em>Error: couldn't read the script file.</em></p>");
                } catch (Error ex) {
                    BUILDER.append("<p><em>Error: unexpected error while reading script file.</em></p>");
                }

            } else {
                Path origin = PEXFILE.getParent();
                while (origin.getNameCount() > 0 && !Files.isReadable(origin)) {
                    origin = origin.getParent();
                }

                if (Configurator.validFile(origin)) {
                    try (final LittleEndianRAF INPUT = LittleEndianRAF.open(origin);
                            final ArchiveParser PARSER = ArchiveParser.createParser(origin, INPUT)) {
                        final PexFile SCRIPT = PARSER.getScript(PEXFILE);

                        final List<String> CODE = new LinkedList<>();
                        try {
                            SCRIPT.disassemble(CODE, AssemblyLevel.STRIPPED);
                        } catch (Exception ex) {
                            BUILDER.append("Error disassembling script: ").append(ex.getMessage());
                        }

                        BUILDER.append("<p<code><pre>");
                        CODE.forEach(s -> BUILDER.append(s).append('\n'));
                        BUILDER.append("</pre></code></p>");

                    } catch (RuntimeException ex) {
                        BUILDER.append("<p><em>Error: disassembly failed.</em></p>");
                    } catch (java.io.IOException ex) {
                        BUILDER.append("<p><em>Error: couldn't read the script file.</em></p>");
                    } catch (Error ex) {
                        BUILDER.append("<p><em>Error: unexpected error while reading script file.</em></p>");
                    }
                }
            }
        }*/
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

        final SortedSet<String> OWNERS = analysis.SCRIPT_ORIGINS.get(this.NAME.toIString());
        return null != OWNERS && OWNERS.contains(mod);
    }

    /**
     * @return A flag indicating if the <code>Script</code> is undefined.
     *
     */
    @Override
    public boolean isUndefined() {
        if (null != this.TYPE && !this.TYPE.isEmpty()) {
            return false;
        }

        return !Script.NATIVE_SCRIPTS.contains(this.NAME.toIString());
    }

    /**
     * A list of scripts that only exist implicitly.
     */
    static final java.util.List<IString> NATIVE_SCRIPTS = java.util.Arrays.asList(IString.get("ActiveMagicEffect"),
            IString.get("Alias"),
            IString.get("Debug"),
            IString.get("Form"),
            IString.get("Game"),
            IString.get("Input"),
            IString.get("Math"),
            IString.get("ModEvent"),
            IString.get("SKSE"),
            IString.get("StringUtil"),
            IString.get("UI"),
            IString.get("Utility"),
            IString.get("CommonArrayFunctions"),
            IString.get("ScriptObject"),
            IString.get("InputEnableLayer")
    );

    final private TString NAME;
    final private TString TYPE;
    final private List<MemberDesc> MEMBERS;
    private Script parent;

}
