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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
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
     * @see AnalyzableElement#getInfo(Optional<resaver.Analysis>, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(Optional<resaver.Analysis> analysis, ESS save) {
        final StringBuilder BUILDER = new StringBuilder();
        BUILDER.append("<html>");

        BUILDER.append(String.format("<h3>STRUCTURE DEFINITION %ss</h3>", this.NAME));

        analysis.map(an -> an.STRUCT_ORIGINS.get(this.NAME.toIString())).ifPresent(providers -> {
            if (!providers.isEmpty()) {
                if (providers.size() > 1) {
                    BUILDER.append("<p>WARNING: MORE THAN ONE MOD PROVIDES THIS STRUCT!<br />Exercise caution when editing or deleting this struct!</p>");
                }

                String probablyProvider = providers.last();
                BUILDER.append(String.format("<p>This struct probably came from \"%s\".</p>", probablyProvider));
                BUILDER.append("<p>Full list of providers:</p>");
                BUILDER.append("<ul>");
                providers.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                BUILDER.append("</ul>");
            }
        });

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

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(Optional<resaver.Analysis>, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean  matches(Optional<resaver.Analysis> analysis, String mod) {
        Objects.requireNonNull(analysis);
        Objects.requireNonNull(mod);

        return analysis
                .map(an -> an.STRUCT_ORIGINS.get(this.NAME.toIString()))
                .orElse(Collections.emptySortedSet())
                .contains(mod);
    }

    /**
     * @return A flag indicating if the <code>Struct</code> is undefined.
     *
     */
    @Override
    public boolean isUndefined() {
        return false;
    }

    final private TString NAME;
    final private List<MemberDesc> MEMBERS;

}
