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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import static resaver.ess.ChangeFlagConstantsNPC.*;

/**
 * Describes a ChangeForm containing an NPC.
 *
 * @author Mark Fairchild
 */
public class ChangeFormNPC extends GeneralElement implements ChangeFormData {

    /**
     * Creates a new <code>ChangeForm</code>.
     *
     * @param input The input stream.
     * @param flags The change form flags.
     * @param context The <code>ESSContext</code> info.
     * @throws ElementException
     * 
     */
    public ChangeFormNPC(ByteBuffer input, Flags.Int flags, ESS.ESSContext context) throws ElementException {
        this(input, flags, false, context);
    }
    
    /**
     * Creates a new <code>ChangeForm</code>.
     *
     * @param input The input stream.
     * @param flags The change form flags.
     * @param context The <code>ESSContext</code> info.
     * @param inline Indicates that the NPC_ changeform appears as an element
     * of another changeform so unparsed data in <code>input</code> should be 
     * ignored.
     * @throws ElementException
     * 
     */
    public ChangeFormNPC(ByteBuffer input, Flags.Int flags, boolean inline, ESS.ESSContext context) throws ElementException {
        Objects.requireNonNull(input);

        if (flags.getFlag(CHANGE_FORM_FLAGS)) {
            this.CHANGEFORMFLAGS = super.readElement(input, CHANGE_FORM_FLAGS, in -> new ChangeFormFlags(in));
        } else {
            this.CHANGEFORMFLAGS = null;
        }

        try {
            if (flags.getFlag(CHANGE_ACTOR_BASE_DATA)) {
                super.readBytes(input, "ACBS", 24);
            }

            if (flags.getFlag(CHANGE_ACTOR_BASE_FACTIONS)) {
                super.readVSElemArray(input, "FACTIONRANKS", in -> new FactionRank(in, context));
            }

            if (flags.getFlag(CHANGE_ACTOR_BASE_SPELLLIST)) {
                super.readVSElemArray(input, "SPELLS", in -> context.readRefID(input));
                super.readVSElemArray(input, "SPELLS_LEVELLED", in -> context.readRefID(input));
                super.readVSElemArray(input, "SHOUTS", in -> context.readRefID(input));
            }

            if (flags.getFlag(CHANGE_ACTOR_BASE_AIDATA)) {
                super.readBytes(input, "AIDT", 20);
            }

            if (flags.getFlag(CHANGE_ACTOR_BASE_FULLNAME)) {
                super.readWString(input, "FULLNAME");
            }

            if (flags.getFlag(CHANGE_NPC_SKILLS)) {
                super.readBytes(input, "DNAM", 52);
            }

            if (flags.getFlag(CHANGE_NPC_CLASS)) {
                super.readRefID(input, "CCLASS", context);
            }

            if (flags.getFlag(CHANGE_NPC_RACE)) {
                super.readRefID(input, "RACE", context);
                super.readRefID(input, "OLDRACE", context);
            }

            if (flags.getFlag(CHANGE_NPC_FACE)) {
                super.readElement(input, "FACEDATA", in -> new FaceData(input, context));
            }

            if (flags.getFlag(CHANGE_NPC_GENDER)) {
                super.readByte(input, "GENDER");
            }

            if (flags.getFlag(CHANGE_DEFAULT_OUTFIT)) {
                super.readRefID(input, "DEFAULT_OUTFIT", context);
            }

            if (flags.getFlag(CHANGE_SLEEP_OUTFIT)) {
                super.readRefID(input, "SLEEP_OUTFIT", context);
            }

            if (!inline && super.readUnparsed(input)) {
                throw new UnparsedException();
            }
            
        } catch (UnparsedException ex) {
            throw new ElementException("Unparsed data in NPC_", ex, this);            
        } catch (RuntimeException ex) {
            super.readUnparsed(input);
            throw new ElementException("Error reading NPC_", ex, this);
        }
    }

    /**
     * @return The <code>ChangeFormFlags</code> field.
     */
    public ChangeFormFlags getChangeFormFlags() {
        return this.CHANGEFORMFLAGS;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return super.hasVal("FULLNAME") ? super.getVal("FULLNAME").toString() : "";
    }

    /**
     * @see ChangeFormData#getChangeConstants() 
     * @return 
     */
    @Override
    public ChangeFlagConstants[] getChangeConstants() {
        return ChangeFlagConstantsNPC.values();
    }
    
    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(Optional<resaver.Analysis> analysis, ESS save) {
        final StringBuilder BUILDER = new StringBuilder();

        BUILDER.append("<p>NPC:</p>");

        if (this.hasVal("FULLNAME")) {
            BUILDER.append(String.format("<p>FullName: %s\n", this.getVal("FULLNAME")));
        }

        if (null != this.CHANGEFORMFLAGS) {
            BUILDER.append(String.format("<p>ChangeFormFlags: %s\n", this.CHANGEFORMFLAGS));
        }

        if (this.hasVal("ACBS")) {
            BUILDER.append("<p>Base stats: ");
            for (byte b : (byte[]) super.getVal("ACBS")) {
                BUILDER.append(String.format("%02x", b));
            }
            BUILDER.append("\n");
        }

        if (this.hasVal("FACTIONRANKS")) {
            Element[] ranks = (Element[]) super.getVal("FACTIONRANKS");
            BUILDER.append(String.format("<p>%s faction ranks.</p><ul>", ranks.length));
            Arrays.asList(ranks).forEach(v -> BUILDER.append(String.format("<li>%s", v)));
            BUILDER.append("</ul>");
        }

        if (super.hasVal("SPELLS")) {
            Element[] spells = (Element[]) super.getVal("SPELLS");
            Element[] spells_levelled = (Element[]) super.getVal("SPELLS_LEVELLED");
            Element[] shouts = (Element[]) super.getVal("SHOUTS");

            BUILDER.append(String.format("<p>%s spells.</p><ul>", spells.length));
            Arrays.asList(spells).forEach(v -> BUILDER.append(String.format("<li>%s", v)));
            BUILDER.append("</ul>");

            BUILDER.append(String.format("<p>%s levelled spells.</p><ul>", spells_levelled.length));
            Arrays.asList(spells_levelled).forEach(v -> BUILDER.append(String.format("<li>%s", v)));
            BUILDER.append("</ul>");

            BUILDER.append(String.format("<p>%s shouts.</p><ul>", shouts.length));
            Arrays.asList(shouts).forEach(v -> BUILDER.append(String.format("<li>%s", v)));
            BUILDER.append("</ul>");
        }

        if (super.hasVal("AIDT")) {
            BUILDER.append("<p>AI:</p><code>");
            for (byte b : (byte[]) super.getVal("AIDT")) {
                BUILDER.append(String.format("%02x", b));
            }
            BUILDER.append("</code>");
        }

        if (super.hasVal("DNAM")) {
            BUILDER.append("<p>Skills:</p><code>");
            for (byte b : (byte[]) super.getVal("DNAM")) {
                BUILDER.append(String.format("%02x", b));
            }
            BUILDER.append("</code>");
        }

        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Optional<resaver.Analysis> analysis, String mod) {
        return false;
    }

    final private ChangeFormFlags CHANGEFORMFLAGS;

    /**
     * Faction rank.
     */
    static private class FactionRank extends GeneralElement {

        public FactionRank(ByteBuffer input, ESS.ESSContext context) throws ElementException{
            super.readRefID(input, "FACTION", context);
            super.readByte(input, "RANK");
        }

        @Override
        public String toString() {
            return String.format("Rank %d with %s", this.getVal("RANK"), this.getVal("FACTION"));
        }
    }

    /**
     * Face
     */
    static private class FaceData extends GeneralElement {

        public FaceData(ByteBuffer input, ESS.ESSContext context) throws ElementException {
            byte facePresent = super.readByte(input, "FACEPRESENT");

            if (facePresent != 0) {
                super.readRefID(input, "HAIRCOLOR", context);
                super.readInt(input, "SKINTONE");
                super.readRefID(input, "SKIN", context);
                super.readVSElemArray(input, "HEADPARTS", in -> context.readRefID(in));
                byte faceDataPrsent = super.readByte(input, "FACEDATAPRESENT");
                
                if (faceDataPrsent != 0) {
                    super.readFloats(input, "MORPHS", super.readInt(input, "MORPHS_COUNT"));
                    super.readInts(input, "PRESETS", super.readInt(input, "PRESETS_COUNT"));
                }
            }
        }
    }
}
