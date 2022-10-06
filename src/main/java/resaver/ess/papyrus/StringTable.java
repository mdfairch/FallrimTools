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
package resaver.ess.papyrus;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import resaver.ess.ESS;
import resaver.ess.WStringElement;

/**
 * An abstraction describing a string table.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class StringTable extends ArrayList<TString> implements PapyrusElement {

    /**
     * Creates a new <code>TString</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @return The new <code>TString</code>.
     * @throws PapyrusFormatException
     */
    public TString read(ByteBuffer input) throws PapyrusFormatException {
        Objects.requireNonNull(input);

        int index;

        if (this.STR32) {
            // SkyrimSE, Fallout4, and SkyrimLE with CrashFixes uses 32bit string indices.            
            index = input.getInt();

        } else {
            index = Short.toUnsignedInt(input.getShort());
            // SkyrimLegendary and Fallout4 use 16bit string indices.
            // Various corrections are possible though.

            if (index == 0xFFFF && !this.STBCORRECTION) {
                index = input.getInt();
            }
        }

        if (index < 0 || index >= this.size()) {
            throw new PapyrusFormatException(String.format("Invalid TString index: %d / %d", index, this.size()));
        }

        TString newString = this.get(index);
        return newString;
    }

    /**
     * Creates a new <code>StringTable</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>ESSContext</code> info.
     * @throws PapyrusElementException
     */
    public StringTable(ByteBuffer input, ESS.ESSContext context) throws PapyrusElementException {
        this.STR32 = context.isStr32();

        int strCount;

        if (this.STR32) {
            // SkyrimSE uses 32bit string indices.            
            strCount = input.getInt();
            STBCORRECTION = false;

        } else {
            // Skyrim Legendary (without CrashFixes) and old versions of 
            // Fallout4 use 16bit string indices.
            // Various corrections are possible though.           
            strCount = Short.toUnsignedInt(input.getShort());

            // Large string table version.
            if (strCount == 0xFFFF) {
                strCount = input.getInt();
            }

            // Fallback for catching the stringtable bug.
            if ((context.getGame().isFO4() && strCount < 7000) || (context.getGame().isSkyrim()&& strCount < 20000)) {
                strCount |= 0x10000;
                STBCORRECTION = true;
            } else {
                STBCORRECTION = false;
            }
        }

        // Store the string count.
        this.STRCOUNT = strCount;
        
        // Read the actual strings.
        try {
            this.ensureCapacity(strCount);
            for (int i = 0; i < strCount; i++) {
                try {
                    final WStringElement WSTR = WStringElement.read(input);
                    final TString TSTR = this.STR32
                            ? new TString32(WSTR, i)
                            : new TString16(WSTR, i);
                    this.add(TSTR);

                } catch (BufferUnderflowException ex) {
                    throw new PapyrusException("Error reading string #" + i, ex, null);
                }
            }
        } catch (BufferUnderflowException ex) {
            this.TRUNCATED = true;
            String msg = String.format("Error; read %d/%d strings.", this.size(), strCount);
            throw new PapyrusElementException(msg, ex, this);
        }

        this.TRUNCATED = false;
    }

    /**
     * Creates an empty <code>StringTable</code> with the truncated flag.
     */
    public StringTable() {
        this.STBCORRECTION = false;
        this.STR32 = false;
        this.TRUNCATED = true;
        this.STRCOUNT = 0;
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        if (this.STBCORRECTION) {
            throw new IllegalStateException("String-Table-Bug correction in effect. Cannot write.");
        } else if (this.TRUNCATED) {
            throw new IllegalStateException("StringTable is truncated. Cannot write.");
        }

        if (this.STR32) {
            // SkyrimSE uses 32bit string indices.
            output.putInt(this.size());

        } else // SkyrimLegendary and Fallout4 use 16bit string indices.
        // Various corrections are possible though.           
        // Large string table version.
        {
            if (this.size() > 0xFFF0 && !this.STBCORRECTION) {
                output.putShort((short) 0xFFFF);
                output.putInt(this.size());
            } else {
                output.putShort((short) this.size());
            }
        }

        // Write the actual strings.
        this.forEach(tstr -> tstr.writeFull(output));
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 0;

        if (this.STR32) {
            sum += 4;
        } else if (this.size() > 0xFFF0 && !this.STBCORRECTION) {
            sum += 6;
        } else {
            sum += 2;
        }

        sum += this.parallelStream().mapToInt(v -> v.calculateFullSize()).sum();
        return sum;
    }

    /**
     *
     * @param str
     * @return
     */
    public TString resolve(String str) {
        return this.stream().filter(tstr -> tstr.equals(str)).findFirst().orElse(null);
    }

    /**
     * Checks if the <code>StringTable</code> contains a <code>TString</code>
     * that matches a specified string value.
     *
     * @param val The value to match against.
     * @return True if the <code>StringTable</code> contains a matching
     * <code>TString</code>, false otherwise.
     */
    public boolean containsMatching(String val) {
        return this.stream().anyMatch(v -> v.equals(val));
    }

    /**
     * Adds a new string to the <code>StringTable</code> and returns the
     * corresponding <code>TString</code>.
     *
     * @param val The value of the new string.
     * @return The new <code>TString</code>, or the existing one if the
     * <code>StringTable</code> already contained a match.
     */
    public TString addString(String val) {
        Optional<TString> match = this.stream().filter(v -> v.equals(val)).findFirst();
        if (match.isPresent()) {
            return match.get();
        }

        TString tstr = this.STR32
                ? new TString32(val, this.size())
                : new TString16(val, this.size());
        this.add(tstr);
        return tstr;
    }

    /**
     * @return A flag indicating that the string table is truncated.
     */
    public boolean isTruncated() {
        return this.TRUNCATED;
    }

    /**
     * A flag indicating that the string-table-bug correction is in effect. This
     * means that the table is NOT SAVABLE.
     *
     * @return
     */
    public boolean hasSTB() {
        return this.STBCORRECTION;
    }

    /**
     * @return For a truncated <code>StringTable</code> returns the number of
     * missing strings. Otherwise returns 0.
     */
    public int getMissingCount() {
        return this.STRCOUNT - this.size();
    }

    /**
     * A flag indicating that the string-table-bug correction is in effect.
     */
    final private boolean STBCORRECTION;

    /**
     * Stores the truncated condition.
     */
    final private boolean TRUNCATED;

    /**
     * Stores the parsing context information.
     */
    final private boolean STR32;

    /**
     * Stores the declared string table size. If the <code>StringTable</code> is
     * truncated, this will not actually match the size of the list.
     */
    final private int STRCOUNT;

    /**
     * TString implementation for 16 bit TStrings.
     */
    final private class TString16 extends TString {

        /**
         * Creates a new <code>TString16</code> from a <code>WStringElement</code> and
         * an index.
         *
         * @param wstr The <code>WStringElement</code>.
         * @param index The index of the <code>TString</code>.
         */
        private TString16(WStringElement wstr, int index) {
            super(wstr, index);
        }

        /**
         * Creates a new <code>TString16</code> from a character sequence and an
         * index.
         *
         * @param cs The <code>CharSequence</code>.
         * @param index The index of the <code>TString</code>.
         */
        private TString16(CharSequence cs, int index) {
            super(cs, index);
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            if (this.getIndex() > 0xFFF0 && !STBCORRECTION) {
                output.putShort((short) 0xFFFF);
                output.putInt(this.getIndex());
            } else {
                output.putShort((short) this.getIndex());
            }
        }

        /**
         * @see resaver.ess.Element#calculateSize()
         * @return The size of the <code>Element</code> in bytes.
         */
        @Override
        public int calculateSize() {
            return (this.getIndex() > 0xFFF0 && !STBCORRECTION ? 6 : 2);
        }

    }

    /**
     * TString implementation for 32 bit TStrings.
     */
    final private class TString32 extends TString {

        /**
         * Creates a new <code>TString32</code> from a <code>WStringElement</code> and
         * an index.
         *
         * @param wstr The <code>WStringElement</code>.
         * @param index The index of the <code>TString</code>.
         */
        private TString32(WStringElement wstr, int index) {
            super(wstr, index);
        }

        /**
         * Creates a new <code>TString32</code> from a character sequence and an
         * index.
         *
         * @param cs The <code>CharSequence</code>.
         * @param index The index of the <code>TString</code>.
         */
        private TString32(CharSequence cs, int index) {
            super(cs, index);
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         * @throws IOException
         */
        @Override
        public void write(ByteBuffer output) {
            output.putInt(this.getIndex());
        }

        /**
         * @see resaver.ess.Element#calculateSize()
         * @return The size of the <code>Element</code> in bytes.
         */
        @Override
        public int calculateSize() {
            return 4;
        }

    }
}
