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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import resaver.ess.papyrus.Papyrus;
import resaver.ess.papyrus.PapyrusException;

/**
 * Describes 3-byte formIDs from Skyrim savegames.
 *
 * @author Mark Fairchild
 */
final public class GlobalData implements Element {

    /**
     * Creates a new <code>GlobalData</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>ESSContext</code> info.
     * @param model A <code>ModelBuilder</code>.
     * @throws PapyrusException
     */
    public GlobalData(ByteBuffer input, ESS.ESSContext context, ModelBuilder model) throws PapyrusException {
        this.TYPE = input.getInt();
        int blockSize = input.getInt();
        final ByteBuffer subSection = input.slice().order(ByteOrder.LITTLE_ENDIAN);
        ((Buffer) subSection).limit(blockSize);
        ((Buffer) input).position(((Buffer) input).position() + blockSize);

        switch (this.TYPE) {
            case 3:
                this.BLOCK = new GlobalVariableTable(subSection, context);
                break;
            case 1001:
                this.BLOCK = new Papyrus(subSection, context, model);
                break;
            default:
                final byte[] DATA = new byte[blockSize];
                subSection.get(DATA);
                this.BLOCK = new DefaultGlobalDataBlock(DATA);
                break;
        }

        long calculatedSize = this.calculateSize() - 8;
        
        if (calculatedSize != blockSize) {
            throw new IllegalStateException(String.format("Missing data for table %d, calculated size is %d but block size is %d.", this.TYPE, calculatedSize, blockSize));
        }
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.TYPE);
        output.putInt(this.BLOCK.calculateSize());
        this.BLOCK.write(output);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 8 + this.BLOCK.calculateSize();
    }

    /**
     * @return The value of the type field.
     */
    public int getType() {
        return this.TYPE;
    }

    /**
     * @return The data block.
     */
    public GlobalDataBlock getDataBlock() {
        return this.BLOCK;
    }

    /**
     *
     * @return
     */
    public Papyrus getPapyrus() {
        /*if (!(this.TYPE == 1001 && this.BLOCK instanceof Papyrus)) {
            throw new IllegalStateException("Not a papyrus block.");
        }*/

        return (Papyrus) this.BLOCK;
    }

    /**
     * @see Object#toString()
     * @return
     */
    @Override
    public String toString() {
        return super.toString() + ": type " + Integer.toString(this.TYPE);
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.BLOCK);
        return hash;
    }

    /**
     * @see Object#equals(java.lang.Object)
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GlobalData other = (GlobalData) obj;
        return Objects.equals(this.BLOCK, other.BLOCK);
    }

    final private int TYPE;
    final private GlobalDataBlock BLOCK;

}
