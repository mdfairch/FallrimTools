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

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.universalchardet.UniversalDetector;
import resaver.ess.Plugin;

/**
 * A StringsFile stores reads and stores strings from the mod stringtables;
 * mostly just applies to Skyrim.esm and the DLCs.
 *
 * @author Mark Fairchild
 */
public class StringsFile {

    /**
     * Reads a <code>StringsFile</code> from a file.
     *
     * @param file The path to the file.
     * @param plugin The <code>Plugin</code> that the <code>StringsFile</code>
     * supplies.
     * @return The <code>StringsFile</code>.
     * @throws IOException
     */
    static public StringsFile readStringsFile(Path file, Plugin plugin) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(plugin);
        
        try (FileChannel channel = FileChannel.open(file)) {
            final int SIZE = (int) channel.size();
            final ByteBuffer BUFFER = ByteBuffer.allocate(SIZE);
            int bytesRead = channel.read(BUFFER);
            assert bytesRead == SIZE;
            ((Buffer) BUFFER).flip();
            return readStringsFile(file, plugin, BUFFER);
        }
    }

    /**
     * Reads a <code>StringsFile</code> from a <code>LittleEndianInput</code>.
     *
     * @param file The filename.
     * @param plugin The <code>Plugin</code> that the <code>StringsFile</code>
     * supplies.
     * @param input The input stream.
     * @return The <code>StringsFile</code>.
     */
    static public StringsFile readStringsFile(Path file, Plugin plugin, ByteBuffer input) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(input);
        
        StringsFile.Type type = StringsFile.Type.match(file);
        return new StringsFile(file, plugin, input, type);
    }

    /**
     * Reads a <code>StringsFile</code> from a <code>LittleEndianInput</code>.
     *
     * @param name The name of the stringtable.
     * @param plugin The <code>Plugin</code> that the <code>StringsFile</code>
     * supplies.
     * @param input The input stream.
     * @param type The type of stringtable.
     */
    private StringsFile(Path path, Plugin plugin, ByteBuffer input, Type type) {
        try {
            Objects.requireNonNull(input);
            Objects.requireNonNull(type);
            input.order(ByteOrder.LITTLE_ENDIAN);

            this.PATH = Objects.requireNonNull(path);
            this.PLUGIN = Objects.requireNonNull(plugin);

            final int COUNT = input.getInt();
            final int SIZE = input.getInt();
            final int DATASTART = 8 + COUNT * 8;
            
            if (input.remaining() != COUNT*8 + SIZE) {
                LOG.severe(String.format("Size sanity check failed in %s: %d remaining vs %d expected", 
                        path.getFileName().toString(),
                        input.remaining(),
                        COUNT*8 + SIZE
                        ));
            }
            
            this.TABLE = new HashMap<>(COUNT);

            final ByteBuffer DIRECTORY = input.slice().order(ByteOrder.LITTLE_ENDIAN);
            ((Buffer) DIRECTORY).limit(COUNT * 8);

            for (int i = 0; i < COUNT; i++) {
                final int STRINGID = DIRECTORY.getInt();
                final int OFFSET = DIRECTORY.getInt();

                ((Buffer) input).position(DATASTART + OFFSET);

                if (type == Type.STRINGS) {
                    byte[] bytes = mf.BufferUtil.getZStringRaw(input);
                    String string = mf.BufferUtil.mozillaString(bytes);
                    this.TABLE.put(STRINGID, string);
                } else {
                    int length = input.getInt();
                    byte[] bytes = new byte[length];
                    input.get(bytes);
                    String string = mf.BufferUtil.mozillaString(bytes);
                    this.TABLE.put(STRINGID, string);
                }
            }
        } catch (RuntimeException ex) {
            LOG.log(Level.SEVERE, String.format("StringsFile format error: %s %s", path.toString(), ex.getMessage()));
            throw ex;
        }
    }

    /**
     * Retrieves a string using its string ID.
     *
     * @param stringID
     * @return
     */
    public String get(int stringID) {
        return this.TABLE.get(stringID);
    }

    /**
     * @see Object#toString()
     * @return
     */
    @Override
    public String toString() {
        return this.PATH.getFileName().toString();
    }

    /**
     * The reference for accessing the stringtable.
     */
    final public Map<Integer, String> TABLE;

    /**
     * The <code>Plugin</code> that the <code>StringsFile</code> supplies.
     */
    final public Plugin PLUGIN;

    /**
     * The name of the stringtable.
     */
    final public Path PATH;

    static final private java.util.Set<Charset> CHARSET_LOG = new java.util.HashSet<>();
    static final private Logger LOG = Logger.getLogger(StringsFile.class.getName());
    static final private FileSystem FS = FileSystems.getDefault();

    /**
     * The three different types of Strings file.
     */
    static public enum Type {
        STRINGS("glob:**.Strings"),
        ILSTRINGS("glob:**.ILStrings"),
        DLSTRINGS("glob:**.DLStrings");

        static public Type match(Path file) {
            if (STRINGS.GLOB.matches(file)) {
                return STRINGS;
            } else if (ILSTRINGS.GLOB.matches(file)) {
                return ILSTRINGS;
            } else if (DLSTRINGS.GLOB.matches(file)) {
                return DLSTRINGS;
            }
            return null;
        }

        private Type(String glob) {
            this.GLOB = FS.getPathMatcher(glob);
        }

        final private PathMatcher GLOB;
    };

    /**
     * Makes a string from a byte array in a region-friendly way. Thank you
     * Mozilla!
     *
     * @param bytes
     * @return
     */
    static String makeString(byte[] bytes) {
        final UniversalDetector DETECTOR = new UniversalDetector(null);

        DETECTOR.handleData(bytes, 0, bytes.length);
        DETECTOR.dataEnd();
        final String ENCODING = DETECTOR.getDetectedCharset();
        DETECTOR.reset();

        final Charset CHARSET = (null == ENCODING ? UTF_8 : Charset.forName(ENCODING));
        assert null != CHARSET;

        if (CHARSET_LOG.add(CHARSET)) {
            LOG.info(String.format("Detected a new character encoding: %s.", CHARSET));
        }

        final String STR = new String(bytes, CHARSET);
        return STR;
    }

}
