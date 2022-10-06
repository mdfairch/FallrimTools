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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.nio.ByteBuffer;

/**
 * Describes a variable in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
public abstract class Parameter implements PapyrusElement {

    /**
     * Creates a new <code>Parameter</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @return The new <code>Parameter</code>.
     * @throws PapyrusFormatException
     */
    static public Parameter read(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        Type TYPE = Type.read(input);
        switch (TYPE) {
            case NULL:
                return new Null();
            case IDENTIFIER:
                TString id = context.readTString(input);
                return new ID(id);
            case STRING:
                TString str = context.readTString(input);
                return new Str(str);
            case INTEGER:
                int i = input.getInt();
                return new Int(i);
            case FLOAT:
                float f = input.getFloat();
                return new Flt(f);
            case BOOLEAN:
                byte b = input.get();
                return new Bool(b);
            case TERM:
                throw new IllegalStateException("Terms cannot be read.");
            case UNKNOWN8:
                TString u8 = context.readTString(input);
                return new Unk8(u8);
            default:
                throw new PapyrusFormatException("Illegal Parameter type: " + TYPE);
        }

    }

    /**
     * Creates a term, a label for doing substitutions.
     *
     * @param value
     * @return
     */
    static public Parameter createTerm(String value) {
        return new Term(value);
    }

    /**
     * @return The type of the parameter.
     */
    abstract public Type getType();

    /**
     * @return A flag indicating if the parameter is an identifier to a temp
     * variable.
     */
    public boolean isTemp() {
        return false;
    }

    /**
     * @return A flag indicating if the parameter is an Autovariable.
     */
    public boolean isAutovar() {
        return false;
    }

    /**
     * @return A flag indicating if the parameter is an None variable.
     */
    public boolean isNonevar() {
        return false;
    }

    /**
     * @return Returns the identifier value of the <code>Parameter</code>, if
     * possible.
     */
    public TString getIDValue() {
        if (this instanceof ID) {
            return ((ID) this).VALUE;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * @return Returns the string value of the <code>Parameter</code>, if
     * possible.
     */
    public TString getTStrValue() {
        if (this instanceof Str) {
            return ((Str) this).VALUE;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * @return Returns the integer value of the <code>Parameter</code>, if
     * possible.
     */
    public int getIntValue() {
        if (this instanceof Int) {
            return ((Int) this).VALUE;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * @return Short string representation.
     */
    abstract public String toValueString();

    /**
     * An appropriately parenthesized string form of the parameter.
     *
     * @return
     */
    public String paren() {
        if (this.getType() == Type.TERM) {
            return "(" + this.toValueString() + ")";
        } else {
            return this.toValueString();
        }
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return this.getType() + ":" + this.toValueString();
    }

    static final Predicate<String> TEMP_PATTERN = Pattern.compile("^::.+$", Pattern.CASE_INSENSITIVE).asPredicate();
    static final Predicate<String> NONE_PATTERN = Pattern.compile("^::NoneVar$", Pattern.CASE_INSENSITIVE).asPredicate();
    static final Predicate<String> AUTOVAR_PATTERN = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE).asPredicate();

    /**
     * Types of parameters. Not quite a perfect overlap with the other Type
     * class.
     */
    static public enum Type implements PapyrusElement {
        NULL,
        IDENTIFIER,
        STRING,
        INTEGER,
        FLOAT,
        BOOLEAN,
        VARIANT,
        STRUCT,
        UNKNOWN8,
        TERM;

        static public Type read(ByteBuffer input) throws PapyrusFormatException {
            Objects.requireNonNull(input);
            int val = Byte.toUnsignedInt(input.get());
            if (val < 0 || val >= VALUES.length) {
                throw new PapyrusFormatException("Invalid type: " + val);
            }
            return Type.values()[val];
        }

        @Override
        public void write(ByteBuffer output) {
            output.put((byte) this.ordinal());
        }

        @Override
        public int calculateSize() {
            return 1;
        }

        static final private Type[] VALUES = Type.values();
    }

    /**
     * An opcode parameter that stores Null.
     */
    static final public class Null extends Parameter {

        public Null() {
        }

        @Override
        public Type getType() {
            return Type.NULL;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
        }

        @Override
        public int calculateSize() {
            return 1;
        }

        /**
         * @return String representation.
         */
        @Override
        public String toValueString() {
            return "NULL";
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.getType());
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
     * An opcode parameter that stores an identifier.
     */
    static final public class ID extends Parameter {

        public ID(TString val) {
            this.VALUE = Objects.requireNonNull(val);
        }

        @Override
        public Type getType() {
            return Type.IDENTIFIER;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            this.VALUE.write(output);
        }

        @Override
        public int calculateSize() {
            return 1 + this.VALUE.calculateSize();
        }

        /**
         * @return String representation.
         */
        @Override
        public String toValueString() {
            return this.VALUE.toString();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.getType());
            hash = 41 * hash + Objects.hashCode(this.VALUE);
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
            return this.VALUE.equals(other.VALUE);
        }

        @Override
        public boolean isTemp() {
            return TEMP_PATTERN.test(this.VALUE.toString())
                    && !AUTOVAR_PATTERN.test(this.VALUE.toString())
                    && !NONE_PATTERN.test(this.VALUE.toString());
        }

        /**
         * @return A flag indicating if the parameter is an Autovariable.
         */
        @Override
        public boolean isAutovar() {
            return AUTOVAR_PATTERN.test(this.VALUE.toString());
        }

        /**
         * @return A flag indicating if the parameter is an None variable.
         */
        @Override
        public boolean isNonevar() {
            return NONE_PATTERN.test(this.VALUE.toString());
        }

        final public TString VALUE;
    }

    /**
     * An opcode parameter that stores a string.
     */
    static final public class Str extends Parameter {

        public Str(TString val) {
            this.VALUE = Objects.requireNonNull(val);
        }

        @Override
        public Type getType() {
            return Type.STRING;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            this.VALUE.write(output);
        }

        @Override
        public int calculateSize() {
            return 1 + this.VALUE.calculateSize();
        }

        /**
         * @return String representation.
         */
        @Override
        public String toValueString() {
            return this.VALUE.toString().replace("\n", "\\n");
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.getType());
            hash = 41 * hash + Objects.hashCode(this.VALUE);
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
            return this.VALUE.equals(other.VALUE);
        }

        final public TString VALUE;
    }

    /**
     * An opcode parameter that stores an integer.
     */
    static final public class Int extends Parameter {

        public Int(int val) {
            this.VALUE = val;
        }

        @Override
        public Type getType() {
            return Type.INTEGER;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            output.putInt(this.VALUE);
        }

        @Override
        public int calculateSize() {
            return 5;
        }

        @Override
        public String toValueString() {
            return Integer.toString(this.VALUE);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.getType());
            hash = 41 * hash + Integer.hashCode(this.VALUE);
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

        final public int VALUE;
    }

    /**
     * An opcode parameter that stores a float.
     */
    static final public class Flt extends Parameter {

        public Flt(float val) {
            this.VALUE = val;
        }

        @Override
        public Type getType() {
            return Type.FLOAT;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            output.putFloat(this.VALUE);
        }

        @Override
        public int calculateSize() {
            return 5;
        }

        @Override
        public String toValueString() {
            return Float.toString(this.VALUE);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.getType());
            hash = 41 * hash + Float.hashCode(this.VALUE);
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

        final public float VALUE;
    }

    /**
     * An opcode parameter that stores a boolean.
     */
    static final public class Bool extends Parameter {

        public Bool(byte val) {
            this.VALUE = val;
        }

        @Override
        public Type getType() {
            return Type.BOOLEAN;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            output.put(this.VALUE);
        }

        @Override
        public int calculateSize() {
            return 2;
        }

        @Override
        public String toValueString() {
            return Boolean.toString(this.VALUE != 0);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.getType());
            hash = 41 * hash + Byte.hashCode(this.VALUE);
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

        final public byte VALUE;
    }

    /**
     * An opcode parameter that stores a boolean.
     */
    static final public class Term extends Parameter {

        public Term(String val) {
            this.VALUE = Objects.requireNonNull(val);
        }

        @Override
        public Type getType() {
            return Type.TERM;
        }

        @Override
        public void write(ByteBuffer output) {
            throw new IllegalStateException("Terms can't be written.");
        }

        @Override
        public int calculateSize() {
            throw new IllegalStateException("Terms don't have a serialized size.");
        }

        @Override
        public String toValueString() {
            return this.VALUE;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.getType());
            hash = 41 * hash + Objects.hashCode(this.VALUE);
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
            return this.VALUE.equals(other.VALUE);
        }

        final public String VALUE;
    }

    /**
     * An opcode parameter that stores a variant.
     */
    static final public class Unk8 extends Parameter {

        public Unk8(TString val) {
            this.VALUE = Objects.requireNonNull(val);
        }

        @Override
        public Type getType() {
            return Type.UNKNOWN8;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            this.VALUE.write(output);
        }

        @Override
        public int calculateSize() {
            return 1 + this.VALUE.calculateSize();
        }

        @Override
        public String toValueString() {
            return this.VALUE.toString().replace("\n", "\\n");
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.getType());
            hash = 41 * hash + Objects.hashCode(this.VALUE);
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
            final Unk8 other = (Unk8) obj;
            return this.VALUE.equals(other.VALUE);
        }

        final public TString VALUE;
    }
}
