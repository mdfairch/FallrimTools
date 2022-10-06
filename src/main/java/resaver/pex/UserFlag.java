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
import java.util.Set;
import resaver.pex.StringTable.TString;

/**
 * Describes a user flag from a PEX file.
 *
 * @author Mark Fairchild
 */
final public class UserFlag {

    /**
     * Creates a UserFlag by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The <code>StringTable</code> for the <code>Pex</code>.
     * @throws IOException Exceptions aren't handled.
     */
    UserFlag(ByteBuffer input, StringTable strings) throws IOException {
        this.NAME = strings.read(input);
        this.FLAGINDEX = input.get();
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    void write(ByteBuffer output) throws IOException {
        this.NAME.write(output);
        output.put(this.FLAGINDEX);
    }

    /**
     * Collects all of the strings used by the UserFlag and adds them to a set.
     *
     * @param strings The set of strings.
     */
    public void collectStrings(Set<TString> strings) {
        strings.add(this.NAME);
    }

    /**
     * Tests if a userFlags field includes this UserFlag.
     *
     * @param userFlags A userFlags field.
     * @return True if the field includes this UserFlag, false otherwise.
     */
    public boolean matches(int userFlags) {
        return ((userFlags >>> this.FLAGINDEX) & 1) != 0;
    }

    /**
     * @return The size of the <code>UserFlag</code>, in bytes.
     *
     */
    public int calculateSize() {
        return 3;
    }
    
    /**
     * Pretty-prints the UserFlag.
     *
     * @return A string representation of the UserFlag.
     */
    @Override
    public String toString() {
        final String FORMAT = "%s";
        return String.format(FORMAT, this.NAME);
    }

    final private TString NAME;
    final private byte FLAGINDEX;

}
