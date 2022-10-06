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
package resaver.pex;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import resaver.Game;
import resaver.IString;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import resaver.ListException;
import resaver.pex.StringTable.TString;

/**
 * Describes a Skyrim PEX script and will read and write it from streams.
 *
 * @author Mark Fairchild
 */
final public class PexFile {

    /**
     * Reads a script file and creates a PexFile object to represent it.
     *
     * Exceptions are not handled. At all. Not even a little bit.
     *
     * @param data An array of bytes containing the script data.
     * @return The PexFile object.
     *
     * @throws IOException
     *
     */
    static public PexFile readScript(ByteBuffer data) throws IOException {
        final int MAGIC = data.getInt(0);

        // Prepare input stream. The DataInput interface just happen to be 
        // perfect for this kind of thing.
        switch (MAGIC) {
            case 0xdec057fa:
                return new PexFile(data, Game.FALLOUT4);
            case 0xfa57c0de:
                return new PexFile(data, Game.SKYRIM_LE);
            default:
                throw new IOException("Invalid magic number.");
        }
    }

    /**
     * Reads a script file and creates a PexFile object to represent it.
     *
     * Exceptions are not handled. At all. Not even a little bit.
     *
     * @param scriptFile The script file to read, which must exist and be
     * readable.
     * @return The PexFile object.
     *
     * @throws FileNotFoundException
     * @throws IOException
     *
     */
    static public PexFile readScript(Path scriptFile) throws FileNotFoundException, IOException {
        try (final FileChannel CHANNEL = FileChannel.open(scriptFile, StandardOpenOption.READ)) {
            final ByteBuffer input = ByteBuffer.allocate((int) Files.size(scriptFile));
            CHANNEL.read(input);
            ((Buffer) input).flip();
            return readScript(input);
        }
    }

    /**
     * Writes a PexFile object to a script file.
     *
     * Exceptions are not handled. At all. Not even a little bit.
     *
     * @param script The PexFile object to write.
     * @param scriptFile The script file to write. If it exists, it must be a
     * file and it must be writable.
     *
     * @throws FileNotFoundException
     * @throws IOException
     *
     */
    static public void writeScript(PexFile script, Path scriptFile) throws FileNotFoundException, IOException {
        assert !Files.exists(scriptFile) || Files.isRegularFile(scriptFile);
        assert !Files.exists(scriptFile) || Files.isWritable(scriptFile);

        try (final FileChannel CHANNEL = FileChannel.open(scriptFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            final ByteBuffer output = ByteBuffer.allocate(2 * script.calculateSize());
            script.write(output);
            ((Buffer)output).flip();
            CHANNEL.write(output);
        }
    }

    /**
     * Creates a Pex by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param game The game for which the script was compiled.
     * @throws IOException Exceptions aren't handled.
     */
    private PexFile(ByteBuffer input, Game game) throws IOException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(game);

        try {
            this.GAME = game;
            this.HEADER = new Header(input);

            this.STRINGS = new StringTable(input);
            this.DEBUG = new DebugInfo(input, this.STRINGS);

            int flagCount = Short.toUnsignedInt(input.getShort());
            this.USERFLAGDEFS = new ArrayList<>(flagCount);
            while (0 < flagCount) {
                this.USERFLAGDEFS.add(new UserFlag(input, this.STRINGS));
                flagCount--;
            }

            int scriptCount = Short.toUnsignedInt(input.getShort());
            if (scriptCount < 1) {
                throw new IllegalStateException("Pex files must contain at least one script.");
            }

            this.SCRIPTS = new ArrayList<>(scriptCount);
            while (0 < scriptCount) {
                Pex pex = new Pex(input, game, this.USERFLAGDEFS, this.STRINGS);
                this.SCRIPTS.add(pex);
                scriptCount--;
            }
        } catch (IOException | ListException ex) {
            ex.printStackTrace(System.err);
            throw ex;
        }
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    public void write(ByteBuffer output) throws IOException {
        this.HEADER.write(output);
        this.STRINGS.write(output);
        this.DEBUG.write(output);

        output.putShort((short) this.USERFLAGDEFS.size());
        for (UserFlag flag : this.USERFLAGDEFS) {
            flag.write(output);
        }

        output.putShort((short) this.SCRIPTS.size());
        for (Pex pex : this.SCRIPTS) {
            pex.write(output);
        }

        /*
        output.putShort((short) this.OBJECTS.size());
        for (Pex obj : this.OBJECTS) {
            obj.write(output);
        }*/
    }

    /**
     * @return The size of the <code>PexFile</code>, in bytes.
     *
     */
    public int calculateSize() {
        int sum = 0;
        sum += this.HEADER.calculateSize();
        sum += this.STRINGS.calculateSize();
        sum += this.DEBUG.calculateSize();
        sum += 2 + this.USERFLAGDEFS.stream().mapToInt(f -> f.calculateSize()).sum();
        sum += 2 + this.SCRIPTS.stream().mapToInt(f -> f.calculateSize()).sum();
        return sum;
    }

    /**
     * Rebuilds the string table. This is necessary if ANY strings in ANY of the
     * PexFile's members has changed at all. Otherwise, writing the PexFile will
     * produce an invalid file.
     *
     */
    public void rebuildStringTable() {
        final Set<TString> INUSE = new java.util.LinkedHashSet<>();
        this.DEBUG.collectStrings(INUSE);
        this.USERFLAGDEFS.forEach(flag -> flag.collectStrings(INUSE));
        this.SCRIPTS.forEach(obj -> obj.collectStrings(INUSE));
        this.STRINGS.rebuildStringTable(INUSE);
    }

    /**
     * Tries to disassemble the script.
     *
     * @param level Partial disassembly flag.
     * @param code The code strings.
     */
    public void disassemble(List<String> code, AssemblyLevel level) {
        this.SCRIPTS.forEach(v -> v.disassemble(code, level));
    }

    /**
     * Pretty-prints the PexFile.
     *
     * @return A string representation of the PexFile.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.HEADER);
        buf.append(this.DEBUG);
        buf.append("USER FLAGS\n");
        buf.append(this.USERFLAGDEFS);

        if (this.SCRIPTS != null) {
            this.SCRIPTS.forEach(obj -> buf.append("\n\nOBJECT\n").append(obj).append('\n'));
        }

        return buf.toString();

    }

    /**
     * @return The compilation date of the <code>PexFile</code>.
     */
    public long getDate() {
        return this.HEADER.compilationTime;
    }

    /**
     * @return The filename of the <code>PexFile</code>, determined from the
     * header.
     */
    public IString getFilename() {
        final String SOURCE = this.HEADER.soureFilename;
        final String REGEX = "(psc)$";
        final String REPLACEMENT = "pex";
        final Pattern PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
        final Matcher MATCHER = PATTERN.matcher(SOURCE);
        final String COMPILED = MATCHER.replaceAll(REPLACEMENT);
        return IString.get(COMPILED);
    }

    final public Game GAME;
    final public Header HEADER;
    final public StringTable STRINGS;
    final public DebugInfo DEBUG;
    final public List<UserFlag> USERFLAGDEFS;
    final public List<Pex> SCRIPTS;

    /**
     * Describes the header of a PexFile file. Useless beyond that.
     *
     */
    final public class Header {

        /**
         * Creates a Header by reading from a DataInput.
         *
         * @param input A ByteBuffer for a Skyrim PEX file.
         * @throws IOException Exceptions aren't handled.
         */
        private Header(ByteBuffer input) throws IOException {
            this.magic = input.getInt();
            this.version = input.getInt();
            this.compilationTime = input.getLong();
            this.soureFilename = mf.BufferUtil.getUTF(input);
            this.userName = mf.BufferUtil.getUTF(input);
            this.machineName = mf.BufferUtil.getUTF(input);
        }

        /**
         * Write the object to a <code>ByteBuffer</code>.
         *
         * @param output The <code>ByteBuffer</code> to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        private void write(ByteBuffer output) throws IOException {
            output.putInt(this.magic);
            output.putInt(this.version);
            output.putLong(this.compilationTime);
            mf.BufferUtil.putWString(output, this.soureFilename);
            mf.BufferUtil.putWString(output, this.userName);
            mf.BufferUtil.putWString(output, this.machineName);
        }

        /**
         * @return The size of the <code>Header</code>, in bytes.
         *
         */
        public int calculateSize() {
            return 22 + this.soureFilename.length() + this.userName.length() + this.machineName.length();
        }

        /**
         * Pretty-prints the Header.
         *
         * @return A string representation of the Header.
         */
        @Override
        public String toString() {
            return new StringBuilder()
                    .append(this.soureFilename)
                    .append(" compiled at ")
                    .append(this.compilationTime)
                    .append(" by ")
                    .append(this.userName)
                    .append(" on ")
                    .append(this.machineName)
                    .append(".\n")
                    .toString();
        }

        private int magic = 0;
        private int version = 0;
        private long compilationTime = 0;
        private String soureFilename = "";
        private String userName = "";
        private String machineName = "";

    }

    /**
     * Describe the debugging info section of a PEX file.
     *
     */
    final public class DebugInfo {

        /**
         * Creates a DebugInfo by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The <code>StringTable</code> for the
         * <code>PexFile</code>.
         * @throws IOException Exceptions aren't handled.
         */
        private DebugInfo(ByteBuffer input, StringTable strings) throws IOException {
            this.hasDebugInfo = input.get();
            this.DEBUGFUNCTIONS = new ArrayList<>(0);
            this.PROPERTYGROUPS = new ArrayList<>(0);
            this.STRUCTORDERS = new ArrayList<>(0);

            if (this.hasDebugInfo == 0) {

            } else {
                this.modificationTime = input.getLong();

                int functionCount = Short.toUnsignedInt(input.getShort());
                this.DEBUGFUNCTIONS.ensureCapacity(functionCount);
                for (int i = 0; i < functionCount; i++) {
                    this.DEBUGFUNCTIONS.add(new DebugFunction(input, strings));
                }

                if (GAME.isFO4()) {
                    int propertyCount = Short.toUnsignedInt(input.getShort());
                    this.PROPERTYGROUPS.ensureCapacity(propertyCount);
                    for (int i = 0; i < propertyCount; i++) {
                        this.PROPERTYGROUPS.add(new PropertyGroup(input, strings));
                    }

                    int orderCount = Short.toUnsignedInt(input.getShort());
                    this.STRUCTORDERS.ensureCapacity(orderCount);
                    for (int i = 0; i < orderCount; i++) {
                        this.STRUCTORDERS.add(new StructOrder(input, strings));
                    }

                }
            }
        }

        /**
         * Write the object to a <code>ByteBuffer</code>.
         *
         * @param output The <code>ByteBuffer</code> to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        private void write(ByteBuffer output) throws IOException {
            output.put(this.hasDebugInfo);

            if (this.hasDebugInfo != 0) {
                output.putLong(this.modificationTime);

                output.putShort((short) this.DEBUGFUNCTIONS.size());
                for (DebugFunction function : this.DEBUGFUNCTIONS) {
                    function.write(output);
                }

                if (GAME.isFO4()) {
                    output.putShort((short) this.PROPERTYGROUPS.size());
                    for (PropertyGroup function : this.PROPERTYGROUPS) {
                        function.write(output);
                    }

                    output.putShort((short) this.STRUCTORDERS.size());
                    for (StructOrder function : this.STRUCTORDERS) {
                        function.write(output);
                    }
                }
            }
        }

        /**
         * Removes all debug info.
         */
        public void clear() {
            this.hasDebugInfo = 0;
            this.DEBUGFUNCTIONS.clear();
            this.PROPERTYGROUPS.clear();
            this.STRUCTORDERS.clear();
        }

        /**
         * Collects all of the strings used by the DebugInfo and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        public void collectStrings(Set<TString> strings) {
            this.DEBUGFUNCTIONS.forEach(f -> f.collectStrings(strings));
            this.PROPERTYGROUPS.forEach(f -> f.collectStrings(strings));
            this.STRUCTORDERS.forEach(f -> f.collectStrings(strings));
        }

        /**
         * @return The size of the <code>DebugInfo</code>, in bytes.
         *
         */
        public int calculateSize() {
            int sum = 1;
            if (this.hasDebugInfo != 0) {
                sum += 8;
                sum += 2 + this.DEBUGFUNCTIONS.stream().mapToInt(f -> f.calculateSize()).sum();

                if (GAME.isFO4()) {
                    sum += 2 + this.PROPERTYGROUPS.stream().mapToInt(p -> p.calculateSize()).sum();
                    sum += 2 + this.STRUCTORDERS.stream().mapToInt(p -> p.calculateSize()).sum();
                }
            }

            return sum;
        }

        /**
         * Pretty-prints the DebugInfo.
         *
         * @return A string representation of the DebugInfo.
         */
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("DEBUGINFO\n");
            this.DEBUGFUNCTIONS.forEach(function -> buf.append('\t').append(function).append('\n'));
            buf.append('\n');
            return buf.toString();
        }

        private byte hasDebugInfo;
        private long modificationTime;
        final private ArrayList<DebugFunction> DEBUGFUNCTIONS;
        final private ArrayList<PropertyGroup> PROPERTYGROUPS;
        final private ArrayList<StructOrder> STRUCTORDERS;

    }

}
