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
import java.util.LinkedList;

/**
 * Describes GRUP records.
 *
 * @author Mark Fairchild
 */
public class RecordGrup extends Record {

    /**
     * Creates a new RecordGRUP by reading it from a LittleEndianInput.
     *
     * @param code The record code, which must be RecordCode.GRUP.
     * @param headerData The header data (unused).
     * @param input The LittleEndianInput to read.
     * @param ctx The mod descriptor.
     * @throws RecordException
     * @throws FieldException
     * 
     */
    public RecordGrup(RecordCode code, ByteBuffer headerData, ByteBuffer input, ESPContext ctx) throws RecordException, FieldException {
        this.CODE = code;
        this.HEADER = headerData;
        this.RECORDS = new LinkedList<>();

        while (input.hasRemaining()) {
            Record record = Record.readRecord(input, ctx);
            this.RECORDS.add(record);
        }
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     * @param output The ByteBuffer.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.CODE.toString().getBytes(UTF_8));
        output.putInt(this.calculateSize());
        output.put(this.HEADER);
        this.RECORDS.forEach(record -> record.write(output));
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        int sum = 24;
        sum += this.RECORDS.stream().mapToInt(v -> v.calculateSize()).sum();
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
    final private ByteBuffer HEADER;
    final private List<Record> RECORDS;

}
