/*
 * Copyright 2018 Mark.
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
import java.util.List;
import java.util.Set;
import resaver.IString;

/**
 * Describes the debugging information for a function.
 *
 */
final class DebugFunction {

    /**
     * Creates a DebugFunction by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The <code>StringTable</code> for the <code>PexFile</code>.
     * @throws IOException Exceptions aren't handled.
     */
    DebugFunction(ByteBuffer input, StringTable strings) throws IOException {
        this.OBJECTNAME = strings.read(input);
        this.STATENAME = strings.read(input);
        this.FUNCNAME = strings.read(input);
        this.FUNCTYPE = input.get();
        int instructionCount = Short.toUnsignedInt(input.getShort());
        this.INSTRUCTIONS = new ArrayList<>(instructionCount);
        for (int i = 0; i < instructionCount; i++) {
            this.INSTRUCTIONS.add(Short.toUnsignedInt(input.getShort()));
        }
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    void write(ByteBuffer output) throws IOException {
        this.OBJECTNAME.write(output);
        this.STATENAME.write(output);
        this.FUNCNAME.write(output);
        output.put(this.FUNCTYPE);
        output.putShort((short) this.INSTRUCTIONS.size());
        this.INSTRUCTIONS.forEach(instr -> output.putShort(instr.shortValue()));
    }

    /**
     * Collects all of the strings used by the DebugFunction and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    public void collectStrings(Set<StringTable.TString> strings) {
        strings.add(this.OBJECTNAME);
        strings.add(this.STATENAME);
        strings.add(this.FUNCNAME);
    }

    /**
     * Generates a qualified name for the object of the form "OBJECT.FUNCTION".
     *
     * @return A qualified name.
     *
     */
    public IString getFullName() {
        return IString.format("%s.%s", this.OBJECTNAME, this.FUNCNAME);
    }

    /**
     * @return The size of the <code>DebugFunction</code>, in bytes.
     *
     */
    public int calculateSize() {
        return 9 + 2 * this.INSTRUCTIONS.size();
    }
    
    /**
     * Pretty-prints the DebugFunction.
     *
     * @return A string representation of the DebugFunction.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("%s %s.%s (type %d): ", this.OBJECTNAME, this.STATENAME, this.FUNCNAME, this.FUNCTYPE));
        this.INSTRUCTIONS.forEach((java.lang.Integer instr) -> buf.append(String.format("%04x ", instr)));
        return buf.toString();
    }

    final private StringTable.TString OBJECTNAME;
    final private StringTable.TString STATENAME;
    final private StringTable.TString FUNCNAME;
    final private byte FUNCTYPE;
    final private List<Integer> INSTRUCTIONS;

}
