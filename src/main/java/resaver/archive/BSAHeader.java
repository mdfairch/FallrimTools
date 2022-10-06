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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Describes a BSA header.
 *
 * @author Mark Fairchild
 */
final class BSAHeader {

    /**
     * Creates a new <code>BSAHeader</code> by reading from a
     * <code>LittleEndianDataInput</code>.
     *
     * @param input The file from which to readFully.
     * @param name The filename.
     * @throws IOException
     */
    public BSAHeader(ByteBuffer input, String name) throws IOException {
        Objects.requireNonNull(input);
        this.NAME = Objects.requireNonNull(name);        
        
        this.MAGIC = new byte[4];
        input.get(this.MAGIC);

        Type type;
        try {
            String magic = new String(this.MAGIC);
            type = Type.valueOf(magic.trim());
            if (type != Type.BSA) {
                throw new IOException("Invalid archive format: " + new String(this.MAGIC));
            }
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid archive format: " + new String(this.MAGIC), ex);
        }

        this.TYPE = type;
        this.VERSION = input.getInt();
        this.FOLDER_OFFSET = input.getInt();
        this.ARCHIVE_FLAGS = input.getInt();
        this.FOLDER_COUNT = input.getInt();
        this.FILE_COUNT = input.getInt();
        this.TOTAL_FOLDERNAME_LENGTH = input.getInt();
        this.TOTAL_FILENAME_LENGTH = input.getInt();
        this.FILE_FLAGS = input.getInt();
        
        this.INCLUDE_DIRECTORYNAMES = (this.ARCHIVE_FLAGS & 0x1) != 0;
        this.INCLUDE_FILENAMES = (this.ARCHIVE_FLAGS & 0x2) != 0;
        this.ISCOMPRESSED = (this.ARCHIVE_FLAGS & 0x4) != 0;
        this.EMBED_FILENAME = (this.ARCHIVE_FLAGS & 0x100) != 0;
    }

    @Override
    public String toString() {
        return this.NAME;
    }

    static final int SIZE = 36;
    final public String NAME;
    final public Type TYPE;
    final public byte[] MAGIC;
    final public int VERSION;
    final public int FOLDER_OFFSET;
    final public int ARCHIVE_FLAGS;
    final public int FOLDER_COUNT;
    final public int FILE_COUNT;
    final public int TOTAL_FOLDERNAME_LENGTH;
    final public int TOTAL_FILENAME_LENGTH;
    final public int FILE_FLAGS;

    final public boolean INCLUDE_DIRECTORYNAMES;
    final public boolean INCLUDE_FILENAMES;
    final public boolean ISCOMPRESSED;
    final public boolean EMBED_FILENAME;

}
