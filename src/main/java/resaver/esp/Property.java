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
package resaver.esp;

import java.nio.ByteBuffer;
import resaver.IString;

/**
 * Describes a property entry in a VMAD's scripts.
 *
 * @author Mark Fairchild
 */
final public class Property implements Entry {

    /**
     * Creates a new Property by reading it from a LittleEndianInput.
     *
     * @param input The <code>ByteBuffer</code> to read.
     * @param ctx
     */
    public Property(ByteBuffer input, ESPContext ctx) {
        this.NAME = IString.get(mf.BufferUtil.getWString(input));
        ctx.pushContext("prop:" + this.NAME);

        this.TYPE = input.get();
        this.STATUS = input.get();
        
        try {
            this.DATA = PropertyData.readPropertyData(this.TYPE, input, ctx);
        } finally {
            ctx.popContext();
        }
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     * @param output The ByteBuffer.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.NAME.getUTF8());
        output.put(this.TYPE);
        output.put(this.STATUS);
        this.DATA.write(output);
    }

    /**
     * @return The calculated size of the Script.
     */
    @Override
    public int calculateSize() {
        int sum = 4 + this.NAME.length() + this.DATA.calculateSize();
        return sum;
    }

    @Override
    public String toString() {
        return String.format("%s: %d (%02x): %s", this.NAME, this.TYPE, this.STATUS, this.DATA);
    }

    private final IString NAME;
    private final byte TYPE;
    private final byte STATUS;
    private final PropertyData DATA;

}
