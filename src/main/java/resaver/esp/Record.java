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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import resaver.IString;
import resaver.ess.papyrus.EID;
import static resaver.esp.Entry.advancingSlice;

/**
 * Base class for the records of an ESP file.
 *
 * @author Mark Fairchild
 */
abstract public class Record implements Entry {

    /**
     * Reads a field from an ESP file input and returns it. Usually only one
     * field is readFully, but if the field is an XXXX type, the next field will
     * be returned as well.
     *
     * @param parentCode The recordcode of the containing record.
     * @param input The <code>ByteBuffer</code> to read.
     * @param ctx The mod descriptor.
     * @return A list of fields that were readFully.
     * @throws FieldException
     */
    static final public FieldList readField(RecordCode parentCode, ByteBuffer input, ESPContext ctx) throws FieldException {
        return readFieldAux(parentCode, input, 0, ctx);
    }

    /**
     * Reads a field from an ESP file input and returns it. Usually only one
     * field is readFully, but if the field is an XXXX type, the next field will
     * be returned as well.
     *
     * @param parentCode The recordcode of the containing record.
     * @param input The LittleEndianInput to readFully.
     * @param bigSize The size of the field, if it was specified externally (a
     * "XXXX" record).
     * @param ctx The mod descriptor.
     * @return A list of fields that were readFully.
     *
     */
    static private FieldList readFieldAux(RecordCode parentCode, ByteBuffer input, int bigSize, ESPContext ctx) throws FieldException {
        assert input.hasRemaining();
        IString CODE = IString.get("null");
        
        try {
            // Read the record identification code.
            final byte[] CODEBYTES = new byte[4];
            input.get(CODEBYTES);
            CODE = IString.get(new String(CODEBYTES));
            ctx.pushContext(CODE);

            // Read the record size.
            final boolean BIG = bigSize > 0;
            final int DATASIZE = Short.toUnsignedInt(input.getShort());
            final int ACTUALSIZE = (BIG ? bigSize : DATASIZE);

            // This list will hold between zero and two fields that are read.
            final FieldList FIELDS = new FieldList();

            if (ACTUALSIZE == 0) {
                ctx.popContext();
                return FIELDS;
            }

            final ByteBuffer FIELDINPUT = advancingSlice(input, ACTUALSIZE);

            // Depending on what code we found, pick a subclass to readFully in the
            // rest of the data.
            if (CODE.equals(IString.get("XXXX"))) {
                FieldXXXX xxxx = new FieldXXXX(CODE, FIELDINPUT);
                FieldList fieldsRead = readFieldAux(parentCode, input, xxxx.getData(), ctx);
                FIELDS.add(xxxx);
                FIELDS.addAll(fieldsRead);

            } else if (CODE.equals(IString.get("VMAD"))) {
                FieldVMAD field = new FieldVMAD(parentCode, CODE, FIELDINPUT, BIG, ctx);
                FIELDS.add(field);

            } else if (CODE.equals(IString.get("EDID"))) {
                FieldEDID field = new FieldEDID(CODE, FIELDINPUT, ACTUALSIZE, BIG, ctx);
                FIELDS.add(field);

            } else if (CODE.equals(IString.get("FULL"))) {
                FieldFull field = new FieldFull(CODE, FIELDINPUT, ACTUALSIZE, BIG, ctx);
                FIELDS.add(field);

            } else if (CODE.equals(IString.get("NAME")) && (parentCode == RecordCode.ACHR
                    || parentCode == RecordCode.REFR)) {
                FieldName field = new FieldName(CODE, FIELDINPUT, ACTUALSIZE, BIG, ctx);
                FIELDS.add(field);

            } else {
                Field field = new FieldSimple(CODE, FIELDINPUT, ACTUALSIZE, BIG, ctx);
                FIELDS.add(field);
            }

            ctx.popContext();
            return FIELDS;
            
        } catch (RuntimeException ex) {
            throw new FieldException(ex, CODE.toString(), ctx.toString());
        }
    }

    /**
     * Returns the record code.
     *
     * @return The record code.
     */
    abstract public RecordCode getCode();

    /**
     * Reads a record from an ESP file input and returns it.
     *
     * @param input The LittleEndianInput to readFully.
     * @param ctx The mod descriptor.
     * @return The next Record from input.
     * @throws RecordException
	 * @throws FieldException
     */
    static public Record readRecord(ByteBuffer input, ESPContext ctx) throws RecordException, FieldException {
        // Read the record identification code.
        final byte[] CODEBYTES = new byte[4];
        input.get(CODEBYTES);
        final String CODESTRING = new String(CODEBYTES);
        final RecordCode CODE = RecordCode.valueOf(CODESTRING);

        // Read the record size.
        final int DATASIZE = input.getInt();

        // GRUPs get handled differently than other records.
        if (CODE == RecordCode.GRUP) {
            // Read the header.
            final ByteBuffer HEADER = advancingSlice(input, 16);

            // Read the record data.
            final ByteBuffer RECORDINPUT = advancingSlice(input, DATASIZE - 24);

            // Read the rest of the record.
            return new RecordGrup(CODE, HEADER, RECORDINPUT, ctx);

        } else {
            // Read the header.
            final Header HEADER = new Header(input, ctx);

            // Read the record data.
            final ByteBuffer RECORDINPUT = advancingSlice(input, DATASIZE);

            // Read the rest of the record. Handle compressed records separately.
            if (HEADER.isCompressed()) {
                try {
                    return new RecordCompressed(CODE, HEADER, RECORDINPUT, ctx);
                } catch (DataFormatException ex) {
                    throw new IllegalStateException("Failed to read compressd record. " + ctx, ex);
                }

            } else {
                return new RecordBasic(CODE, HEADER, RECORDINPUT, ctx);
            }
        }
    }

    /**
     * Reads a record from an ESP file input and returns it.
     *
     * @param input The LittleEndianInput to readFully.
     * @param ctx The mod descriptor.
     * @throws RecordException
	 * @throws FieldException
     * 
     */
    static public void skimRecord(ByteBuffer input, ESPContext ctx) throws RecordException, FieldException {
        // Read the record identification code.
        final byte[] CODEBYTES = new byte[4];
        input.get(CODEBYTES);
        final String CODESTRING = new String(CODEBYTES);
        final RecordCode CODE;
       
        try {
            CODE = RecordCode.valueOf(CODESTRING);
        } catch (Exception ex) {
            throw ex;
        }

        // Read the record size.
        final int DATASIZE = input.getInt();

        // GRUPs get handled differently than other records.
        if (CODE == RecordCode.GRUP) {
            final ByteBuffer HEADER = advancingSlice(input, 16);

            final int PREFIX = HEADER.getInt();
            final int TYPE = HEADER.getInt();

            switch (TYPE) {
                case 0:
                    final ByteBuffer TOP = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(PREFIX);
                    ((Buffer) TOP).flip();
                    String tops = new String(TOP.array());
                    ctx.pushContext("TOP(" + tops + ")");
                    break;
                case 1:
                    ctx.pushContext("GRUP Wrld children " + EID.pad8(PREFIX));
                case 2:
                    ctx.pushContext("Interior Block " + PREFIX);
                    break;
                case 3:
                    ctx.pushContext("Interior SubBlock " + PREFIX);
                    break;
                case 4:
                    ctx.pushContext("Exerior Block: " + grupCoords(PREFIX));
                case 5:
                    ctx.pushContext("Exerior SubBlock: " + grupCoords(PREFIX));
                    break;
                case 6:
                    ctx.pushContext("CELL children " + EID.pad8(PREFIX));
                    break;
                case 7:
                    ctx.pushContext("Children of TOPIC" + EID.pad8(PREFIX));
                    break;
                case 8:
                    ctx.pushContext("CELL Persistent Children" + EID.pad8(PREFIX));
                    break;
                case 9:
                    ctx.pushContext("CELL Temprorary Children" + EID.pad8(PREFIX));
                    break;
                default:
                    ctx.pushContext("UnkBlock " + EID.pad8(PREFIX));
                    break;
            }

            // Get the record data.
            final ByteBuffer RECORDINPUT = advancingSlice(input, DATASIZE - 24);

            try {
                // Read the rest of the record.
                while (RECORDINPUT.hasRemaining()) {
                    Record.skimRecord(RECORDINPUT, ctx);
                }
            } finally {
                ctx.popContext();
            }

        } else {
            // Read the header.
            final Header HEADER = new Header(input, ctx);
            ctx.pushContext(EID.pad8(HEADER.ID));

            // Read the record data.
            final ByteBuffer RECORDINPUT = advancingSlice(input, DATASIZE);

            // Read the rest of the record. Handle compressed records separately.
            if (HEADER.isCompressed()) {
                try {
                    RecordCompressed.skimRecord(CODE, HEADER, RECORDINPUT, ctx);
                } catch (DataFormatException ex) {
                    throw new IllegalStateException("Failed to read compressd record. " + ctx, ex);                    
                }

            } else {
                RecordBasic.skimRecord(CODE, HEADER, RECORDINPUT, ctx);
            }

            ctx.popContext();
        }
    }

    /**
     * Header represents the standard header for all records except GRUP.
     *
     * @author Mark Fairchild
     */
    static public class Header implements Entry {

        /**
         * Creates a new Header by reading it from a LittleEndianInput.
         *
         * @param input The LittleEndianInput to readFully.
         * @param ctx The mod descriptor.
         */
        public Header(ByteBuffer input, ESPContext ctx) {
            this.FLAGS = input.getInt();

            int id = input.getInt();
            int newID = ctx.remapFormID(id);
            this.ID = newID;

            this.REVISION = input.getInt();
            this.VERSION = input.getShort();
            this.UNKNOWN = input.getShort();
        }

        /**
         * @see Entry#write(transposer.ByteBuffer)
         */
        @Override
        public void write(ByteBuffer output) {
            output.putInt(this.FLAGS);
            output.putInt(this.ID);
            output.putInt(this.REVISION);
            output.putShort(this.VERSION);
            output.putShort(this.UNKNOWN);
        }

        /**
         * @return The calculated size of the field.
         * @see Entry#calculateSize()
         */
        @Override
        public int calculateSize() {
            return 16;
        }

        /**
         * Checks if the header indicates a compressed record.
         *
         * @return True if the field data is compressed, false otherwise.
         */
        public boolean isCompressed() {
            return (this.FLAGS & 0x00040000) != 0;
        }

        /**
         * Checks if the header indicates localization (TES4 record only).
         *
         * @return True if the record is a TES4 and localization is enabled.
         */
        public boolean isLocalized() {
            return (this.FLAGS & 0x00000080) != 0;
        }

        final public int FLAGS;
        final public int ID;
        final public int REVISION;
        final public short VERSION;
        final public short UNKNOWN;

    }
    
    static private mf.Pair<Integer, Integer>grupCoords(int code) {
        return mf.Pair.of(code & 0xFFFF, code >>> 4);
    }
}
