/*
 * Copyright 2017 Mark Fairchild.
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
package resaver.ess;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static j2html.TagCreator.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import resaver.IString;
import resaver.ListException;
import resaver.ess.papyrus.EID;
import resaver.ess.papyrus.PapyrusContext;

/**
 * A very generalized element. It's not quite as efficient or customizable as
 * other elements, but it's good for elements that can have a range of different
 * members depending on flags.
 *
 * This should generally only be used for elements of which there are not very
 * many.
 *
 * @author Mark Fairchild
 */
public class GeneralElement implements AnalyzableElement {

    /**
     * Create a new <code>GeneralElement</code>.
     */
    protected GeneralElement() {
        this.DATA = new LinkedHashMap<>();
    }

    /**
     * @return The number of sub-elements in the <code>Element</code>.
     */
    final public int count() {
        return this.DATA.size();
    }

    /**
     * @return A simple heuristic for deciding if the GeneralElement will fit on
     * a single line.
     */
    final public boolean isSimple() {
        for (Object val : this.DATA.values()) {
            if (val instanceof GeneralElement) {
                if (!((GeneralElement) val).isSimple()) {
                    return false;
                }
            } else if (val != null && val.getClass().isArray()) {
                return false;
            }
        }

        return this.count() < 4;
    }

    /**
     * @return Retrieves a copy of the <name,value> map.
     *
     */
    final public Map<IString, Object> getValues() {
        return this.DATA;
    }

    /**
     * Tests whether the <code>GeneralElement</code> contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name,value> map.
     *
     */
    final public boolean hasVal(Enum<?> name) {
        Objects.requireNonNull(name);
        return this.hasVal(name.toString());
    }

    /**
     * Tests whether the <code>GeneralElement</code> contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name,value> map.
     *
     */
    final public boolean hasVal(String name) {
        Objects.requireNonNull(name);
        return this.hasVal(IString.get(name));
    }

    /**
     * Retrieves a value by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match.
     */
    final public Object getVal(String name) {
        Objects.requireNonNull(name);
        return this.getVal(IString.get(name));
    }

    /**
     * Retrieves an <code>Element</code> by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not an <code>Element</code>.
     */
    final public Element getElement(String name) {
        Objects.requireNonNull(name);
        Object val = this.getVal(name);
        if (null != val && val instanceof Element) {
            return (Element) val;
        }
        return null;
    }

    /**
     * Retrieves a <code>GeneralElement</code> by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not a <code>GeneralElement</code>.
     */
    final public GeneralElement getGeneralElement(String name) {
        Objects.requireNonNull(name);
        Object val = this.getVal(name);
        if (null != val && val instanceof GeneralElement) {
            return (GeneralElement) val;
        }
        return null;
    }

    /**
     * Retrieves a <code>GeneralElement</code> by name from an
     * <code>Enum</code>.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not a <code>GeneralElement</code>.
     */
    final public Element getElement(Enum<?> name) {
        return this.getElement(Objects.requireNonNull(name.toString()));
    }

    /**
     * Reads a byte.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The byte.
     */
    final public byte readByte(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        byte val = input.get();
        return this.addValue(name, val);
    }

    /**
     * Reads a short.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The short.
     */
    final public short readShort(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        short val = input.getShort();
        return this.addValue(name, val);
    }

    /**
     * Reads an int.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The int.
     */
    final public int readInt(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        int val = input.getInt();
        return this.addValue(name, val);
    }

    /**
     * Reads an long.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The long.
     */
    final public long readLong(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        long val = input.getLong();
        return this.addValue(name, val);
    }

    /**
     * Reads a float.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The float.
     */
    final public float readFloat(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        float val = input.getFloat();
        return this.addValue(name, val);
    }

    /**
     * Reads a wstring.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The string.
     * @throws ElementException
     *
     */
    final public WStringElement readWString(ByteBuffer input, String name) throws ElementException {
        return this.readElement(input, name, WStringElement::read);
    }

    /**
     * Reads a zstring.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The string.
     * @throws ElementException
     *
     */
    final public String readZString(ByteBuffer input, String name) throws ElementException {
        ElementReader<ZString> reader = i -> new ZString(i);
        return this.readElement(input, name, reader).VAL;
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param reader The element reader.
     * @param <T> The element type.
     * @return The element.
     * @throws ElementException
     *
     */
    final public <T extends Element> T readElement(ByteBuffer input, Enum<?> name, ElementReader<T> reader) throws ElementException {
        return this.readElement(input, Objects.requireNonNull(name.toString()), reader);
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param reader The element reader.
     * @param <T> The element type.
     * @return The element.
     * @throws ElementException
     *
     */
    final public <T extends Element> T readElement(ByteBuffer input, String name, ElementReader<T> reader) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        Objects.requireNonNull(reader);

        try {
            T element = reader.read(input);
            return this.addValue(name, element);
        } catch (ElementException ex) {
            this.setIncomplete();
            this.addValue(name, ex.getPartial());
            throw new ElementException("Error reading " + name, ex, this);
        }
    }

    /**
     * Reads a 32bit ID.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The Papyrus context data.
     * @return The ID.
     * @throws ElementException
     *
     */
    final public EID readID32(ByteBuffer input, String name, PapyrusContext context) throws ElementException {
        Objects.requireNonNull(context);
        return this.readElement(input, name, i -> context.readEID32(input));
    }

    /**
     * Reads a 64bit ID.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The Papyrus context data.
     * @return The ID.
     * @throws ElementException
     *
     */
    final public EID readID64(ByteBuffer input, String name, PapyrusContext context) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        return this.readElement(input, name, i -> context.readEID64(input));
    }

    /**
     * Reads a refid.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The <code>ESSContext</code>.
     * @return The RefID.
     * @throws ElementException
     *
     */
    final public RefID readRefID(ByteBuffer input, String name, ESS.ESSContext context) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        try {
            return this.readElement(input, name, i -> context.readRefID(i));
        } catch (RuntimeException ex) {
            this.setIncomplete();
            throw new ElementException("Error reading refID " + name, ex, this);
        }
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The RefID.
     * @throws ElementException
     */
    final public VSVal readVSVal(ByteBuffer input, String name) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        try {
            return this.readElement(input, name, i -> new VSVal(i));
        } catch (BufferUnderflowException ex) {
            this.setIncomplete();
            throw new ElementException("Error reading VSVal " + name, ex, this);
        }
    }

    /**
     * Reads a fixed-length byte array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param count The size of the array.
     * @return The array.
     * @throws ElementException
     */
    final public byte[] readBytes(ByteBuffer input, String name, int count) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        try {
            if (count < 0) {
                throw new IllegalArgumentException("Negative array count: " + count);
            } else if (256 < count) {
                throw new IllegalArgumentException("Excessive array count: " + count);
            }

            final byte[] VAL = this.addValue(name, new byte[count]);
            input.get(VAL);
            return VAL;
        } catch (RuntimeException ex) {
            this.setIncomplete();
            throw new ElementException("Error reading " + name, ex, this);
        }
    }

    /**
     * Reads a fixed-length short array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param count The size of the array.
     * @return The array.
     * @throws ElementException
     *
     */
    final public short[] readShorts(ByteBuffer input, String name, int count) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        try {
            if (count < 0) {
                throw new IllegalArgumentException("Negative array count: " + count);
            } else if (256 < count) {
                throw new IllegalArgumentException("Excessive array count: " + count);
            }

            final short[] VAL = this.addValue(name, new short[count]);
            for (int i = 0; i < count; i++) {
                VAL[i] = input.getShort();
            }
            return VAL;
        } catch (RuntimeException ex) {
            this.setIncomplete();
            throw new ElementException("Error reading " + name, ex, this);
        }
    }

    /**
     * Reads a fixed-length int array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param count The size of the array.
     * @return The array.
     * @throws ElementException
     *
     */
    final public int[] readInts(ByteBuffer input, String name, int count) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        try {
            if (count < 0) {
                throw new IllegalArgumentException("Negative array count: " + count);
            } else if (256 < count) {
                throw new IllegalArgumentException("Excessive array count: " + count);
            }

            final int[] VAL = this.addValue(name, new int[count]);
            for (int i = 0; i < count; i++) {
                VAL[i] = input.getInt();
            }
            return VAL;
        } catch (RuntimeException ex) {
            this.setIncomplete();
            throw new ElementException("Error reading " + name, ex, this);
        }
    }

    /**
     * Reads a fixed-length long array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param count The size of the array.
     * @return The array.
     * @throws ElementException
     *
     */
    final public long[] readLongs(ByteBuffer input, String name, int count) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        try {
            if (count < 0) {
                throw new IllegalArgumentException("Negative array count: " + count);
            } else if (256 < count) {
                throw new IllegalArgumentException("Excessive array count: " + count);
            }

            final long[] VAL = this.addValue(name, new long[count]);
            for (int i = 0; i < count; i++) {
                VAL[i] = input.getLong();
            }
            return VAL;
        } catch (RuntimeException ex) {
            this.setIncomplete();
            throw new ElementException("Error reading " + name, ex, this);
        }
    }

    /**
     * Reads a fixed-length float array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param count The size of the array.
     * @return The array.
     * @throws ElementException
     *
     */
    final public float[] readFloats(ByteBuffer input, String name, int count) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        try {
            if (count < 0) {
                throw new IllegalArgumentException("Negative array count: " + count);
            } else if (256 < count) {
                throw new IllegalArgumentException("Excessive array count: " + count);
            }

            final float[] VAL = this.addValue(name, new float[count]);
            for (int i = 0; i < count; i++) {
                VAL[i] = input.getFloat();
            }
            return VAL;
        } catch (RuntimeException ex) {
            this.setIncomplete();
            throw new ElementException("Error reading " + name, ex, this);
        }
    }

    /**
     * Reads a fixed-length element array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param count The size of the array.
     * @param reader The element reader.
     * @return The array.
     * @param <T> The element type.
     * @throws ElementException
     *
     */
    final public <T extends Element> Element[] readElements(ByteBuffer input, String name, int count, ElementReader<T> reader) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        Objects.requireNonNull(reader);

        try {
            if (count < 0) {
                throw new IllegalArgumentException("Negative array count: " + count);
            } else if (256 < count) {
                throw new IllegalArgumentException("Excessive array count: " + count);
            }

            final Element[] VAL = this.addValue(name, new Element[count]);

            for (int i = 0; i < count; i++) {
                try {
                    T element = reader.read(input);
                    VAL[i] = element;
                } catch (ElementException ex) {
                    VAL[i] = ex.getPartial();
                    throw new ListException(i, count, ex);
                } catch (RuntimeException ex) {
                    throw new ListException(i, count, ex);
                }
            }
            return VAL;
        } catch (RuntimeException ex) {
            this.setIncomplete();
            throw new ElementException("Error reading " + name, ex, this);
        }
    }

    /**
     * Reads a fixed-length byte array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     * @throws ElementException
     *
     */
    final public byte[] readBytesVS(ByteBuffer input, String name) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            this.setIncomplete();
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readBytes(input, name, COUNT.getValue());
    }

    /**
     * Reads a fixed-length short array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     * @throws ElementException
     *
     */
    final public short[] readShortsVS(ByteBuffer input, String name) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            this.setIncomplete();
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readShorts(input, name, COUNT.getValue());
    }

    /**
     * Reads a fixed-length int array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     * @throws ElementException
     *
     */
    final public int[] readIntsVS(ByteBuffer input, String name) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            this.setIncomplete();
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readInts(input, name, COUNT.getValue());
    }

    /**
     * Reads a fixed-length long array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     * @throws ElementException
     *
     */
    final public long[] readLongsVS(ByteBuffer input, String name) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            this.setIncomplete();
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readLongs(input, name, COUNT.getValue());
    }

    /**
     * Reads a fixed-length float array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     * @throws ElementException
     *
     */
    final public float[] readFloatsVS(ByteBuffer input, String name) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            this.setIncomplete();
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readFloats(input, name, COUNT.getValue());
    }

    /**
     * Reads an array of elements using a supplier functional.
     *
     * @param input The inputstream.
     * @param reader
     * @param name The name of the new element.
     * @param <T> The element type.
     * @return The array.
     * @throws ElementException
     *
     */
    final public <T extends Element> Element[] readVSElemArray(ByteBuffer input, String name, ElementReader<T> reader) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            this.setIncomplete();
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return readElements(input, name, COUNT.getValue(), reader);
    }

    /**
     * Reads an array of elements using a supplier functional.
     *
     * @param input The inputstream.
     * @param reader
     * @param name The name of the new element.
     * @param <T> The element type.
     * @return The array.
     * @throws ElementException
     *
     */
    final public <T extends Element> Element[] read32ElemArray(ByteBuffer input, String name, ElementReader<T> reader) throws ElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(name);

        final int COUNT = this.readInt(input, name + "_COUNT");
        if (COUNT < 0) {
            this.setIncomplete();
            throw new IllegalArgumentException("Count is negative: " + COUNT);
        }
        return readElements(input, name, COUNT, reader);
    }

    /**
     * Reads all the remaining data in the input in 64k chunks.
     *
     * @param input The inputstream.
     * @return A flag indicating that at least one byte was read.
     */
    final public boolean readUnparsed(ByteBuffer input) {
        int count = 0;
        while (input.hasRemaining()) {
            int size = Math.min(65536, input.capacity() - input.position());
            byte[] val = new byte[size];
            input.get(val);
            this.addValue("UNPARSED_DATA_" + count, val);
            count++;
        }

        if (count > 0) {
            this.setIncomplete();
        }

        return count > 0;
    }

    /**
     * Flags the <code>GeneralElement</code> as being incomplete.
     */
    public void setIncomplete() {
        this.incomplete = true;
    }

    /**
     * @return True if the <code>GeneralElement</code> has any unparsed data.
     */
    final public boolean hasUnparsed() {
        return this.incomplete;
    }

    /**
     * Adds an object value.
     *
     * @param name The name of the new element.
     * @return The value that was read.
     * @param val The value.
     * @param <T> The type of object being added.
     */
    protected <T> T addValue(String name, T val) {
        if (!SUPPORTED.stream().anyMatch(type -> type.isInstance(val))) {
            throw new IllegalStateException(String.format("Invalid type for %s: %s", name, val.getClass()));
        }

        this.DATA.put(IString.get(name), val);
        return val;
    }

    /**
     * Removes a value.
     *
     * @param name The name of the value to remove.
     * @return A flag indicating whether a value was found.
     */
    protected boolean removeValue(String name) {
        return this.DATA.remove(IString.get(name)) != null;
    }

    /**
     * @see Element#write(resaver.ByteBuffer)
     * @param output
     */
    @Override
    public void write(ByteBuffer output) {
        this.DATA.values().forEach(v -> {
            if (Element.class.isInstance(v)) {
                Element element = (Element) v;
                element.write(output);
            } else if (v instanceof Byte) {
                output.put((Byte) v);
            } else if (v instanceof Short) {
                output.putShort((Short) v);
            } else if (v instanceof Integer) {
                output.putInt((Integer) v);
            } else if (v instanceof Float) {
                output.putFloat((Float) v);
            } else if (v instanceof String) {
                mf.BufferUtil.putZString(output, (String) v);
            } else if (v instanceof byte[]) {
                output.put((byte[]) v);
            } else if (v instanceof short[]) {
                final short[] ARR = (short[]) v;
                for (short s : ARR) {
                    output.putShort(s);
                }
            } else if (v instanceof int[]) {
                final int[] ARR = (int[]) v;
                for (int i : ARR) {
                    output.putInt(i);
                }
            } else if (v instanceof float[]) {
                final float[] ARR = (float[]) v;
                for (float f : ARR) {
                    output.putFloat(f);
                }
            } else if (v instanceof Element[]) {
                final Element[] ARR = (Element[]) v;
                for (Element e : ARR) {
                    e.write(output);
                }
            } else if (v == null) {
                throw new IllegalStateException("Null element!");
            } else {
                throw new IllegalStateException("Unknown element: " + v.getClass());
            }
        });
    }

    /**
     * @see Element#calculateSize()
     * @return
     */
    @Override
    public int calculateSize() {
        if (this.DATA.containsValue(null)) {
            throw new NullPointerException("GeneralElement may not contain null.");
        }

        int sum = 0;

        for (Object v : this.DATA.values()) {
            if (v instanceof Element) {
                sum += ((Element) v).calculateSize();
            } else if (v instanceof Byte) {
                sum += 1;
            } else if (v instanceof Short) {
                sum += 2;
            } else if (v instanceof Integer) {
                sum += 4;
            } else if (v instanceof Float) {
                sum += 4;
            } else if (v instanceof String) {
                sum += 1 + ((String) v).getBytes().length;
            } else if (v instanceof byte[]) {
                sum += 1 * ((byte[]) v).length;
            } else if (v instanceof short[]) {
                sum += 2 * ((short[]) v).length;
            } else if (v instanceof int[]) {
                sum += 4 * ((int[]) v).length;
            } else if (v instanceof float[]) {
                sum += 4 * ((float[]) v).length;
            } else if (v instanceof Element[]) {
                sum += Arrays.stream((Element[]) v).mapToInt(w -> w.calculateSize()).sum();
            } else if (v == null) {
                throw new IllegalStateException("Null element!");
            } else {
                throw new IllegalStateException("Unknown element: " + v.getClass());
            }
        }

        return sum;
    }

    /**
     * @see java.lang.Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.DATA);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }

        final GeneralElement other = (GeneralElement) obj;
        return Objects.equals(this.DATA, other.DATA);
    }

    /**
     *
     * @return String representation.
     */
    @Override
    public String toString() {
        return toStringFlat(null);
    }

    /**
     * @param name A name to display.
     * @return String representation.
     */
    protected String toStringFlat(String name) {
        if (this.DATA.isEmpty()) {
            return new StringBuilder()
                    .append(null == name ? "" : name)
                    .append("(EMPTY)").toString();

        } else {
            return new StringBuilder()
                    .append(null == name ? "" : name)
                    .append(this.DATA.entrySet()
                            .stream()
                            .map(e -> formatKeyPairFlat(e.getKey(), e.getValue()))
                            .collect(Collectors.joining(", ", "{", "}")))
                    .toString();
        }
    }

    /**
     * @param name A name to display.
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    protected String toStringStructured(String name, int level) {
        CharSequence tabs = indent2(level);

        if (this.isSimple()) {
            return new StringBuilder().append(tabs).append(this.toStringFlat(name)).toString();

        } else {
            if (this.DATA.keySet().isEmpty()) {
                return new StringBuilder()
                        .append(tabs)
                        .append(null == name ? "" : name)
                        .append("{}")
                        .toString();
            }

            return new StringBuilder()
                    .append(tabs)
                    .append(null == name ? "" : name)
                    .append("{")
                    .append(this.DATA.entrySet()
                            .stream()
                            .map(e -> formatKeyPairStructured(e.getKey(), e.getValue(), level))
                            .collect(Collectors.joining("\n", "\n", "\n")))
                    .append(tabs)
                    .append("}")
                    .toString();
        }
    }

    private String formatKeyPairFlat(IString key, Object val) {
        if (val instanceof GeneralElement) {
            GeneralElement element = (GeneralElement) val;
            return element.toStringFlat(key.toString());
        } else if (val instanceof Element[]) {
            return eaToStringFlat(key, (Element[]) val);
        } else if (val instanceof Byte) {
            return String.format("%s=%02x", key, Byte.toUnsignedInt((Byte) val));
        } else if (val instanceof Short) {
            return String.format("%s=%04x", key, Short.toUnsignedInt((Short) val));
        } else if (val instanceof Integer) {
            return String.format("%s=%08x", key, Integer.toUnsignedLong((Integer) val));
        } else if (val instanceof Long) {
            return String.format("%s=%16x", key, (Long) val);
        } else if (val instanceof Object[]) {
            return String.format("%s=%s", key, Arrays.toString((Object[]) val));
        } else if (val instanceof boolean[]) {
            return String.format("%s=%s", key, Arrays.toString((boolean[]) val));
        } else if (val instanceof byte[]) {
            byte[] bytes = (byte[]) val;
            if (bytes.length > 200 && key.toString().toUpperCase().contains("UNPARSED_DATA")) {
                return String.format("%d bytes of unparsed data", bytes.length);
            } else {
                return String.format("%s=%s", key, bytesToHex(bytes));
            }
        } else if (val instanceof char[]) {
            return String.format("%s=%s", key, Arrays.toString((char[]) val));
        } else if (val instanceof double[]) {
            return String.format("%s=%s", key, Arrays.toString((double[]) val));
        } else if (val instanceof float[]) {
            return String.format("%s=%s", key, Arrays.toString((float[]) val));
        } else if (val instanceof int[]) {
            return String.format("%s=%s", key, Arrays.toString((int[]) val));
        } else if (val instanceof long[]) {
            return String.format("%s=%s", key, Arrays.toString((long[]) val));
        } else if (val instanceof short[]) {
            return String.format("%s=%s", key, Arrays.toString((short[]) val));
        } else {
            return String.format("%s=%s", key, Objects.toString(val));
        }
    }

    private String formatKeyPairStructured(IString key, Object val, int level) {
        CharSequence tabs = indent2(level + 1);

        if (val instanceof GeneralElement) {
            GeneralElement element = (GeneralElement) val;
            String str = element.toStringStructured(key.toString(), level + 1);
            return String.format("%s\n", str);
        } else if (val instanceof Element[]) {
            String str = eaToString(key, level + 1, (Element[]) val);
            return String.format("%s\n", str);
        } else if (val instanceof Byte) {
            return String.format("%s%s=%02x", tabs, key, Byte.toUnsignedInt((Byte) val));
        } else if (val instanceof Short) {
            return String.format("%s%s=%04x", tabs, key, Short.toUnsignedInt((Short) val));
        } else if (val instanceof Integer) {
            return String.format("%s%s=%08x", tabs, key, Integer.toUnsignedLong((Integer) val));
        } else if (val instanceof Long) {
            return String.format("%s%s=%16x", tabs, key, (Long) val);
        } else if (val instanceof Object[]) {
            return String.format("%s%s=%s", tabs, key, Arrays.toString((Object[]) val));
        } else if (val instanceof boolean[]) {
            return String.format("%s%s=%s", tabs, key, Arrays.toString((boolean[]) val));
        } else if (val instanceof byte[]) {
            byte[] bytes = (byte[]) val;
            if (bytes.length > 200 && key.toString().toUpperCase().contains("UNPARSED_DATA")) {
                return String.format("%s%s=%d bytes of unparsed data", tabs, key, ((byte[]) val).length);
            } else {
                return String.format("%s%s=%s", tabs, key, bytesToHex((byte[]) val));
            }
        } else if (val instanceof char[]) {
            return String.format("%s%s=%s", tabs, key, Arrays.toString((char[]) val));
        } else if (val instanceof double[]) {
            return String.format("%s%s=%s", tabs, key, Arrays.toString((double[]) val));
        } else if (val instanceof float[]) {
            return String.format("%s%s=%s", tabs, key, Arrays.toString((float[]) val));
        } else if (val instanceof int[]) {
            return String.format("%s%s=%s", tabs, key, Arrays.toString((int[]) val));
        } else if (val instanceof long[]) {
            return String.format("%s%s=%s", tabs, key, Arrays.toString((long[]) val));
        } else if (val instanceof short[]) {
            return String.format("%s%s=%s", tabs, key, Arrays.toString((short[]) val));
        } else {
            return String.format("%s%s=%s", tabs, key, Objects.toString(val));
        }
    }

    /**
     * Tests whether the <code>GeneralElement</code> contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name,value> map.
     *
     */
    final public boolean hasVal(IString name) {
        Objects.requireNonNull(name);
        return this.DATA.containsKey(name);
    }

    /**
     * Retrieves a value by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match.
     */
    private Object getVal(IString name) {
        Objects.requireNonNull(name);
        return this.DATA.get(name);
    }

    /**
     * Appends <code>n</code> indents to a <code>StringBuilder</code>.
     *
     * @param b
     * @param n
     */
    static private void indent(StringBuilder b, int n) {
        for (int i = 0; i < n; i++) {
            b.append('\t');
        }
    }

    static protected CharSequence indent2(int n) {
        final StringBuilder BUF = new StringBuilder();
        for (int i = 0; i < n; i++) {
            BUF.append('\t');
        }
        return BUF;
    }

    /**
     * Creates a string representation of an <code>ElementArrayList</code>.
     *
     * @param name A name to display.
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    static private String eaToString(IString name, int level, Element[] list) {
        final StringBuilder BUF = new StringBuilder();

        if (list.length == 0) {
            indent(BUF, level);
            if (null != name) {
                BUF.append(name);
            }
            BUF.append("[]");
            return BUF.toString();
        }

        indent(BUF, level);
        if (null != name) {
            BUF.append(name);
        }

        BUF.append("[\n");

        for (Element e : list) {
            if (e instanceof GeneralElement) {
                GeneralElement element = (GeneralElement) e;
                String str = element.toStringStructured(null, level + 1);
                BUF.append(str).append('\n');
            } else if (e != null) {
                indent(BUF, level + 1);
                String str = e.toString();
                BUF.append(str).append('\n');
            } else {
                BUF.append("null");
            }
        }

        indent(BUF, level);
        BUF.append("]");
        return BUF.toString();
    }

    /**
     * Creates a string representation of an <code>ElementArrayList</code>.
     *
     * @param name A name to display.
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    static private String eaToStringFlat(IString name, Element[] list) {
        final StringBuilder BUF = new StringBuilder();

        if (list.length == 0) {
            if (null != name) {
                BUF.append(name);
            }
            BUF.append("[]");
            return BUF.toString();
        }

        if (null != name) {
            BUF.append(name);
        }

        BUF.append(Arrays.stream(list)
                .map(e -> {
                    if (e instanceof GeneralElement) {
                        GeneralElement element = (GeneralElement) e;
                        return element.toStringFlat(null);
                    } else if (e != null) {
                        return e.toString();
                    } else {
                        return "null";
                    }
                })
                .collect(Collectors.joining(", ", "[", "]")));

        return BUF.toString();
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(resaver.Analysis analysis, ESS save) {
        return body(
                table(
                        tbody(
                                each(this.DATA, (k, v) -> tr(
                                td(k.toString()),
                                td(rawHtml(getInfoFor(k, v, analysis, save)))
                        ))
                        )
                ).withData("border", "1")
        ).toString();
    }

    static private String getInfoFor(IString key, Object val, resaver.Analysis analysis, ESS save) {
        if (val == null) {
            return "null";
        } else if (val instanceof Linkable) {
            final Linkable LINKABLE = (Linkable) val;
            return LINKABLE.toHTML(null);
        } else if (val instanceof List<?>) {
            final List<?> LIST = (List<?>) val;
            return GeneralElement.formatList(key.toString(), LIST, analysis, save);
        } else if (val instanceof GeneralElement) {
            final GeneralElement GEN = (GeneralElement) val;
            return formatGeneralElement(key.toString(), GEN, analysis, save);
        } else {
            return val.toString();
        }
    }

    static private String formatElement(String key, Object val, resaver.Analysis analysis, ESS save) {
        final StringBuilder BUF = new StringBuilder();
        if (val == null) {
            BUF.append(String.format("%s: <NULL>", key));

        } else if (val instanceof Linkable) {
            final Linkable LINKABLE = (Linkable) val;
            final String STR = LINKABLE.toHTML(null);
            BUF.append(String.format("%s: %s", key, STR));

        } else if (val instanceof List<?>) {
            final List<?> LIST = (List<?>) val;
            final String STR = GeneralElement.formatList(key, LIST, analysis, save);
            BUF.append(String.format("%s: %s", key, STR));

        } else if (val.getClass().isArray()) {
            if (key.startsWith("UNPARSED_DATA")) {
                BUF.append(String.format("+ %d bytes of unparsed data", ((byte[]) val).length));
            } else if (val instanceof Object[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((Object[]) val)));
            } else if (val instanceof boolean[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((boolean[]) val)));
            } else if (val instanceof byte[]) {
                BUF.append(String.format("%s: %s", key, bytesToHex((byte[]) val)));
            } else if (val instanceof char[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((char[]) val)));
            } else if (val instanceof double[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((double[]) val)));
            } else if (val instanceof float[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((float[]) val)));
            } else if (val instanceof int[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((int[]) val)));
            } else if (val instanceof long[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((long[]) val)));
            } else if (val instanceof short[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((short[]) val)));
            }
            final List<?> LIST = (List<?>) val;
            final String STR = GeneralElement.formatList(key, LIST, analysis, save);
            BUF.append(String.format("%s: %s", key, STR));

        } else if (val instanceof GeneralElement) {
            final GeneralElement GEN = (GeneralElement) val;
            final String STR = GeneralElement.formatGeneralElement(key, GEN, analysis, save);
            BUF.append(String.format("%s: %s", key, STR));

        } else {
            BUF.append(String.format("%s: %s", key, val));
        }
        return BUF.toString();
    }

    static private String formatGeneralElement(String key, GeneralElement gen, resaver.Analysis analysis, ESS save) {
        final StringBuilder BUF = new StringBuilder();
        gen.getValues().forEach((k, v) -> {
            final String S = GeneralElement.formatElement(k.toString(), v, analysis, save);
            BUF.append(String.format("<p>%s</p>", S));
        });
        return BUF.toString();
    }

    static private String formatList(String key, List<?> list, resaver.Analysis analysis, ESS save) {
        final StringBuilder BUF = new StringBuilder();
        //BUF.append(String.format("<p>%s</p>", key));
        int i = 0;
        for (Object val : list) {
            final String K = Integer.toString(i);
            final String S = GeneralElement.formatElement(K, val, analysis, save);
            BUF.append(String.format("<p>%s</p>", S));
            i++;
        }
        //BUF.append("<");
        return BUF.toString();
    }

    /**
     * Stores the actual data.
     */
    final private Map<IString, Object> DATA;
    private boolean incomplete = false;

    static final private Set<Class<?>> SUPPORTED = new HashSet<>(Arrays.asList(
            new Class<?>[]{
                Element.class,
                Byte.class,
                Short.class,
                Integer.class,
                Float.class,
                String.class,
                byte[].class,
                short[].class,
                int[].class,
                long[].class,
                float[].class,
                Object[].class

            }
    ));

    @FunctionalInterface
    static public interface ElementReader<T extends Element> {

        T read(ByteBuffer input) throws ElementException;
    }

    static class ZString implements Element {

        ZString(ByteBuffer input) {
            VAL = mf.BufferUtil.getZString(input);
        }

        @Override
        public void write(ByteBuffer output) {
            mf.BufferUtil.putZString(output, VAL);
        }

        @Override
        public int calculateSize() {
            return 1 + VAL.getBytes(UTF_8).length;
        }
        final public String VAL;
    }

    /**
     * Taken from
     * https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
     *
     */
    static private final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    static private String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Equivalent to disjunctive-normal-form of searchMatch(String[]).
     *
     * @param terms An array of clauses. Each clause is an array of patterns.
     * Each pattern is an array of fields. Each field is a String.
     * @return True if the terms all evaluate to true and each clause is
     * satisfied.
     * @throws IllegalArgumentException If a pattern is invalid.
     */
    public boolean searchMatches(String[][][] terms) {
        assert terms != null : "Bad terms: null";
        boolean termsResult = true;

        for (String[][] clauses : terms) {
            assert clauses != null : "Bad clauses: null";
            boolean clausesResult = false;

            for (String[] pattern : clauses) {
                assert clauses != null : "Bad pattern: null";
                clausesResult = clausesResult || searchMatch(pattern);
                if (clausesResult) {
                    break;
                }
            }

            termsResult = clausesResult && termsResult;
            if (!termsResult) {
                break;
            }
        }

        return termsResult;
    }

    /**
     * Searches a GeneralElement tree by a list of value names. The final value
     * matches data of types: VSVal, RefID, Strings, or any numeric primitive.
     * It will match a GenericElement by the class name. It will match anything
     * else by string comparison.
     *
     * @param pattern
     * @return
     * @throws IllegalArgumentException If a pattern is invalid.
     */
    public boolean searchMatch(String[] pattern) {
        assert pattern != null : "Invalid pattern: null";
        assert pattern.length > 0 : "Invalid pattern, two fields required: " + Arrays.toString(pattern);

        String expected = pattern[pattern.length - 1];
        boolean negate = pattern[0].equalsIgnoreCase("!");

        if (this.hasVal("inventory_count")) {
            VSVal count = (VSVal) this.getVal("inventory_count");
            if (count.getValue() == 6) {
                int k = 0;
            }
        }

        return negate
                ? !searchMatch_aux(pattern, expected, this, 1)
                : searchMatch_aux(pattern, expected, this, 0);
    }

    private boolean searchMatch_aux(String[] pattern, String expected, Object cursor, int index) {
        assert pattern != null;
        assert expected != null;
        assert cursor != null;

        String field = pattern[index];
        assert field != null;
        assert !field.isBlank();

        if (index < pattern.length - 2) {
            return searchMatch_recurse(pattern, expected, cursor, index, field);
        } else if (index == pattern.length - 2) {
            return searchMatch_penultimate(pattern, expected, cursor, index, field);
        } else if (index == pattern.length - 1) {
            return searchMatch_terminus(pattern, expected, cursor, field);
        } else {
            throw new IllegalArgumentException("Went too far.");
        }
    }

    private boolean searchMatch_recurse(String[] pattern, String expected, Object cursor, int index, String field) {
        if (cursor instanceof GeneralElement) {
            GeneralElement element = (GeneralElement) cursor;
            return element.hasVal(field) 
                    ? searchMatch_aux(pattern, expected, element.getVal(field), index + 1)
                    : false;
        } else {
            return false;
        }
    }

    private boolean searchMatch_penultimate(String[] pattern, String expected, Object cursor, int index, String field) {
        if (cursor instanceof GeneralElement) {
            GeneralElement element = (GeneralElement) cursor;
            return element.hasVal(field) 
                    ? searchMatch_values(expected, element.getVal(field))
                    : searchMatch_values(expected, field);
        } else {
            return false;
        }
    }

    private boolean searchMatch_terminus(String[] pattern, String expected, Object cursor, String field) {
        if (cursor instanceof GeneralElement) {
            GeneralElement element = (GeneralElement) cursor;
            return element.hasVal(field) 
                    ? searchMatch_values(expected, element.getVal(field))
                    : searchMatch_values(expected, field);
        } else {
            return false;
        }
    }

    private boolean searchMatch_values(String expected, Object value) {
        try {
            if (value == null) {
                return expected.equalsIgnoreCase("null");
            } else if (value instanceof String) {
                return expected == value;
            } else if (value instanceof Byte) {
                return Integer.parseInt(expected, 16) == (Byte) value;
            } else if (value instanceof Short) {
                return Integer.parseInt(expected, 16) == (Short) value;
            } else if (value instanceof Integer) {
                return Integer.parseInt(expected, 16) == (Short) value;
            } else if (value instanceof Long) {
                return Integer.parseInt(expected, 16) == (Integer) value;
            } else if (value instanceof Float) {
                return floatsKindaEqual(Float.parseFloat(expected), (Float) value);
            } else if (value instanceof VSVal) {
                return Integer.parseInt(expected, 10) == ((VSVal) value).getValue();
            } else if (value instanceof RefID) {
                RefID ref = (RefID) value;
                return ref.equals(Integer.parseInt(expected, 16));
            } else if (value instanceof GeneralElement) {
                GeneralElement element = (GeneralElement) value;
                if (element.hasVal(expected)) {
                    return true;
                } else {
                    return expected.equalsIgnoreCase(value.getClass().getName());
                }
            } else {
                return expected.equalsIgnoreCase(value.toString());
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Bad pattern.");
        }
    }

    private boolean floatsKindaEqual(float f1, float f2) {
        if (f1 == f2) {
            return true;
        } else if (f1 == 0.0f) {
            return Math.abs(f2) < 1.0f;
        } else if (f2 == 0.0f) {
            return Math.abs(f1) < 1.0f;
        } else {
            float diff = Math.abs(f1 - f2) / (Math.abs(f1) + Math.abs(f2));
            return diff < 0.05;
        }
    }

}
