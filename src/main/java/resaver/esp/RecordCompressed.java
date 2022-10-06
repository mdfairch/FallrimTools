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
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.zip.DataFormatException;
import mf.BufferUtil;

/**
 * RecordCompressed represents all records that are compressed.
 *
 * @author Mark Fairchild
 */
public class RecordCompressed extends Record {

    /**
     * Skims a RecordCompressed by reading it from a LittleEndianInput.
     *
     * @param recordCode The record code.
     * @param header The header.
     * @param input The <code>ByteBuffer</code> to read.
     * @param ctx The mod descriptor.
     */
    static public void skimRecord(RecordCode recordCode, Record.Header header, ByteBuffer input, ESPContext ctx) throws DataFormatException {
        assert input.hasRemaining();

        final int DECOMPRESSED_SIZE = input.getInt();
        ByteBuffer uncompressed = BufferUtil.inflateZLIB(input, DECOMPRESSED_SIZE);
        uncompressed.order(ByteOrder.LITTLE_ENDIAN);

        final FieldList FIELDS = new FieldList();

        while (uncompressed.hasRemaining()) {
            FieldList newFields = Record.readField(recordCode, uncompressed, ctx);
            FIELDS.addAll(newFields);
        }

        ctx.PLUGIN_INFO.addRecord(header.ID, recordCode, FIELDS);
    }

    /**
     * Creates a new RecordCompressed by reading it from a LittleEndianInput.
     *
     * @param recordCode The record code.
     * @param header The header.
     * @param input The LittleEndianInput to readFully.
     * @param ctx The mod descriptor.
     * @throws java.util.zip.DataFormatException
     */
    public RecordCompressed(RecordCode recordCode, Record.Header header, ByteBuffer input, ESPContext ctx) throws DataFormatException {
        assert input.hasRemaining();
        this.CODE = recordCode;
        this.HEADER = header;
        this.FIELDS = new FieldList();

        final int DECOMPRESSED_SIZE = input.getInt();
        ByteBuffer uncompressed = BufferUtil.inflateZLIB(input, DECOMPRESSED_SIZE);
        uncompressed.order(ByteOrder.LITTLE_ENDIAN);

        while (uncompressed.hasRemaining()) {
            FieldList newFields = Record.readField(recordCode, uncompressed, ctx);
            FIELDS.addAll(newFields);
        }
    }

    /**
     *
     * @return The total size of the uncompressed data in the
     * <code>Record</code>.
     */
    private int getUncompressedSize() {
        return this.FIELDS.stream().mapToInt(f -> f.calculateSize()).sum();
    }

    /**
     */
    private ByteBuffer getUncompressedData() {
        final ByteBuffer DATA = ByteBuffer.allocate(this.getUncompressedSize());
        this.FIELDS.forEach(field -> field.write(DATA));
        return DATA;
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     * @param output The ByteBuffer.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.CODE.toString().getBytes(UTF_8));

        final ByteBuffer UNCOMPRESSED = this.getUncompressedData();
        final int UNCOMPRESSED_SIZE = UNCOMPRESSED.capacity();
        ((Buffer) UNCOMPRESSED).flip();
        ByteBuffer COMPRESSED = BufferUtil.deflateZLIB(UNCOMPRESSED, UNCOMPRESSED_SIZE);

        final int COMPRESSED_SIZE = COMPRESSED.limit();
        output.putInt(4 + COMPRESSED_SIZE);
        this.HEADER.write(output);
        output.putInt(UNCOMPRESSED_SIZE);
        output.put(COMPRESSED);
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        final ByteBuffer UNCOMPRESSED = this.getUncompressedData();
        final int UNCOMPRESSED_SIZE = UNCOMPRESSED.capacity();

        ((Buffer) UNCOMPRESSED).flip();
        ByteBuffer COMPRESSED = BufferUtil.deflateZLIB(UNCOMPRESSED, UNCOMPRESSED_SIZE);
        final int COMPRESSED_SIZE = COMPRESSED.capacity();

        return 28 + COMPRESSED_SIZE;
    }

    /**
     * Returns the record code.
     *
     * @return The record code.
     */
    @Override
    public RecordCode getCode() {
        return this.CODE;
    }

    /**
     * Returns a String representation of the Record, which will just be the
     * code string.
     *
     * @return A string representation.
     *
     */
    @Override
    public String toString() {
        return this.getCode().toString();
    }

    final private RecordCode CODE;
    final private Record.Header HEADER;
    final private FieldList FIELDS;

}
