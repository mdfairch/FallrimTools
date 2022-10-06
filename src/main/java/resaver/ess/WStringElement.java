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
import resaver.WString;

/**
 * Extends <code>IString</code> by handling charsets and storing the original
 * byte sequence.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class WStringElement extends WString implements Element {

    /**
     * Creates a new <code>WStringElement</code> by reading from a
     * <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @return The new <code>WStringElement</code>.
     */
    static public WStringElement read(ByteBuffer input) {
        final byte[] BYTES = mf.BufferUtil.getWStringRaw(input);
        return new WStringElement(BYTES);
    }

    /**
     * Copy constructor.
     *
     * @param other The original <code>WString</code>.
     */
    public WStringElement(WStringElement other) {
        super(other);
    }

    /**
     * Creates a new <code>WStringElement</code> from a character sequence; the
     * byte array is generated from the string using UTF-8 encoding.
     *
     * @param cs The <code>CharSequence</code>.
     */
    public WStringElement(CharSequence cs) {
        super(cs);
    }

    /**
     * Creates a new <code>WStringElement</code> from a character sequence and a
     * byte array.
     *
     * @param cs The <code>CharSequence</code>.
     * @param bytes The byte array.
     */
    private WStringElement(byte[] bytes) {
        super(bytes);
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        super.write(output);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return super.calculateSize();
    }

}
