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
package resaver;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Extends <code>IString</code> by handling charsets and storing the original
 * byte sequence.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class WString extends IString {

    /**
     * Creates a new <code>WString</code> by reading from a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @return The new <code>WString</code>.
     */
    static public WString read(ByteBuffer input) {
        final byte[] BYTES = mf.BufferUtil.getWStringRaw(input);
        return new WString(BYTES);
    }

    /**
     * Copy constructor.
     *
     * @param other The original <code>WString</code>.
     */
    public WString(WString other) {
        super(other);
        this.RAW_BYTES = other.RAW_BYTES;
    }

    /**
     * Creates a new <code>WString</code> from a character sequence; the byte
     * array is generated from the string using UTF-8 encoding.
     *
     * @param cs The <code>CharSequence</code>.
     */
    public WString(CharSequence cs) {
        super(cs);
        this.RAW_BYTES = null;
    }

    /**
     * Creates a new <code>WString</code> from a character sequence and a byte
     * array.
     *
     * @param bytes The byte array.
     */
    protected WString(byte[] bytes) {
        super(mf.BufferUtil.mozillaString(bytes));
        this.RAW_BYTES = Arrays.equals(super.getUTF8(), bytes)
                ? null
                : bytes;
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    public void write(ByteBuffer output) {
        final byte[] BYTES = this.getUTF8();
        
        if (BYTES.length > 0xFFFF) {
            output.putShort((short)0xFFFF);
            output.put(BYTES, 0, 0xFFFF);
        } else {
            output.putShort((short)BYTES.length);
            output.put(BYTES);
        }
    }

    /**
     * @return The size of the <code>WString</code> in bytes.
     */
    public int calculateSize() {
        final byte[] BYTES = this.getUTF8();
        return BYTES.length > 0xFFFF
                ? 2 + 0xFFFF
                : 2 + BYTES.length;
    }

    /**
     * @see java.lang.String#getBytes()
     * @see resaver.IString#getUTF8()
     * @return An array of bytes representing the <code>IString</code>.
     */
    @Override
    public byte[] getUTF8() {
        return this.RAW_BYTES == null
                ? super.getUTF8()
                : this.RAW_BYTES;
    }
    
    final private byte[] RAW_BYTES;
    
    /**
     * Tests for case-insensitive value-equality with another
     * <code>TString</code>, <code>IString</code>, or <code>String</code>.
     *
     * @param obj The object to which to compare.
     * @see java.lang.String#equalsIgnoreCase(java.lang.String)
     */
    //@Override
    //public boolean equals(Object obj) {
    //    return super.equals(obj);
    //}

}
