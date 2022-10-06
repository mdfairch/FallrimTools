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
package resaver.pex;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.Objects;

/**
 * Describes the different opcodes that can appear in PEX functions, and stores
 * the number of arguments that they accept. If the number of arguments is
 * negative, it indicates that a variable number of arguments can follow the
 * main arguments.
 *
 * @author Mark Fairchild
 */
public enum Opcode {
    NOP(0), // 0    00
    IADD(3), // 1    01
    FADD(3), // 2    02
    ISUB(3), // 3    03
    FSUB(3), // 4    04
    IMUL(3), // 5    05
    FMUL(3), // 6    06
    IDIV(3), // 7    07
    FDIV(3), // 8    08
    IMOD(3), // 9    09
    NOT(2), // 10   0a
    INEG(2), // 11   0b
    FNEG(2), // 12   0c
    ASSIGN(2), // 13   0d
    CAST(2), // 14   0e
    CMP_EQ(3), // 15   0f
    CMP_LT(3), // 16   10
    CMP_LE(3), // 17   11
    CMP_GT(3), // 18   12
    CMP_GE(3), // 19   13
    JMP(1), // 20   14
    JMPT(2), // 21   15
    JMPF(2), // 22   16
    CALLMETHOD(-3), // 23   17
    CALLPARENT(-2), // 24   18
    CALLSTATIC(-3), // 25   19
    RETURN(1), // 26   1a
    STRCAT(3), // 27   1b
    PROPGET(3), // 28   1c
    PROPSET(3), // 29   1d
    ARR_CREATE(2), // 30   1e
    ARR_LENGTH(2), // 31   1f
    ARR_GET(3), // 32   20
    ARR_SET(3), // 33   21
    ARR_FIND(4), // 34   22
    ARR_RFIND(4), // 35   23
    IS(3), // 36   24
    STRUCT_CREATE(1), // 37   25
    STRUCT_GET(3), // 38   26
    STRUCT_SET(3), // 39   27
    ARR_STRUCT_FIND(5), // 40   28
    ARR_STRUCT_RFIND(3), // 41   29
    ARR_ADD(3), // 42   2a
    ARR_INSERT(3), // 43   2b
    ARR_REMOVE_LAST(1), // 44   2c
    ARR_REMOVE(3), // 45   2d
    ARR_CLEAR(1), // 46   2e
    ARR_GET_MATCHING(6); // 47  2f

    public boolean isConditional() {
        return (this == JMPT) || (this == JMPF);
    }

    public boolean isBranching() {
        return (this == JMPT) || (this == JMPF) || (this == JMP);
    }

    public boolean isArithmetic() {
        return this == IADD || this == FADD
                || this == ISUB || this == FSUB
                || this == IMUL || this == FMUL
                || this == IDIV || this == FDIV
                || this == IMOD;
    }

    private Opcode(int args) {
        this.ARGS = args;
    }

    /**
     * Read a <code>DataType</code> from an input stream.
     *
     * @param input The input stream.
     * @return The <code>DataType</code>.
     */
    static Opcode read(ByteBuffer input) throws IOException {
        Objects.requireNonNull(input);

        int index = Byte.toUnsignedInt(input.get());
        if (index < 0 || index >= VALUES.length) {
            throw new IOException("Invalid Opcode.");
        }

        return VALUES[index];
    }

    /**
     * @return A flag indicating whether the <code>Opcode</code> takes a
     * variable number of arguments.
     */
    public boolean hasExtraTerms() {
        return this.ARGS < 0;
    }

    /**
     * @return The number of fixed terms.
     */
    public int getFixedCount() {
        return this.hasExtraTerms()
                ? 1 - this.ARGS
                : this.ARGS;
    }

    public final int ARGS;
    static final private Opcode[] VALUES = Opcode.values();

}
