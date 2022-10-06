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

/**
 * Describes the ChangeForm flags for a ChangeForm.
 *
 * @author Mark Fairchild
 */
final public class ChangeFormFlags implements Element {

    /**
     * Creates a new <code>ChangeFormFlags</code>.
     *
     * @param input The input stream.
     */
    public ChangeFormFlags(ByteBuffer input) {
        this.FLAGS = Flags.readIntFlags(input);
        this.UNKNOWN = input.getShort();
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer) 
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);
        this.FLAGS.write(output);
        output.putShort(this.UNKNOWN);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 2 + this.FLAGS.calculateSize();
    }

    /**
     * @return The flag field.
     */
    public Flags.Int getFlags() {
        return this.FLAGS;
    }

    /**
     * @return The unknown field.
     */
    public short getUnknown() {
        return this.UNKNOWN;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(this.FLAGS.toString())
                .append(" (")
                .append(this.UNKNOWN)
                .append(")")
                .toString();
    }

    final private Flags.Int FLAGS;
    final private short UNKNOWN;

}
