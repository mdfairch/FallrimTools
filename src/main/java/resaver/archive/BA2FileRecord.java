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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.DataFormatException;
import mf.BufferUtil;

/**
 * Describes a BA2 file record.
 *
 * @author Mark Fairchild
 */
class BA2FileRecord {

    /**
     * Creates a new <code>FileRecord</code> by reading from a
     * <code>ByteBuffer</code>. The name field will be set to null.
     *
     * @param input The file from which to read..
     * @param header
     * @throws IOException
     *
     */
    public BA2FileRecord(ByteBuffer input, BA2Header header) throws IOException {
        this.NAMEHASH = input.getInt();

        this.EXT = new byte[4];
        input.get(this.EXT);

        this.DIRHASH = input.getInt();
        this.FLAGS = input.getInt();
        this.OFFSET = input.getLong();
        this.FILESIZE = input.getInt();
        this.REALSIZE = input.getInt();
        this.ALIGN = input.getInt();
        this.name = null;
    }

    @Override
    public String toString() {
        if (this.name == null) {
            return String.format("%d bytes at offset %d", Math.max(this.FILESIZE, this.REALSIZE), this.OFFSET);
        } else {
            return this.name;
        }
    }

    public Optional<ByteBuffer> getData(FileChannel channel) {
        Objects.requireNonNull(channel);

        try {
        if (this.FILESIZE == 0) {
            final ByteBuffer DATA = ByteBuffer.allocate(this.REALSIZE);
            channel.read(DATA, this.OFFSET);
            return Optional.of(DATA);

        } else if (this.FILESIZE == this.REALSIZE) {
            final ByteBuffer DATA = ByteBuffer.allocate(this.FILESIZE);
            channel.read(DATA, this.OFFSET);
            return Optional.of(DATA);

        } else {
                final ByteBuffer COMPRESSED = ByteBuffer.allocate(this.FILESIZE);
                channel.read(COMPRESSED, this.OFFSET);
                ((Buffer) COMPRESSED).flip();
                final ByteBuffer DATA = BufferUtil.inflateZLIB(COMPRESSED, this.REALSIZE, this.FILESIZE);
                return Optional.of(DATA);
        }
        } catch (IOException | DataFormatException ex) {
            return Optional.empty();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        try {
            return Paths.get(this.name);
        } catch (java.nio.file.InvalidPathException ex) {
            return null;
        }
    }

    static public final int SIZE = 36;
    final public int NAMEHASH;
    final public byte[] EXT;
    final public int DIRHASH;
    final public int FLAGS;
    final public long OFFSET;
    final public int FILESIZE;
    final public int REALSIZE;
    final public int ALIGN;
    private String name;

}
