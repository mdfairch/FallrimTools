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

import resaver.Game;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Describes header of Skyrim savegames.
 *
 * @author Mark Fairchild
 */
final public class Header implements Element {

    /**
     * Creates a new <code>Header</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param path The path to the file.
     * @throws IOException
     */
    public Header(ByteBuffer input, Path path) throws IOException {
        Objects.requireNonNull(input);

        final String PREFIX = mf.BufferUtil.readSizedString(input.slice(), 4, false);
        switch (PREFIX) {
            case "TES4": {
                this.MAGIC = new byte[12];
                break;
            }
            case "TESV": {
                this.MAGIC = new byte[13];
                break;
            }
            case "FO4_": {
                this.MAGIC = new byte[12];
                break;
            }
            default:
                throw new IllegalArgumentException("Unrecognized header: " + PREFIX);
        }
        input.get(this.MAGIC);

        // Read the header size.
        final int HEADERSIZE = input.getInt();
        if (HEADERSIZE >= 256) {
            throw new IllegalArgumentException("Invalid header size " + HEADERSIZE);
        }

        // Read the version number.
        this.VERSION = input.getInt();

        // Identify which game produced the savefile.
        // Bit of a business, really.
        final String MAGICSTRING = new String(this.MAGIC).toUpperCase();

        switch (MAGICSTRING) {
            case "TESV_SAVEGAME":
                if (this.VERSION <= 9 && Game.SKYRIM_LE.testFilename(path)) {
                    this.GAME = Game.SKYRIM_LE;
                } else if (this.VERSION >= 12 && Game.SKYRIM_SE.testFilename(path)) {
                    this.GAME = Game.SKYRIM_SE;
                } else if (this.VERSION >= 12 && Game.SKYRIM_SW.testFilename(path)) {
                    this.GAME = Game.SKYRIM_SW;
                } else {
                    throw new IllegalArgumentException("Unknown version of Skyrim: " + this.VERSION);
                }
                break;
            case "FO4_SAVEGAME":
                if (11 <= this.VERSION && Game.FALLOUT4.testFilename(path)) {
                    this.GAME = Game.FALLOUT4;
                } else {
                    throw new IllegalArgumentException("Unknown version of Fallout4: " + this.VERSION);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown game: " + MAGICSTRING);
        }

        this.SAVENUMBER = input.getInt();
        this.NAME = WStringElement.read(input);
        this.LEVEL = input.getInt();
        this.LOCATION = WStringElement.read(input);
        this.GAMEDATE = WStringElement.read(input);
        this.RACEID = WStringElement.read(input);
        this.SEX = input.getShort();
        this.CURRENT_XP = input.getFloat();
        this.NEEDED_XP = input.getFloat();
        this.FILETIME = input.getLong();
        this.SCREENSHOT_WIDTH = input.getInt();
        this.SCREENSHOT_HEIGHT = input.getInt();
        this.compression = this.GAME == Game.SKYRIM_SE
                ? CompressionType.read(input)
                : null;

        if (HEADERSIZE != this.partialSize()) {
            throw new IllegalArgumentException(String.format("Header size should be %d, found %d.", HEADERSIZE, this.partialSize()));
        }

        switch (this.GAME) {
            case SKYRIM_LE:
                this.BYPP = 3;
                break;
            case FALLOUT4:
            case SKYRIM_SE:
                this.BYPP = 4;
                break;
            default:
                throw new IllegalArgumentException("Invalid game: " + this.GAME);
        }

        this.SCREENSHOT = new byte[this.BYPP * this.SCREENSHOT_WIDTH * this.SCREENSHOT_HEIGHT];
        input.get(this.SCREENSHOT);

        if (this.SCREENSHOT.length < 10) {
            this.IMAGE = null;
        } else {
            this.IMAGE = new BufferedImage(this.SCREENSHOT_WIDTH, this.SCREENSHOT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            int x = 0;
            int y = 0;

            for (int i = 0; i < this.SCREENSHOT.length; i += this.BYPP) {
                int rgb = 0;
                rgb |= (this.SCREENSHOT[i + 2] & 0xFF);
                rgb |= (this.SCREENSHOT[i + 1] & 0xFF) << 8;
                rgb |= (this.SCREENSHOT[i + 0] & 0xFF) << 16;
                if (this.BYPP == 4) {
                    rgb |= (this.SCREENSHOT[i + 3] & 0xFF) << 24;
                }
                IMAGE.setRGB(x, y, rgb);

                x++;
                if (x >= this.SCREENSHOT_WIDTH) {
                    x = 0;
                    y++;
                }
            }
        }
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.MAGIC);
        output.putInt(this.partialSize());
        output.putInt(this.VERSION);
        output.putInt(this.SAVENUMBER);
        this.NAME.write(output);
        output.putInt(this.LEVEL);
        this.LOCATION.write(output);
        this.GAMEDATE.write(output);
        this.RACEID.write(output);
        output.putShort(this.SEX);
        output.putFloat(this.CURRENT_XP);
        output.putFloat(this.NEEDED_XP);
        output.putLong(this.FILETIME);
        output.putInt(this.SCREENSHOT_WIDTH);
        output.putInt(this.SCREENSHOT_HEIGHT);
        if (this.compression != null) {
            this.compression.write(output);
        }

        output.put(this.SCREENSHOT);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 4;
        sum += this.partialSize();
        sum += this.MAGIC.length;
        sum += this.SCREENSHOT.length;
        return sum;
    }

    /**
     * The size of the header, not including the magic string, the size itself,
     * or the screenshot.
     *
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    private int partialSize() {
        int sum = 0;
        sum += 4; // version
        sum += 4; // savenumber
        sum += this.NAME.calculateSize();
        sum += 4; // level
        sum += this.LOCATION.calculateSize();
        sum += this.GAMEDATE.calculateSize();
        sum += this.RACEID.calculateSize();
        sum += 2; // sex
        sum += 4; // current xp
        sum += 4; // needed xp
        sum += 8; // filtime
        sum += 8; // screenshot size
        sum += this.compression == null ? 0 : this.compression.calculateSize();
        return sum;
    }

    /**
     * Verifies that two instances of <code>Header</code> are identical.
     *
     * @param h1 The first <code>Header</code>.
     * @param h2 The second <code>Header</code>.
     * @throws IllegalStateException Thrown if the two instances of
     * <code>Header</code> are not equal.
     */
    static public void verifyIdentical(Header h1, Header h2) throws IllegalStateException {
        if (!Arrays.equals(h1.MAGIC, h2.MAGIC)) {
            throw new IllegalStateException(String.format("Magic mismatch: %s vs %s.", Arrays.toString(h1.MAGIC), Arrays.toString(h2.MAGIC)));
        } else if (!h1.NAME.equals(h2.NAME)) {
            throw new IllegalStateException(String.format("Name mismatch: %s vs %s.", h1.NAME, h2.NAME));
        } else if (!h1.LOCATION.equals(h2.LOCATION)) {
            throw new IllegalStateException(String.format("Location mismatch: %s vs %s.", h1.LOCATION, h2.LOCATION));
        } else if (!h1.GAMEDATE.equals(h2.GAMEDATE)) {
            throw new IllegalStateException(String.format("GameDate mismatch: %s vs %s.", h1.GAMEDATE, h2.GAMEDATE));
        } else if (!h1.RACEID.equals(h2.RACEID)) {
            throw new IllegalStateException(String.format("RaceID mismatch: %s vs %s.", h1.RACEID, h2.RACEID));
        } else if (!h1.GAME.equals(h2.GAME)) {
            throw new IllegalStateException(String.format("Game mismatch: %s vs %s.", h1.GAME, h2.GAME));
        } else if (h1.VERSION != h2.VERSION) {
            throw new IllegalStateException(String.format("Version mismatch: %d vs %d.", h1.VERSION, h2.VERSION));
        } else if (h1.SAVENUMBER != h2.SAVENUMBER) {
            throw new IllegalStateException(String.format("SaveNumber mismatch: %d vs %d.", h1.SAVENUMBER, h2.SAVENUMBER));
        } else if (h1.LEVEL != h2.LEVEL) {
            throw new IllegalStateException(String.format("Level mismatch: %d vs %d.", h1.LEVEL, h2.LEVEL));
        } else if (h1.SEX != h2.SEX) {
            throw new IllegalStateException(String.format("Sex mismatch: %d vs %d.", h1.SEX, h2.SEX));
        } else if (h1.CURRENT_XP != h2.CURRENT_XP) {
            throw new IllegalStateException(String.format("CurrentXP mismatch: %f vs %f.", h1.CURRENT_XP, h2.CURRENT_XP));
        } else if (h1.NEEDED_XP != h2.NEEDED_XP) {
            throw new IllegalStateException(String.format("NeededXP mismatch: %f vs %f.", h1.NEEDED_XP, h2.NEEDED_XP));
        } else if (h1.FILETIME != h2.FILETIME) {
            throw new IllegalStateException(String.format("FileTime mismatch: %d vs %d.", h1.FILETIME, h2.FILETIME));
        } else if (h1.SCREENSHOT_WIDTH != h2.SCREENSHOT_WIDTH) {
            throw new IllegalStateException(String.format("ScreenShotWidth mismatch: %d vs %d.", h1.SCREENSHOT_WIDTH, h2.SCREENSHOT_WIDTH));
        } else if (h1.SCREENSHOT_HEIGHT != h2.SCREENSHOT_HEIGHT) {
            throw new IllegalStateException(String.format("ScreenShotHeight mismatch: %d vs %d.", h1.SCREENSHOT_HEIGHT, h2.SCREENSHOT_HEIGHT));
        } else if (h1.BYPP != h2.BYPP) {
            throw new IllegalStateException(String.format("BYPP mismatch: %d vs %d.", h1.BYPP, h2.BYPP));
        } else if (h1.compression != h2.compression) {
            throw new IllegalStateException(String.format("Compression mismatch: %s vs %s.", h1.compression, h2.compression));
        } else if (!Arrays.equals(h1.SCREENSHOT, h2.SCREENSHOT)) {
            throw new IllegalStateException("Screenshot mismatch.");
        }
    }

    /**
     * @param width The width for scaling.
     * @return A <code>ImageIcon</code> that can be used to display the
     * screenshot.
     */
    public javax.swing.ImageIcon getImage(int width) {
        if (this.IMAGE == null) {
            return null;
        }

        double scale = ((double) width) / ((double) this.SCREENSHOT_WIDTH);
        int newWidth = (int) (scale * this.SCREENSHOT_WIDTH);
        int newHeight = (int) (scale * this.SCREENSHOT_HEIGHT);

        final BufferedImage IMG = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        final java.awt.geom.AffineTransform XFORM = AffineTransform.getScaleInstance(scale, scale);
        final java.awt.Graphics2D G = IMG.createGraphics();
        G.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        G.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        G.drawRenderedImage(this.IMAGE, XFORM);
        return new javax.swing.ImageIcon(IMG);
    }

    /**
     * @return The <code>CompressionType</code> of the savefile.
     */
    public CompressionType getCompression() {
        return this.compression == null
                ? CompressionType.UNCOMPRESSED
                : this.compression;
    }

    /**
     * @param newCompressionType The new <code>CompressionType</code> for the
     * <code>Header</code>.
     * 
     * @throws IllegalArgumentException Thrown if the
     * <code>CompressionType</code> is <code>null</code> for a savefile that supports
     * compression, or if this method is called for any savefile that does not support compression.
     */
    public void setCompression(CompressionType newCompressionType) {
        switch (GAME) {
            case SKYRIM_SE:
            case SKYRIM_SW:
            case SKYRIM_VR:
                if (newCompressionType != null) {
                    this.compression = newCompressionType;
                    return;
                } else {
                    throw new IllegalArgumentException("The compression type must not be null.");
                }
            case SKYRIM_LE:
            case FALLOUT4:
            case FALLOUT_VR:
            default:
                throw new IllegalArgumentException("Compression not supported.");
        }
    }

    final public byte[] MAGIC;
    final public int VERSION;
    final public int SAVENUMBER;
    final public WStringElement NAME;
    final public int LEVEL;
    final public WStringElement LOCATION;
    final public WStringElement GAMEDATE;
    final public WStringElement RACEID;
    final public short SEX;
    final public float CURRENT_XP;
    final public float NEEDED_XP;
    final public long FILETIME;
    final public int SCREENSHOT_WIDTH;
    final public int SCREENSHOT_HEIGHT;
    final public int BYPP;
    private CompressionType compression;
    final public Game GAME;
    final public byte[] SCREENSHOT;
    final private BufferedImage IMAGE;

}
