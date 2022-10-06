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
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the job of reading scripts out of BSA files.
 *
 * @author Mark Fairchild
 */
public class BA2Parser extends ArchiveParser {

    /**
     * Creates a new <code>BA2Parser</code>.
     *
     * @param path
     * @param channel
     * @throws IOException
     * @see ArchiveParser#ArchiveParser(java.lang.String,
     * resaver.LittleEndianRAF)
     */
    protected BA2Parser(Path path, FileChannel channel) throws IOException {
        super(path, channel);

        // Read the header.
        final ByteBuffer HEADERBLOCK = ByteBuffer.allocate(BA2Header.SIZE).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(HEADERBLOCK);
        ((Buffer)HEADERBLOCK).flip();
        this.HEADER = new BA2Header(HEADERBLOCK);
        
        this.FILES = new ArrayList<>(this.HEADER.FILE_COUNT);
        final ByteBuffer FILERECORDS = ByteBuffer.allocate(this.HEADER.FILE_COUNT * BA2FileRecord.SIZE).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(FILERECORDS);
        ((Buffer)FILERECORDS).flip();
        FILERECORDS.order(ByteOrder.LITTLE_ENDIAN);
        
        // Read file records.
        for (int i = 0; i < this.HEADER.FILE_COUNT; i++) {
            BA2FileRecord file = new BA2FileRecord(FILERECORDS, this.HEADER);
            this.FILES.add(file);
        }

        // Read the filename table.
        channel.position(this.HEADER.NAMETABLE_OFFSET);
        final ByteBuffer NAMEBUFFER = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(NAMEBUFFER);

        for (int i = 0; i < this.HEADER.FILE_COUNT; i++) {
            ((Buffer)NAMEBUFFER).flip();
            String fileName = mf.BufferUtil.getWString(NAMEBUFFER);
            this.FILES.get(i).setName(fileName);
            NAMEBUFFER.compact();
            channel.read(NAMEBUFFER);
        }
        
    }

    /**
     * @see ArchiveParser#getFiles(java.nio.file.Path,
     * java.nio.file.PathMatcher)
     */
    @Override
    public Map<Path, Optional<ByteBuffer>> getFiles(Path dir, PathMatcher matcher) throws IOException {
        return this.FILES.stream()
                .filter(file -> dir == null || file.getPath().startsWith(dir))
                .filter(file -> matcher.matches(file.getPath()))
                .collect(Collectors.toMap(
                        file -> super.PATH.resolve(file.getPath()),                        
                        file -> file.getData(CHANNEL)));
    }

    /**
     * @see ArchiveParser#getFilenames(java.nio.file.Path, java.nio.file.PathMatcher) 
     */
    @Override
    public Map<Path, Path> getFilenames(Path dir, PathMatcher matcher) throws IOException {
        return this.FILES.stream()
                .filter(file -> dir == null || file.getPath().startsWith(dir))
                .filter(file -> matcher.matches(file.getPath()))
                .collect(Collectors.toMap(
                        record -> super.PATH.getFileName().resolve(record.getPath()),
                        record -> record.getPath()));
    }

    final BA2Header HEADER;
    final List<BA2FileRecord> FILES;

}
