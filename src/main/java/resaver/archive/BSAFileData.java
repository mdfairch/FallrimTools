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
import java.util.Optional;
import java.util.zip.DataFormatException;
import mf.BufferUtil;

/**
 * Describes a block of BSA file data.
 *
 * @author Mark Fairchild
 */
public class BSAFileData {

    static Optional<ByteBuffer> getData(FileChannel channel, BSAFileRecord record, BSAHeader header) {
        try {
            channel.position(record.OFFSET);
            ByteBuffer buffer = ByteBuffer.allocate(record.FILESIZE);
            int bytesRead = channel.read(buffer);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            ((Buffer) buffer).flip();
            if (bytesRead != record.FILESIZE) {
                throw new IllegalStateException(String.format("Read %d bytes but expected %d bytes.", bytesRead, record.FILESIZE));
            }

            // If the filename is embedded, readFully it from the data block.
            // Otherwise retrieve it from the file record.
            if (header.EMBED_FILENAME) {
                byte b = buffer.get();
                buffer.position(b + 2);
            }

            // If the file is compressed, inflateZLIB it. Otherwise just readFully it in.
            if (!record.ISCOMPRESSED) {
                return Optional.of(buffer);

            } else {
                int uncompressedSize = buffer.getInt();
                ByteBuffer uncompressedData; // = ByteBuffer.allocate(uncompressedSize);

                switch (header.VERSION) {
                    case 104:
                        uncompressedData = BufferUtil.inflateZLIB(buffer, uncompressedSize, record.FILESIZE);
                        break;
                    case 105:
                        uncompressedData = BufferUtil.inflateLZ4(buffer, uncompressedSize);
                        break;
                    default:
                        throw new IOException("Unknown version " + header.VERSION);
                }

                uncompressedData.order(ByteOrder.LITTLE_ENDIAN);
                return Optional.of(uncompressedData);
            }
        } catch (net.jpountz.lz4.LZ4Exception | DataFormatException | IOException ex) {
            return Optional.empty();
        }
    }

}
