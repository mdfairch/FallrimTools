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
import java.util.List;
import resaver.IString;

/**
 * Describes a script entry in a VMAD field.
 *
 * @author Mark Fairchild
 */
public class Script implements Entry {

    /**
     * Creates a new Script by reading it from a LittleEndianInput.
     *
     * @param input The <code>ByteBuffer</code> to read.
     * @param ctx
     */
    public Script(ByteBuffer input, ESPContext ctx) {
        this.NAME = IString.get(mf.BufferUtil.getWString(input));
        //if (this.NAME.isEmpty()) {
        //    this.PROPERTIES = null;
        //    this.STATUS = 0;
        //    return;
        //}

        ctx.pushContext("script:" + (this.NAME.isEmpty() ? "BLANK" : this.NAME));

        this.STATUS = input.get();

        int propertyCount = Short.toUnsignedInt(input.getShort());
        this.PROPERTIES = new java.util.ArrayList<>(propertyCount);

        try {
            for (int i = 0; i < propertyCount; i++) {
                Property prop = new Property(input, ctx);
                this.PROPERTIES.add(prop);
            }

        } finally {
            ctx.popContext();
        }
    }

    /**
     * Writes the Script.
     *
     * @param output The ByteBuffer to write.
     */
    @Override
    public void write(ByteBuffer output) {
        if (this.NAME.isEmpty()) {
            output.put(this.NAME.getUTF8());
            return;
        }

        output.put(this.NAME.getUTF8());
        output.put(this.STATUS);
        output.putShort((short) this.PROPERTIES.size());
        this.PROPERTIES.forEach(prop -> prop.write(output));
    }

    /**
     * @return The calculated size of the Script.
     */
    @Override
    public int calculateSize() {
        if (this.NAME.isEmpty()) {
            return 2;
        }

        int sum = 5 + NAME.length();
        sum += this.PROPERTIES.stream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
    }

    final public IString NAME;
    final private byte STATUS;
    final List<Property> PROPERTIES;

}
