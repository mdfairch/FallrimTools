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
import java.util.Objects;
import resaver.Analysis;
import static resaver.ess.ChangeFlagConstantsAchr.*;

/**
 * Describes a ChangeForm containing an NPC Reference.
 *
 * @author Mark Fairchild
 */
public class ChangeFormACHR extends GeneralElement implements ChangeFormData {

    /**
     * Creates a new <code>ChangeFormACHR</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param changeFlags The ChangeFlags.
     * @param refid The ChangeForm refid.
     * @param analysis
     * @param context The <code>ESSContext</code> info.
     * @throws ElementException
     */
    public ChangeFormACHR(ByteBuffer input, Flags.Int changeFlags, RefID refid, resaver.Analysis analysis, ESS.ESSContext context) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(changeFlags);

        int initialType;

        if (refid.getType() == RefID.Type.CREATED) {
            initialType = 5;
        } else if (changeFlags.getFlag(CHANGE_REFR_PROMOTED) || changeFlags.getFlag(CHANGE_REFR_CELL_CHANGED)) {
            initialType = 6;
        } else if (changeFlags.getFlag(CHANGE_REFR_HAVOK_MOVE) || changeFlags.getFlag(CHANGE_REFR_MOVE)) {
            initialType = 4;
        } else {
            initialType = 0;
        }

        try {
            super.readElement(input, "INITIAL", in -> new ChangeFormInitialData(in, initialType, context));

            if (changeFlags.getFlag(CHANGE_REFR_HAVOK_MOVE)) {
                this.readBytesVS(input, "HAVOK");
            }

            super.readInt(input, "UNKNOWN_INTEGER");
            super.readBytes(input, "UNKNOWN_BYTES", 4);
            
            if (changeFlags.getFlag(CHANGE_FORM_FLAGS)) {
                super.readElement(input, CHANGE_FORM_FLAGS, in -> new ChangeFormFlags(in));
            }

            if (changeFlags.getFlag(CHANGE_REFR_BASEOBJECT)) {
                super.readRefID(input, "BASE_OBJECT", context);
            }

            if (changeFlags.getFlag(CHANGE_REFR_SCALE)) {
                super.readFloat(input, "SCALE");
            }

            if (changeFlags.getFlags(
                    CHANGE_REFR_EXTRA_OWNERSHIP,
                    CHANGE_REFR_PROMOTED, 
                    CHANGE_ACTOR_EXTRA_PACKAGE_DATA,
                    CHANGE_ACTOR_EXTRA_MERCHANT_CONTAINER,
                    CHANGE_ACTOR_EXTRA_DISMEMBERED_LIMBS,
                    CHANGE_REFR_EXTRA_ACTIVATING_CHILDREN,
                    CHANGE_REFR_EXTRA_ENCOUNTER_ZONE,
                    CHANGE_REFR_EXTRA_CREATED_ONLY,
                    CHANGE_REFR_EXTRA_GAME_ONLY,
                    CHANGE_ACTOR_LEVELED_ACTOR
                    )) {
                super.readElement(input, "EXTRADATA", in -> new ChangeFormExtraData(in, context));
            }

            if (changeFlags.getFlags(CHANGE_REFR_INVENTORY, CHANGE_REFR_LEVELED_INVENTORY)) {
                super.readVSElemArray(input, "INVENTORY", in -> new ChangeFormInventoryItem(in, context));
            }

            if (changeFlags.getFlag(CHANGE_REFR_ANIMATION)) {
                super.readBytesVS(input, "ANIMATIONS");
            }

            if (super.readUnparsed(input)) {
                throw new UnparsedException();
            }
            
        } catch (UnparsedException ex) {
            throw new ElementException("Unparsed data in ACHR", ex, this);            
        } catch (RuntimeException | ElementException ex) {
            super.readUnparsed(input);
            throw new ElementException("Failed to read ACHR", ex, this);
        }
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Analysis analysis, String mod) {
        return false;
    }

    
    /**
     * @see ChangeFormData#getChangeConstants() 
     * @return 
     */
    @Override
    public ChangeFlagConstants[] getChangeConstants() {
        return ChangeFlagConstantsAchr.values();
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
        BUILDER.append("<pre><code>");
        BUILDER.append(super.toStringStructured("ACHR", 0));
        BUILDER.append("</code></pre>");
        return BUILDER.toString();
    }

}
