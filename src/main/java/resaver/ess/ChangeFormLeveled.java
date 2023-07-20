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

import static j2html.TagCreator.*;
import j2html.tags.ContainerTag;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import resaver.Analysis;
import static resaver.ess.ChangeFlagConstantsRefr.*;

/**
 * Describes a ChangeForm containing a leveled list.
 *
 * @author Mark Fairchild
 */
final public class ChangeFormLeveled implements ChangeFormData {

    /**
     * Creates a new <code>ChangeForm</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param flags The change form flags.
     * @param context The <code>ESSContext</code> info.
     */
    public ChangeFormLeveled(ByteBuffer input, Flags.Int flags, ESS.ESSContext context) {
        Objects.requireNonNull(input);
        if (flags.getFlag(CHANGE_FORM_FLAGS)) {
            this.FLAGS = Optional.of(new ChangeFormFlags(input));
        } else {
            this.FLAGS = Optional.empty();
        }
        
        if (flags.getFlag(31)) {
            int formCount = input.get();
            this.ENTRIES = new ArrayList<>(formCount);

            for (int i = 0; i < formCount; i++) {
                final LeveledEntry ENTRY = new LeveledEntry(input, context);
                this.ENTRIES.add(ENTRY);
            }
        } else {
            this.ENTRIES = null;
        }
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);

        if (this.FLAGS.isPresent()) {
            this.FLAGS.get().write(output);
        }

        if (null != this.ENTRIES) {
            output.put((byte) this.ENTRIES.size());
            this.ENTRIES.forEach(entry -> entry.write(output));
        }
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 0;

        if (this.FLAGS.isPresent()) {
            sum += this.FLAGS.get().calculateSize();
        }

        if (null != this.ENTRIES) {
            sum += 1;
            sum += this.ENTRIES.stream().mapToInt(v -> v.calculateSize()).sum();
        }

        return sum;
    }

    /**
     * @return The <code>ChangeFormFlags</code> field.
     */
    //public ChangeFormFlags getRefID() {
    //    return this.FLAGS;
    //}

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        if (null == this.ENTRIES) {
            return "";

        } else {
            return "(" + this.ENTRIES.size() + " leveled entries)";
        }
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.FLAGS);
        hash = 41 * hash + Objects.hashCode(this.ENTRIES);
        return hash;
    }

    /**
     * @see Object#equals()
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!Objects.equals(this.getClass(), obj.getClass())) {
            return false;
        }

        final ChangeFormLeveled other = (ChangeFormLeveled) obj;
        return Objects.equals(this.FLAGS, other.FLAGS) && Objects.equals(this.ENTRIES, other.ENTRIES);
    }

    /**
     * @see ChangeFormData#getChangeConstants() 
     * @return 
     */
    @Override
    public ChangeFlagConstants[] getChangeConstants() {
        return ChangeFlagConstantsLVLN.values();
    }
    
    /**
     * @see AnalyzableElement#getInfo(Optional<resaver.Analysis>, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(Optional<resaver.Analysis> analysis, ESS save) {
        return p(
                hr(),
                p("FORMLIST"),
                p(String.format("ChangeFormFlags: %s", this.FLAGS.map(f -> f.toString()).orElse("NONE"))),
                p(
                        ol(attrs("start=0"),
                                ENTRIES.stream().map(e -> e.toHTML(null)).map(n -> li(n)).toArray(ContainerTag[]::new)
                        )
                )
        ).toString();
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
    final private List<LeveledEntry> ENTRIES;

    public enum ChangeFlagConstantsLVLN implements ChangeFlagConstants {
        CHANGE_LEVELED_LIST_ADDED_OBJECT(31);

        /**
         * Returns the flag position.
         *
         * @return
         */
        @Override
        public int getPosition() {
            return this.VAL;
        }

        private ChangeFlagConstantsLVLN(int n) {
            this.VAL = n;
        }

        final private int VAL;
    }
    
}
