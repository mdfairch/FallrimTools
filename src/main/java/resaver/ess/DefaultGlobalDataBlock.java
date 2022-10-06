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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Describes a the data for a <code>GlobalData</code> when it is not parsed.
 *
 * @author Mark Fairchild
 */
final public class DefaultGlobalDataBlock implements GlobalDataBlock {

    /**
     * Creates a new <code>DefaultGlobalDataBlock</code> by supplying it with a
     * byte buffer.
     *
     * @param data The data.
     */
    public DefaultGlobalDataBlock(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data must not be null.");
        }
        this.DATA = data;
    }

    /**
     * @return A read-only view of the data.
     */
    public ByteBuffer getData() {
        return ByteBuffer.wrap(this.DATA).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }
    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer) 
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.DATA);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return this.DATA.length;
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + java.util.Arrays.hashCode(this.DATA);
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
        final DefaultGlobalDataBlock other = (DefaultGlobalDataBlock) obj;
        return java.util.Arrays.equals(this.DATA, other.DATA);
    }

    final private byte[] DATA;

}
