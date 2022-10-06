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
package resaver.ess;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A Skyrim variable-size value.
 *
 * @author Mark Fairchild
 */
final public class VSVal implements Element {

    /**
     * Creates a new <code>ChangeForm</code> from an integer.
     *
     * @param val The value.
     */
    public VSVal(int val) {
        if (val <= 0x40) {
            val <<= 2;
            this.DATA = new byte[1];
            this.DATA[0] = (byte) (val);
        } else if (val <= 0x4000) {
            val <<= 2;
            this.DATA = new byte[2];
            this.DATA[0] = (byte) (val | 0x1);
            this.DATA[1] = (byte) (val >> 8);
        } else if (val <= 0x40000000) {
            val <<= 2;
            this.DATA = new byte[3];
            this.DATA[0] = (byte) (val | 0x2);
            this.DATA[1] = (byte) (val >> 8);
            this.DATA[2] = (byte) (val >> 16);
        } else {
            throw new IllegalArgumentException("VSVal cannot stores values greater than 0x40000000: " + val);
        }
    }

    /**
     * Creates a new <code>ChangeForm</code> by reading from a
     * <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     */
    public VSVal(ByteBuffer input) {
        byte firstByte = input.get();
        int size = firstByte & 0x3;

        switch (size) {
            case 0:
                this.DATA = new byte[]{firstByte};
                break;
            case 1:
                this.DATA = new byte[]{firstByte, input.get()};
                break;
            default:
                this.DATA = new byte[]{firstByte, input.get(), input.get()};
                break;
        }
    }

    /**
     * @see resaver.ess.Element#write(ByteBuffer)
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
     * @return String representation.
     */
    @Override
    public String toString() {
        return String.format("%d", this.getValue());
    }

    /**
     * @return The value stored in the VSVal.
     */
    public int getValue() {
        int size = this.DATA[0] & 0x3;

        switch (size) {
            case 0: {
                int value = Byte.toUnsignedInt(this.DATA[0]) >>> 2;
                return value;
            }
            case 1: {
                int value = (Byte.toUnsignedInt(this.DATA[0])
                        | (Byte.toUnsignedInt(this.DATA[1]) << 8)) >>> 2;
                return value;
            }
            default: {
                int value = (Byte.toUnsignedInt(this.DATA[0]) 
                        | (Byte.toUnsignedInt(this.DATA[1]) << 8) 
                        | (Byte.toUnsignedInt(this.DATA[2]) << 16)) >>> 2;
                return value;
            }
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.DATA);
    }

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
        final VSVal other = (VSVal) obj;
        return Arrays.equals(this.DATA, other.DATA);
    }

    final private byte[] DATA;

}
