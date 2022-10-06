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
 * Describes the eleven types of variable data.
 *
 * @author Mark Fairchild
 */
public enum Type implements PapyrusElement {
    NULL,
    REF,
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    VARIANT,
    STRUCT,
    INVALID_8(false),
    INVALID_9(false),
    INVALID_10(false),
    REF_ARRAY,
    STRING_ARRAY,
    INTEGER_ARRAY,
    FLOAT_ARRAY,
    BOOLEAN_ARRAY,
    VARIANT_ARRAY,
    STRUCT_ARRAY;

    /**
     * Read a <code>Type</code> from an input stream.
     *
     * @param input The input stream.
     * @return The <code>Type</code>.
     * @throws PapyrusFormatException
     */
    static Type read(ByteBuffer input) throws PapyrusFormatException {
        Objects.requireNonNull(input);

        int val = input.get();
        if (val < 0 || val >= VALUES.length) {
            throw new PapyrusFormatException("Invalid type value: " + val);
        }

        final Type T = VALUES[val];
        if (!T.VALID) {
            throw new PapyrusFormatException("Invalid type value: " + T);
        }

        return T;
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);
        output.put((byte) this.CODE);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 1;
    }

    /**
     * @return True if the <code>Type</code> is an array type, false otherwise.
     */
    public boolean isArray() {
        return (this.CODE >= 10);
    }

    /**
     * @return True iff the <code>Type</code> is a reference type.
     */
    public boolean isRefType() {
        return this == REF || this == REF_ARRAY
                || this == STRUCT || this == STRUCT_ARRAY;
        //|| this == UNKNOWN6 || this == UNKNOWN6_ARRAY;
    }

    /**
     * Create a new <code>Type</code>.
     *
     * @param val
     */
    private Type() {
        this(true);
    }

    /**
     * Create a new <code>Type</code>.
     *
     * @param valid
     */
    private Type(boolean valid) {
        this.VALID = valid;
        this.CODE = this.ordinal();
    }

    final private boolean VALID;
    final private int CODE;
    static final private Type[] VALUES = values();
}
