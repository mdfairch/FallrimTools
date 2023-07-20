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
import resaver.Analysis;
import resaver.esp.RecordCode;
import static resaver.ess.ChangeFlagConstantsRefr.*;

/**
 * Describes a ChangeForm containing a placed Reference.
 *
 * @author Mark Fairchild
 */
public class ChangeFormRefr extends GeneralElement implements ChangeFormData {

    /**
     * Creates a new <code>ChangeFormRefr</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param changeFlags The ChangeFlags.
     * @param refid The ChangeForm refid.
     * @param analysis
     * @param context The <code>ESSContext</code> info.
     * @throws ElementException
     * 
     */
    public ChangeFormRefr(ByteBuffer input, Flags.Int changeFlags, RefID refid, Optional<resaver.Analysis> analysis, ESS.ESSContext context) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(changeFlags);
        Objects.requireNonNull(analysis);
        Objects.requireNonNull(context);

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
            ChangeFormInitialData initial = super.readElement(input, "INITIAL", in -> new ChangeFormInitialData(in, initialType, context));

            if (changeFlags.getFlag(CHANGE_REFR_HAVOK_MOVE)) {
                super.readBytesVS(input, "HAVOK");
            }

            if (changeFlags.getFlag(CHANGE_FORM_FLAGS)) {
                super.readElement(input, CHANGE_FORM_FLAGS, in -> new ChangeFormFlags(in));
            }

            if (changeFlags.getFlag(CHANGE_REFR_BASEOBJECT)) {
                super.readRefID(input, "BASE_OBJECT", context);
            }

            if (changeFlags.getFlag(CHANGE_REFR_SCALE)) {
                super.readFloat(input, "SCALE");
            }

            //if (changeFlags.getFlag(CHANGE_REFR_MOVE)) {
            //    super.readRefID(input, "MOVE_CELL", context);
            //    super.readFloats(input, "MOVE_POS", 3);
            //    super.readFloats(input, "MOVE_ROT", 3);
            //}

            if (changeFlags.getFlag(CHANGE_REFR_EXTRA_OWNERSHIP)
                    || changeFlags.getFlag(CHANGE_OBJECT_EXTRA_LOCK)
                    || changeFlags.getFlag(CHANGE_REFR_EXTRA_ENCOUNTER_ZONE)
                    || changeFlags.getFlag(CHANGE_REFR_EXTRA_GAME_ONLY)
                    || changeFlags.getFlag(CHANGE_OBJECT_EXTRA_AMMO)
                    || changeFlags.getFlag(CHANGE_DOOR_EXTRA_TELEPORT)
                    || changeFlags.getFlag(CHANGE_REFR_PROMOTED)
                    || changeFlags.getFlag(CHANGE_REFR_EXTRA_ACTIVATING_CHILDREN)
                    || changeFlags.getFlag(CHANGE_OBJECT_EXTRA_ITEM_DATA)) {
                super.readElement(input, "EXTRADATA", in -> new ChangeFormExtraData(in, context));
            }

            if (changeFlags.getFlag(CHANGE_REFR_INVENTORY) || changeFlags.getFlag(CHANGE_REFR_LEVELED_INVENTORY)) {
                super.readVSElemArray(input, "INVENTORY", in -> new ChangeFormInventoryItem(in, context));
            }

            if (changeFlags.getFlag(CHANGE_REFR_PROMOTED)) {
                super.readVSElemArray(input, "PROMOTION", in -> context.readRefID(in));
            }
            
            if (changeFlags.getFlag(CHANGE_REFR_ANIMATION)) {
                super.readBytesVS(input, "ANIMATIONS");
            }

            if (analysis.isPresent()) {
                Element baseObjectRef = initial.getElement("BASE_OBJECT");
                if (initialType == 5 && baseObjectRef != null && baseObjectRef instanceof RefID) {
                    RefID ref = (RefID) baseObjectRef;

                    if (ref.PLUGIN != null && ref.isValid() && ref.getType() != RefID.Type.CREATED) {
                        RecordCode code = analysis.get().getType(ref.PLUGIN, ref.FORMID);
                        if (code == RecordCode.EXPL) {
                            super.readElement(input, "EXPLOSION", in -> new ChangeFormExtraDataData.Explosion(in, context));
                        }
                    }
                }
            };
            
            if (super.readUnparsed(input)) {
                throw new UnparsedException();
            }
            
        } catch (UnparsedException ex) {
            throw new ElementException("Unparsed data in REFR", ex, this);            
        } catch (RuntimeException | ElementException ex) {
            super.readUnparsed(input);
            throw new ElementException("Failed to read REFR", ex, this);
        }
    }

    /**
     * Attempts to clear the havok flag and data.
     * @return A flag indicating whether the havok data was cleared, or
     * false if there was no havok data.
     */
    public boolean clearHavok() {
        // If there is unparsed data, don't mess with this REFR.
        // If the havok flag is set and there is no havok data, something 
        // is wrong. Same if it's not set and there IS havok data.
        if (this.hasUnparsed() || !this.hasVal("HAVOK") || !this.hasVal("HAVOK_COUNT")) {
            return false;
        }
        
        final byte[] havokData = (byte[]) this.getVal("HAVOK");
        Arrays.fill(havokData, (byte)0);
        
        //this.removeValue("HAVOK_COUNT");
        //this.removeValue("HAVOK");
        return true;
    }
    
    /**
     * @see ChangeFormData#getChangeConstants() 
     * @return 
     */
    @Override
    public ChangeFlagConstants[] getChangeConstants() {
        return ChangeFlagConstantsRefr.values();
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
        BUILDER.append("<pre><code>");
        BUILDER.append(super.toStringStructured("REFR", 0));
        BUILDER.append("</code></pre>");
        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, java.lang.String) 
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Optional<Analysis> analysis, String mod) {
        return false;
    }

}
