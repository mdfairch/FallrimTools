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

import java.util.Objects;
import java.nio.ByteBuffer;

/**
 * Describes an EID of a papyrus element.
 *
 * @author Mark Fairchild
 */
abstract public class EID implements PapyrusElement, Comparable<EID> {

    /**
     * Reads a four-byte <code>EID</code> from a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @param pap The <code>Papyrus</code> structure to which the
     * <code>EID</code> belongs.
     * @return The <code>EID</code>.
     */
    static public EID read4byte(ByteBuffer input, Papyrus pap) {
        Objects.requireNonNull(input);
        final int VAL = input.getInt();
        return make4byte(VAL, pap);
    }

    /**
     * Reads an eight-byte <code>EID</code> from a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @param pap The <code>Papyrus</code> structure to which the
     * <code>EID</code> belongs.
     * @return The <code>EID</code>.
     */
    static public EID read8byte(ByteBuffer input, Papyrus pap) {
        Objects.requireNonNull(input);
        final long VAL = input.getLong();
        return make8Byte(VAL, pap);
    }

    /**
     * Makes a four-byte <code>EID</code> from an int.
     *
     * @param val The id value.
     * @param pap The <code>Papyrus</code> structure to which the
     * <code>EID</code> belongs.
     * @return The <code>EID</code>.
     */
    static public EID make4byte(int val, Papyrus pap) {
        return pap.EIDS.computeIfAbsent(val, v -> new EID32(val));
    }

    /**
     * Makes an eight-byte <code>EID</code> from a long.
     *
     * @param val The id value.
     * @param pap The <code>Papyrus</code> structure to which the
     * <code>EID</code> belongs.
     * @return The <code>EID</code>.
     */
    static public EID make8Byte(long val, Papyrus pap) {
        return pap.EIDS.computeIfAbsent(val, v -> new EID64(val));
    }

    /**
     * An implementation of EID for 32 bit IDs.
     */
    static final public class EID32 extends EID {

        private EID32(int val) {
            this.VALUE = val;
        }

        @Override
        public long longValue() {
            return this.VALUE;
        }

        @Override
        public void write(ByteBuffer output) {
            output.putInt(this.VALUE);
        }

        @Override
        public int calculateSize() {
            return 4;
        }

        @Override
        public String toString() {
            return pad8(this.VALUE);
        }

        @Override
        public boolean is4Byte() {
            return true;
        }

        @Override
        public EID derive(long id) {
            return new EID32((int) id);
        }

        final public int VALUE;
    }

    /**
     * An implementation of EID for 64 bit IDs.
     */
    static final public class EID64 extends EID {

        private EID64(long val) {
            this.VALUE = val;
        }

        @Override
        public long longValue() {
            return this.VALUE;
        }

        @Override
        public void write(ByteBuffer output) {
            output.putLong(this.VALUE);
        }

        @Override
        public boolean is8Byte() {
            return true;
        }

        @Override
        public int calculateSize() {
            return 8;
        }

        @Override
        public String toString() {
            return pad16(this.VALUE);
        }

        @Override
        public EID derive(long id) {
            return new EID64(id);
        }

        final public long VALUE;
    }

    /**
     * Creates a new <code>EID</code>.
     */
    private EID() {

    }

    /**
     * @return The <code>EID</code> as a 64bit int.
     */
    abstract public long longValue();

    /**
     * @return A flag indicating if the <code>EID</code> is undefined.
     *
     */
    public boolean isUndefined() {
        return this.isZero();
    }

    /**
     * @return A flag indicating if the <code>EID</code> is zero.
     *
     */
    public boolean isZero() {
        return this.longValue() == 0;
    }

    /**
     * Creates a new <code>EID</code> of the same size using a new value.
     *
     * @param id
     * @return
     */
    abstract public EID derive(long id);

    /**
     * @return A flag indicating if the <code>EID</code> is 4-byte.
     */
    public boolean is4Byte() {
        return false;
    }

    /**
     * @return A flag indicating if the <code>EID</code> is 8-byte.
     */
    public boolean is8Byte() {
        return false;
    }

    @Override
    public int compareTo(EID other) {
        Objects.requireNonNull(other);
        return Long.compareUnsigned(this.longValue(), other.longValue());
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.longValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EID)) {
            return false;
        }

        final EID other = (EID) obj;
        return this.longValue() == other.longValue();
    }

    /**
     * Pads an EID to return an 8 character hexadecimal string.
     *
     * @param id
     * @return
     */
    static public String pad8(int id) {
        String hex = Integer.toHexString(id);
        int length = hex.length();
        return ZEROES[8 - length] + hex;
    }

    /**
     * Pads an EID to return an 8 character hexadecimal string.
     *
     * @param id
     * @return
     */
    static public String pad16(long id) {
        String hex = Long.toHexString(id);
        int length = hex.length();
        return ZEROES[16 - length] + hex;
    }

    /**
     * An array of strings of zeroes with the length matching the index.
     */
    static final private String[] ZEROES = makeZeroes();

    static private String[] makeZeroes() {
        String[] zeroes = new String[16];
        zeroes[0] = "";

        for (int i = 1; i < zeroes.length; i++) {
            zeroes[i] = zeroes[i - 1] + "0";
        }

        return zeroes;
    }

}
