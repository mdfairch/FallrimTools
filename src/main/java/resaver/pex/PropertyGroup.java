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
import resaver.pex.StringTable.TString;

/**
 * Describes the debugging information for a property group.
 *
 */
final class PropertyGroup {

    /**
     * Creates a DebugFunction by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The <code>StringTable</code> for the <code>PexFile</code>.
     * @throws IOException Exceptions aren't handled.
     */
    PropertyGroup(ByteBuffer input, StringTable strings) throws IOException {
        this.OBJECTNAME = strings.read(input);
        this.GROUPNAME = strings.read(input);
        this.DOCSTRING = strings.read(input);
        this.USERFLAGS = input.getInt();

        int propertyCount = Short.toUnsignedInt(input.getShort());
        this.PROPERTIES = new ArrayList<>(propertyCount);
        for (int i = 0; i < propertyCount; i++) {
            this.PROPERTIES.add(strings.read(input));
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
        this.GROUPNAME.write(output);
        this.DOCSTRING.write(output);
        output.putInt(this.USERFLAGS);
        output.putShort((short) this.PROPERTIES.size());
        for (TString prop : this.PROPERTIES) {
            prop.write(output);
        }
    }

    /**
     * Collects all of the strings used by the DebugFunction and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    public void collectStrings(Set<StringTable.TString> strings) {
        strings.add(this.OBJECTNAME);
        strings.add(this.GROUPNAME);
        strings.add(this.DOCSTRING);
        strings.addAll(this.PROPERTIES);
    }

    /**
     * Generates a qualified name for the object of the form "OBJECT.FUNCTION".
     *
     * @return A qualified name.
     *
     */
    public IString getFullName() {
        return IString.format("%s.%s", this.OBJECTNAME, this.GROUPNAME);
    }

    /**
     * @return The size of the <code>PropertyGroup</code>, in bytes.
     *
     */
    public int calculateSize() {
        return 12 + 2 * this.PROPERTIES.size();
    }
    
    /**
     * Pretty-prints the DebugFunction.
     *
     * @return A string representation of the DebugFunction.
     */
    @Override
    public String toString() {
        return String.format("%s.%s []", this.OBJECTNAME, this.GROUPNAME, this.PROPERTIES.toString());
    }

    final private TString OBJECTNAME;
    final private TString GROUPNAME;
    final private TString DOCSTRING;
    final private int USERFLAGS;
    private final List<TString> PROPERTIES;

}
