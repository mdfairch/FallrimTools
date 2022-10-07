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
package resaver.archive;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import static mf.BufferUtil.getZString;


/**
 * Describes a BSA folder record.
 *
 * @author Mark Fairchild
 */
final class BSAFolderRecord {

    /**
     * Creates a new <code>BSAFolder</code> by reading from a
     * <code>LittleEndianDataInput</code>.
     *
     * @param input The file from which to read.
     * @param header
     * @param channel
     * @param names
     *
     */
    public BSAFolderRecord(ByteBuffer input, BSAHeader header, FileChannel channel, Supplier<String> names) throws IOException {
        this.NAMEHASH = input.getLong();
        this.COUNT = input.getInt();

        switch (header.VERSION) {
            case 103:
            case 104:
                this.PADDING = 0;
                this.OFFSET = input.getInt();
                break;
            case 105:
                this.PADDING = input.getInt();
                this.OFFSET = input.getLong();
                break;
            default:
                throw new IOException("Unknown header version " + header.VERSION);
        }

        final ByteBuffer BLOCK = ByteBuffer.allocate(1024 + this.COUNT * BSAFileRecord.SIZE);
        channel.read(BLOCK, this.OFFSET - header.TOTAL_FILENAME_LENGTH);
        BLOCK.order(ByteOrder.LITTLE_ENDIAN);
        ((Buffer) BLOCK).flip();
        
        this.NAME = header.INCLUDE_DIRECTORYNAMES ? getZString(BLOCK) : null;
        this.PATH = pathWithFallback(this.NAME);
        this.FILERECORDS = new ArrayList<>(this.COUNT);

        for (int i = 0; i < this.COUNT; i++) {
            try {
                BSAFileRecord file = new BSAFileRecord(BLOCK, header, names);
                this.FILERECORDS.add(file);
            } catch (java.nio.BufferUnderflowException ex) {
                throw new IOException(String.format("Buffer underflow while reading file %d/%d in %s.", i, this.COUNT, this.NAME));
            }
        }
    }

    @Override
    public String toString() {
        return this.NAME;
    }

    static final int SIZE = 24;

    /**
     * A 64bit hash of the folder NAME.
     */
    final public long NAMEHASH;

    /**
     * The number of files in the folder.
     */
    final public int COUNT;

    final public int PADDING;
    final public long OFFSET;

    final public String NAME;
    final public Path PATH;
    final List<BSAFileRecord> FILERECORDS;

    static private Path pathWithFallback(String s) {
        try {
            return Paths.get(s);
        } catch (InvalidPathException ex) {
            return Paths.get(s.replaceAll("[^a-zA-Z0-9._]", ""));
        }         
    }
}
