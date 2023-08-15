/*
 * Copyright 2018 Mark Fairchild.
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
import java.util.Optional;
import static resaver.ess.ChangeFlagConstantsRefr.*;

/**
 * Describes a ChangeForm containing a leveled list.
 *
 * @author Mark Fairchild
 */
final public class ChangeFormRela extends GeneralElement implements ChangeFormData {

    /**
     * Creates a new <code>ChangeForm</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param changeFlags The ChangeFlags.
     * @param refid The ChangeForm refid.
     * @param context The <code>ESSContext</code> info.
     * @throws ElementException
     * 
     */
    public ChangeFormRela(ByteBuffer input, Flags.Int changeFlags, RefID refid, ESS.ESSContext context) throws ElementException {
        Objects.requireNonNull(input);
        if (changeFlags.getFlag(CHANGE_FORM_FLAGS)) {
            this.FLAGS = Optional.of(new ChangeFormFlags(input));
        } else {
            this.FLAGS = Optional.empty();
        }
        
        RefID person1 = null, person2 = null, association = null;
        Integer rank = null;
        
        try {
            if (refid.getType() == RefID.Type.CREATED) {
                person1 = super.readRefID(input, "PERSON1", context);
                person2 = super.readRefID(input, "PERSON2", context);
                association = super.readRefID(input, "ASSOCIATION", context);                
            }
            if (changeFlags.getFlag(ChangeFlagConstantsRela.RANK)) rank = super.readInt(input, "RANK");
            
        } catch (UnparsedException ex) {
            throw new ElementException("Unparsed data in RELA", ex, this);            
        } catch (RuntimeException ex) {
            super.readUnparsed(input);
            throw new ElementException("Error reading RELA", ex, this);
        } finally {
            PERSON1 = person1;
            PERSON2 = person2;
            ASSOCIATION = association;
            RANK = rank;
        }
    }

    /**
     * @see ChangeFormData#getChangeConstants() 
     * @return 
     */
    @Override
    public ChangeFlagConstants[] getChangeConstants() {
        return ChangeFlagConstantsRela.values();
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
        BUILDER.append(super.toStringStructured("RELA", 0));
        BUILDER.append("</code></pre>");
        return BUILDER.toString();
    }
    
    /**
     * @see AnalyzableElement#matches(Optional<resaver.Analysis>, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Optional<resaver.Analysis> analysis, String mod) {
        return false;
    }

    final private Optional<ChangeFormFlags> FLAGS;    
    final public RefID PERSON1;
    final public RefID PERSON2;
    final public RefID ASSOCIATION;
    final public Integer RANK;
    
    static public enum ChangeFlagConstantsRela implements ChangeFlagConstants {
        UNK0(0), 
        RANK(1);

        /**
         * Returns the flag position.
         *
         * @return
         */
        @Override
        public int getPosition() {
            return this.VAL;
        }

        private ChangeFlagConstantsRela(int n) {
            this.VAL = n;
        }

        final private int VAL;
    }
}
