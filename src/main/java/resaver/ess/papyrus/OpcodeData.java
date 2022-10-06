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

import java.nio.BufferUnderflowException;
import resaver.ListException;
import java.util.List;
import java.util.Objects;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import resaver.pex.Opcode;

/**
 * Describes a script member in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class OpcodeData implements PapyrusElement {

    /**
     * A reusable instance of the NOP instruction.
     */
    static final public OpcodeData NOP = new OpcodeData();

    /**
     * Creates a new <code>OpcodeData</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     * @throws ListException
     */
    public OpcodeData(ByteBuffer input, PapyrusContext context) throws ListException, PapyrusFormatException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        int code = input.get();
        if (code < 0 || code >= OPCODES.length) {
            throw new PapyrusFormatException("Invalid opcode: " + code);
        }

        this.OPCODE = OPCODES[code];
        final int FIXED_COUNT = OPCODE.getFixedCount();
        this.PARAMETERS = new java.util.ArrayList<>(FIXED_COUNT);

        for (int i = 0; i < FIXED_COUNT; i++) {
            try {
                Parameter var = Parameter.read(input, context);
                this.PARAMETERS.add(var);
            } catch (PapyrusFormatException | BufferUnderflowException ex) {
                throw new ListException(i, FIXED_COUNT, ex);
            }
        }

        if (this.OPCODE.hasExtraTerms() && !this.PARAMETERS.isEmpty()) {
            final Parameter ARGS_PARAMETER = this.PARAMETERS.get(this.PARAMETERS.size() - 1);
            final int VAR_COUNT = ARGS_PARAMETER.getIntValue();

            for (int i = 0; i < VAR_COUNT; i++) {
                try {
                    Parameter var = Parameter.read(input, context);
                    this.PARAMETERS.add(var);
                } catch (PapyrusFormatException | BufferUnderflowException ex) {
                    throw new ListException(i + FIXED_COUNT, VAR_COUNT + FIXED_COUNT, ex);
                }
            }
        }
    }

    /**
     * Creates a new <code>OpcodeData</code> for the NOP instruction.
     */
    private OpcodeData() {
        this.OPCODE = Opcode.NOP;
        this.PARAMETERS = new java.util.ArrayList<>(0);
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put((byte) this.OPCODE.ordinal());
        this.PARAMETERS.forEach(var -> var.write(output));
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 1;
        sum += this.PARAMETERS.stream().mapToInt(var -> var.calculateSize()).sum();
        return sum;
    }

    /**
     * @return The opcode.
     */
    public Opcode getOpcode() {
        return this.OPCODE;
    }

    /**
     * @return The list of instruction parameters.
     */
    public List<Parameter> getParameters() {
        return java.util.Collections.unmodifiableList(this.PARAMETERS);
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder BUILDER = new StringBuilder();
        BUILDER.append(this.OPCODE);
        this.PARAMETERS.forEach(p -> BUILDER.append(' ').append(p.toValueString()));
        return BUILDER.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.OPCODE);
        hash = 29 * hash + Objects.hashCode(this.PARAMETERS);
        return hash;
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
        final OpcodeData other = (OpcodeData) obj;
        if (this.OPCODE != other.OPCODE) {
            return false;
        }
        return Objects.equals(this.PARAMETERS, other.PARAMETERS);
    }

    final private Opcode OPCODE;
    final private List<Parameter> PARAMETERS;
    static final private Opcode[] OPCODES = Opcode.values();

}
