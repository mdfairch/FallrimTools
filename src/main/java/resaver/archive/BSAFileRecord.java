/*
 * Copyright 2016 Mark Fairchild
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

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Describes a BSA file record.
 *
 * @author Mark Fairchild
 */
class BSAFileRecord {

    /**
     * Creates a new <code>FileRecord</code> by reading from a
     * <code>LittleEndianDataInput</code>. The name field will be set to null.
     *
     * @param input The file from which to readFully.
     * @param header
     * @param names
     *
     */
    public BSAFileRecord(ByteBuffer input, BSAHeader header, Supplier<String> names) {
        this.NAMEHASH = input.getLong();

        int size = input.getInt();
        final int BIT30 = 1 << 30;
        boolean compressToggle = ((size & BIT30) != 0);

        this.FILESIZE = size & ~BIT30;
        this.OFFSET = input.getInt();
        this.ISCOMPRESSED = header.ISCOMPRESSED ^ compressToggle;
        this.NAME = names.get();
    }

    @Override
    public String toString() {
        if (this.NAME == null) {
            return String.format("%d bytes at offset %d", this.FILESIZE, this.OFFSET);
        } else {
            return this.NAME;
        }
    }

    public Path getPath() {
        try {
            return Paths.get(this.NAME);
        } catch (java.nio.file.InvalidPathException ex) {
            return null;
        }
    }

    static final int SIZE = 16;
    
    final public long NAMEHASH;
    final public int FILESIZE;
    final public int OFFSET;
    final public boolean ISCOMPRESSED;
    final public String NAME;
}
