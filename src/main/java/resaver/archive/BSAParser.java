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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Handles the job of reading scripts out of BSA files.
 *
 * @author Mark Fairchild
 */
public class BSAParser extends ArchiveParser {

    /**
     * Creates a new <code>BSAParser</code>.
     *
     * @param path
     * @param channel
     * @throws IOException
     * @see ArchiveParser#ArchiveParser(java.nio.file.Path,
     * java.nio.channels.FileChannel)
     */
    public BSAParser(Path path, FileChannel channel) throws IOException {
        super(path, channel);

        try {
            // Read the header.
            final ByteBuffer HEADERBLOCK = ByteBuffer.allocate(BSAHeader.SIZE);
            channel.read(HEADERBLOCK);
            HEADERBLOCK.order(ByteOrder.LITTLE_ENDIAN);
            ((Buffer) HEADERBLOCK).flip();
            this.HEADER = new BSAHeader(HEADERBLOCK, path.getFileName().toString());

            // Read the filename table indirectly.
            final Supplier<String> NAMES = this.HEADER.INCLUDE_FILENAMES
                    ? this.getNames(channel)
                    : () -> null;

            // Allocate storage for the folder records and file records.
            this.FOLDERRECORDS = new ArrayList<>(this.HEADER.FOLDER_COUNT);

            // Read folder records.
            final ByteBuffer FOLDERBLOCK = ByteBuffer.allocate(this.HEADER.FOLDER_COUNT * BSAFolderRecord.SIZE);
            channel.read(FOLDERBLOCK, this.HEADER.FOLDER_OFFSET);
            FOLDERBLOCK.order(ByteOrder.LITTLE_ENDIAN);
            ((Buffer) FOLDERBLOCK).flip();

            for (int i = 0; i < this.HEADER.FOLDER_COUNT; i++) {
                BSAFolderRecord folder = new BSAFolderRecord(FOLDERBLOCK, this.HEADER, channel, NAMES);
                this.FOLDERRECORDS.add(folder);
            }
        } catch (IOException ex) {
            throw new IOException("Failed to parse " + path, ex);
        }
    }

    private Supplier<String> getNames(FileChannel channel) throws IOException {
        long FILENAMES_OFFSET = this.HEADER.FOLDER_OFFSET
                + this.HEADER.FOLDER_COUNT * BSAFolderRecord.SIZE
                + this.HEADER.TOTAL_FOLDERNAME_LENGTH + this.HEADER.FOLDER_COUNT
                + this.HEADER.FILE_COUNT * BSAFileRecord.SIZE;

        final ByteBuffer FILENAMESBLOCK = ByteBuffer.allocate(this.HEADER.TOTAL_FILENAME_LENGTH);
        channel.read(FILENAMESBLOCK, FILENAMES_OFFSET);
        FILENAMESBLOCK.order(ByteOrder.LITTLE_ENDIAN);
        ((Buffer) FILENAMESBLOCK).flip();

        return () -> mf.BufferUtil.getZString(FILENAMESBLOCK);
    }

    /**
     * @see ArchiveParser#getFiles(java.nio.file.Path,
     * java.nio.file.PathMatcher)
     */
    @Override
    public Map<Path, Optional<ByteBuffer>> getFiles(Path dir, PathMatcher matcher) throws IOException {
        return this.FOLDERRECORDS.stream()
                .filter(block -> dir == null || dir.equals(block.PATH))
                .flatMap(block -> block.FILERECORDS.stream())
                .filter(rec -> matcher.matches(rec.getPath()))
                .collect(Collectors.toMap(
                        record -> super.PATH.getFileName().resolve(record.getPath()),
                        record -> BSAFileData.getData(super.CHANNEL, record, HEADER)));
    }

    /**
     * @see ArchiveParser#getFilenames(java.nio.file.Path,
     * java.nio.file.PathMatcher)
     */
    @Override
    public Map<Path, Path> getFilenames(Path dir, PathMatcher matcher) throws IOException {
        return this.FOLDERRECORDS.stream()
                .filter(block -> dir == null || dir.equals(block.PATH))
                .flatMap(block -> block.FILERECORDS.stream())
                .filter(rec -> matcher.matches(rec.getPath()))
                .collect(Collectors.toMap(
                        record -> super.PATH.getFileName().resolve(record.getPath()),
                        record -> record.getPath()));
    }

    @Override
    public String toString() {
        return this.NAME;
    }

    final BSAHeader HEADER;
    final List<BSAFolderRecord> FOLDERRECORDS;

}
