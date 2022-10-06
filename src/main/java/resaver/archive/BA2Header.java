/*
 * Copyright 2018 Mark.
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
package resaver.archive;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Describes a BA2 header.
 *
 * @author Mark
 */
public class BA2Header {

    /**
     * Creates a new <code>BA2Header</code> by reading from a
     * <code>ByteBuffer</code>.
     * 
     * @param input The file from which to readFully.
     * @throws IOException
     */
    public BA2Header(ByteBuffer input) throws IOException {
        this.MAGIC1 = new byte[4];
        input.get(this.MAGIC1);
        this.VERSION = input.getInt();
        this.MAGIC2 = new byte[4];
        input.get(this.MAGIC2);

        Type type;
        try {
            String magic = new String(this.MAGIC1);
            type = Type.valueOf(magic);
            if (type != Type.BTDX) {
                throw new IOException("Invalid archive format: " + new String(this.MAGIC1));
            }
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid archive format: " + new String(this.MAGIC1), ex);
        }

        BA2Subtype subtype;
        try {
            String magic = new String(this.MAGIC2);
            subtype = BA2Subtype.valueOf(magic);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid archive format: " + new String(this.MAGIC2), ex);
        }

        this.TYPE = type;
        this.SUBTYPE = subtype;
        this.FILE_COUNT = input.getInt();
        this.NAMETABLE_OFFSET = input.getLong();
    }

    static final int SIZE = 24;
    final public Type TYPE;
    final public int VERSION;
    final public BA2Subtype SUBTYPE;
    final public byte[] MAGIC1;
    final public byte[] MAGIC2;
    final public int FILE_COUNT;
    final public long NAMETABLE_OFFSET;

}
