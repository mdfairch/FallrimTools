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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Describes a local variable or parameter of a Function. A VariableType is
 * essential an ordered pair consisting of a name and a type.
 *
 * @author Mark Fairchild
 */
final class VariableType {

    /**
     * The role of the <code>VariableType</code>.
     */
    static public enum Role {
        PARAM, LOCAL
    };

    /**
     * Creates a VariableType by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The <code>StringTable</code> for the <code>Pex</code>.
     * @throws IOException Exceptions aren't handled.
     */
    static public VariableType readLocal(ByteBuffer input, StringTable strings) throws IOException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(strings);
        return new VariableType(input, strings, Role.LOCAL);
    }

    /**
     * Creates a VariableType by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The <code>StringTable</code> for the <code>Pex</code>.
     * @throws IOException Exceptions aren't handled.
     */
    static public VariableType readParam(ByteBuffer input, StringTable strings) throws IOException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(strings);
        return new VariableType(input, strings, Role.PARAM);
    }

    /**
     * Creates a VariableType by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The <code>StringTable</code> for the <code>Pex</code>.
     * @param role The role, as a function parameter or local variable.
     * @throws IOException Exceptions aren't handled.
     */
    private VariableType(ByteBuffer input, StringTable strings, Role role) throws IOException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(strings);
        Objects.requireNonNull(role);
        this.name = strings.read(input);
        this.TYPE = strings.read(input);
        this.ROLE = role;
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    void write(ByteBuffer output) throws IOException {
        this.name.write(output);
        this.TYPE.write(output);
    }

    /**
     * Calculates the size of the VariableType, in bytes.
     *
     * @return The size of the VariableType.
     *
     */
    public int calculateSize() {
        return 4;
    }

    /**
     * @return A flag indicating whether the <code>VariableType</code> is a temp
     * or not.
     */
    public boolean isTemp() {
        return TEMP_PATTERN.asPredicate().test(this.name.toString());
    }

    /**
     * Collects all of the strings used by the VariableType and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    public void collectStrings(Set<StringTable.TString> strings) {
        strings.add(this.name);
        strings.add(this.TYPE);
    }

    /**
     * Pretty-prints the VariableType.
     *
     * @return A string representation of the VariableType.
     */
    @Override
    public String toString() {
        final String FORMAT = "%s %s";
        return String.format(FORMAT, this.TYPE, this.name);
    }

    /**
     * @return Checks if the <code>VariableType</code> is a local variable.
     */
    public boolean isLocal() {
        return this.ROLE == Role.LOCAL;
    }
    
    /**
     * @return Checks if the <code>VariableType</code> is a parameter.
     */
    public boolean isParam() {
        return this.ROLE == Role.PARAM;
    }
    
    public StringTable.TString name;
    final public StringTable.TString TYPE;
    final public Role ROLE;

    static final Pattern TEMP_PATTERN = Pattern.compile("^::.+$");
}
