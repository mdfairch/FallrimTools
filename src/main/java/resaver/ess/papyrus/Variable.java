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

import resaver.ListException;
import java.nio.ByteBuffer;
import java.util.Objects;
import resaver.ess.Element;
import resaver.ess.Linkable;

/**
 * Describes a variable in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
abstract public class Variable implements PapyrusElement, Linkable {

    /**
     * Creates a new <code>List</code> of <code>Variable</code> by reading from
     * a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @param count The number of variables.
     * @param context The <code>PapyrusContext</code> info.
     * @return The new <code>List</code> of <code>Variable</code>.
     * @throws ListException
     */
    static public java.util.List<Variable> readList(ByteBuffer input, int count, PapyrusContext context) throws ListException {
        final java.util.List<Variable> VARIABLES = new java.util.ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            try {
                Variable var = Variable.read(input, context);
                VARIABLES.add(var);
            } catch (PapyrusFormatException ex) {
                throw new ListException(i, count, ex);
            }
        }

        return VARIABLES;
    }

    /**
     * Creates a new <code>Variable</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @return The new <code>Variable</code>.
     * @throws PapyrusFormatException
     */
    static public Variable read(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        final Type TYPE = Type.read(input);

        switch (TYPE) {
            case NULL:
                return new Null(input);
            case REF:
                return new Ref(input, context);
            case STRING:
                return new Str(input, context);
            case INTEGER:
                return new Int(input);
            case FLOAT:
                return new Flt(input);
            case BOOLEAN:
                return new Bool(input);
            case VARIANT:
                return new Variant(input, context);
            case STRUCT:
                return new Struct(input, context);
            case REF_ARRAY:
            case STRING_ARRAY:
            case INTEGER_ARRAY:
            case FLOAT_ARRAY:
            case BOOLEAN_ARRAY:
            case VARIANT_ARRAY:
            case STRUCT_ARRAY:
                return new Array(TYPE, input, context);
            default:
                throw new PapyrusException("Illegal typecode for variable", null, null);
        }
    }

    /**
     * @return The EID of the papyrus element.
     */
    abstract public Type getType();

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        return this.toString();
    }

    /**
     * @return A string representation that only includes the type field.
     */
    public String toTypeString() {
        return this.getType().toString();
    }

    /**
     * Checks if the variable stores a reference to something.
     *
     * @return
     */
    public boolean hasRef() {
        return false;
    }

    /**
     * Checks if the variable stores a reference to a particular something.
     *
     * @param id
     * @return
     */
    public boolean hasRef(EID id) {
        return false;
    }

    /**
     * Returns the variable's refid or null if the variable isn't a reference
     * type.
     *
     * @return
     */
    public EID getRef() {
        return null;
    }

    /**
     * Returns the variable's REFERENT or null if the variable isn't a reference
     * type.
     *
     * @return
     */
    public GameElement getReferent() {
        return null;
    }

    /**
     * @return A string representation that doesn't include the type field.
     */
    abstract public String toValueString();

    /**
     * Variable that stores nothing.
     */
    static final public class Null extends Variable {

        public Null(ByteBuffer input) {
            this.VALUE = input.getInt();
        }

        @Override
        public int calculateSize() {
            return 5;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            output.putInt(this.VALUE);
        }

        @Override
        public Type getType() {
            return Type.NULL;
        }

        @Override
        public String toValueString() {
            return "NULL";
        }

        @Override
        public String toString() {
            return "NULL";
        }

        final private int VALUE;
    }

    /**
     * ABT for a variable that stores some type of ref.
     */
    static abstract private class AbstractRef extends Variable {

        public AbstractRef(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
            Objects.requireNonNull(input);
            this.REFTYPE = context.readTString(input);
            this.REF = context.readEID(input);
            this.REFERENT = context.findReferrent(this.REF);
        }

        public AbstractRef(TString type, EID id, PapyrusContext context) {
            this.REF = Objects.requireNonNull(id);
            this.REFTYPE = Objects.requireNonNull(type);
            this.REFERENT = context.findReferrent(this.REF);
        }

        public boolean isNull() {
            return this.REF.isZero();
        }

        public TString getRefType() {
            return this.REFTYPE;
        }

        @Override
        public boolean hasRef() {
            return true;
        }

        @Override
        public boolean hasRef(EID id) {
            return Objects.equals(this.REF, id);
        }

        @Override
        public EID getRef() {
            return this.REF;
        }

        @Override
        public GameElement getReferent() {
            return this.REFERENT;
        }

        @Override
        public int calculateSize() {
            int sum = 1;
            sum += this.REFTYPE.calculateSize();
            sum += this.REF.calculateSize();
            return sum;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            this.REFTYPE.write(output);
            this.REF.write(output);
        }

        /**
         * @see Variable#toTypeString()
         * @return
         */
        @Override
        public String toTypeString() {
            return this.REFTYPE.toString();
        }

        @Override
        public String toValueString() {
            return this.getReferent() != null
                    ? this.REFERENT.toString()
                    : this.REF.toString() + " (" + this.REFTYPE + ")";
        }

        @Override
        public String toHTML(Element target) {
            if (null != this.REFERENT) {
                final String REFLINK = this.REFERENT.toHTML(this);
                return String.format("%s : %s", this.getType(), REFLINK);
            } else {
                final String DEFLINK = Linkable.makeLink("script", this.REFTYPE, this.REFTYPE.toString());
                return String.format("%s : %s (%s)", this.getType(), this.REF, DEFLINK);
            }
        }

        @Override
        public String toString() {
            return this.getType() + " : " + this.toValueString();
        }

        final private TString REFTYPE;
        final private EID REF;
        final private GameElement REFERENT;
    }

    /**
     * Variable that stores a ref. Note to self: a ref is a pointer to a papyrus
     * element, unlike a RefID which points to a form or changeform.
     *
     */
    static final public class Ref extends AbstractRef {

        public Ref(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
            super(input, context);
        }

        public Ref(TString type, EID id, PapyrusContext context) {
            super(type, id, context);
        }

        public Ref derive(long id, PapyrusContext context) {
            Ref derivative = new Ref(this.getRefType(), this.getRef().derive(id), context);
            return derivative;
        }

        @Override
        public Type getType() {
            return Type.REF;
        }

    }

    /**
     * Variable that stores a Variant.
     *
     */
    static final public class Variant extends Variable {

        public Variant(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
            Objects.requireNonNull(input);
            final Variable var = Variable.read(input, context);
            this.VALUE = var;
        }

        public Variable getValue() {
            return this.VALUE;
        }

        @Override
        public int calculateSize() {
            return 1 + this.VALUE.calculateSize();
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            this.VALUE.write(output);
        }

        @Override
        public Type getType() {
            return Type.VARIANT;
        }

        @Override
        public boolean hasRef() {
            return this.VALUE.hasRef();
        }

        @Override
        public boolean hasRef(EID id) {
            return this.VALUE.hasRef(id);
        }

        @Override
        public EID getRef() {
            return this.VALUE.getRef();
        }

        @Override
        public GameElement getReferent() {
            return this.VALUE.getReferent();
        }

        @Override
        public String toValueString() {
            return this.VALUE.toValueString();
        }

        @Override
        public String toHTML(Element target) {
            return String.format("%s[%s]", this.getType(), this.VALUE.toHTML(target));
        }

        @Override
        public String toString() {
            return this.getType() + ":" + this.VALUE.toString();
        }

        final private Variable VALUE;
    }

    /**
     * Variable that stores an UNKNOWN7.
     *
     */
    static final public class Struct extends AbstractRef {

        public Struct(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
            super(input, context);
        }

        public Struct(TString type, EID id, PapyrusContext context) {
            super(type, id, context);
        }

        public Struct derive(long id, PapyrusContext context) {
            return new Struct(this.getRefType(), this.getRef().derive(id), context);
        }

        @Override
        public Type getType() {
            return Type.STRUCT;
        }

    }

    /**
     * Variable that stores a string.
     */
    static final public class Str extends Variable {

        public Str(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
            Objects.requireNonNull(input);
            this.VALUE = context.readTString(input);
        }

        public Str(String newValue, PapyrusContext context) {
            Objects.requireNonNull(newValue);
            this.VALUE = context.addTString(newValue);
        }

        public TString getValue() {
            return this.VALUE;
        }

        @Override
        public int calculateSize() {
            return 1 + this.VALUE.calculateSize();
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            this.VALUE.write(output);
        }

        @Override
        public Type getType() {
            return Type.STRING;
        }

        @Override
        public String toValueString() {
            //return String.format("\"%s\"", this.VALUE);
            return "\"" + this.VALUE + "\"";
        }

        @Override
        public String toString() {
            //return String.format("%s:\"%s\"", this.getType(), this.VALUE);
            return this.getType() + ":" + this.toValueString();
        }

        //final private StringTable STRINGS;
        final private TString VALUE;
    }

    /**
     * Variable that stores an integer.
     */
    static final public class Int extends Variable {

        public Int(ByteBuffer input) {
            this.VALUE = input.getInt();
        }

        public Int(int val) {
            this.VALUE = val;
        }

        public int getValue() {
            return this.VALUE;
        }

        @Override
        public int calculateSize() {
            return 5;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            output.putInt(this.VALUE);
        }

        @Override
        public Type getType() {
            return Type.INTEGER;
        }

        @Override
        public String toValueString() {
            //return String.format("%d", this.VALUE);
            return Integer.toString(this.VALUE);
        }

        @Override
        public String toString() {
            //return String.format("%s:%d", this.getType(), this.VALUE);
            return this.getType() + ":" + this.toValueString();
        }

        final private int VALUE;
    }

    /**
     * Variable that stores a float.
     */
    static final public class Flt extends Variable {

        public Flt(ByteBuffer input) {
            this.VALUE = input.getFloat();
        }

        public Flt(float val) {
            this.VALUE = val;
        }

        public float getValue() {
            return this.VALUE;
        }

        @Override
        public int calculateSize() {
            return 5;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            output.putFloat(this.VALUE);
        }

        @Override
        public Type getType() {
            return Type.FLOAT;
        }

        @Override
        public String toValueString() {
            //return String.format("%f", this.VALUE);
            return Float.toString(this.VALUE);
        }

        @Override
        public String toString() {
            //return String.format("%s:%f", this.getType(), this.VALUE);
            return this.getType() + ":" + this.toValueString();
        }

        final private float VALUE;
    }

    /**
     * Variable that stores a boolean.
     */
    static final public class Bool extends Variable {

        public Bool(ByteBuffer input) {
            Objects.requireNonNull(input);
            this.VALUE = input.getInt();
        }

        public Bool(boolean val) {
            this.VALUE = (val ? 1 : 0);
        }

        public boolean getValue() {
            return this.VALUE != 0;
        }

        @Override
        public int calculateSize() {
            return 5;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);
            output.putInt(this.VALUE);
        }

        @Override
        public Type getType() {
            return Type.BOOLEAN;
        }

        @Override
        public String toValueString() {
            //return String.format("%s", Boolean.toString(this.VALUE != 0));
            return Boolean.toString(this.VALUE != 0);
        }

        @Override
        public String toString() {
            //return String.format("%s:%s", this.getType(), Boolean.toString(this.VALUE != 0));
            return this.getType() + ":" + this.toValueString();
        }

        final private int VALUE;
    }

    /**
     * Variable that stores an ARRAY.
     */
    static final public class Array extends Variable {

        protected Array(Type type, ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
            Objects.requireNonNull(type);
            Objects.requireNonNull(input);
            this.TYPE = type;
            this.REFTYPE = this.TYPE.isRefType() ? context.readTString(input) : null;
            this.ARRAYID = context.readEID(input);
            this.ARRAY = context.findArray(this.ARRAYID);
        }

        public EID getArrayID() {
            return this.ARRAYID;
        }

        public ArrayInfo getArray() {
            return this.ARRAY;
        }

        public Type getElementType() {
            return Type.values()[this.TYPE.ordinal() - 7];
        }

        @Override
        public Type getType() {
            return this.TYPE;
        }

        @Override
        public boolean hasRef() {
            return true;
        }

        @Override
        public boolean hasRef(EID id) {
            return Objects.equals(this.ARRAYID, id);
        }

        @Override
        public EID getRef() {
            return this.getArrayID();
        }

        @Override
        public GameElement getReferent() {
            return null;
        }

        @Override
        public void write(ByteBuffer output) {
            this.getType().write(output);

            if (this.TYPE.isRefType()) {
                this.REFTYPE.write(output);
            }

            this.ARRAYID.write(output);
        }

        @Override
        public int calculateSize() {
            int sum = 1;
            sum += (this.TYPE.isRefType() ? this.REFTYPE.calculateSize() : 0);
            sum += this.ARRAYID.calculateSize();
            return sum;
        }

        @Override
        public String toTypeString() {
            if (null == this.ARRAY) {
                if (this.TYPE.isRefType()) {
                    return "" + this.REFTYPE + "[ ]";
                } else {
                    return this.getElementType() + "[ ]";
                }
            }

            if (this.TYPE.isRefType()) {
                return this.TYPE + ":" + "" + this.REFTYPE + "[" + Integer.toString(this.ARRAY.getLength()) + "]";
            } else {
                return this.TYPE + ":" + this.getElementType() + "[" + Integer.toString(this.ARRAY.getLength()) + "]";
            }
        }

        @Override
        public String toValueString() {
            if (null != this.getArray()) {
                return "" + this.ARRAYID + ": " + this.getArray().toValueString();
            } else {
                return this.ARRAYID.toString();
            }
        }

        @Override
        public String toHTML(Element target) {
            final String LINK = Linkable.makeLink("array", this.ARRAYID, this.ARRAYID.toString());
            return String.format("%s : %s", this.toTypeString(), LINK);
        }

        @Override
        public String toString() {
            return this.toTypeString() + " " + this.ARRAYID;
        }

        final private Type TYPE;
        final private EID ARRAYID;
        final private TString REFTYPE;
        final private ArrayInfo ARRAY;
    }

    /**
     * Variable that stores an integer.
     */
    /*static final public class Array6 extends Variable {

        public Array6(ByteBuffer input, StringTable strtab) throws IOException {
            Objects.requireNonNull(input);
            Objects.requireNonNull(ctx);
            this.VALUE = new byte[8];
            int read = input.read(this.VALUE);
            assert read == 8;
        }

        public byte[] getValue() {
            return this.VALUE;
        }

        @Override
        public int calculateSize() {
            return 1 + VALUE.length;
        }

        @Override
        public void write(ByteBuffer output) throws IOException {
            this.getType().write(output);
            output.write(this.VALUE);
        }

        @Override
        public Type getType() {
            return Type.UNKNOWN6_ARRAY;
        }

        @Override
        public String toValueString() {
            //return String.format("%d", this.VALUE);
            return Arrays.toString(this.VALUE);
        }

        @Override
        public String toString() {
            //return String.format("%s:%d", this.getType(), this.VALUE);
            return this.getType() + ":" + this.toValueString();
        }

        final private byte[] VALUE;
    }*/
}
