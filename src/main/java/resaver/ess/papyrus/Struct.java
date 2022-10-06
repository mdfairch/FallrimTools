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
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.stream.Collectors;
import resaver.Analysis;
import resaver.ess.Element;
import resaver.ess.ESS;
import resaver.ess.Linkable;

/**
 * Describes a structure in a Fallout 4 savegame.
 *
 * @author Mark Fairchild
 */
final public class Struct extends Definition {

    /**
     * Creates a new <code>Structure</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     */
    public Struct(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException, PapyrusElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        this.NAME = context.readTString(input);

        try {
            int count = input.getInt();
            this.MEMBERS = MemberDesc.readList(input, count, context);
        } catch (ListException ex) {
            throw new PapyrusElementException("Failed to read Struct members.", ex, this);
        }
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        assert null != output;
        this.NAME.write(output);
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
        sum += this.MEMBERS.stream().mapToInt(member -> member.calculateSize()).sum();
        return sum;
    }

    /**
     * @return The ID of the papyrus element.
     */
    @Override
    public TString getName() {
        return this.NAME;
    }

    /**
     * @return The list of <code>MemberDesc</code>.
     */
    @Override
    public List<MemberDesc> getMembers() {
        return java.util.Collections.unmodifiableList(this.MEMBERS);
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        if (null != target && target instanceof MemberDesc) {
            int i = this.getMembers().indexOf(target);
            if (i >= 0) {
                return Linkable.makeLink("struct", this.NAME, i, this.NAME.toString());
            }
        }
        return Linkable.makeLink("struct", this.NAME, this.NAME.toString());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return this.NAME.toString();
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

        BUILDER.append(String.format("<h3>STRUCTURE DEFINITION %ss</h3>", this.NAME));

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

        BUILDER.append(String.format("<p>Contains %d member variables.</p>", this.MEMBERS.size()));

        final List<StructInstance> STRUCTS = save.getPapyrus()
                .getStructInstances()
                .values()
                .stream()
                .filter(instance -> instance.getStruct() == this)
                .collect(Collectors.toList());

        BUILDER.append(String.format("<p>There are %d instances of this structure definition.</p>", STRUCTS.size()));
        if (STRUCTS.size() < 20) {
            BUILDER.append("<ul>");
            STRUCTS.forEach(i -> {
                String s = String.format("<li>%s</a>", i.toHTML(this));
                BUILDER.append(s);
            });
            BUILDER.append("</ul>");
        }

        /*if (null != analysis && analysis.STRUCT_ORIGINS.containsKey(this.NAME)) {
            final java.io.File PEXFILE = analysis.SCRIPTS.get(this.NAME);
            final java.io.File PARENT = PEXFILE.getParentFile();

            BUILDER.append("");
            BUILDER.append(String.format("<hr /><p>Disassembled source code:<br />(from %s)</p>", PEXFILE.getPath()));

            if (PEXFILE.exists() && PEXFILE.canRead()) {
                try {
                    final resaver.pex.Pex SCRIPT = resaver.pex.Pex.readScript(PEXFILE);

                    java.io.StringWriter code = new java.io.StringWriter();
                    SCRIPT.disassemble(code, resaver.pex.AssemblyLevel.STRIPPED);

                    BUILDER.append("<p<code><pre>");
                    BUILDER.append(code.getBuffer());
                    BUILDER.append("</pre></code></p>");

                } catch (RuntimeException ex) {
                    BUILDER.append("<p><em>Error: disassembly failed.</em></p>");
                } catch (java.io.IOException ex) {
                    BUILDER.append("<p><em>Error: couldn't read the script file.</em></p>");
                } catch (Error ex) {
                    BUILDER.append("<p><em>Error: unexpected error while reading script file.</em></p>");
                }

            } else if (PARENT.exists() && PARENT.isFile()) {
                try (resaver.LittleEndianRAF input = new resaver.LittleEndianRAF(PARENT, "r")) {
                    resaver.bsa.BSAParser BSA = new resaver.bsa.BSAParser(PARENT.getName(), input);
                    final resaver.pex.Pex SCRIPT = BSA.getScript(PEXFILE.getName());

                    java.io.StringWriter code = new java.io.StringWriter();
                    SCRIPT.disassemble(code, resaver.pex.AssemblyLevel.STRIPPED);

                    BUILDER.append("<p<code><pre>");
                    BUILDER.append(code.getBuffer());
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
         */
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

        //final SortedSet<String> OWNERS = analysis.SCRIPT_ORIGINS.get(this.NAME);
        //return null != OWNERS && OWNERS.contains(mod);
        return false;
    }

    /**
     * @return A flag indicating if the <code>Script</code> is undefined.
     *
     */
    @Override
    public boolean isUndefined() {
        return false;
    }

    final private TString NAME;
    final private List<MemberDesc> MEMBERS;

}
