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

/**
 * RecordBasic represents all records that are not a GRUP and do not contain
 * compressed fields.
 *
 * @author Mark Fairchild
 */
public class RecordBasic extends Record {

    /**
     * Skims a RecordBasic by reading it from a LittleEndianInput.
     *
     * @param recordCode The record code.
     * @param header The header.
     * @param input The <code>ByteBuffer</code> to read.
     * @param ctx The mod descriptor.
     * @throws RecordException 
     * 
     */
    static public void skimRecord(RecordCode recordCode, Record.Header header, ByteBuffer input, ESPContext ctx) throws RecordException {
        try {
            final FieldList FIELDS = new FieldList();

            while (input.hasRemaining()) {
                FieldList newFields = Record.readField(recordCode, input, ctx);
                FIELDS.addAll(newFields);
            }

            ctx.PLUGIN_INFO.addRecord(header.ID, recordCode, FIELDS);
        } catch (RuntimeException | FieldException ex) {
            throw new RecordException("Error reading record " + recordCode, ex, ctx.toString());
        }
    }

    /**
     * Creates a new RecordBasic by reading it from a LittleEndianInput.
     *
     * @param recordCode The record code.
     * @param header The header.
     * @param input The <code>ByteBuffer</code> to read.
     * @param ctx The mod descriptor.
     * @throws RecordException
     */
    public RecordBasic(RecordCode recordCode, Record.Header header, ByteBuffer input, ESPContext ctx) throws RecordException {
        try {
            this.CODE = recordCode;
            this.HEADER = header;       
            this.FIELDS = new FieldList();

            while (input.hasRemaining()) {
                FieldList newFields = Record.readField(recordCode, input, ctx);
                FIELDS.addAll(newFields);
            }
        } catch (RuntimeException | FieldException ex) {
            throw new RecordException("Error reading record " + recordCode, ex, ctx.toString());            
        }
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     * @param output The ByteBuffer.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.CODE.toString().getBytes(UTF_8));
        output.putInt(this.calculateSize() - 24);
        this.HEADER.write(output);
        this.FIELDS.forEach(field -> field.write(output));
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        int sum = 24;
        sum += this.FIELDS.stream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
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
     * @return The record header.
     */
    public Record.Header getHeader() {
        return this.HEADER;
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
