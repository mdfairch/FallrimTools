/*
 * Copyright 2016 Mark.
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
package resaver.pex;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import resaver.IString;
import resaver.ListException;

/**
 * An abstraction describing a string table.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class StringTable extends ArrayList<StringTable.TString> {

    /**
     * Creates a new <code>TString</code> by reading from a
     * <code>DataInput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @return The new <code>TString</code>.
     * @throws IOException
     */
    public TString read(ByteBuffer input) throws IOException {
        Objects.requireNonNull(input);

        int index = Short.toUnsignedInt(input.getShort());

        if (index < 0 || index >= this.size()) {
            throw new IOException(String.format("Invalid TString index: %d (size %d)", index, this.size()));
        }

        TString newString = this.get(index);
        return newString;
    }

    /**
     * @return Returns a reusable instance of a blank <code>TString</code>.
     */
    public TString blank() {
        return this.addString(IString.BLANK);
    }

    /**
     * Creates a new <code>DataInput</code> by reading from a
     * <code>LittleEndianByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @throws ListException
     */
    public StringTable(ByteBuffer input) throws ListException {
        int strCount = Short.toUnsignedInt(input.getShort());
        this.ensureCapacity(strCount);

        for (int i = 0; i < strCount; i++) {
            try {
                final String STR = mf.BufferUtil.getUTF(input);
                final TString TSTR = new TString(STR, i);
                this.add(TSTR);
            } catch (RuntimeException ex) {
                throw new ListException("Error reading string", i, strCount, ex);
            }
        }
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     * @throws IOException
     */
    public void write(ByteBuffer output) throws IOException {
        output.putShort((short) this.size());

        for (TString tstr : this) {
            try {
                tstr.writeFull(output);
            } catch (IOException ex) {
                throw new IOException("Error writing string #" + tstr.INDEX, ex);
            }
        }
    }

    /**
     * Rebuilds the string table. This is necessary if ANY strings in ANY of the
     * Pex's members has changed at all. Otherwise, writing the Pex will produce
     * an invalid file.
     *
     * @param inUse The <code>Set</code> of strings that are still in use.
     *
     */
    public void rebuildStringTable(Set<TString> inUse) {
        this.retainAll(this);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    public int calculateSize() {
        int sum = 0;

        if (this.size() > 0xFFF0) {
            sum += 6;
        } else {
            sum += 2;
        }

        sum += this.stream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
    }
    
    /**
     * Adds a new string to the <code>StringTable</code> and returns the
     * corresponding <code>TString</code>.
     *
     * @param val The value of the new string.
     * @return The new <code>TString</code>, or the existing one if the
     * <code>StringTable</code> already contained a match.
     */
    public TString addString(IString val) {
        Optional<TString> match = this.stream().filter(v -> v.equals(val)).findFirst();
        if (match.isPresent()) {
            return match.get();
        }

        TString tstr = new TString(val, this.size());
        this.add(tstr);
        return tstr;
    }

    /**
     * A case-insensitive string with value semantics that reads and writes as a
     * two-byte index into a string table.
     *
     * @author Mark Fairchild
     */
    static public class TString extends resaver.WString {

        /**
         * Creates a new <code>TString</code> from a character sequence and an
         * index.
         *
         * @param cs The <code>CharSequence</code>.
         * @param index The index of the <code>TString</code>.
         */
        private TString(CharSequence cs, int index) {
            super(cs);
            this.INDEX = index;
        }

        /**
         * @see WString#write(ByteBuffer)
         * @param output The output stream.
         * @throws IOException
         */
        public void writeFull(ByteBuffer output) throws IOException {
            mf.BufferUtil.putWString(output, super.toString());
        }

        /**
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            output.putShort((short) this.INDEX);
        }

        final private int INDEX;

    }

}
