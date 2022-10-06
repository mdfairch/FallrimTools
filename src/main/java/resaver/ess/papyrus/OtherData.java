/*
 * Copyright 2017 Mark.
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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import resaver.ess.Flags;
import resaver.ess.GeneralElement;
import resaver.ess.Element;
import resaver.ess.ElementException;
import resaver.ess.Linkable;

/**
 *
 * @author Mark Fairchild
 */
public class OtherData extends GeneralElement implements PapyrusElement {

    /*static final private int[] SIZES_SLE = {8, 8, 8, 16, 16, -1, 17, };
    static final private int[] SIZES_SSE = {};
    static final private int[] SIZES_FO4 = {};*/
    public OtherData(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
        try {
            this.ARRAY1 = this.read32ElemArray(input, "Array1", (in) -> new Array1(in, context));
            LOG.info(String.format("Read ARRAY1, %d elements.", this.ARRAY1.length));

            this.ARRAY1A = this.read32ElemArray(input, "Array1a", (in) -> new Array1A(in, context));
            LOG.info(String.format("Read ARRAY1A, %d elements.", this.ARRAY1A.length));

            this.ARRAY2 = this.read32ElemArray(input, "Array2", (in) -> new Array2(in, context));
            LOG.info(String.format("Read ARRAY2, %d elements.", this.ARRAY2.length));

            this.ARRAY3 = this.read32ElemArray(input, "Array3", (in) -> new Array3(in, context));
            LOG.info(String.format("Read ARRAY3, %d elements.", this.ARRAY3.length));

            this.ARRAY4 = this.read32ElemArray(input, "Array4", (in) -> new Array4(in, context));
            LOG.info(String.format("Read ARRAY4, %d elements.", this.ARRAY4.length));

            this.SCRIPTS = this.read32ElemArray(input, "Scripts", (in) -> new LString(in));
            LOG.info(String.format("Read SCRIPTS, %d element.", this.SCRIPTS.length));

            this.ARRAY4A = this.read32ElemArray(input, "Array4A", (in) -> new Array4A(in, context));
            LOG.info(String.format("Read ARRAY4A, %d elements.", this.ARRAY4A.length));

            this.ARRAY4B = this.read32ElemArray(input, "Array4b", (in) -> new Array4B(in, context));
            LOG.info(String.format("Read ARRAY4B, %d elements.", this.ARRAY4B.length));

            this.ARRAY4C = this.read32ElemArray(input, "Array4c", (in) -> new Array4C(in, context));
            LOG.info(String.format("Read ARRAY4C, %d elements.", this.ARRAY4C.length));

            this.ARRAY4D = this.read32ElemArray(input, "Array4d", (in) -> new Array4D(in, context));
            LOG.info(String.format("Read ARRAY4D, %d elements.", this.ARRAY4D.length));

            this.ARRAY5 = this.read32ElemArray(input, "Array5", (in) -> new Array5(in, context));
            LOG.info(String.format("Read ARRAY5, %d elements.", this.ARRAY5.length));

            this.ARRAY6 = this.read32ElemArray(input, "Array6", (in) -> new Array6(in, context));
            LOG.info(String.format("Read ARRAY6, %d elements.", this.ARRAY6.length));

            this.ARRAY7 = this.read32ElemArray(input, "Array7", (in) -> new Array7(in, context));
            LOG.info(String.format("Read ARRAY7, %d elements.", this.ARRAY7.length));

            this.ARRAY8 = this.read32ElemArray(input, "Array8", (in) -> new Array8(in, context));
            LOG.info(String.format("Read ARRAY8, %d elements.", this.ARRAY8.length));

            this.ARRAY9 = null;
            //LOG.info(String.format("Read ARRAY9, %d elements.", this.ARRAY9.length));

            this.ARRAY10 = null;
            //LOG.info(String.format("Read ARRAY10, %d elements.", this.ARRAY10.length));

            this.ARRAY11 = null;
            //LOG.info(String.format("Read ARRAY11, %d elements.", this.ARRAY11.length));

            this.ARRAY12 = null;
            //LOG.info(String.format("Read ARRAY12, %d elements.", this.ARRAY12.length));

            this.ARRAY13 = null;
            //LOG.info(String.format("Read ARRAY13, %d elements.", this.ARRAY13.length));

            this.ARRAY14 = null;
            //.info(String.format("Read ARRAY14, %d elements.", this.ARRAY14.length));

            this.ARRAY15 = null;
            //LOG.info(String.format("Read ARRAY15, %d elements.", this.ARRAY15.length));
        } catch (ElementException ex) {
            throw new PapyrusFormatException(ex);
        }
    }

    public List<Element[]> getArrays() {
        return Arrays.asList(
                this.ARRAY1,
                this.ARRAY2,
                this.ARRAY3,
                this.ARRAY4,
                this.SCRIPTS,
                this.ARRAY4A,
                this.ARRAY4B,
                this.ARRAY4C,
                this.ARRAY4D,
                this.ARRAY5,
                this.ARRAY6,
                this.ARRAY7,
                this.ARRAY8,
                this.ARRAY9,
                this.ARRAY10,
                this.ARRAY11,
                this.ARRAY12,
                this.ARRAY13,
                this.ARRAY14,
                this.ARRAY15
        );
    }

    final public Element[] ARRAY1;
    final public Element[] ARRAY1A;
    final public Element[] ARRAY2;
    final public Element[] ARRAY3;
    final public Element[] ARRAY4;
    final public Element[] SCRIPTS;
    final public Element[] ARRAY4A;
    final public Element[] ARRAY4B;
    final public Element[] ARRAY4C;
    final public Element[] ARRAY4D;
    final public Element[] ARRAY5;
    final public Element[] ARRAY6;
    final public Element[] ARRAY7;
    final public Element[] ARRAY8;
    final public Element[] ARRAY9;
    final public Element[] ARRAY10;
    final public Element[] ARRAY11;
    final public Element[] ARRAY12;
    final public Element[] ARRAY13;
    final public Element[] ARRAY14;
    final public Element[] ARRAY15;

    static class Array1 extends GeneralElement {

        public Array1(ByteBuffer input, PapyrusContext context) throws ElementException {
            final EID ID1 = super.readID32(input, "ID1", context);
            final EID ID2 = super.readID32(input, "ID2", context);
            this.LINK1 = context.findAny(ID1);
            this.THREAD = context.findActiveScript(ID2);
        }

        @Override
        public String toString() {
            if (null == this.THREAD) {
                return "INVALID (" + super.toString() + ")";
            } else {
                return this.THREAD.toString() + "(" + super.toString() + ")";
            }
        }

        final private Linkable LINK1;
        final private ActiveScript THREAD;
    }

    static class Array1A extends GeneralElement {

        public Array1A(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readID32(input, "ID1", context);
            final EID ID = context.getGame().isFO4()
                    ? super.readID64(input, "ID2", context)
                    : super.readID32(input, "ID2", context);

            this.THREAD = context.findActiveScript(ID);
        }

        @Override
        public String toString() {
            if (null == this.THREAD) {
                return "INVALID (" + super.toString() + ")";
            } else {
                return this.THREAD.toString() + "(" + super.toString() + ")";
            }
        }

        final private ActiveScript THREAD;
    }

    static class Array2 extends GeneralElement {

        public Array2(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readID32(input, "ID1", context);
            final EID ID = context.getGame().isFO4()
                    ? super.readID64(input, "ID2", context)
                    : super.readID32(input, "ID2", context);

            this.THREAD = context.findActiveScript(ID);
        }

        @Override
        public String toString() {
            if (this.THREAD == null) {
                return "null(" + super.toString() + ")";
            } else {
                return this.THREAD.toString() + "(" + super.toString() + ")";
            }
        }

        final private ActiveScript THREAD;
    }

    static class Array3 extends GeneralElement {

        public Array3(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readByte(input, "type");
            super.readShort(input, "str1");
            super.readShort(input, "unk1");
            super.readShort(input, "str2");
            super.readInt(input, "unk2");
            super.readElement(input, "flags", Flags::readShortFlags);
            super.readRefID(input, "refID", context);
        }
    }

    static class Array4 extends GeneralElement {

        public Array4(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readShort(input, "str1");
            super.readShort(input, "unk1");
            super.readByte(input, "unk2");
            super.readShort(input, "str2");
            super.readInt(input, "unk3");
            super.readElement(input, "flags", Flags::readShortFlags);
            super.readRefID(input, "refID", context);
        }
    }

    static class Array4A extends GeneralElement {

        public Array4A(ByteBuffer input, PapyrusContext context) {
        }
    }

    static class Array4B extends GeneralElement {

        public Array4B(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readByte(input, "unk1");
            super.readShort(input, "unk2");
            super.readShort(input, "unk3");
            super.readRefID(input, "ref1", context);
            super.readRefID(input, "ref2", context);
            super.readRefID(input, "ref3", context);
            super.readRefID(input, "ref4", context);
        }
    }

    static class Array4C extends GeneralElement {

        public Array4C(ByteBuffer input, PapyrusContext context) throws ElementException {
            final byte FLAG = super.readByte(input, "flag");
            super.readInt(input, "data");
            super.readRefID(input, "ref", context);

            if (FLAG >= 0 && FLAG <= 6) {
                super.readInts(input, "data1array", 3);
            }

            if (FLAG == 0) {
                super.readInts(input, "data2array", 4);
            }

            if (FLAG >= 0 && FLAG <= 3) {
                super.readByte(input, "unk");
            }
        }
    }

    static class Array4D extends GeneralElement {

        public Array4D(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readByte(input, "flag1");
            super.readInt(input, "unk2");
            super.readByte(input, "flag2");
            super.readRefID(input, "ref", context);
        }
    }

    static class Array5 extends GeneralElement {

        public Array5(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readShort(input, "unk1");
            super.readShort(input, "unk2");
            super.readRefID(input, "ref1", context);
            super.readRefID(input, "ref2", context);
            super.readRefID(input, "ref3", context);
            super.readShort(input, "unk4");
        }
    }

    static class Array6 extends GeneralElement {

        public Array6(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readShort(input, "unk");
            super.readElement(input, "flags", Flags::readShortFlags);
            super.readRefID(input, "ref", context);
        }
    }

    static class Array7 extends GeneralElement {

        public Array7(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readShort(input, "unk");
            super.readElement(input, "flags", Flags::readShortFlags);
            super.readRefID(input, "ref", context);
        }
    }

    static class Array8 extends GeneralElement {

        public Array8(ByteBuffer input, PapyrusContext context) throws ElementException {
            super.readShort(input, "unk");
            super.readShort(input, "type");
            super.readRefID(input, "ref", context);
            final int COUNT1 = super.readInt(input, "count1");
            final int COUNT2 = super.readInt(input, "count2");
            super.readElements(input, "refArray1", COUNT1, in -> context.readRefID(in));
            super.readElements(input, "refArray2", COUNT2, in -> context.readRefID(in));
        }
    }

    static class LString extends GeneralElement {

        public LString(ByteBuffer input) throws ElementException {
            final int COUNT = this.readInt(input, "COUNT");
            assert 0 <= COUNT;
            final byte[] BYTES = super.readBytes(input, "MEMBERS", COUNT);
            this.STRING = new String(BYTES);
        }

        @Override
        public String toString() {
            return this.STRING;
        }

        final public String STRING;
    }

    static final private Logger LOG = Logger.getLogger(OtherData.class.getCanonicalName());
}
