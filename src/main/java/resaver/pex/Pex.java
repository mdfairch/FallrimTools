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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import resaver.Game;
import resaver.IString;
import resaver.Scheme;
import resaver.pex.StringTable.TString;

/**
 * Describes an object from a PEX file.
 *
 * @author Mark Fairchild
 */
final public class Pex {

    /**
     * Creates a PexObject by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param game The game for which the script was compiled.
     * @param strings The <code>StringTable</code> for the <code>Pex</code>.
     * @param flag The <code>UserFlag</code> list.
     * @throws IOException Exceptions aren't handled.
     */
    Pex(ByteBuffer input, Game game, List<UserFlag> flags, StringTable strings) throws IOException {
        Objects.requireNonNull(input);
        this.GAME = game;
        this.USERFLAGDEFS = Objects.requireNonNull(flags);
        this.STRINGS = Objects.requireNonNull(strings);

        this.NAME = strings.read(input);
        this.size = input.getInt();
        this.PARENTNAME = strings.read(input);
        this.DOCSTRING = strings.read(input);

        if (game.isFO4()) {
            this.CONSTFLAG = input.get();
        } else {
            this.CONSTFLAG = -1;
        }

        this.USERFLAGS = input.getInt();
        this.AUTOSTATENAME = strings.read(input);
        this.AUTOVARMAP = new java.util.HashMap<>();

        if (game.isFO4()) {
            int numStructs = Short.toUnsignedInt(input.getShort());
            this.STRUCTS = new ArrayList<>(numStructs);
            for (int i = 0; i < numStructs; i++) {
                this.STRUCTS.add(new Struct(input, strings));
            }
        } else {
            this.STRUCTS = new ArrayList<>(0);
        }

        int numVariables = Short.toUnsignedInt(input.getShort());
        this.VARIABLES = new ArrayList<>(numVariables);
        for (int i = 0; i < numVariables; i++) {
            this.VARIABLES.add(new Variable(input, strings));
        }

        int numProperties = Short.toUnsignedInt(input.getShort());
        this.PROPERTIES = new ArrayList<>(numProperties);
        for (int i = 0; i < numProperties; i++) {
            this.PROPERTIES.add(new Property(input, strings));
        }

        int numStates = Short.toUnsignedInt(input.getShort());
        this.STATES = new ArrayList<>(numStates);
        for (int i = 0; i < numStates; i++) {
            this.STATES.add(new State(input, strings));
        }

        this.PROPERTIES.forEach(prop -> {
            if (prop.hasAutoVar()) {
                for (Variable var : this.VARIABLES) {
                    if (prop.AUTOVARNAME.equals(var.NAME)) {
                        this.AUTOVARMAP.put(prop, var);
                        break;
                    }
                }
                assert this.AUTOVARMAP.containsKey(prop);
            }

        });
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     *
     */
    void write(ByteBuffer output) throws IOException {
        this.NAME.write(output);

        this.size = this.calculateSize();
        output.putInt(this.size);

        this.PARENTNAME.write(output);
        this.DOCSTRING.write(output);

        if (this.GAME.isFO4()) {
            output.put(this.CONSTFLAG);
        }

        output.putInt(this.USERFLAGS);
        this.AUTOSTATENAME.write(output);

        if (this.GAME.isFO4()) {
            output.putShort((short) this.STRUCTS.size());
            for (Struct struct : this.STRUCTS) {
                struct.write(output);
            }
        }

        output.putShort((short) this.VARIABLES.size());
        for (Variable var : this.VARIABLES) {
            var.write(output);
        }

        output.putShort((short) this.PROPERTIES.size());
        for (Property prop : this.PROPERTIES) {
            prop.write(output);
        }

        output.putShort((short) this.STATES.size());
        for (State state : this.STATES) {
            state.write(output);
        }
    }

    /**
     * Calculates the size of the Pex, in bytes.
     *
     * @return The size of the Pex.
     *
     */
    public int calculateSize() {
        int sum = 0;
        sum += 4; // size
        sum += 2; // parentClassName
        sum += 2; // DOCSTRING
        sum += 4; // userFlags
        sum += 2; // autoStateName
        sum += 6; // array sizes
        sum += this.VARIABLES.stream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.PROPERTIES.stream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.STATES.stream().mapToInt(v -> v.calculateSize()).sum();

        if (this.GAME.isFO4()) {
            sum += 1;
            sum += 2;
            sum += this.STRUCTS.stream().mapToInt(v -> v.calculateSize()).sum();
        }

        return sum;
    }

    /**
     * Collects all of the strings used by the Pex and adds them to a set.
     *
     * @param strings The set of strings.
     */
    public void collectStrings(Set<TString> strings) {
        strings.add(this.NAME);
        strings.add(this.PARENTNAME);
        strings.add(this.DOCSTRING);
        strings.add(this.AUTOSTATENAME);
        this.STRUCTS.forEach(f -> f.collectStrings(strings));
        this.VARIABLES.forEach(f -> f.collectStrings(strings));
        this.PROPERTIES.forEach(f -> f.collectStrings(strings));
        this.STATES.forEach(f -> f.collectStrings(strings));
    }

    /**
     * Retrieves a set of the struct names in this <code>Pex</code>.
     *
     * @return A <code>Set</code> of struct names.
     *
     */
    public Set<IString> getStructNames() {
        return this.STRUCTS.stream().map(p -> p.NAME).collect(Collectors.toSet());
    }

    /**
     * Retrieves a set of the property names in this <code>Pex</code>.
     *
     * @return A <code>Set</code> of property names.
     *
     */
    public Set<IString> getPropertyNames() {
        return this.PROPERTIES.stream().map(p -> p.NAME).collect(Collectors.toSet());
    }

    /**
     * Retrieves a set of the variable names in this <code>Pex</code>.
     *
     * @return A <code>Set</code> of property names.
     *
     */
    public Set<IString> getVariableNames() {
        return this.VARIABLES.stream().map(p -> p.NAME).collect(Collectors.toSet());
    }

    /**
     * Retrieves a set of the function names in this <code>Pex</code>.
     *
     * @return A <code>Set</code> of function names.
     *
     */
    public Set<IString> getFunctionNames() {
        final Set<IString> NAMES = new java.util.HashSet<>();
        this.STATES.forEach(state -> state.FUNCTIONS.forEach(func -> NAMES.add(func.getFullName())));
        return NAMES;
    }

    /**
     * Returns a set of UserFlag objects matching a userFlags field.
     *
     * @param userFlags The flags to match.
     * @return The matching UserFlag objects.
     */
    public Set<UserFlag> getFlags(int userFlags) {
        final Set<UserFlag> FLAGS = new java.util.HashSet<>();

        this.USERFLAGDEFS.forEach(flag -> {
            if (flag.matches(userFlags)) {
                FLAGS.add(flag);
            }
        });

        return FLAGS;
    }

    /**
     * Tries to disassemble the script.
     *
     * @param code The code strings.
     * @param level Partial disassembly flag.
     */
    public void disassemble(List<String> code, AssemblyLevel level) {
        final StringBuilder S = new StringBuilder();

        if (this.PARENTNAME == null) {
            S.append(String.format("ScriptName %s", this.NAME));
        } else {
            S.append(String.format("ScriptName %s extends %s", this.NAME, this.PARENTNAME));
        }

        final Set<UserFlag> FLAGOBJS = this.getFlags(this.USERFLAGS);
        FLAGOBJS.forEach(flag -> S.append(" ").append(flag));
        code.add(S.toString());

        if (null != this.DOCSTRING && !this.DOCSTRING.isEmpty()) {
            code.add(String.format("{%s}\n", this.DOCSTRING));
        }

        code.add("");

        final Map<Property, Variable> AUTOVARS = new java.util.HashMap<>();
        this.PROPERTIES.stream().filter(p -> p.hasAutoVar()).forEach(p -> {
            this.VARIABLES.stream().filter(v -> v.NAME.equals(p.AUTOVARNAME)).forEach(v -> AUTOVARS.put(p, v));
        });

        List<Property> sortedProp = new ArrayList<>(this.PROPERTIES);
        sortedProp.sort((a, b) -> a.NAME.compareTo(b.NAME));
        sortedProp.sort((a, b) -> a.TYPE.compareTo(b.TYPE));

        List<Variable> sortedVars = new ArrayList<>(this.VARIABLES);
        sortedVars.sort((a, b) -> a.NAME.compareTo(b.NAME));
        sortedVars.sort((a, b) -> a.TYPE.compareTo(b.TYPE));

        code.add(";");
        code.add("; PROPERTIES");
        code.add(";");
        sortedProp.forEach(v -> v.disassemble(code, level, AUTOVARS));
        code.add("");
        code.add(";");
        code.add("; VARIABLES");
        code.add(";");
        sortedVars.stream().filter(v -> !AUTOVARS.containsValue(v)).forEach(v -> v.disassemble(code, level));
        code.add("");
        code.add(";");
        code.add("; STATES");
        code.add(";");
        this.STATES.forEach(v -> v.disassemble(code, level, this.AUTOSTATENAME.equals(v.NAME), AUTOVARS));
    }

    /**
     * Pretty-prints the Pex.
     *
     * @return A string representation of the Pex.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("ScriptName %s extends %s %s\n", this.NAME, this.PARENTNAME, getFlags(this.USERFLAGS)));
        buf.append(String.format("{%s}\n", this.DOCSTRING));
        buf.append(String.format("\tInitial state: %s\n", this.AUTOSTATENAME));
        buf.append("\n");

        this.PROPERTIES.forEach(prop -> buf.append(prop.toString()));
        this.VARIABLES.forEach(var -> buf.append(var.toString()));
        this.STATES.forEach(state -> buf.append(state.toString()));

        return buf.toString();
    }

    final public Game GAME;
    final public TString NAME;
    public int size;
    final public TString PARENTNAME;
    final public TString DOCSTRING;
    final public byte CONSTFLAG;
    final public int USERFLAGS;
    final public TString AUTOSTATENAME;
    final private List<Struct> STRUCTS;
    final private List<Variable> VARIABLES;
    final private List<Property> PROPERTIES;
    final private List<State> STATES;
    final private Map<Property, Variable> AUTOVARMAP;

    final private List<UserFlag> USERFLAGDEFS;
    final private StringTable STRINGS;

    /**
     * Describes a Struct from a PEX file.
     *
     */
    public final class Struct {

        /**
         * Creates a Struct by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The <code>StringTable</code> for the <code>Pex</code>.
         * @throws IOException Exceptions aren't handled.
         */
        private Struct(ByteBuffer input, StringTable strings) throws IOException {
            this.NAME = strings.read(input);

            int numMembers = Short.toUnsignedInt(input.getShort());
            this.MEMBERS = new ArrayList<>(numMembers);
            for (int i = 0; i < numMembers; i++) {
                this.MEMBERS.add(new Member(input, strings));
            }
        }

        /**
         * Write the this Struct to a <code>ByteBuffer</code>. No IO error
         * handling is performed.
         *
         * @param output The <code>ByteBuffer</code> to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        private void write(ByteBuffer output) throws IOException {
            this.NAME.write(output);

            output.putShort((short) this.MEMBERS.size());
            for (Member member : this.MEMBERS) {
                member.write(output);
            }
        }

        /**
         * Calculates the size of the Property, in bytes.
         *
         * @return The size of the Property.
         *
         */
        public int calculateSize() {
            int sum = 0;
            sum += 2; // NAME
            sum += 2; // Count
            sum += this.MEMBERS.stream().mapToInt(v -> v.calculateSize()).sum();
            return sum;
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        public void collectStrings(Set<TString> strings) {
            strings.add(this.NAME);
            this.MEMBERS.forEach(f -> f.collectStrings(strings));
        }

        /**
         * Generates a qualified NAME for the object.
         *
         * @return A qualified NAME.
         */
        public IString getFullName() {
            return IString.format("%s.%s", Pex.this.NAME, this.NAME);
        }

        final public TString NAME;
        final private List<Member> MEMBERS;

        /**
         * Describes a Member of a Struct.
         *
         */
        public final class Member {

            /**
             * Creates a Member by reading from a DataInput.
             *
             * @param input A datainput for a Skyrim PEX file.
             * @param strings The <code>StringTable</code> for the
             * <code>Pex</code>.
             * @throws IOException Exceptions aren't handled.
             */
            private Member(ByteBuffer input, StringTable strings) throws IOException {
                this.NAME = strings.read(input);
                this.TYPE = strings.read(input);
                this.USERFLAGS = input.getInt();
                this.VALUE = VData.readVariableData(input, strings);
                this.CONSTFLAG = input.get();
                this.DOC = strings.read(input);
            }

            /**
             * Write the this.ct to a <code>ByteBuffer</code>. No IO error
             * handling is performed.
             *
             * @param output The <code>ByteBuffer</code> to write.
             * @throws IOException IO errors aren't handled at all, they are
             * simply passed on.
             */
            private void write(ByteBuffer output) throws IOException {
                this.NAME.write(output);
                this.TYPE.write(output);
                output.putInt(this.USERFLAGS);
                this.VALUE.write(output);
                output.put(this.CONSTFLAG);
                this.DOC.write(output);
            }

            /**
             * Calculates the size of the Property, in bytes.
             *
             * @return The size of the Property.
             *
             */
            public int calculateSize() {
                int sum = 0;
                sum += 2; // NAME
                sum += 2; // type
                sum += 2; // docstring
                sum += 4; // userflags;
                sum += 1; // const flag
                sum += this.VALUE.calculateSize();
                return sum;
            }

            /**
             * Collects all of the strings used by the Function and adds them to
             * a set.
             *
             * @param strings The set of strings.
             */
            public void collectStrings(Set<TString> strings) {
                strings.add(this.NAME);
                strings.add(this.TYPE);
                strings.add(this.DOC);
                this.VALUE.collectStrings(strings);
            }

            /**
             * Generates a qualified NAME for the object.
             *
             * @return A qualified NAME.
             */
            public IString getFullName() {
                return IString.format("%s.%s.%s", Pex.this.NAME, Struct.this.NAME, this.NAME);
            }

            /**
             * Pretty-prints the Member.
             *
             * @return A string representation of the Member.
             */
            @Override
            public String toString() {
                StringBuilder buf = new StringBuilder();

                if (this.CONSTFLAG != 0) {
                    buf.append("const ");
                }

                buf.append(this.TYPE);
                buf.append(" ");
                buf.append(this.NAME);
                buf.append(" = ");
                buf.append(this.VALUE);
                return buf.toString();
            }

            final public TString NAME;
            final public TString TYPE;
            final public TString DOC;
            final public int USERFLAGS;
            final public byte CONSTFLAG;
            final public VData VALUE;
        }

    }

    /**
     * Describes a Property from a PEX file.
     *
     */
    public final class Property {

        /**
         * Creates a Property by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The <code>StringTable</code> for the <code>Pex</code>.
         * @throws IOException Exceptions aren't handled.
         */
        private Property(ByteBuffer input, StringTable strings) throws IOException {
            this.NAME = strings.read(input);
            this.TYPE = strings.read(input);
            this.DOC = strings.read(input);
            this.USERFLAGS = input.getInt();
            this.FLAGS = input.get();

            if (this.hasAutoVar()) {
                this.AUTOVARNAME = strings.read(input);
            } else {
                this.AUTOVARNAME = null;
            }

            if (this.hasReadHandler()) {
                this.READHANDLER = new Function(input, false, strings);
            } else {
                this.READHANDLER = null;
            }

            if (this.hasWriteHandler()) {
                this.WRITEHANDLER = new Function(input, false, strings);
            } else {
                this.WRITEHANDLER = null;
            }
        }

        /**
         * Write the this.ct to a <code>ByteBuffer</code>. No IO error handling
         * is performed.
         *
         * @param output The <code>ByteBuffer</code> to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        private void write(ByteBuffer output) throws IOException {
            this.NAME.write(output);
            this.TYPE.write(output);
            this.DOC.write(output);
            output.putInt(this.USERFLAGS);
            output.put(this.FLAGS);

            if (this.hasAutoVar()) {
                this.AUTOVARNAME.write(output);
            }

            if (this.hasReadHandler()) {
                this.READHANDLER.write(output);
            }

            if (this.hasWriteHandler()) {
                this.WRITEHANDLER.write(output);
            }
        }

        /**
         * Calculates the size of the Property, in bytes.
         *
         * @return The size of the Property.
         *
         */
        public int calculateSize() {
            int sum = 0;
            sum += 2; // NAME
            sum += 2; // type
            sum += 2; // docstring
            sum += 4; // userflags;
            sum += 1; // flags

            if (this.hasAutoVar()) {
                sum += 2; // autovarname
            }

            if (this.hasReadHandler()) {
                sum += this.READHANDLER.calculateSize();
            }
            if (this.hasWriteHandler()) {
                sum += this.WRITEHANDLER.calculateSize();
            }

            return sum;
        }

        /**
         * Indicates whether the <code>Property</code> is conditional.
         *
         * @return True if the <code>Property</code> is conditional, false
         * otherwise.
         */
        public boolean isConditional() {
            return (this.USERFLAGS & 2) != 0;
        }

        /**
         * Indicates whether the <code>Property</code> is conditional.
         *
         * @return True if the <code>Property</code> is conditional, false
         * otherwise.
         */
        public boolean isHidden() {
            return (this.USERFLAGS & 1) != 0;
        }

        /**
         * Indicates whether the <code>Property</code> has an autovar.
         *
         * @return True if the <code>Property</code> has an autovar, false
         * otherwise.
         */
        public boolean hasAutoVar() {
            return (this.FLAGS & 4) != 0;
        }

        /**
         * Indicates whether the <code>Property</code> has a read handler
         * function or not.
         *
         * @return True if the <code>Property</code> has a read handler, false
         * otherwise.
         */
        public boolean hasReadHandler() {
            return (this.FLAGS & 5) == 1;
        }

        /**
         * Indicates whether the <code>Property</code> has a write handler
         * function or not.
         *
         * @return True if the <code>Property</code> has a write handler, false
         * otherwise.
         */
        public boolean hasWriteHandler() {
            return (this.FLAGS & 6) == 2;
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        public void collectStrings(Set<TString> strings) {
            strings.add(this.NAME);
            strings.add(this.TYPE);
            strings.add(this.DOC);

            if (this.hasAutoVar()) {
                strings.add(this.AUTOVARNAME);
            }

            if (this.hasReadHandler()) {
                this.READHANDLER.collectStrings(strings);
            }

            if (this.hasWriteHandler()) {
                this.WRITEHANDLER.collectStrings(strings);
            }
        }

        /**
         * Generates a qualified NAME for the object.
         *
         * @return A qualified NAME.
         */
        public IString getFullName() {
            return IString.format("%s.%s", Pex.this.NAME, this.NAME);
        }

        /**
         * Tries to disassemble the Property.
         *
         * @param code The code strings.
         * @param level Partial disassembly flag.
         * @param autovars Map of properties to their autovariable.
         */
        public void disassemble(List<String> code, AssemblyLevel level, Map<Property, Variable> autovars) {
            Objects.requireNonNull(autovars);
            final StringBuilder S = new StringBuilder();

            S.append(String.format("%s Property %s", this.TYPE, this.NAME));

            if (autovars.containsKey(this) || this.hasAutoVar()) {
                assert autovars.containsKey(this);
                assert this.hasAutoVar();
                assert autovars.get(this).NAME.equals(this.AUTOVARNAME);

                final Variable AUTOVAR = autovars.get(this);
                if (AUTOVAR.DATA.getType() != DataType.NONE) {
                    S.append(" = ").append(AUTOVAR.DATA);
                }

                S.append(" AUTO");
                final Set<UserFlag> FLAGOBJS = Pex.this.getFlags(AUTOVAR.USERFLAGS);
                FLAGOBJS.forEach(flag -> S.append(" ").append(flag.toString()));
            }

            final Set<UserFlag> FLAGOBJS = Pex.this.getFlags(this.USERFLAGS);
            FLAGOBJS.forEach(flag -> S.append(" ").append(flag.toString()));

            if (autovars.containsKey(this) || this.hasAutoVar()) {
                final Variable AUTOVAR = autovars.get(this);
                S.append("  ;; --> ").append(AUTOVAR.NAME);
            }

            code.add(S.toString());

            if (null != this.DOC && !this.DOC.isEmpty()) {
                code.add(String.format("{%s}", this.DOC));
            }

            if (this.hasReadHandler()) {
                assert null != this.READHANDLER;
                this.READHANDLER.disassemble(code, level, "GET", autovars, 1);
            }

            if (this.hasWriteHandler()) {
                assert null != this.WRITEHANDLER;
                this.WRITEHANDLER.disassemble(code, level, "SET", autovars, 1);
            }

            if (this.hasReadHandler() || this.hasWriteHandler()) {
                code.add("EndProperty");
            }
        }

        /**
         * Pretty-prints the Property.
         *
         * @return A string representation of the Property.
         */
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(String.format("\tProperty %s %s", this.TYPE.toString(), this.NAME.toString()));

            if (this.hasAutoVar()) {
                buf.append(String.format(" AUTO(%s) ", this.AUTOVARNAME));
            }

            buf.append(getFlags(this.USERFLAGS));

            buf.append(String.format("\n\t\tDoc: %s\n", this.DOC));
            buf.append(String.format("\t\tFlags: %d\n", this.FLAGS));

            if (this.hasReadHandler()) {
                buf.append("ReadHandler: ");
                buf.append(this.READHANDLER.toString());
            }

            if (this.hasWriteHandler()) {
                buf.append("WriteHandler: ");
                buf.append(this.WRITEHANDLER.toString());
            }

            buf.append("\n");
            return buf.toString();
        }

        final public TString NAME;
        final public TString TYPE;
        final public TString DOC;
        final public int USERFLAGS;
        final public byte FLAGS;
        final public TString AUTOVARNAME;
        final private Function READHANDLER;
        final private Function WRITEHANDLER;
    }

    /**
     * Describes a State in a PEX file.
     *
     */
    public final class State {

        /**
         * Creates a State by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The <code>StringTable</code> for the <code>Pex</code>.
         * @throws IOException Exceptions aren't handled.
         */
        private State(ByteBuffer input, StringTable strings) throws IOException {
            this.NAME = strings.read(input);

            int numFunctions = Short.toUnsignedInt(input.getShort());
            this.FUNCTIONS = new ArrayList<>(numFunctions);
            for (int i = 0; i < numFunctions; i++) {
                this.FUNCTIONS.add(new Function(input, true, strings));
            }
        }

        /**
         * Write the object to a <code>ByteBuffer</code>.
         *
         * @param output The <code>ByteBuffer</code> to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        private void write(ByteBuffer output) throws IOException {
            this.NAME.write(output);
            output.putShort((short) this.FUNCTIONS.size());
            for (Function function : this.FUNCTIONS) {
                function.write(output);
            }
        }

        /**
         * Calculates the size of the State, in bytes.
         *
         * @return The size of the State.
         *
         */
        public int calculateSize() {
            int sum = 0;
            sum += 2; // NAME
            sum += 2; // array size
            sum += this.FUNCTIONS.stream().mapToInt(v -> v.calculateSize()).sum();
            return sum;
        }

        /**
         * Collects all of the strings used by the State and adds them to a set.
         *
         * @param strings The set of strings.
         */
        public void collectStrings(Set<TString> strings) {
            strings.add(this.NAME);
            this.FUNCTIONS.forEach(function -> function.collectStrings(strings));
        }

        /**
         * Tries to disassembleInstruction the script.
         *
         * @param code The code strings.
         * @param level Partial disassembly flag.
         * @param autostate A flag indicating if this state is the autostate.
         * @param autovars Map of properties to their autovariable.
         */
        public void disassemble(List<String> code, AssemblyLevel level, boolean autostate, Map<Property, Variable> autovars) {
            final Set<IString> RESERVED = new java.util.HashSet<>();
            RESERVED.add(IString.get("GoToState"));
            RESERVED.add(IString.get("GetState"));

            final StringBuilder S = new StringBuilder();

            if (null == this.NAME || this.NAME.isEmpty()) {
                S.append(";");
            }

            if (autostate) {
                S.append("AUTO ");
            }

            S.append("State ");
            S.append(this.NAME);
            code.add(S.toString());
            code.add("");

            final int INDENT = (autostate ? 0 : 1);

            this.FUNCTIONS.stream()
                    .filter(f -> !RESERVED.contains(f.NAME))
                    .forEach(f -> {
                        f.disassemble(code, level, null, autovars, INDENT);
                        code.add("");
                    });

            if (null == this.NAME || this.NAME.isEmpty()) {
                code.add(";EndState");
            } else {
                code.add("EndState");
            }

            code.add("");
        }

        /**
         * Pretty-prints the State.
         *
         * @return A string representation of the State.
         */
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(String.format("\tState %s\n", this.NAME));
            this.FUNCTIONS.forEach(function -> buf.append(function.toString()));
            return buf.toString();
        }

        final public TString NAME;
        final public List<Function> FUNCTIONS;

    }

    /**
     * Describes a Function and it's code.
     *
     */
    public final class Function {

        /**
         * Creates a Function by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param named A flag indicating whether to read a named function or a
         * nameless function.
         * @param strings The <code>StringTable</code> for the <code>Pex</code>.
         * @throws IOException Exceptions aren't handled.
         */
        private Function(ByteBuffer input, boolean named, StringTable strings) throws IOException {
            if (named) {
                this.NAME = strings.read(input);
            } else {
                this.NAME = null;
            }

            this.RETURNTYPE = strings.read(input);
            this.DOC = strings.read(input);
            this.USERFLAGS = input.getInt();
            this.FLAGS = input.get();

            int paramsCount = Short.toUnsignedInt(input.getShort());
            this.PARAMS = new ArrayList<>(paramsCount);
            for (int i = 0; i < paramsCount; i++) {
                this.PARAMS.add(VariableType.readParam(input, strings));
            }

            int localsCount = Short.toUnsignedInt(input.getShort());
            this.LOCALS = new ArrayList<>(localsCount);
            for (int i = 0; i < localsCount; i++) {
                this.LOCALS.add(VariableType.readLocal(input, strings));
            }

            int instructionsCount = Short.toUnsignedInt(input.getShort());
            this.INSTRUCTIONS = new ArrayList<>(instructionsCount);
            for (int i = 0; i < instructionsCount; i++) {
                this.INSTRUCTIONS.add(new Instruction(input, strings));
            }
        }

        /**
         * Write the object to a <code>ByteBuffer</code>. No IO error handling
         * is performed.
         *
         * @param output The <code>ByteBuffer</code> to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        private void write(ByteBuffer output) throws IOException {
            if (null != this.NAME) {
                this.NAME.write(output);
            }

            this.RETURNTYPE.write(output);
            this.DOC.write(output);
            output.putInt(this.USERFLAGS);
            output.put(this.FLAGS);

            output.putShort((short) this.PARAMS.size());
            for (VariableType vt : this.PARAMS) {
                vt.write(output);
            }

            output.putShort((short) this.LOCALS.size());
            for (VariableType vt : this.LOCALS) {
                vt.write(output);
            }

            output.putShort((short) this.INSTRUCTIONS.size());
            for (Instruction inst : this.INSTRUCTIONS) {
                inst.write(output);
            }
        }

        /**
         * Calculates the size of the Function, in bytes.
         *
         * @return The size of the Function.
         *
         */
        public int calculateSize() {
            int sum = 0;

            if (null != this.NAME) {
                sum += 2; // NAME
            }

            sum += 2; // returntype
            sum += 2; // docstring
            sum += 4; // userflags
            sum += 1; // flags
            sum += 6; // array sizes
            sum += this.PARAMS.stream().mapToInt(v -> v.calculateSize()).sum();
            sum += this.LOCALS.stream().mapToInt(v -> v.calculateSize()).sum();
            sum += this.INSTRUCTIONS.stream().mapToInt(v -> v.calculateSize()).sum();

            return sum;
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        public void collectStrings(Set<TString> strings) {
            if (null != this.NAME) {
                strings.add(this.NAME);
            }

            strings.add(this.RETURNTYPE);
            strings.add(this.DOC);

            this.PARAMS.forEach(param -> param.collectStrings(strings));
            this.LOCALS.forEach(local -> local.collectStrings(strings));
            this.INSTRUCTIONS.forEach(instr -> instr.collectStrings(strings));

        }

        /**
         * Generates a qualified NAME for the Function of the form
         * "OBJECT.FUNCTION".
         *
         * @return A qualified NAME.
         */
        public IString getFullName() {
            if (this.NAME != null) {
                return IString.format("%s.%s", Pex.this.NAME, this.NAME);
            } else {
                return IString.format("%s.()", Pex.this.NAME);
            }
        }

        /**
         * @return True if the function is global, false otherwise.
         */
        public boolean isGlobal() {
            return (this.FLAGS & 0x01) != 0;
        }

        /**
         * @return True if the function is native, false otherwise.
         */
        public boolean isNative() {
            return (this.FLAGS & 0x02) != 0;
        }

        /**
         * Tries to disassembleInstruction the script.
         *
         * @param code The code strings.
         * @param level Partial disassembly flag.
         * @param nameOverride Provides the function NAME; useful for functions
         * that don't have a NAME stored internally.
         * @param autovars A map of properties to their autovars.
         * @param indent The indent level.
         */
        public void disassemble(List<String> code, AssemblyLevel level, String nameOverride, Map<Property, Variable> autovars, int indent) {
            final StringBuilder S = new StringBuilder();

            S.append(Disassembler.tab(indent));

            if (null != this.RETURNTYPE && !this.RETURNTYPE.isEmpty() && !this.RETURNTYPE.equals("NONE")) {
                S.append(this.RETURNTYPE).append(" ");
            }

            if (null != nameOverride) {
                S.append(String.format("Function %s%s", nameOverride, Disassembler.paramList(this.PARAMS)));
            } else {
                S.append(String.format("Function %s%s", this.NAME, Disassembler.paramList(this.PARAMS)));
            }

            final Set<UserFlag> FLAGOBJS = Pex.this.getFlags(this.USERFLAGS);
            FLAGOBJS.forEach(flag -> S.append(String.format(" " + flag.toString())));

            if (this.isGlobal()) {
                S.append(" GLOBAL");
            }
            if (this.isNative()) {
                S.append(" NATIVE");
            }

            code.add(S.toString());

            if (null != this.DOC && !this.DOC.isEmpty()) {
                code.add(String.format("%s{%s}", Disassembler.tab(indent + 1), this.DOC));
            }

            Set<IString> GROUPS = this.LOCALS
                    .stream()
                    .filter(v -> v.isTemp())
                    .map(v -> v.TYPE)
                    .collect(Collectors.toSet());

            GROUPS.forEach(t -> {
                final StringBuilder DECL = new StringBuilder();
                DECL.append(Disassembler.tab(indent + 1));
                DECL.append("; ").append(t).append(' ');
                DECL.append(this.LOCALS
                        .stream()
                        .filter(v -> v.isTemp())
                        .filter(v -> v.TYPE == t)
                        .map(v -> v.name)
                        .collect(Collectors.joining(", ")));
                code.add(DECL.toString());
            });

            /*this.LOCALS.forEach(v -> {
                code.add(String.format("%s%s %s", Disassembler.tab(indent + 1), v.TYPE, v.NAME));
            });*/
            List<VariableType> types = new java.util.ArrayList<>(this.PARAMS);
            types.addAll(this.LOCALS);

            TermMap terms = new TermMap();
            autovars.forEach((p, v) -> {
                terms.put(new VData.ID(v.NAME), new VData.Term(p.NAME.toString()));
            });

            List<Instruction> block = new ArrayList<>(this.INSTRUCTIONS);

            switch (level) {
                case STRIPPED:
                    Disassembler.preMap(block, types, terms);
                    break;
                case BYTECODE:
                    Disassembler.preMap(block, types, terms);
                    block.forEach(v -> code.add(String.format("%s%s", Disassembler.tab(indent + 1), v)));
                    break;
                case FULL:
                    try {
                    Disassembler.preMap(block, types, terms);
                    List<String> code2 = Disassembler.disassemble(block, types, indent + 1);
                    code.addAll(code2);
                } catch (DisassemblyException ex) {
                    code.addAll(ex.getPartial());
                    final String MSG = String.format("Error disassembling %s.", this.getFullName());
                    throw new IllegalStateException(MSG, ex);
                }
            }

            code.add(String.format("%sEndFunction", Disassembler.tab(indent)));
        }

        /**
         * Pretty-prints the Function.
         *
         * @return A string representation of the Function.
         */
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();

            if (this.NAME != null) {
                buf.append(String.format("Function %s ", this.NAME));
            } else {
                buf.append("Function (UNNAMED) ");
            }

            buf.append(this.PARAMS.toString());
            buf.append(String.format(" returns %s\n", this.RETURNTYPE.toString()));
            buf.append(String.format("\tDoc: %s\n", this.DOC.toString()));
            buf.append(String.format("\tFlags: %s\n", getFlags(this.USERFLAGS)));
            buf.append("\tLocals: ");
            buf.append(this.LOCALS.toString());
            buf.append("\n\tBEGIN\n");

            this.INSTRUCTIONS.forEach(instruction -> {
                buf.append("\t\t");
                buf.append(instruction.toString());
                buf.append("\n");
            });

            buf.append("\tEND\n\n");

            return buf.toString();
        }

        final public TString NAME;
        final public TString RETURNTYPE;
        final public TString DOC;
        final public int USERFLAGS;
        final public byte FLAGS;
        final private List<VariableType> PARAMS;
        final private List<VariableType> LOCALS;
        final private List<Instruction> INSTRUCTIONS;

        /**
         * Describes a single executable Instruction in a Function.
         *
         */
        public final class Instruction {

            /**
             * Creates a new Instruction.
             *
             * @param code
             * @param args
             */
            public Instruction(Opcode code, List<VData> args) {
                this.OP = (byte) code.ordinal();
                this.OPCODE = code;
                this.ARGS = new ArrayList<>(args);
            }

            /**
             * Creates an Instruction by reading from a DataInput.
             *
             * @param input A datainput for a Skyrim PEX file.
             * @param strings The <code>StringTable</code> for the
             * <code>Pex</code>.
             * @throws IOException Exceptions aren't handled.
             */
            private Instruction(ByteBuffer input, StringTable strings) throws IOException {
                this.OPCODE = Opcode.read(input);
                this.OP = (byte) this.OPCODE.ordinal();

                if (this.OPCODE.ARGS > 0) {
                    this.ARGS = new ArrayList<>(this.OPCODE.ARGS);
                    for (int i = 0; i < OPCODE.ARGS; i++) {
                        this.ARGS.add(VData.readVariableData(input, strings));
                    }
                } else if (this.OPCODE.ARGS < 0) {
                    this.ARGS = new ArrayList<>(-this.OPCODE.ARGS);
                    for (int i = 0; i < 1 - this.OPCODE.ARGS; i++) {
                        this.ARGS.add(VData.readVariableData(input, strings));
                    }

                    VData count = this.ARGS.get(-this.OPCODE.ARGS);
                    if (!(count instanceof VData.Int)) {
                        throw new IOException("Invalid instruction");
                    }

                    int numVargs = ((VData.Int) count).getValue();
                    for (int i = 0; i < numVargs; i++) {
                        this.ARGS.add(VData.readVariableData(input, strings));
                    }

                } else {
                    this.ARGS = new ArrayList<>(0);
                }
            }

            /**
             * Write the object to a <code>ByteBuffer</code>.
             *
             * @param output The <code>ByteBuffer</code> to write.
             * @throws IOException IO errors aren't handled at all, they are
             * simply passed on.
             */
            private void write(ByteBuffer output) throws IOException {
                output.put(this.OP);

                for (VData vd : this.ARGS) {
                    vd.write(output);
                }
            }

            /**
             * Calculates the size of the Instruction, in bytes.
             *
             * @return The size of the Instruction.
             *
             */
            public int calculateSize() {
                int sum = 0;
                sum += 1; // opcode
                sum += ARGS.stream().mapToInt(v -> v.calculateSize()).sum();
                return sum;
            }

            /**
             * Collects all of the strings used by the Instruction and adds them
             * to a set.
             *
             * @param strings The set of strings.
             */
            public void collectStrings(Set<TString> strings) {
                this.ARGS.forEach(arg -> arg.collectStrings(strings));
            }

            /**
             * Pretty-prints the Instruction.
             *
             * @return A string representation of the Instruction.
             */
            @Override
            public String toString() {
                final String FORMAT = "%s %s";
                return String.format(FORMAT, this.OPCODE, this.ARGS);
            }

            /**
             * Checks for instruction arguments that are in a replacement
             * scheme, and replaces them.
             *
             * @param scheme The replacement scheme.
             *
             */
            public void remapVariables(Scheme scheme) {
                int firstArg;

                // These five instruction types include identifiers to
                // properties or functions, which are separate 
                // namespaces. We use firstArg to skip over those 
                // identifiers
                switch (this.OPCODE) {
                    case CALLSTATIC:
                        firstArg = 2;
                        break;
                    case CALLMETHOD:
                    case CALLPARENT:
                    case PROPGET:
                    case PROPSET:
                        firstArg = 1;
                        break;
                    default:
                        firstArg = 0;
                        break;
                }

                // Remap identifiers 
                for (int i = firstArg; i < this.ARGS.size(); i++) {
                    VData arg = this.ARGS.get(i);

                    if (arg.getType() == DataType.IDENTIFIER) {
                        VData.ID id = (VData.ID) arg;
                        if (scheme.containsKey(id.getValue())) {
                            IString newValue = scheme.get(id.getValue());
                            TString newStr = Pex.this.STRINGS.addString(newValue);
                            id.setValue(newStr);
                        }
                    }
                }
            }

            final public byte OP;
            final public Opcode OPCODE;
            final public List<VData> ARGS;
        }
    }

    /**
     * Describes a PEX file variable entry. A variable consists of a NAME, a
     * type, user flags, and VData.
     *
     */
    public final class Variable {

        /**
         * Creates a Variable by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The <code>StringTable</code> for the <code>Pex</code>.
         * @throws IOException Exceptions aren't handled.
         */
        private Variable(ByteBuffer input, StringTable strings) throws IOException {
            this.NAME = strings.read(input);
            this.TYPE = strings.read(input);
            this.USERFLAGS = input.getInt();
            this.DATA = VData.readVariableData(input, strings);

            if (Pex.this.GAME.isFO4()) {
                this.CONST = input.get();
            } else {
                this.CONST = 0;
            }
        }

        /**
         * Write the object to a <code>ByteBuffer</code>.
         *
         * @param output The <code>ByteBuffer</code> to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        private void write(ByteBuffer output) throws IOException {
            this.NAME.write(output);
            this.TYPE.write(output);
            output.putInt(this.USERFLAGS);
            this.DATA.write(output);

            if (Pex.this.GAME.isFO4()) {
                output.put(this.CONST);
            }
        }

        /**
         * Calculates the size of the VData, in bytes.
         *
         * @return The size of the VData.
         *
         */
        public int calculateSize() {
            int sum = 0;
            sum += 2; // NAME
            sum += 2; // type
            sum += 4; // userflags
            sum += this.DATA.calculateSize();
            if (Pex.this.GAME.isFO4()) {
                sum += 1;
            }
            return sum;
        }

        /**
         * Collects all of the strings used by the Variable and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        public void collectStrings(Set<TString> strings) {
            strings.add(this.NAME);
            strings.add(this.TYPE);
            this.DATA.collectStrings(strings);
        }

        /**
         * Indicates whether the <code>Property</code> is conditional.
         *
         * @return True if the <code>Property</code> is conditional, false
         * otherwise.
         */
        public boolean isConditional() {
            return (this.USERFLAGS & 2) != 0;
        }

        /**
         * Tries to disassemble Instruction the script.
         *
         * @param code The code strings.
         * @param level Partial disassembly flag.
         */
        public void disassemble(List<String> code, AssemblyLevel level) {
            final StringBuilder S = new StringBuilder();

            if (this.DATA.getType() != DataType.NONE) {
                S.append(String.format("%s %s = %s", this.TYPE, this.NAME, this.DATA));
            } else {
                S.append(String.format("%s %s", this.TYPE, this.NAME));
            }

            if (this.CONST != 0) {
                S.append(" ").append("const");
            }

            final Set<UserFlag> FLAGOBJS = Pex.this.getFlags(this.USERFLAGS);
            FLAGOBJS.forEach(flag -> S.append(" ").append(flag.toString()));
            code.add(S.toString());
        }

        /**
         * Pretty-prints the Variable.
         *
         * @return A string representation of the Variable.
         */
        @Override
        public String toString() {
            final String FORMAT = "\tVariable %s %s = %s %s\n\n";
            return String.format(FORMAT, this.TYPE, this.NAME, this.DATA, getFlags(this.USERFLAGS));
        }

        final public TString NAME;
        final public TString TYPE;
        final public int USERFLAGS;
        final public VData DATA;
        final public byte CONST;
    }

    static final private IString[] _EXCLUDED = new IString[]{IString.get("player"), IString.get("playerref")};
    static final java.util.Set<IString> EXCLUDED = new java.util.HashSet<>(Arrays.asList(_EXCLUDED));
}
