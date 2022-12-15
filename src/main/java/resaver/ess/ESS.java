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
package resaver.ess;

import static j2html.TagCreator.*;
import static mf.TryCatch.*;
import java.nio.ByteBuffer;
import java.nio.file.StandardCopyOption;
import resaver.Game;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import mf.BufferUtil;
import mf.Timer;
import resaver.Analysis;
import resaver.ListException;
import resaver.ess.papyrus.Papyrus;
import resaver.ess.papyrus.PapyrusElement;
import resaver.ess.papyrus.PapyrusException;
import resaver.gui.FilterTreeModel;

/**
 * Describes a Skyrim or Fallout4 savegame.
 *
 * @author Mark Fairchild
 *
 */
final public class ESS implements Element {

    /**
     * Reads a savegame and creates an <code>ESS</code> object to represent it.
     *
     * Exceptions are not handled. At all. Not even a little bit.
     *
     * @param saveFile The file containing the savegame.
     * @param model A <code>ModelBuilder</code>.
     * @return A <code>Result</code> object with details about results.
     * @throws IOException
     * @throws ElementException
     *
     */
    static public Result readESS(Path saveFile, ModelBuilder model) throws IOException, ElementException {
        Objects.requireNonNull(saveFile);
        Objects.requireNonNull(model);

        // Timer, for analyzing stuff.
        final Timer TIMER = Timer.startNew("reading savefile");

        // Doublecheck that the savefile has a correct extension.
        if (!Game.FILTER_ALL.accept(saveFile.toFile())) {
            throw new IOException(String.format("Filename extension not recognized: %s", saveFile));
        }

        // Read the savefile.
        // If the F4SE co-save is present, readRefID it too.
        try {
            //try (LittleEndianInputStream input = LittleEndianInputStream.openCtxDig(saveFile)) {
            try ( FileChannel channel = FileChannel.open(saveFile, StandardOpenOption.READ)) {
                int saveSize = (int) Files.size(saveFile);
                ByteBuffer input = ByteBuffer.allocate(saveSize).order(ByteOrder.LITTLE_ENDIAN);
                channel.read(input);
                ((Buffer) input).flip();

                final ESS ESS = new ESS(input, saveFile, model);
                final FilterTreeModel TREEMODEL = model.finish(ESS);

                TIMER.stop();
                final float SIZE = ESS.calculateSize() / 1048576.0f;
                LOG.fine(String.format("Savegame read: %.1f mb in %s (%s).", SIZE, TIMER.getFormattedTime(), saveFile));
                return ESS.new Result(null, TIMER, TREEMODEL);
            }
        } catch (IOException | DataFormatException ex) {
            String msg = String.format("Failed to load %s\n%s", saveFile, ex.getMessage());
            throw new IOException(msg, ex);
        } finally {
        }
    }

    /**
     * Writes out a savegame.
     *
     * Exceptions are not handled. At all. Not even a little bit.
     *
     * @param ess The <code>ESS</code> object.
     * @param saveFile The file into which to write the savegame.
     * @param testingMode More aggressive writing that rewrites changeforms.
     * @return A <code>Result</code> object with details about results.
     * @throws IOException
     * @throws ElementException
     *
     */
    static public Result writeESS(ESS ess, Path saveFile, boolean testingMode) throws IOException, ElementException {
        Objects.requireNonNull(ess);
        Objects.requireNonNull(saveFile);

        if (ess.isBroken()) {
            throw new IOException(String.format("%s is truncated and can't be saved.", ess.getOriginalFile().getFileName()));
        }

        final Timer TIMER = Timer.startNew("writing savefile");
        final Game GAME = ess.getHeader().GAME;
        Path backup = null;

        if (Files.exists(saveFile)) {
            backup = makeBackupFile(saveFile);
        }

        if (ess.COSAVE != null) {
            String filename = saveFile.getFileName().toString();
            String cosaveName = filename.replaceAll(GAME.SAVE_EXT + "$", GAME.COSAVE_EXT);
            final Path COSAVE_FILE = saveFile.resolveSibling(cosaveName);
            if (Files.exists(COSAVE_FILE)) {
                makeBackupFile(COSAVE_FILE);
            }
            Files.write(COSAVE_FILE, ess.COSAVE);
        }

        try ( FileChannel channel = FileChannel.open(saveFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ess.write(channel, testingMode);
        }

        final float SIZE = Files.size(saveFile) / 1048576.0f;
        TIMER.stop();
        LOG.fine(String.format("Savegame written: %.1f mb in %s (%s).", SIZE, TIMER.getFormattedTime(), saveFile));
        return ess.new Result(backup, TIMER, null);
    }

    /**
     * Creates a new <code>ESS</code> by reading from a <code>ByteBuffer</code>.
     *
     * @param input The input stream for the savegame.
     * @param saveFile The file containing the <code>ESS</code>.
     * @param model A <code>ModelBuilder</code>.
     * @throws IOException
     * @throws DataFormatException
     * @throws ElementException
     *
     */
    private ESS(ByteBuffer buffer, Path saveFile, ModelBuilder model) throws IOException, DataFormatException, ElementException {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(saveFile);
        Objects.requireNonNull(model);
        this.REFIDS = new java.util.HashMap<>(100_000);
        this.ORIGINAL_FILE = saveFile;

        LOG.fine("Reading savegame.");

        // Read the header. This includes the magic string.
        this.HEADER = new Header(buffer, saveFile);

        final Game GAME = this.HEADER.GAME;
        LOG.fine("Reading savegame: read header.");

        // Determine the filename of the co-save.
        String filename = saveFile.getFileName().toString();
        String cosaveName = filename.replaceAll(GAME.SAVE_EXT + "$", GAME.COSAVE_EXT);
        Path cosaveFile = saveFile.resolveSibling(cosaveName);
        this.COSAVE = Files.exists(cosaveFile) ? Files.readAllBytes(cosaveFile) : null;

        // Store the offset where the header ends and the body begins.
        int startingOffset = ((Buffer) buffer).position();

        // This is the stream that will be used for the remainder of the 
        // constructor.
        final ByteBuffer INPUT;

        // Do the decompression, if necessary.
        final CompressionType COMPRESSION = this.HEADER.getCompression();

        if (COMPRESSION.isCompressed()) {
            final int UNCOMPRESSED_LEN = buffer.getInt();
            final int COMPRESSED_LEN = buffer.getInt();
            if (UNCOMPRESSED_LEN < 0 || COMPRESSED_LEN < 0) {
                throw new IOException("Compression error. You might need to set [SAVEGAME]uiCompression=1 in SkyrimCustom.ini.");
            }

            //final ByteBuffer UNCOMPRESSED = ByteBuffer.allocate(UNCOMPRESSED_LEN);
            final ByteBuffer COMPRESSED = ByteBuffer.allocate(COMPRESSED_LEN);
            COMPRESSED.put(buffer);
            ((Buffer) COMPRESSED).flip();
            COMPRESSED.order(ByteOrder.LITTLE_ENDIAN);
            if (buffer.hasRemaining()) {
                throw new IllegalStateException("Some data was not compressed.");
            }

            switch (COMPRESSION) {
                case ZLIB:
                    LOG.fine("ZLIB DECOMPRESSION");
                    INPUT = BufferUtil.inflateZLIB(COMPRESSED, UNCOMPRESSED_LEN, COMPRESSED_LEN);
                    INPUT.order(ByteOrder.LITTLE_ENDIAN);
                    break;
                case LZ4:
                    LOG.fine("LZ4 DECOMPRESSION");
                    INPUT = BufferUtil.inflateLZ4(COMPRESSED, UNCOMPRESSED_LEN);
                    INPUT.order(ByteOrder.LITTLE_ENDIAN);
                    break;
                default:
                    throw new IOException("Unknown compression type: " + COMPRESSION);
            }

            ORIGINAL_SIZE = UNCOMPRESSED_LEN;
        } else {
            LOG.fine("NO FILE COMPRESSION");
            INPUT = buffer.slice();
            INPUT.order(ByteOrder.LITTLE_ENDIAN);
            ORIGINAL_SIZE = INPUT.remaining();
        }

        // sanity check
        int headerSize = this.HEADER.calculateSize();
        if (headerSize != startingOffset) {
            throw new PositionException("Header", headerSize, startingOffset);
        }

        // Make a CRC for the ESS header block.
        CRC32 CRC32 = new CRC32();
        ((Buffer) buffer).position(0);
        ((Buffer) buffer).limit(startingOffset);
        CRC32.update(buffer);

        // Update the CRC with the ESS body block.
        CRC32.update(INPUT);
        ((Buffer) INPUT).flip();
        this.DIGEST = CRC32.getValue();

        final mf.Counter SUM = new mf.Counter(buffer.capacity());
        SUM.addCountListener(sum -> {
            if (!this.isBroken() && sum != ((Buffer) INPUT).position()) {
                throw new PositionException("ESS", sum, ((Buffer) INPUT).position());
            }
        });

        // Read the form version.
        this.FORMVERSION = INPUT.get();
        SUM.click();
        //LOG.info(String.format("Detected %s with form version %d, %s in %s.", GAME, this.FORMVERSION, COMPRESSION, saveFile.getParent().relativize(saveFile)));

        switch (GAME) {
            case SKYRIM_LE:
                if (this.FORMVERSION < 73) {
                    throw new IllegalArgumentException("Invalid formVersion: " + this.FORMVERSION);
                }
                this.VERSION_STRING = null;
                break;
            case SKYRIM_SE:
            case SKYRIM_SW:
            case SKYRIM_VR:
                if (this.FORMVERSION < 77) {
                    throw new IllegalArgumentException("Invalid formVersion: " + this.FORMVERSION);
                }
                this.VERSION_STRING = null;
                break;
            case FALLOUT4:
            case FALLOUT_VR:
                if (this.FORMVERSION < 60) {
                    throw new IllegalArgumentException("Invalid formVersion: " + this.FORMVERSION);
                }
                this.VERSION_STRING = mf.BufferUtil.getWString(INPUT);
                SUM.click(2 + this.VERSION_STRING.length());
                break;
            default:
                throw new IllegalArgumentException("Unrecognized game.");
        }

        // Read the PLUGIN info section.
        PluginInfo pluginsPartial;
        
        try {
            pluginsPartial = new PluginInfo(INPUT, this.supportsESL());
        } catch (PluginInfo.PluginOverflowException ex) {
            this.truncated = true;
            pluginsPartial = (PluginInfo) ex.getPartial();
            LOG.log(Level.SEVERE, "Error while reading Plugins.", ex);
        }
        this.PLUGINS = pluginsPartial;
        
        SUM.click(this.PLUGINS.calculateSize());
        LOG.fine("Reading savegame: read plugin table.");

        // Add the plugins to the model.
        model.addPluginInfo(this.PLUGINS);

        // Read the file location table.
        this.FLT = new FileLocationTable(INPUT, GAME);
        this.TABLE1 = new ArrayList<>(this.FLT.TABLE1COUNT);
        this.TABLE2 = new ArrayList<>(this.FLT.TABLE2COUNT);
        this.TABLE3 = new ArrayList<>(this.FLT.TABLE3COUNT);
        this.CHANGEFORMS = new ChangeFormCollection(this.FLT.changeFormCount);

        SUM.click(this.FLT.calculateSize());
        LOG.fine("Reading savegame: read file location table.");

        // Read the FormID table.
        int[] formIDs = null;

        try {
            ((Buffer) INPUT).position(this.FLT.formIDArrayCountOffset - startingOffset);
            int formIDCount = INPUT.getInt();
            formIDs = new int[formIDCount];
            for (int formIDIndex = 0; formIDIndex < formIDCount; formIDIndex++) {
                try {
                    formIDs[formIDIndex] = INPUT.getInt();
                } catch (BufferUnderflowException ex) {
                    throw new ListException("Truncation in the FormID array.", formIDIndex, formIDCount, ex);
                }
            }

            LOG.fine(MessageFormat.format("Reading savegame: read %d formids.", formIDCount));
        } catch (ListException | IllegalArgumentException ex) {
            this.truncated = true;
            LOG.log(Level.SEVERE, "Error while reading FormID array.", ex);
        } catch (BufferUnderflowException ex) {
            this.truncated = true;
            LOG.log(Level.SEVERE, "FormID table missing.", ex);
        } finally {
            this.FORMIDARRAY = formIDs;
        }

        ((Buffer) INPUT).position(this.FLT.table1Offset - startingOffset);

        // Read the first and second sets of data tables.
        ESSContext context = this.getContext();

        for (int tableIndex = 0; tableIndex < this.FLT.TABLE1COUNT; tableIndex++) {
            try {
                GlobalData DATA = new GlobalData(INPUT, context, model);
                if (DATA.getType() < 0 || DATA.getType() > 100) {
                    throw new IllegalArgumentException("Invalid type for Table1: " + DATA.getType());
                }
                this.TABLE1.add(DATA);
                LOG.log(Level.FINE, "Reading savegame: \tGlobalData type {0}.", DATA.getType());
                LOG.fine("Reading savegame: read global data table 1.");
            } catch (PapyrusException ex) {
                throw new IOException(String.format("Error; read %d/%d GlobalData from table #2; something stupid happened.", tableIndex, this.FLT.TABLE2COUNT), ex);
            } catch (RuntimeException ex) {
                throw new IOException(String.format("Error; read %d/%d GlobalData from table #2.", tableIndex, this.FLT.TABLE2COUNT), ex);
            }
        }

        SUM.click(this.TABLE1.stream().mapToInt(t -> t.calculateSize()).sum());
        LOG.fine("Reading savegame: read GlobalDataTable #1.");

        for (int tableIndex = 0; tableIndex < this.FLT.TABLE2COUNT; tableIndex++) {
            try {
                GlobalData DATA = new GlobalData(INPUT, context, model);
                if (DATA.getType() < 100 || DATA.getType() > 1000) {
                    throw new IllegalArgumentException("Invalid type for Table1: " + DATA.getType());
                }
                this.TABLE2.add(DATA);
                LOG.log(Level.FINE, "Reading savegame: \tGlobalData type {0}.", DATA.getType());
            } catch (PapyrusException ex) {
                throw new IOException(String.format("Error; read %d/%d GlobalData from table #2; something stupid happened.", tableIndex, this.FLT.TABLE2COUNT), ex);
            } catch (RuntimeException ex) {
                throw new IOException(String.format("Error; read %d/%d GlobalData from table #2.", tableIndex, this.FLT.TABLE2COUNT), ex);
            }
        }

        SUM.click(this.TABLE2.stream().mapToInt(t -> t.calculateSize()).sum());
        LOG.fine("Reading savegame: read GlobalDataTable #2.");

        // Get the GlobalVariableTable.
        this.GLOBALS = this.TABLE1.stream()
                .filter(b -> b.getType() == 3 && b.getDataBlock() instanceof GlobalVariableTable)
                .map(b -> (GlobalVariableTable) b.getDataBlock())
                .findAny().orElse(new GlobalVariableTable());
        model.addGlobalVariableTable(this.GLOBALS);

        // Read the changeforms.
        for (int changeFormIndex = 0; changeFormIndex < this.FLT.changeFormCount; changeFormIndex++) {
            try {
                ChangeForm FORM = new ChangeForm(INPUT, context);
                this.CHANGEFORMS.add(FORM);
            } catch (RuntimeException ex) {
                throw new IOException(String.format("Error; read %d/%d ChangeForm definitions.", changeFormIndex, this.FLT.changeFormCount), ex);
            }
        }

        model.addChangeForms(this.CHANGEFORMS);

        SUM.click(this.CHANGEFORMS.stream().mapToInt(t -> t.calculateSize()).sum());
        LOG.fine("Reading savegame: read changeform table.");

        // Read the third set of data tables.
        Papyrus papyrusPartial = null;

        for (int tableIndex = 0; tableIndex < this.FLT.TABLE3COUNT; tableIndex++) {
            try {
                GlobalData DATA = new GlobalData(INPUT, context, model);
                if (DATA.getType() < 1000 || DATA.getType() > 1100) {
                    throw new IllegalArgumentException("Invalid type for Table1: " + DATA.getType());
                }
                this.TABLE3.add(DATA);
                LOG.log(Level.FINE, "Reading savegame: \tGlobalData type {0}.", DATA.getType());

            } catch (PapyrusException ex) {
                LOG.log(Level.SEVERE, "Error reading GlobalData 1001 (Papyrus).", ex);
                this.truncated = true;
                ex.printStackTrace(System.err);
                papyrusPartial = ex.getPartial();

            } catch (RuntimeException ex) {
                if (!this.truncated) {
                    this.truncated = true;
                    throw new IOException(String.format("Error; read %d/%d GlobalData from table #3.", tableIndex, this.FLT.TABLE3COUNT), ex);
                }
            }
        }

        // Grab the Papyrus block.
        this.PAPYRUS = this.TABLE3.stream()
                .filter(b -> b.getType() == 1001 && b.getDataBlock() instanceof Papyrus)
                .map(b -> (Papyrus) b.getDataBlock())
                .findAny().orElse(papyrusPartial);

        // Grab the Animations block.
        this.ANIMATIONS = this.TABLE3.stream()
                .filter(b -> b.getType() == 1002 && b.getDataBlock() instanceof DefaultGlobalDataBlock)
                .map(b -> (DefaultGlobalDataBlock) b.getDataBlock())
                .map(b -> {
                    try {
                        return new AnimObjects(b.getData(), context);
                    } catch (ElementException ex) {
                        LOG.log(Level.WARNING, "Error reading GlobalData 1002 (Animations).", ex);
                        ex.printStackTrace(System.err);
                        return ex.getPartial() instanceof AnimObjects
                            ? (AnimObjects) ex.getPartial()
                            : new AnimObjects();
                    }
                }).findAny().orElse(new AnimObjects());
        model.addAnimations(this.ANIMATIONS);

        SUM.click(this.TABLE3.stream().mapToInt(t -> t.calculateSize()).sum());
        LOG.fine("Reading savegame: read GlobalDataTable #3.");

        // Try to readRefID the visited worldspaces block.
        int[] visitedWorldSpaces = null;
        assert this.FORMIDARRAY != null || this.isBroken();

        try {
            // Read the worldspaces-visited table. Skip past the FormID array since
            // it was readRefID earlier.
            int skipFormIDArray = this.FLT.formIDArrayCountOffset - startingOffset + (4 + 4 * this.FORMIDARRAY.length);
            ((Buffer) INPUT).position(skipFormIDArray);

            int worldspaceIDCount = INPUT.getInt();
            visitedWorldSpaces = new int[worldspaceIDCount];
            for (int worldspaceIndex = 0; worldspaceIndex < worldspaceIDCount; worldspaceIndex++) {
                visitedWorldSpaces[worldspaceIndex] = INPUT.getInt();
            }

            LOG.fine("Reading savegame: read visited worldspace array.");
        } catch (BufferUnderflowException | IllegalArgumentException | NullPointerException ex) {
            if (!this.isBroken()) {
                this.truncated = true;
                LOG.log(Level.SEVERE, "Error reading VisitedWorldSpace array.", ex);
            }
        } finally {
            this.VISITEDWORLDSPACEARRAY = visitedWorldSpaces;
        }

        // Read whatever is left.
        final int U3SIZE = INPUT.limit() - ((Buffer) INPUT).position();
        LOG.fine(String.format("Reading savegame: read unknown block. %d bytes present.", U3SIZE));
        this.UNKNOWN3 = new byte[U3SIZE];
        INPUT.get(this.UNKNOWN3);

        if (!this.isBroken()) {
            long calculatedBodySize = this.calculateBodySize();
            long bodyPosition = ((Buffer) INPUT).position();
            if (calculatedBodySize != bodyPosition) {
                throw new IllegalStateException(String.format("Missing data, calculated body size is %d but actual body size is %d.", calculatedBodySize, bodyPosition));
            }

            if (!COMPRESSION.isCompressed()) {
                long calculatedSize = this.calculateSize();
                long fileSize = Files.size(saveFile);
                if (calculatedSize != fileSize) {
                    throw new IllegalStateException(String.format("Missing data, calculated file size size is %d but actual file size is %d.", calculatedSize, fileSize));
                }
            }
        }
    }

    /**
     * Writes the <code>ESS</code> to a <code>ByteBuffer</code>.
     *
     * @param channel The output channel for the savegame.
     * @param testingMode More aggressive writing that rewrites changeforms.
     * @throws IOException
     * @throws ElementException
     *
     */
    public void write(FileChannel channel, boolean testingMode) throws IOException, ElementException {
        final CompressionType COMPRESSION = this.HEADER.getCompression();

        // Write the header, with a litte of extra room for compression prefixes.
        ByteBuffer headerBlock = ByteBuffer.allocate(this.HEADER.calculateSize() + 8).order(ByteOrder.LITTLE_ENDIAN);
        this.HEADER.write(headerBlock);

        ((Buffer) headerBlock).flip();
        channel.write(headerBlock);
        headerBlock.compact();

        // Write the body to a ByteBuffer.
        final int UNCOMPRESSED_LEN = this.calculateBodySize();
        final ByteBuffer UNCOMPRESSED = ByteBuffer.allocate(UNCOMPRESSED_LEN).order(ByteOrder.LITTLE_ENDIAN);
        this.write(UNCOMPRESSED, testingMode);
        ((Buffer) UNCOMPRESSED).flip();

        // Do the decompression, if necessary.
        if (COMPRESSION.isCompressed()) {
            final ByteBuffer COMPRESSED;
            switch (COMPRESSION) {
                case ZLIB:
                    COMPRESSED = BufferUtil.deflateZLIB(UNCOMPRESSED, UNCOMPRESSED_LEN);
                    break;
                case LZ4:
                    COMPRESSED = BufferUtil.deflateLZ4(UNCOMPRESSED, UNCOMPRESSED_LEN);
                    break;
                default:
                    throw new IOException("Unknown compression type: " + COMPRESSION);
            }

            headerBlock.putInt(((Buffer) UNCOMPRESSED).limit());
            headerBlock.putInt(((Buffer) COMPRESSED).limit());
            ((Buffer) headerBlock).flip();
            channel.write(headerBlock);
            channel.write(COMPRESSED);

        } else {
            channel.write(UNCOMPRESSED);
        }
    }

    /**
     * Writes the body of the <code>ESS</code> to a <code>ByteBuffer</code>. The
     * header and compression prefixes are not written.
     *
     * @param output The output stream for the savegame.
     * @param testingMode More aggressive writing that rewrites changeforms.
     * @throws ElementException
     *
     */
    private void write(ByteBuffer output, boolean testingMode) throws ElementException {
        // Write the form version.
        output.put(this.FORMVERSION);

        // Write the version string.
        if (null != this.VERSION_STRING) {
            mf.BufferUtil.putWString(output, this.VERSION_STRING);
        }

        // Write the PLUGIN info section.
        this.PLUGINS.write(output);
        LOG.fine("Writing savegame: wrote plugin table.");

        // Rebuild and then write the file location table.
        this.FLT.rebuild(this);
        this.FLT.write(output);
        LOG.fine("Writing savegame: rebuilt and wrote file location table.");

        try {
            for (GlobalData data : this.TABLE1) {
                try {
                    data.write(output);
                    LOG.log(Level.FINE, "Writing savegame: \tGlobalData type {0}.", data.getType());
                } catch (RuntimeException ex) {
                    throw new ListException("Error in GlobalDataTable1." + data.getType(), data.getType(), 0, ex);
                }
            }
        } catch (ListException ex) {
            throw new ElementException("Error in GlobalDataTable1", ex, this);
        }
        LOG.fine("Writing savegame: wrote GlobalDataTable #1.");

        try {
            for (GlobalData data : this.TABLE2) {
                try {
                    data.write(output);
                    LOG.log(Level.FINE, "Writing savegame: \tGlobalData type {0}.", data.getType());
                } catch (RuntimeException ex) {
                    throw new ListException("Error in GlobalDataTable2." + data.getType(), data.getType(), 0, ex);
                }
            }
        } catch (ListException ex) {
            throw new ElementException("Error in GlobalDataTable2", ex, this);
        }
        LOG.fine("Writing savegame: wrote GlobalDataTable #2.");

        for (ChangeForm form : this.CHANGEFORMS) {
            try {
                if (testingMode) {
                    ChangeFormData data = form.getData(analysis, getContext(), false);
                    if (data != null) {
                        form.updateRawData(data);
                    }
                }
                form.write(output);
            } catch (RuntimeException ex) {
                String msg = form.getRefID() != null
                        ? "Error writing ChangeForm for " + form.getRefID().toString()
                        : "Error writing ChangeForm.";
                throw new ElementException(msg, ex, form);
            }
        }
        LOG.fine("Writing savegame: wrote changeform table.");

        try {
            for (GlobalData data : this.TABLE3) {
                try {
                    data.write(output);
                    LOG.log(Level.FINE, "Writing savegame: \tGlobalData type {0}.", data.getType());
                } catch (RuntimeException ex) {
                    throw new ListException("Error in GlobalDataTable3." + data.getType(), data.getType(), 0, ex);
                }
            }
        } catch (ListException ex) {
            throw new ElementException("Error in GlobalDataTable3", ex, this);
        }
        LOG.fine("Writing savegame: wrote GlobalDataTable #3.");

        output.putInt(this.FORMIDARRAY.length);
        for (int formID : this.FORMIDARRAY) {
            output.putInt(formID);
        }
        LOG.fine("Writing savegame: wrote formid array.");

        output.putInt(this.VISITEDWORLDSPACEARRAY.length);
        for (int formID : this.VISITEDWORLDSPACEARRAY) {
            output.putInt(formID);
        }
        LOG.fine("Writing savegame: wrote visited worldspace array.");

        output.put(this.UNKNOWN3);
        LOG.fine("Writing savegame: wrote unknown block.");
    }

    /**
     * Writes the body of the <code>ESS</code> to a <code>ByteBuffer</code>. The
     * header and compression prefixes are not written.
     *
     * @param output The output stream for the savegame.
     */
    @Override
    public void write(ByteBuffer output) {
        throw new UnsupportedOperationException("NEVER CALL THIS");
    }

    /** 
     * @return The original uncompressed size of the body data.
     */
    public int getOriginalSize() {
        return this.ORIGINAL_SIZE;
    }
    /**
     * @see Element#calculateSize()
     * @return
     */
    @Override
    public int calculateSize() {
        return this.HEADER.calculateSize() + this.calculateBodySize();
    }

    /**
     * @see Element#calculateSize()
     * @return
     */
    public int calculateBodySize() {
        int sum = 1; // form version

        if (null != this.VERSION_STRING) {
            sum += this.VERSION_STRING.length() + 2;
        }

        sum += this.PLUGINS.calculateSize();
        sum += this.FLT.calculateSize();

        sum += this.TABLE1.parallelStream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.TABLE2.parallelStream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.CHANGEFORMS.stream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.TABLE3.parallelStream().mapToInt(v -> v.calculateSize()).sum();

        sum += 4;
        sum += this.FORMIDARRAY == null ? 0 : 4 * this.FORMIDARRAY.length;
        sum += 4;
        sum += this.VISITEDWORLDSPACEARRAY == null ? 0 : 4 * this.VISITEDWORLDSPACEARRAY.length;
        sum += this.UNKNOWN3.length;

        return sum;
    }

    /**
     * @param analysis The analysis data.
     */
    public void addNames(resaver.Analysis analysis) {
        this.REFIDS.values().parallelStream().forEach(v -> v.addNames(analysis));
    }

    /**
     * @return The papyrus section.
     */
    public Papyrus getPapyrus() {
        return this.PAPYRUS;
    }

    /**
     * @return The digest of the <code>ESS</code> when it was readRefID from the
     * disk.
     */
    public Long getDigest() {
        return this.DIGEST;
    }

    /**
     * @return The original file containing the <code>ESS</code> when it was
     * readRefID from the disk.
     */
    public Path getOriginalFile() {
        return this.ORIGINAL_FILE;
    }

    /**
     * @return The list of change forms.
     */
    public ChangeFormCollection getChangeForms() {
        return this.CHANGEFORMS == null
                ? new ChangeFormCollection(0)
                : this.CHANGEFORMS;
    }

    /**
     * @return The array of form IDs.
     */
    public int[] getFormIDs() {
        return this.FORMIDARRAY == null
                ? new int[0]
                : this.FORMIDARRAY;
    }

    /**
     * @return The list of plugins.
     */
    public PluginInfo getPluginInfo() {
        return this.PLUGINS;
    }

    /**
     * @return The <code>GlobalVariableTable</code>.
     */
    public GlobalVariableTable getGlobals() {
        return this.GLOBALS;
    }

    /**
     * @return The <code>GlobalVariableTable</code>.
     */
    public AnimObjects getAnimations() {
        return this.ANIMATIONS;
    }

    /**
     * @return A flag indicating whether there is stored cosave data.
     */
    public boolean hasCosave() {
        return this.COSAVE != null;
    }

    /**
     * @return A flag indicating if the savefile has a truncation error.
     */
    public boolean isBroken() {
        return null != this.PAPYRUS 
                ? this.truncated || this.pluginOverflow || this.PAPYRUS.isBroken()
                : this.truncated || this.pluginOverflow;
    }

    /**
     * Removes all <code>ChangeForm</code> objects with havok entries.
     *
     * @param analysis
     * @return The number of forms removed.
     */
    public int[] resetHavok(Analysis analysis) {
        int success = 0;
        int failure = 0;

        final ChangeFlagConstants HAVOK = ChangeFlagConstantsRefr.CHANGE_REFR_HAVOK_MOVE;
        final ESSContext CTX = this.getContext();
        
        for (ChangeForm form : this.CHANGEFORMS) {
            if (form.getType() == ChangeForm.Type.REFR) {
                if (form.getChangeFlags().getFlag(HAVOK)) {
                    ChangeFormData data = form.getData(analysis, CTX, false);

                    ChangeFormRefr refr = (ChangeFormRefr) data;

                    if (refr == null || refr.hasUnparsed()) {
                        failure++;
                    } else {
                        boolean result = refr.clearHavok();
                        if (result) {
                            success++;
                            //Flags.Int newFlags = form.getChangeFlags().without(havokFlag);
                            //form.updateRawData(refr, newFlags);
                            form.updateRawData(refr);
                        } else {
                            failure++;
                        }
                    }
                }
            }
        }

        return new int[]{success, failure};
    }

    /**
     * Removes null entries from form lists.
     *
     * @param analysis
     * @return An array containing two ints; the first is the number of
     * changeforms that had their havok data cleared, the second is the number
     * that couldn't be processed.
     */
    public int[] cleanseFormLists(Analysis analysis) {
        int entries = 0;
        int forms = 0;

        final ESSContext CTX = this.getContext();

        for (ChangeForm form : this.CHANGEFORMS) {
            if (form.getType() == ChangeForm.Type.FLST) {
                ChangeFormData data = form.getData(analysis, CTX, false);
                ChangeFormFLST flst = (ChangeFormFLST) data;

                if (flst != null) {
                    int removed = flst.cleanse();
                    if (removed > 0) {
                        entries += removed;
                        forms++;
                        form.updateRawData(flst);
                    }
                }
            }
        }

        return new int[]{entries, forms};
    }

    /**
     * Removes all script instances that are associated with non-existent
     * created forms.
     *
     * @return The elements that were removed.
     */
    public Set<PapyrusElement> removeNonexistentCreated() {
        final java.util.Set<RefID> EXISTENT = this.getChangeForms().stream()
                .map(v -> v.getRefID())
                .collect(Collectors.toSet());

        final Set<PapyrusElement> NONEXISTENT = this.PAPYRUS.getScriptInstances()
                .values()
                .parallelStream()
                .filter(v -> v.getRefID().getType() == RefID.Type.CREATED)
                .filter(v -> !EXISTENT.contains(v.getRefID()))
                .collect(Collectors.toSet());

        return this.getPapyrus().removeElements(NONEXISTENT);
    }

    /**
     * Removes a <code>Element</code> collection.
     *
     * @param elements The elements to remove.
     * @return The elements that were removed.
     *
     */
    public java.util.Set<Element> removeElements(java.util.Collection<? extends Element> elements) {
        final Set<ChangeForm> ELEM1 = elements.stream()
                .filter(v -> v instanceof ChangeForm)
                .map(v -> (ChangeForm) v)
                .collect(Collectors.toSet());

        final Set<PapyrusElement> ELEM2 = elements.stream()
                .filter(v -> v instanceof PapyrusElement)
                .map(v -> (PapyrusElement) v)
                .collect(Collectors.toSet());

        final Set<Element> REMOVED = new java.util.HashSet<>();
        REMOVED.addAll(this.removeChangeForms(ELEM1));
        REMOVED.addAll(this.getPapyrus().removeElements(ELEM2));
        return REMOVED;
    }

    /**
     * Removes a <code>Set</code> of <code>ChangeForm</code>.
     *
     * @param forms The elements to remove.
     * @return The number of elements removed.
     *
     */
    public Set<ChangeForm> removeChangeForms(java.util.Collection<? extends ChangeForm> forms) {
        if (null == forms || forms.contains(null)) {
            throw new NullPointerException("The set of forms to be removed must not be null and must not contain null.");
        }

        final Set<ChangeForm> REMOVE = new java.util.HashSet<>(forms);
        REMOVE.retainAll(this.CHANGEFORMS);
        this.CHANGEFORMS.removeAll(REMOVE);
        return REMOVE;
    }

    /**
     * @return Flag indicating whether the game has a 32bit string model.
     */
    public boolean isFO4() {
        return this.HEADER.GAME.isFO4();
    }

    /**
     * @return Flag indicating whether the game has a 32bit string model.
     */
    public boolean isSkyrim() {
        return this.HEADER.GAME.isSkyrim();
    }

    /**
     * @return Flag indicating whether the game has a 32bit string model.
     */
    public boolean isStr32() {
        switch (this.HEADER.GAME) {
            case FALLOUT4:
            case FALLOUT_VR:
                return this.FORMVERSION > 61;
            case SKYRIM_LE:
                return false;
            default:
                return true;
        }
    }

    /**
     * @return Flag indicating whether the game is CC enabled.
     */
    public boolean supportsESL() {
        switch (this.HEADER.GAME) {
            case FALLOUT4:
            case FALLOUT_VR:
                return this.FORMVERSION >= 68;
            case SKYRIM_SW:
            case SKYRIM_SE:
            case SKYRIM_VR:
                return this.FORMVERSION >= 78;
            case SKYRIM_LE:
            default:
                return false;
        }
    }

    /**
     * @return Flag indicating whether the game supports savefile compression.
     */
    public boolean supportsCompression() {
        switch (this.HEADER.GAME) {
            case SKYRIM_SW:
            case SKYRIM_SE:
            case SKYRIM_VR:
                return true;
            case FALLOUT4:
            case FALLOUT_VR:
            case SKYRIM_LE:
            default:
                return false;
        }
    }

    /**
     * @return The value of the header field.
     */
    public Header getHeader() {
        return this.HEADER;
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @return
     */
    public String getInfo(resaver.Analysis analysis) {
        String race = this.HEADER.RACEID.toString().replace("Race", "");
        String name = this.HEADER.NAME.toString();
        int level = this.HEADER.LEVEL;
        String gender = (this.HEADER.SEX == 0 ? "male" : "female");
        String location = this.HEADER.LOCATION.toString();
        String gameDate = this.HEADER.GAMEDATE.toString();
        float xp = this.HEADER.CURRENT_XP;
        float nexp = this.HEADER.NEEDED_XP + this.HEADER.CURRENT_XP;

        long time = this.HEADER.FILETIME;
        long millis = time / 10000L - 11644473600000L;
        final java.util.Date DATE = new java.util.Date(millis);

        float fileSize = Try(() -> Files.size(this.ORIGINAL_FILE) / 1048573.0f).Catch(() -> Float.NaN);
        float calculatedSize = this.calculateSize() / 1048576.0f;
        float changeFormsSize = this.CHANGEFORMS.parallelStream().mapToInt(cf -> cf.calculateSize()).sum() / 1048576.0f;
        float papyrusSize = this.PAPYRUS == null
                ? -1.0f
                : this.PAPYRUS.calculateSize() / 1048576.0f;

        
        return p(
                h3(this.ORIGINAL_FILE.toString()),
                h3(String.format("%s the level %s %s %s, in %s on %s (%1.0f/%1.0f xp).", name, level, race, gender, location, gameDate, xp, nexp)),
                code(ul(
                        li(String.format("Version string: %s", this.VERSION_STRING)),
                        li(String.format("Form version: %s", this.FORMVERSION)),
                        li(String.format("Time: %s", DATE.toString())),
                        this.HEADER.getCompression().isCompressed() && Float.isFinite(fileSize)
                                ? li(String.format("Total size: %1.1f mb (%1.1f mb with %s)</li>", calculatedSize, fileSize, this.HEADER.getCompression()))
                                : li(String.format("Total size: %1.1f mb", calculatedSize)),
                        li(String.format("Papyrus size: %1.1f mb", papyrusSize)),
                        li(String.format("ChangeForms size: %1.1f mb", changeFormsSize)),
                        li(analysis != null 
                                ? String.format("Total ScriptData in load order: %1.1f mb", analysis.getScriptDataSize() / 1048576.0f)
                                : "Total ScriptData in load order: not available")
                ))
        ).toString();
    }

    /**
     * Retrieves the plugin corresponding to a formID.
     *
     * @param formID
     * @return
     */
    public Plugin getPluginFor(int formID) {
        final List<Plugin> FULL = this.getPluginInfo().getFullPlugins();
        final List<Plugin> LITE = this.getPluginInfo().getLitePlugins();

        final int INDEX = formID >>> 24;
        final int SUBINDEX = (formID & 0xFFFFFF) >>> 12;

        if (INDEX >= 0 && INDEX < 0xFE && INDEX < FULL.size()) {
            return FULL.get(INDEX);
        } else if (INDEX == 0xFE && SUBINDEX >= 0 && SUBINDEX < LITE.size()) {
            return this.getPluginInfo().hasLite() ? LITE.get(SUBINDEX) : null;
        } else {
            return null;
        }
    }

    /**
     * @return Returns a new <code>ESSContext</code>.
     */
    public ESSContext getContext() {
        return new ESSContext(this);
    }

    /**
     * Creates a new <code>RefID</code> directly.
     *
     * @param val The 3-byte value with which to create the <code>RefID</code>.
     * @return The new <code>RefID</code>.
     */
    public RefID make(int val) {
        //RefID r = new RefID(val, this);
        return this.REFIDS.computeIfAbsent(val, v -> new RefID(val, this));
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return this.HEADER.GAME.NAME;
        /*if (null != this.ORIGINAL_FILE) {
            return this.ORIGINAL_FILE.getFileName().toString();
        } else {
            return "NO FILENAME";
        }*/
    }

    public byte getFormVersion() {
        return this.FORMVERSION;
    }

    public String getVersionString() {
        return this.VERSION_STRING;
    }

    public FileLocationTable getFLT() {
        return this.FLT;
    }

    public int[] getVisitedWorldspaceArray() {
        return this.VISITEDWORLDSPACEARRAY;
    }

    public byte[] getUnknown3() {
        return this.UNKNOWN3;
    }

    public byte[] getCosave() {
        return this.COSAVE;
    }

    public List<GlobalData> getTable1() {
        return this.TABLE1;
    }

    public List<GlobalData> getTable2() {
        return this.TABLE2;
    }

    public List<GlobalData> getTable3() {
        return this.TABLE3;
    }

    public resaver.Analysis getAnalysis() {
        return this.analysis;
    }

    /**
     * Verifies that two instances of <code>ESS</code> are identical.
     *
     * @param ess1 The first <code>ESS</code>.
     * @param ess2 The second <code>ESS</code>.
     * @throws IllegalStateException Thrown if the two instances of
     * <code>ESS</code> are not equal.
     */
    static public void verifyIdentical(ESS ess1, ESS ess2) throws IllegalStateException {
        if (ess1.getFormVersion() != ess2.getFormVersion()) {
            throw new IllegalStateException(String.format("Form version mismatch: %d vs %d.", ess1.getFormVersion(), ess2.getFormVersion()));
        } else if (!Objects.equals(ess1.getVersionString(), ess2.getVersionString())) {
            throw new IllegalStateException(String.format("VersionString mismatch: %s vs %s.", ess1.getVersionString(), ess2.getVersionString()));
        } else if (!Objects.equals(ess1.getPluginInfo(), ess2.getPluginInfo())) {
            throw new IllegalStateException(String.format("PluginInfo mismatch: %s vs %s.", ess1.getPluginInfo(), ess2.getPluginInfo()));
        } else if (!Objects.equals(ess1.getFLT(), ess2.getFLT())) {
            throw new IllegalStateException(String.format("FileLocationTable mismatch: %s vs %s.", ess1.getFLT(), ess2.getFLT()));
        } else if (!Arrays.equals(ess1.getFormIDs(), ess2.getFormIDs())) {
            throw new IllegalStateException("FormIDs mismatch.");
        } else if (!Arrays.equals(ess1.getVisitedWorldspaceArray(), ess2.getVisitedWorldspaceArray())) {
            throw new IllegalStateException("VisitedWorldSpaceArray mismatch.");
        } else if (!Arrays.equals(ess1.getUnknown3(), ess2.getUnknown3())) {
            throw new IllegalStateException("Unknown3 mismatch.");
        } else if (!Arrays.equals(ess1.getCosave(), ess2.getCosave())) {
            throw new IllegalStateException("CoSave mismatch.");
        } else if (ess1.calculateBodySize() != ess2.calculateBodySize()) {
            throw new IllegalStateException(String.format("Body size mismatch: %d vs %d.", ess1.calculateBodySize(), ess2.calculateBodySize()));
        } else if (ess1.calculateSize() != ess2.calculateSize()) {
            throw new IllegalStateException(String.format("Total size mismatch: %d vs %d.", ess1.calculateSize(), ess2.calculateSize()));
        }

        Header.verifyIdentical(ess1.getHeader(), ess2.getHeader());

        final java.util.Iterator<ChangeForm> ITER1 = ess1.getChangeForms().iterator();
        final java.util.Iterator<ChangeForm> ITER2 = ess2.getChangeForms().iterator();

        while (ITER1.hasNext() && ITER2.hasNext()) {
            final ChangeForm CF1 = ITER1.next();
            final ChangeForm CF2 = ITER2.next();
            ChangeForm.verifyIdentical(CF1, CF2);
        }

        if (ITER1.hasNext() != ITER2.hasNext()) {
            throw new IllegalStateException("Missing changeforms.");
        }

        final Papyrus PAP1 = ess1.getPapyrus();
        final Papyrus PAP2 = ess2.getPapyrus();

        if (PAP1.getHeader() != PAP2.getHeader()) {
            throw new IllegalStateException(String.format("Papyrus header mismatch: %d vs %d.", PAP1.getHeader(), PAP2.getHeader()));
        } else if (!PAP1.getStringTable().containsAll(PAP2.getStringTable())) {
            throw new IllegalStateException("StringTable mismatch.");
        } else if (!PAP2.getStringTable().containsAll(PAP1.getStringTable())) {
            throw new IllegalStateException("StringTable mismatch.");
        }

        final ByteBuffer BUF1 = ByteBuffer.allocate(PAP1.calculateSize());
        final ByteBuffer BUF2 = ByteBuffer.allocate(PAP2.calculateSize());
        PAP1.write(BUF1);
        PAP2.write(BUF2);

        if (!Arrays.equals(BUF1.array(), BUF2.array())) {
            throw new IllegalStateException("Papyrus mismatch.");
        }
    }

    final private Header HEADER;
    final private byte FORMVERSION;
    final private String VERSION_STRING;
    final private PluginInfo PLUGINS;
    final private FileLocationTable FLT;
    final private List<GlobalData> TABLE1;
    final private List<GlobalData> TABLE2;
    final private ChangeFormCollection CHANGEFORMS;
    final private List<GlobalData> TABLE3;
    final private int[] FORMIDARRAY;
    final private int[] VISITEDWORLDSPACEARRAY;
    final private byte[] UNKNOWN3;
    final private byte[] COSAVE;
    final Path ORIGINAL_FILE;
    final private Long DIGEST;
    final private Papyrus PAPYRUS;
    final private AnimObjects ANIMATIONS;
    final private GlobalVariableTable GLOBALS;
    final private java.util.Map<Integer, RefID> REFIDS;
    private resaver.Analysis analysis;
    private boolean truncated = false;
    private boolean pluginOverflow = false;
    final private int ORIGINAL_SIZE;

    static final private Logger LOG = Logger.getLogger(ESS.class.getCanonicalName());

    static final public Predicate<Element> THREAD = (Element v)
            -> v instanceof resaver.ess.papyrus.ActiveScript;

    static final public Predicate<Element> OWNABLE = (Element v)
            -> v instanceof resaver.ess.papyrus.ActiveScript
            || v instanceof resaver.ess.papyrus.StackFrame
            || v instanceof resaver.ess.papyrus.ArrayInfo;

    static final public Predicate<Element> DELETABLE = (Element v)
            -> v instanceof resaver.ess.papyrus.Definition
            || v instanceof resaver.ess.papyrus.DefinedElement
            || v instanceof resaver.ess.papyrus.ArrayInfo
            || v instanceof resaver.ess.papyrus.ActiveScript
            || v instanceof resaver.ess.ChangeForm
            || v instanceof resaver.ess.papyrus.SuspendedStack;

    static final public Predicate<Element> PURGEABLE = (Element v)
            -> v instanceof resaver.ess.Plugin;

    /**
     * Creates a backup of a file.
     *
     * @param file The file to backup.
     * @return The backup file.
     * @throws IOException
     */
    static private Path makeBackupFile(Path file) throws IOException {
        final String TIME = new java.text.SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
        final String FILENAME = file.getFileName().toString();
        final Pattern REGEX = Pattern.compile("^(.+)\\.([ -~]+)$");
        final Matcher MATCHER = REGEX.matcher(FILENAME);

        final String NEWNAME;

        if (MATCHER.matches()) {
            final String NAME = MATCHER.group(1);
            final String EXT = MATCHER.group(2);
            NEWNAME = String.format("%s.%s.%s", NAME, TIME, EXT);
        } else {
            NEWNAME = String.format("%s.%s", FILENAME, TIME);
        }

        final Path NEWFILE = file.resolveSibling(NEWNAME);
        Files.copy(file, NEWFILE, StandardCopyOption.REPLACE_EXISTING);
        return NEWFILE;
    }

    /**
     * Stores the results of a load or save operation.
     */
    final public class Result {

        public Result(Path backup, Timer timer, FilterTreeModel model) {
            this.ESS = ESS.this;
            this.GAME = ESS.this.getHeader().GAME;
            this.SAVE_FILE = ESS.this.ORIGINAL_FILE;
            this.BACKUP_FILE = backup;
            this.TIME_S = timer.getElapsed() / 1.0e9;

            double size;
            try {
                size = Files.size(this.SAVE_FILE) / 1048576.0;
            } catch (IOException ex) {
                size = Double.NEGATIVE_INFINITY;
            }
            this.SIZE_MB = size;
            this.MODEL = model;
        }

        final public ESS ESS;
        final public Game GAME;
        final public Path SAVE_FILE;
        final public Path BACKUP_FILE;
        final public double TIME_S;
        final public double SIZE_MB;
        final public FilterTreeModel MODEL;
    }

    /**
     * A factory class for making and reading <code>RefID</code>.
     */
    static public class ESSContext {

        /**
         * Creates a new <code>ESSContext</code> for the specified
         * <code>ESS</code> object.
         *
         * @param ess
         */
        public ESSContext(ESS ess) {
            this.ESS = Objects.requireNonNull(ess);
        }

        /**
         * Creates a new <code>ESSContext</code> for the specified
         * <code>ESS</code> object.
         *
         * @param context
         */
        public ESSContext(ESSContext context) {
            this.ESS = Objects.requireNonNull(context).ESS;
        }

        /**
         * Creates a new <code>RefID</code> by reading from a
         * <code>LittleEndianDataOutput</code>. No error handling is performed.
         *
         * @param input The input stream.
         * @return The new <code>RefID</code>.
         */
        public RefID readRefID(ByteBuffer input) {
            Objects.requireNonNull(input);
            final int B1 = input.get();
            final int B2 = input.get();
            final int B3 = input.get();
            final int VAL
                    = ((B1 & 0xFF) << 16)
                    | ((B2 & 0xFF) << 8)
                    | (B3 & 0xFF);
            return makeRefID(VAL);
        }

        /**
         * Creates a new <code>RefID</code> directly.
         *
         * @param val The 3-byte value with which to create the
         * <code>RefID</code>.
         * @return The new <code>RefID</code>.
         */
        public RefID makeRefID(int val) {
            return this.ESS.make(val);
        }

        /**
         * @return Accessor for the game field.
         */
        public Game getGame() {
            return this.ESS.getHeader().GAME;
        }

        /**
         * @return A flag indicating whether the <code>ESS</code> has 32-bit
         * strings.
         */
        public boolean isStr32() {
            return this.ESS.isStr32();
        }

        /**
         * Does a very general search for an ID.
         *
         * @param number The data to search for.
         * @return Any match of any kind.
         */
        public Linkable broadSpectrumSearch(Number number) {
            try {
                final RefID REFID = this.makeRefID(number.intValue());
                return this.getChangeForm(REFID);
            } catch (RuntimeException ex) {
                LOG.log(Level.WARNING, "RuntimeException during BroadSpectrumMatch.", ex);
                return null;
            }
        }

        /**
         * Finds the <code>ChangeForm</code> corresponding to a
         * <code>RefID</code>.
         *
         * @param refID The <code>RefID</code>.
         * @return The corresponding <code>ChangeForm</code> or null if it was
         * not found.
         */
        public ChangeForm getChangeForm(RefID refID) {
            return this.ESS.getChangeForms().getChangeForm(refID);
        }

        /**
         * Used to resolve plugins loaded from a serialized object.
         * @param plugin
         * @return 
         */
        public Plugin resolvePlugin(Plugin plugin) {
            return this.ESS.PLUGINS.stream()
                    .filter(p -> p.equals(plugin))
                    .findFirst()
                    .orElse(null);
        }
        
        /**
         * @return The <code>Path</code> of the original save file.
         */
        public Path getPath() {
            return this.ESS.getOriginalFile();
        }

        /**
         * @return The <code>ESS</code> itself. May not be full constructed.
         */
        protected ESS getESS() {
            return this.ESS;
        }

        final private ESS ESS;

    }

}
