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

import java.util.Arrays;
import java.nio.ByteBuffer;
import resaver.Analysis;

/**
 * Describes a default ChangeForm.
 *
 * @author Mark Fairchild
 */
final public class ChangeFormDefault implements ChangeFormData {

    /**
     * Creates a new <code>ChangeFormDefault</code> by storing a data buffer.
     *
     * @param input The data buffer.
     */
    public ChangeFormDefault(ByteBuffer input, int size) {
        this.DATA = new byte[size];
        input.get(this.DATA);
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.DATA);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return this.DATA.length;
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

        BUILDER.append("<hr/><p>RAW DATA:</p><code><pre>");

        for (int i = 0; i < this.DATA.length; i++) {
            if (i > 0 && i % 16 == 0) {
                BUILDER.append('\n');
            }

            final byte B = this.DATA[i];
            BUILDER.append(String.format("%02x ", B));
        }

        BUILDER.append("</pre></code>");
        return BUILDER.toString();
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return ""; //(" + this.BUFFER.length + " bytes)";
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
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.DATA);
    }

    /**
     * @see Object#equals(java.lang.Object)
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChangeFormDefault other = (ChangeFormDefault) obj;
        return Arrays.equals(this.DATA, other.DATA);
    }

    final private byte[] DATA;

}
