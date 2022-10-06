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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import resaver.Analysis;

/**
 * Describes a ChangeForm containing a formlist.
 *
 * @author Mark Fairchild
 */
final public class ChangeFormFLST implements ChangeFormData {

    /**
     * Creates a new <code>ChangeForm</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param flags The change flags.
     * @param context The <code>ESSContext</code> info.
     */
    public ChangeFormFLST(ByteBuffer input, Flags.Int flags, ESS.ESSContext context) {
        Objects.requireNonNull(input);

        this.FLAGS = flags.getFlag(1) ? new ChangeFormFlags(input) : null;

        if (flags.getFlag(31)) {
            int formCount = input.getInt();
            if (formCount > 0x3FFF) {
                throw new IllegalArgumentException("Invalid data: found " + formCount + " formCount in FLST.");
            }

            this.FORMS = new ArrayList<>(formCount);

            for (int i = 0; i < formCount; i++) {
                final RefID REF = context.readRefID(input);
                this.FORMS.add(REF);
            }
        } else {
            this.FORMS = null;
        }
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        if (null != this.FLAGS) {
            this.FLAGS.write(output);
        }

        if (null != this.FORMS) {
            output.putInt(this.FORMS.size());
            this.FORMS.forEach(ref -> ref.write(output));
        }
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 0;

        if (null != this.FLAGS) {
            sum += this.FLAGS.calculateSize();
        }

        if (null != this.FORMS) {
            sum += 4;
            sum += this.FORMS.stream().mapToInt(v -> v.calculateSize()).sum();
        }

        return sum;
    }

    /**
     * @return The <code>ChangeFormFlags</code> field.
     */
    public ChangeFormFlags getRefID() {
        return this.FLAGS;
    }

    /**
     * Removes null entries.
     *
     * @return The number of entries removed.
     */
    public int cleanse() {
        if (null == this.FORMS) {
            return 0;
        }

        int size = this.FORMS.size();
        this.FORMS.removeIf(v -> v.isZero());
        return size - this.FORMS.size();
    }

    /**
     * @return A flag indicating that the formlist has nullref entries.
     */
    public boolean containsNullrefs() {
        return this.FORMS.stream().anyMatch(v -> v.isZero());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        if (null == this.FORMS) {
            return "";

        } else if (this.containsNullrefs()) {
            return "(" + this.FORMS.size() + " refs, contains nullrefs)";

        } else {
            return "(" + this.FORMS.size() + " refs)";
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
        hash = 41 * hash + Objects.hashCode(this.FORMS);
        return hash;
    }

    /**
     * @see Object#equals()
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
        }

        final ChangeFormFLST other = (ChangeFormFLST) obj;
        return Objects.equals(this.FLAGS, other.FLAGS) && Objects.equals(this.FORMS, other.FORMS);
    }

    /**
     * @see ChangeFormData#getChangeConstants() 
     * @return 
     */
    @Override
    public ChangeFlagConstants[] getChangeConstants() {
        return new ChangeFlagConstants[0];
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

        BUILDER.append("<hr/><p>FORMLIST:</p>");

        if (null != this.FLAGS) {
            BUILDER.append(String.format("<p>ChangeFormFlags: %s</p>", this.FLAGS));
        }

        if (null != this.FORMS) {
            BUILDER.append(String.format("<p>List size: %d</p><ol start=0>", this.FORMS.size()));
            this.FORMS.forEach(refid -> {
                if (save.getChangeForms().containsKey(refid)) {
                    BUILDER.append(String.format("<li>%s", refid.toHTML(null)));
                } else {
                    BUILDER.append(String.format("<li>%s", refid));
                }
            });
            BUILDER.append("</ol>");
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
    public boolean matches(Analysis analysis, String mod) {
        return false;
    }

    final private ChangeFormFlags FLAGS;
    final private List<RefID> FORMS;

}
