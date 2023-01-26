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
package resaver.esp;

import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.util.Objects;
import resaver.IString;

/**
 * FieldSimple represents all fields that aren't a VMAD section.
 *
 * @author Mark Fairchild
 */
public class FieldSimple implements Field, java.io.Serializable {

    /**
     * Creates a new FieldBasic by reading it from a <code>ByteBuffer</code>.
     *
     * @param code The field code.
     * @param input The <code>ByteBuffer</code> to read.
     * @param size The amount of data.
     * @param big A flag indicating that this is a BIG field.
     * @param ctx
     */
    public FieldSimple(IString code, ByteBuffer input, int size, boolean big, ESPContext ctx) {
        Objects.requireNonNull(input);
        this.SIZE = size;
        this.CODE = code;
        this.BIG = big;
        this.DATA = new byte[size];
        input.get(this.DATA);
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     * @param output The ByteBuffer.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.CODE.getUTF8());

        if (this.BIG) {
            short zero = 0;
            output.putShort(zero);
        } else {
            output.putShort((short) this.SIZE);
        }

        output.put(this.DATA);
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        return 6 + DATA.length;
    }

    /**
     * Returns the field code.
     *
     * @return The field code.
     */
    @Override
    public IString getCode() {
        return this.CODE;
    }

    /**
     * @return The underlying byte array.
     */
    public byte[] getData() {
        return this.DATA;
    }

    /**
     * Returns a copy of the data section in a <code>ByteBuffer</code>.
     *
     * @return A <code>ByteBuffer</code>
     */
    public ByteBuffer getByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(this.SIZE);
        buffer.put(this.DATA);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        ((Buffer) buffer).flip();
        return buffer;
    }

    /**
     * Returns a String representation of the Field, which will just be the code
     * string.
     *
     * @return A string representation.
     *
     */
    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();
        BUF.append(this.getCode()).append("=");

        for (int i = 0; i < this.DATA.length; i++) {
            BUF.append(String.format("%02x", this.DATA[i]));
        }

        return BUF.toString();
    }

    final private int SIZE;
    final private IString CODE;
    final private byte[] DATA;
    final boolean BIG;

}
