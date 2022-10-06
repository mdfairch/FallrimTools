/*
 * Copyright 2018 Mark.
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

/**
 * Describes an entry in a leveled list.
 *
 * @author Mark Fairchild
 */
public class LeveledEntry implements Element, Linkable {

    /**
     * Creates a new <code>Plugin</code> by reading from an input stream.
     *
     * @param input The input stream.
     * @param context The <code>ESSContext</code> info.
     */
    public LeveledEntry(ByteBuffer input, ESS.ESSContext context) {
        Objects.requireNonNull(input);
        this.REFID = context.readRefID(input);
        this.LEVEL = input.get();
        this.COUNT = input.getShort();
        this.CHANCE = input.get();
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer) 
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);
        this.REFID.write(output);
        output.put((byte) this.LEVEL);
        output.putShort((short) this.COUNT);
        output.put((byte) this.CHANCE);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 4 + this.REFID.calculateSize();
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        return String.format("%d (%s) = %d", this.LEVEL, this.REFID.toHTML(target), this.COUNT);
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return String.format("%d (%s) = %d", this.LEVEL, this.REFID, this.COUNT);
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.REFID);
        hash = 29 * hash + Integer.hashCode(this.LEVEL);
        hash = 29 * hash + Integer.hashCode(this.COUNT);
        hash = 29 * hash + Integer.hashCode(this.CHANCE);
        return hash;
    }

    /**
     * @see Object#equals(java.lang.Object)
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
        } else {
            final LeveledEntry other = (LeveledEntry) obj;
            return this.LEVEL == other.LEVEL
                    && this.COUNT == other.COUNT
                    && this.CHANCE == other.CHANCE
                    && Objects.equals(this.REFID, other.REFID);
        }
    }

    final public RefID REFID;
    final public int LEVEL;
    final public int COUNT;
    final public int CHANCE;

}
