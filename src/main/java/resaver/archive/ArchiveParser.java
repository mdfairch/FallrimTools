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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author Mark
 */
abstract public class ArchiveParser implements Closeable {

    /**
     * Creates a new <code>ArchiveParser</code> by reading from a
     * <code>LittleEndianRAF</code>. A reference to the
     * <code>LittleEndianRAF</code> will be kept.
     *
     * @param path The path of the archive.
     * @param channel The input file.
     * @return The <code>ArchiveParser</code>.
     * @throws IOException
     */
    static public ArchiveParser createParser(Path path, FileChannel channel) throws IOException {
        ByteBuffer magic = ByteBuffer.allocate(4);
        channel.read(magic, 0);
        
        if (Arrays.equals(magic.array(), BSA_MAGIC)) {
            return new BSAParser(path, channel);
        } else if (Arrays.equals(magic.array(), BA2GEN_MAGIC)) {
            return new BA2Parser(path, channel);
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        this.CHANNEL.close();
    }

    /**
     * Creates a new <code>ArchiveParser</code> with a name and a
     * <code>LittleEndianRAF</code>. A reference to the
     * <code>LittleEndianRAF</code> will be kept.
     *
     * @param path The path of the archive.
     * @param channel The file from which to read.
     * @throws IOException
     */
    protected ArchiveParser(Path path, FileChannel channel) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(channel);
        this.PATH = path;
        this.NAME = path.getFileName().toString();
        this.CHANNEL = channel;
    }

    final protected FileChannel CHANNEL;
    final protected String NAME;
    final protected Path PATH;

    /**
     * Creates a <code>Map</code> pairing <code>Path</code> and <code>ByteBuffer</code>.
     *
     * @param dir A base directory to search in.
     * @param matcher A <code>PathMatcher</code> to determine files of interest.
     * @return The <code>Map</code>.
     * @throws IOException
     *
     */
    abstract public Map<Path, Optional<ByteBuffer>> getFiles(Path dir, PathMatcher matcher) throws IOException;
    
    /**
     * Creates a <code>Map</code> pairing full <code>Path</code> to <code>
     *
     * @param dir A base directory to search in.
     * @param matcher A <code>PathMatcher</code> to determine files of interest.
     * @return The <code>Map</code>.
     * @throws IOException
     *
     */
    abstract public Map<Path, Path> getFilenames(Path dir, PathMatcher matcher) throws IOException;
    
    static private final byte[] BSA_MAGIC = new byte[]{'B', 'S', 'A', 0};
    static private final byte[] BA2GEN_MAGIC = new byte[]{'B', 'T', 'D', 'X'};
}
