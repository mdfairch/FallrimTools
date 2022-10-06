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
package resaver.esp;

import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Stores the data for a script property. Basically a fancy VarArg.
 *
 * @author Mark Fairchild
 */
abstract public class PropertyData implements Entry {

    static public PropertyData readPropertyData(byte type, ByteBuffer input, ESPContext ctx) {
        assert input.hasRemaining() || type == 0 : "No input available, type = " + type;

        switch (type) {
            case 0:
                return new NullData(input);
            case 1:
                return new ObjectData(input);
            case 2:
                return new StringData(input);
            case 3:
                return new IntData(input);
            case 4:
                return new FloatData(input);
            case 5:
                return new BoolData(input);
            case 6:
                return new VarData(input);
            case 7:
                return new StructData(input, ctx);
            case 11:
                return new ArrayData<>(input, () -> new ObjectData(input));
            case 12:
                return new ArrayData<>(input, () -> new StringData(input));
            case 13:
                return new ArrayData<>(input, () -> new IntData(input));
            case 14:
                return new ArrayData<>(input, () -> new FloatData(input));
            case 15:
                return new ArrayData<>(input, () -> new BoolData(input));
            case 16:
                return new ArrayData<>(input, () -> new VarData(input));
            case 17:
                return new ArrayData<>(input, () -> new StructData(input, ctx));
            default:
                throw new IllegalStateException(String.format("Invalid property type: %d", type));
        }
    }

    /**
     * Stores a Null property data.
     *
     */
    static public class NullData extends PropertyData {

        public NullData(ByteBuffer input) {
            //this.DATA = input.readInt();
        }

        @Override
        public void write(ByteBuffer output) {
            //output.putInt(DATA);
        }

        @Override
        public int calculateSize() {
            return 0;
        }

        @Override
        public String toString() {
            return "NULL";
        }
        //final private int DATA;
    }

    /**
     * Stores an Object property data.
     */
    static public class ObjectData extends PropertyData {

        public ObjectData(ByteBuffer input) {
            this.DATA = input.getLong();
        }

        @Override
        public void write(ByteBuffer output) {
            output.putLong(DATA);
        }

        @Override
        public int calculateSize() {
            return 8;
        }

        @Override
        public String toString() {
            return String.format("%08x", this.DATA);
        }

        final private long DATA;
    }

    /**
     * Stores a String property data.
     */
    static public class StringData extends PropertyData {

        public StringData(ByteBuffer input) {
            this.DATA = mf.BufferUtil.getUTF(input);
        }

        @Override
        public void write(ByteBuffer output) {
            output.put(DATA.getBytes(UTF_8));
        }

        @Override
        public int calculateSize() {
            return 2 + this.DATA.length();
        }

        @Override
        public String toString() {
            return this.DATA;
        }

        final private String DATA;
    }

    /**
     * Stores an integer property data.
     */
    static public class IntData extends PropertyData {

        public IntData(ByteBuffer input) {
            this.DATA = input.getInt();
        }

        @Override
        public void write(ByteBuffer output) {
            output.putInt(DATA);
        }

        @Override
        public int calculateSize() {
            return 4;
        }

        @Override
        public String toString() {
            return Integer.toString(this.DATA);
        }

        final private int DATA;
    }

    /**
     * Stores a float property data.
     */
    static public class FloatData extends PropertyData {

        public FloatData(ByteBuffer input) {
            this.DATA = input.getFloat();
        }

        @Override
        public void write(ByteBuffer output) {
            output.putFloat(DATA);
        }

        @Override
        public int calculateSize() {
            return 4;
        }

        @Override
        public String toString() {
            return Float.toString(this.DATA);
        }

        final private float DATA;
    }

    /**
     * Stores a boolean property data.
     */
    static public class BoolData extends PropertyData {

        public BoolData(ByteBuffer input) {
            this.DATA = (input.get() != 0);
        }

        @Override
        public void write(ByteBuffer output) {
            output.put(this.DATA ? (byte) 1 : (byte) 0);
        }

        @Override
        public int calculateSize() {
            return 1;
        }

        @Override
        public String toString() {
            return Boolean.toString(this.DATA);
        }

        final private boolean DATA;
    }

    /**
     * Stores a variant property data.
     */
    static public class VarData extends PropertyData {

        public VarData(ByteBuffer input) {
            this.DATA = input.getInt();
        }

        @Override
        public void write(ByteBuffer output) {
            output.putInt(DATA);
        }

        @Override
        public int calculateSize() {
            return 4;
        }

        @Override
        public String toString() {
            return String.format("VAR: %s", this.DATA);
        }

        final private int DATA;
    }

    /**
     * Stores a struct property data.
     */
    static public class StructData extends PropertyData {

        public StructData(ByteBuffer input, ESPContext ctx) {
            int memberCount = input.getInt();
            this.MEMBERS = new java.util.ArrayList<>(memberCount);

            for (int i = 0; i < memberCount; i++) {
                Property p = new Property(input, ctx);
                this.MEMBERS.add(p);
            }
        }

        @Override
        public void write(ByteBuffer output) {
            output.putInt(this.MEMBERS.size());
            this.MEMBERS.forEach(p -> p.write(output));
        }

        @Override
        public int calculateSize() {
            return 4 + this.MEMBERS.stream().mapToInt(v -> v.calculateSize()).sum();
        }

        @Override
        public String toString() {
            return this.MEMBERS.stream()
                    .map(v -> v.toString())
                    .collect(Collectors.joining("; ", "{", "}"));
        }

        final private List<Property> MEMBERS;

    }

    /**
     * Stores an Array property data.
     *
     * @param <T> The type of PropertyData stored in the array.
     */
    static public class ArrayData<T extends PropertyData> extends PropertyData {

        protected ArrayData(ByteBuffer input, Supplier<T> reader) {
            int memberCount = input.getInt();
            this.MEMBERS = new java.util.ArrayList<>(memberCount);

            for (int i = 0; i < memberCount; i++) {
                T member = reader.get();
                this.MEMBERS.add(member);
            }
        }

        @Override
        public void write(ByteBuffer output) {
            output.putInt(this.MEMBERS.size());
            this.MEMBERS.forEach(t -> t.write(output));
        }

        @Override
        public int calculateSize() {
            int sum = 4;
            sum += this.MEMBERS.stream().mapToInt(t -> t.calculateSize()).sum();
            return sum;
        }

        @Override
        public String toString() {
            return MEMBERS.stream()
                    .map(v -> v.toString())
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        final private List<T> MEMBERS;
    }

}
