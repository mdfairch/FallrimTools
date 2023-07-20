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

import java.util.Objects;
import java.util.SortedSet;
import java.nio.ByteBuffer;
import java.util.Optional;
import resaver.ess.AnalyzableElement;
import resaver.ess.ESS;
import resaver.ess.Element;
import resaver.ess.Linkable;

/**
 * Describes a function parameter in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class QueuedUnbind implements PapyrusElement, AnalyzableElement, Linkable, HasID {

    /**
     * Creates a new <code>MemberData</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     *
     */
    public QueuedUnbind(ByteBuffer input, PapyrusContext context) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);
        this.ID = context.readEID(input);
        this.UNKNOWN = input.getInt();
        this.INSTANCE = context.findScriptInstance(this.ID);
        assert null != this.INSTANCE : "QueuedUnbind could not be associated with a script instance!";
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        this.ID.write(output);
        output.putInt(this.UNKNOWN);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 4 + this.ID.calculateSize();
    }

    /**
     * @return The ID of the papyrus element.
     */
    @Override
    public EID getID() {
        return this.ID;
    }

    /**
     * @return The unknown field.
     */
    public int getUnknown() {
        return this.UNKNOWN;
    }

    /**
     * @return The corresponding <code>ScriptInstance</code>.
     */
    public ScriptInstance getScriptInstance() {
        return this.INSTANCE;
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
        if (null != this.INSTANCE && null != this.INSTANCE.getScript()) {
            BUILDER.append(String.format("<html><h3>QUEUED UNBIND of %s</h3>", this.INSTANCE.getScript().toHTML(this)));
        } else if (null != this.INSTANCE) {
            BUILDER.append(String.format("<html><h3>QUEUED UNBIND of %s</h3>", this.INSTANCE.getScriptName()));
        } else {
            BUILDER.append(String.format("<html><h3>QUEUED UNBIND of %s</h3>", this.ID));
        }

        if (null == this.INSTANCE) {
            BUILDER.append(String.format("<p>Instance: %s</p>", this.ID));
        } else {
            BUILDER.append(String.format("<p>Instance: %s</p>", this.INSTANCE.toHTML(this)));
        }

        BUILDER.append(String.format("<p>Unknown: %s</p>", EID.pad8(this.UNKNOWN)));

        Linkable UNK = save.getPapyrus().getContext().broadSpectrumSearch(this.UNKNOWN);
        if (null != UNK) {
            BUILDER.append("<p>Potential match for UNKNOWN found using general search:<br/>");
            BUILDER.append(UNK.toHTML(this));
            BUILDER.append("</p>");
        }

        analysis.map(an -> an.SCRIPT_ORIGINS.get(this.INSTANCE.getScriptName().toIString())).ifPresent(providers -> {
            if (!providers.isEmpty()) {
                String probablyProvider = providers.last();
                BUILDER.append(String.format("The queued unbinding probably came from mod \"%s\".\n\n", probablyProvider));

                if (providers.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    providers.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }                
            }
        });

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
        return this.INSTANCE.matches(analysis, mod);
    }

    /**
     * @return A flag indicating if the <code>ScriptInstance</code> is
     * undefined.
     *
     */
    public boolean isUndefined() {
        return this.INSTANCE.isUndefined();
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        return Linkable.makeLink("unbind", this.ID, this.toString());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        if (null == this.INSTANCE) {
            return this.ID + ": " + EID.pad8(this.UNKNOWN) + " (MISSING INSTANCE)";
        } else {
            return this.ID + ": " + EID.pad8(this.UNKNOWN) + " (" + INSTANCE.getScriptName() + ")";
        }
    }

    final private EID ID;
    final private int UNKNOWN;
    final private ScriptInstance INSTANCE;

}
