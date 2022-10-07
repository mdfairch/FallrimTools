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
import resaver.IString;

/**
 * FieldBasic represents all fields that aren't a VMAD section.
 *
 * @author Mark Fairchild
 */
public class FieldXXXX implements Field {

    /**
     * Creates a new FieldBasic by reading it from a LittleEndianInput.
     *
     * @param code The record code.
     * @param input The <code>ByteBuffer</code> to read.
     */
    public FieldXXXX(IString code, ByteBuffer input) {
        assert input.hasRemaining();
        assert code.equals(IString.get("XXXX"));

        this.CODE = code;
        this.DATA = input.getInt();
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.CODE.getUTF8());
        output.putShort((short) 4);
        output.putInt(this.DATA);
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        return 10;
    }

    /**
     * Returns the field code.
     *
     * @return The field code.
     */
    @Override
    public IString getCode() {
        return this.CODE;
    }

    /**
     * Returns a copy of the data section.
     *
     * @return A copy of the data array.
     */
    public int getData() {
        return this.DATA;
    }

    /**
     * Returns a String representation of the Field, which will just be the code
     * string.
     *
     * @return A string representation.
     *
     */
    @Override
    public String toString() {
        return this.getCode().toString();
    }

    final private IString CODE;
    final private int DATA;

}
