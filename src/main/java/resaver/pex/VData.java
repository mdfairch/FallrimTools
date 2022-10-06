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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import resaver.pex.StringTable.TString;

/**
 * Describes the data stored by a variable, property, or parameter.
 *
 * @author Mark Fairchild
 */
abstract public class VData {

    /**
     * Creates a <code>VData</code> by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The string table.
     * @return The new <code>VData</code>.
     * @throws IOException Exceptions aren't handled.
     */
    static public VData readVariableData(ByteBuffer input, StringTable strings) throws IOException {
        final DataType TYPE = DataType.read(input);

        switch (TYPE) {
            case NONE:
                return new None();
            case IDENTIFIER: {
                int index = Short.toUnsignedInt(input.getShort());
                if (index < 0 || index >= strings.size()) {
                    throw new IOException();
                }
                return new ID(strings.get(index));
            }
            case STRING: {
                int index = Short.toUnsignedInt(input.getShort());
                if (index < 0 || index >= strings.size()) {
                    throw new IOException();
                }
                return new Str(strings.get(index));
            }
            case INTEGER: {
                int val = input.getInt();
                return new Int(val);
            }
            case FLOAT: {
                float val = input.getFloat();
                return new Flt(val);
            }
            case BOOLEAN: {
                boolean val = input.get() != 0;
                return new Bool(val);
            }
            default:
                throw new IOException();
        }
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @param strings The string table.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    abstract void write(ByteBuffer output) throws IOException;

    /**
     * Calculates the size of the VData, in bytes.
     *
     * @return The size of the VData.
     *
     */
    abstract int calculateSize();

    /**
     * Collects all of the strings used by the VData and adds them to a set.
     *
     * @param strings The set of strings.
     */
    void collectStrings(Set<TString> strings) {
    }

    /**
     * The <code>VData</code> is a <code>Term</code>, returns it encloded in
     * brackets. Otherwise it is identical to <code>toString()</code>.
     */
    String paren() {
        return this.toString();
    }

    /**
     * @return Returns the type of the VData.
     *
     */
    abstract public DataType getType();

    /**
     * VData that stores nothing.
     */
    static public class None extends VData {

        private None() {
        }

        @Override
        void write(ByteBuffer output) throws IOException {
            output.put((byte) this.getType().ordinal());
        }

        @Override
        int calculateSize() {
            return 1;
        }

        @Override
        public DataType getType() {
            return DataType.NONE;
        }

        @Override
        public String toString() {
            return "NONE";
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + None.class.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            return true;
        }

    }

    /**
     * VData that stores an identifier.
     */
    static public class ID extends VData {

        ID(TString val) {
            this.value = Objects.requireNonNull(val);
        }

        @Override
        void write(ByteBuffer output) throws IOException {
            output.put((byte) this.getType().ordinal());
            this.value.write(output);
        }

        @Override
        int calculateSize() {
            return 3;
        }

        @Override
        public void collectStrings(Set<TString> strings) {
            strings.add(this.value);
        }

        @Override
        public DataType getType() {
            return DataType.IDENTIFIER;
        }

        @Override
        public String toString() {
            //return String.format("ID[%s]", this.VALUE);
            return this.value.toString();
        }

        public TString getValue() {
            return this.value;
        }

        void setValue(TString val) {
            this.value = Objects.requireNonNull(val);
        }

        public boolean isTemp() {
            return TEMP_PATTERN.test(this.value.toString())
                    && !AUTOVAR_PATTERN.test(this.value.toString())
                    && !NONE_PATTERN.test(this.value.toString());
        }

        public boolean isAutovar() {
            return AUTOVAR_PATTERN.test(this.value.toString());
        }

        public boolean isNonevar() {
            return NONE_PATTERN.test(this.value.toString());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + this.value.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            final ID other = (ID) obj;
            return Objects.equals(this.value, other.value);
        }

        private TString value;
        static final Predicate<String> TEMP_PATTERN = Pattern.compile("^::.+$", Pattern.CASE_INSENSITIVE).asPredicate();
        static final Predicate<String> NONE_PATTERN = Pattern.compile("^::NoneVar$", Pattern.CASE_INSENSITIVE).asPredicate();
        static final Predicate<String> AUTOVAR_PATTERN = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE).asPredicate();
    }

    /**
     * VData that stores a string.
     */
    static public class Str extends VData {

        private Str(TString val) {
            this.VALUE = Objects.requireNonNull(val);
        }

        @Override
        void write(ByteBuffer output) throws IOException {
            output.put((byte) this.getType().ordinal());
            this.VALUE.write(output);
        }

        @Override
        int calculateSize() {
            return 3;
        }

        @Override
        void collectStrings(Set<TString> strings) {
            strings.add(this.VALUE);
        }

        @Override
        public DataType getType() {
            return DataType.STRING;
        }

        @Override
        public String toString() {
            return String.format("\"%s\"", this.VALUE);
        }

        public TString getString() {
            return this.VALUE;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + this.VALUE.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            final Str other = (Str) obj;
            return Objects.equals(this.VALUE, other.VALUE);
        }

        final private TString VALUE;
    }

    /**
     * VData that stores an integer.
     */
    static public class Int extends VData {

        private Int(int val) {
            this.VALUE = val;
        }

        @Override
        void write(ByteBuffer output) throws IOException {
            output.put((byte) this.getType().ordinal());
            output.putInt(this.VALUE);
        }

        @Override
        int calculateSize() {
            return 5;
        }

        @Override
        public DataType getType() {
            return DataType.INTEGER;
        }

        @Override
        public String toString() {
            return String.format("%d", this.VALUE);
        }

        public int getValue() {
            return this.VALUE;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Integer.hashCode(this.VALUE);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            final Int other = (Int) obj;
            return this.VALUE == other.VALUE;
        }

        final private int VALUE;
    }

    /**
     * VData that stores a float.
     */
    static public class Flt extends VData {

        private Flt(float val) {
            this.VALUE = val;
        }

        @Override
        void write(ByteBuffer output) throws IOException {
            output.put((byte) this.getType().ordinal());
            output.putFloat(this.VALUE);
        }

        @Override
        int calculateSize() {
            return 5;
        }

        @Override
        public DataType getType() {
            return DataType.FLOAT;
        }

        @Override
        public String toString() {
            return String.format("%g", this.VALUE);
        }

        public float getValue() {
            return this.VALUE;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Float.hashCode(this.VALUE);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            final Flt other = (Flt) obj;
            return this.VALUE == other.VALUE;
        }

        final private float VALUE;
    }

    /**
     * VData that stores a boolean.
     */
    static public class Bool extends VData {

        private Bool(boolean val) {
            this.VALUE = val;
        }

        @Override
        void write(ByteBuffer output) throws IOException {
            output.put((byte) this.getType().ordinal());
            output.put(this.VALUE ? (byte) 1 : (byte) 0);
        }

        @Override
        int calculateSize() {
            return 2;
        }

        @Override
        public DataType getType() {
            return DataType.BOOLEAN;
        }

        @Override
        public String toString() {
            return String.format("%b", this.VALUE);
        }

        public boolean getValue() {
            return this.VALUE;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Boolean.hashCode(this.VALUE);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            final Bool other = (Bool) obj;
            return this.VALUE == other.VALUE;
        }

        final private boolean VALUE;
    }

    /**
     * VData that stores a "term", for disassembly purposes.
     */
    static class Term extends VData {

        public Term(String val) {
            this.VALUE = Objects.requireNonNull(val);
            this.PVALUE = "(" + this.VALUE + ")";
        }

        @Override
        void write(ByteBuffer output) throws IOException {
            throw new IllegalStateException("Not valid for Terms.");
        }

        @Override
        int calculateSize() {
            throw new IllegalStateException("Not valid for Terms.");
        }

        @Override
        public void collectStrings(Set<TString> strings) {
            throw new IllegalStateException("Not valid for Terms.");
        }

        @Override
        public DataType getType() {
            return DataType.IDENTIFIER;
        }

        @Override
        public String toString() {
            return this.VALUE;
        }

        public String getValue() {
            return this.VALUE;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.VALUE);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            final Term other = (Term) obj;
            return Objects.equals(this.VALUE, other.VALUE);
        }

        @Override
        public String paren() {
            return this.PVALUE;
        }

        final private String VALUE;
        final private String PVALUE;
    }

    /**
     * VData that stores a string literal, for disassembly purposes.
     */
    static class StrLit extends VData {

        public StrLit(String val) {
            this.VALUE = Objects.requireNonNull(val);
        }

        @Override
        void write(ByteBuffer output) throws IOException {
            throw new IllegalStateException("Not valid for Terms.");
        }

        @Override
        int calculateSize() {
            throw new IllegalStateException("Not valid for Terms.");
        }

        @Override
        public void collectStrings(Set<TString> strings) {
            throw new IllegalStateException("Not valid for Terms.");
        }

        @Override
        public DataType getType() {
            return DataType.STRING;
        }

        @Override
        public String toString() {
            return ("\"" + this.VALUE + "\"").replace("\n", "\\n");
        }

        public String getValue() {
            return this.VALUE;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.VALUE);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            final Term other = (Term) obj;
            return Objects.equals(this.VALUE, other.VALUE);
        }

        final private String VALUE;
    }

}
