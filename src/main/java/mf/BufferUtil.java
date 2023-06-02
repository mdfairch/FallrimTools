/*
 * Copyright 2020 Mark.
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
package mf;

import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Objects;
import java.util.logging.Logger;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.mozilla.universalchardet.UniversalDetector;
import resaver.ess.ESS;

/**
 *
 * @author Mark
 */
public class BufferUtil {

    /**
     * Reads an LString (4-byte length-prefixed).
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The <code>String</code>.
     */
    static public String getLString(ByteBuffer buffer) {
        int length = buffer.getInt();
        return readSizedString(buffer, length, false);
    }

    /**
     * Reads a raw WString (4-byte length-prefixed).
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The raw byte data, excluding the terminus (if any).
     */
    static public byte[] getLStringRaw(ByteBuffer buffer) {
        int length = buffer.getInt();
        return readSizedStringRaw(buffer, length, false);
    }

    /**
     * Reads a WString (2-byte length-prefixed).
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The <code>String</code>.
     */
    static public String getWString(ByteBuffer buffer) {
        int length = Short.toUnsignedInt(buffer.getShort());
        return readSizedString(buffer, length, false);
    }

    /**
     * Reads a raw WString (2-byte length-prefixed).
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The raw byte data, excluding the terminus (if any).
     */
    static public byte[] getWStringRaw(ByteBuffer buffer) {
        int length = Short.toUnsignedInt(buffer.getShort());
        return readSizedStringRaw(buffer, length, false);
    }

    /**
     * Reads a BString (1-byte length-prefixed).
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The <code>String</code>.
     */
    static public String getBString(ByteBuffer buffer) {
        int length = Byte.toUnsignedInt(buffer.get());
        return readSizedString(buffer, length, false);
    }

    /**
     * Reads an LString (4-byte length-prefixed).
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The <code>String</code>.
     */
    static public String getLZString(ByteBuffer buffer) {
        int length = buffer.getInt();
        return readSizedString(buffer, length, true);
    }

    /**
     * Reads a WString (2-byte length-prefixed).
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The <code>String</code>.
     */
    static public String getWZString(ByteBuffer buffer) {
        int length = Short.toUnsignedInt(buffer.getShort());
        return readSizedString(buffer, length, true);
    }

    /**
     * Reads a BString (1-byte length-prefixed).
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The <code>String</code>.
     */
    static public String getBZString(ByteBuffer buffer) {
        int length = Byte.toUnsignedInt(buffer.get());
        return readSizedString(buffer, length, true);
    }

    /**
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The <code>String</code>.
     */
    static public String getZString(ByteBuffer buffer) {
        final byte[] BYTES = getZStringRaw(buffer);
        return BYTES == null ? null : new String(BYTES, UTF_8);
    }

    /**
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The raw byte data, excluding the terminus (if any).
     */
    static public byte[] getZStringRaw(ByteBuffer buffer) {
        final int start = ((Buffer) buffer).position();

        //while (buffer.get() != 0);
        byte b = buffer.get();
        while (b != 0) {
            b = buffer.get();
        }

        final int LENGTH = ((Buffer) buffer).position() - start;
        ((Buffer) buffer).position(start);
        
        if (LENGTH <= 0) {
            throw new IllegalArgumentException("Found invalid ZString length of " + LENGTH);
        }
        
        return readSizedStringRaw(buffer, LENGTH, true);
    }

    /**
     * Reads a string with a known size.
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @param zterminated A flag indicating whether the string is 0-terminated.
     * @param size The size of the string, including the terminus (if any).
     * @return The <code>String</code>.
     */
    static public String readSizedString(ByteBuffer buffer, int size, boolean zterminated) {
        final byte[] BYTES = readSizedStringRaw(buffer, size, zterminated);
        return BYTES == null ? null : new String(BYTES, UTF_8);
    }

    /**
     * Reads a string with a known size.
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @param zterminated A flag indicating whether the string is 0-terminated.
     * @param size The size of the string, including the terminus (if any).
     * @return The raw byte data, excluding the terminus (if any).
     */
    static public byte[] readSizedStringRaw(ByteBuffer buffer, int size, boolean zterminated) {
        try {
            int length = zterminated ? size - 1 : size;
            if (length < 0) {
                return null;
                
            } else if (length == 0) {                
                return new byte[0];
                
            } else {
                byte[] bytes = new byte[length];
                buffer.get(bytes);
                int terminus = zterminated ? buffer.get() : 0;
                if (terminus != 0) {
                    throw new IllegalArgumentException("Missing terminus");
                }
                return bytes;
            }
            
        } catch (BufferUnderflowException ex) {
            throw ex;
        }
    }

    /**
     * Reads a UTF8 string.
     *
     * @param buffer The <code>ByteBuffer</code> to read.
     * @return The <code>String</code>.
     *
     */
    static public String getUTF(ByteBuffer buffer) {
        int length = Short.toUnsignedInt(buffer.getShort());
        if (length < 0 || length >= 32768) {
            throw new IllegalArgumentException("Invalid string length: " + length);
        }

        if (length < 0) {
            return null;
        } else if (length == 0) {
            return "";
        } else {
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            String str = new String(bytes, UTF_8);
            return str;
        }
    }

    /**
     *
     * @param buffer The <code>ByteBuffer</code> to write.
     * @param string The <code>String</code>.
     * @return The <code>ByteBuffer</code> (allows chaining).
     */
    static public ByteBuffer putZString(ByteBuffer buffer, String string) {
        return buffer.put(string.getBytes(UTF_8)).put((byte) 0);
    }

    /**
     *
     * @param buffer The <code>ByteBuffer</code> to write.
     * @param string The <code>String</code>.
     * @return The <code>ByteBuffer</code> (allows chaining).
     */
    static public ByteBuffer putWString(ByteBuffer buffer, String string) {
        return BufferUtil.putWStringRaw(buffer, string.getBytes(UTF_8));
    }

    /**
     *
     * @param buffer The <code>ByteBuffer</code> to write.
     * @param bytes The <code>String</code>.
     * @return The <code>ByteBuffer</code> (allows chaining).
     */
    static public ByteBuffer putWStringRaw(ByteBuffer buffer, byte[] bytes) {
        return buffer.putShort((short) bytes.length).put(bytes);
    }

    /**
     * Converts a byte array to a string using the fancy Mozilla region
     * detection.
     *
     * @param bytes
     * @return
     */
    static public String mozillaString(byte[] bytes) {
        DETECTOR.handleData(bytes, 0, bytes.length);
        DETECTOR.dataEnd();
        final String ENCODING = DETECTOR.getDetectedCharset();
        DETECTOR.reset();

        final Charset CHARSET = (null == ENCODING ? UTF_8 : Charset.forName(ENCODING));
        if (CHARSET == null) {
            throw new IllegalStateException("Missing character set.");
        }

        final String STR = new String(bytes, CHARSET);
        return STR;
    }

    /**
     * Simple wrapper for inflating a small <code>ByteBuffer</code> using ZLIB.
     *
     * @param compressed
     * @param uncompressedSize
     * @return
     * @throws java.util.zip.DataFormatException
     */
    static public ByteBuffer inflateZLIB(ByteBuffer compressed, int uncompressedSize) throws java.util.zip.DataFormatException {
        int compressedSize = ((Buffer) compressed).limit() - ((Buffer) compressed).position();
        return inflateZLIB(compressed, uncompressedSize, compressedSize);
    }

    /**
     * Simple wrapper for inflating a small <code>ByteBuffer</code> using ZLIB.
     *
     * @param compressed
     * @param uncompressedSize
     * @param compressedSize
     * @return
     * @throws java.util.zip.DataFormatException
     */
    static public ByteBuffer inflateZLIB(ByteBuffer compressed, int uncompressedSize, int compressedSize) throws java.util.zip.DataFormatException {
        final byte[] UNCOMPRESSED_BYTES = new byte[uncompressedSize];
        final byte[] COMPRESSED_BYTES = new byte[compressedSize];
        compressed.get(COMPRESSED_BYTES);

        final java.util.zip.Inflater INFLATER = new java.util.zip.Inflater();

        try {
            INFLATER.setInput(COMPRESSED_BYTES);
            int bytesInflated = INFLATER.inflate(UNCOMPRESSED_BYTES);
            //if (bytesInflated != uncompressedSize) {
                //throw new IllegalStateException(String.format("Inflated %d bytes but expecting %d bytes.", bytesInflated, uncompressedSize));
            //}
            if (bytesInflated > uncompressedSize) {
                throw new IllegalStateException(String.format("Inflated %d bytes but expecting %d bytes.", bytesInflated, uncompressedSize));
            } else if (bytesInflated < uncompressedSize) {
                LOG.warning(String.format("Inflated %d bytes but expecting %d bytes.", bytesInflated, uncompressedSize));                
            }
            
            return ByteBuffer.wrap(UNCOMPRESSED_BYTES);
        } finally {
            INFLATER.end();
        }
    }

    /**
     * Simple wrapper for inflating a small <code>ByteBuffer</code> using LZ4.
     *
     * @param compressed
     * @param uncompressedSize
     * @return
     */
    static public ByteBuffer inflateLZ4(ByteBuffer compressed, int uncompressedSize) {
        ByteBuffer uncompressed = ByteBuffer.allocate(uncompressedSize);
        final LZ4Factory LZ4FACTORY = LZ4Factory.fastestInstance();
        final LZ4FastDecompressor LZ4DECOMP = LZ4FACTORY.fastDecompressor();
        LZ4DECOMP.decompress(compressed, uncompressed);
        ((java.nio.Buffer) uncompressed).flip();
        return uncompressed;
    }

    /**
     * Simple wrapper for deflating a small <code>ByteBuffer</code> using ZLIB.
     *
     * @param uncompressed
     * @param uncompressedSize
     * @return
     */
    static public ByteBuffer deflateZLIB(ByteBuffer uncompressed, int uncompressedSize) {
        final int SIZE = Math.min(uncompressedSize, uncompressed.limit());
        final byte[] UNCOMPRESSED_BYTES = new byte[SIZE];
        final byte[] COMPRESSED_BYTES = new byte[11 * SIZE / 10];
        uncompressed.get(UNCOMPRESSED_BYTES);

        final java.util.zip.Deflater DEFLATER = new java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION);

        try {
            DEFLATER.setInput(UNCOMPRESSED_BYTES);
            DEFLATER.deflate(COMPRESSED_BYTES);
            DEFLATER.finish();
            if (DEFLATER.getBytesRead() != SIZE) {
                throw new IllegalStateException(String.format("Inflated %d bytes but expecting %d bytes.", DEFLATER.getBytesRead(), SIZE));
            }
            return ByteBuffer.wrap(COMPRESSED_BYTES, 0, (int) DEFLATER.getBytesWritten());
        } finally {
            DEFLATER.end();
        }

    }

    /**
     * Simple wrapper for deflating a small <code>ByteBuffer</code> using LZ4.
     *
     * @param uncompressed
     * @param uncompressedSize
     * @return
     */
    static public ByteBuffer deflateLZ4(ByteBuffer uncompressed, int uncompressedSize) {
        final LZ4Factory LZ4FACTORY = LZ4Factory.fastestInstance();
        final LZ4Compressor LZ4COMP = LZ4FACTORY.fastCompressor();
        final ByteBuffer COMPRESSED = ByteBuffer.allocate(LZ4COMP.maxCompressedLength(uncompressedSize));
        LZ4COMP.compress(uncompressed, COMPRESSED);
        ((java.nio.Buffer) COMPRESSED).flip();
        return COMPRESSED;
    }

    /**
     * Dynamic counterpart to the <code>PeekAtBuffer</code> method.
     * @see mf.BufferUtil.PeekAtBuffer(java.nio.ByteBuffer)
     */
    static public class Peeker {
        public Peeker(ByteBuffer buf) {
            BUF = Objects.requireNonNull(buf);
        }
        
        @Override
        public String toString() {
            return PeekAtBuffer(BUF);
        }
        
        final private ByteBuffer BUF;
    }
    
    /**
     * Returns a string containing a hexadecimal byte formatted view of the
     * next 2048 bytes of a buffer.
     * 
     * @param buf
     * @return 
     */
    static public String PeekAtBuffer(ByteBuffer buf) {
        final StringBuilder peek = new StringBuilder();
        ByteBuffer slice = buf.remaining() > 2048 
                ? buf.slice().limit(2048)
                : buf.slice();
        
        while (slice.hasRemaining()) {
            byte b = slice.get();
            int v = ((int)b) & 0xFF;
            peek.append(String.format("%02x ", v));
        }
        
        return peek.toString();
    }
    
    /**
     * Used to decode strings intelligently.
     */
    static final private UniversalDetector DETECTOR = new UniversalDetector(null);

    /**
     * @return A log of all character sets that have been detected.
     */
    static public java.util.Set<Charset> getCharsetLog() {
        return java.util.Collections.unmodifiableSet(CHARSET_LOG);
    }

    static final private java.util.Set<Charset> CHARSET_LOG = new java.util.HashSet<>();

    static final private Logger LOG = Logger.getLogger(ESS.class.getCanonicalName());
}
