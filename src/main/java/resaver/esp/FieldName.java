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
 * FieldSimple represents a Name field.
 *
 * @author Mark Fairchild
 */
public class FieldName extends FieldSimple {

    /**
     * Creates a new FieldSimple by reading it from a <code>ByteBuffer</code>.
     *
     * @param code The field code.
     * @param input The <code>ByteBuffer</code> to read.
     * @param size The amount of data.
     * @param big A flag indicating that this is a BIG field.
     * @param ctx The mod descriptor.
     */
    public FieldName(IString code, ByteBuffer input, int size, boolean big, ESPContext ctx) {
        super(code, input, size, big, ctx);
        assert size == 4;
        int id = super.getByteBuffer().getInt();
        int newID = ctx.remapFormID(id);
        this.FORMID = newID;
    }

    /**
     * @return The formID value.
     */
    public int getFormID() {
        return this.FORMID;
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
        final StringBuilder BUF = new StringBuilder();
        BUF.append(this.getCode()).append("=").append(this.FORMID);
        return BUF.toString();
    }

    final private int FORMID;
}
