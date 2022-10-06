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

/**
 * A base interface describing anything that can be read from an ESP.
 *
 * @author Mark Fairchild
 */
public interface Entry {

    /**
     * Writes the Entry.
     *
     * @param output The <code>ByteBuffer</code> to write.
     */
    abstract public void write(java.nio.ByteBuffer output);

    /**
     * Calculates the size of the Entry.
     *
     * @return The size of the Field in bytes.
     */
    abstract public int calculateSize();

    /**
     *
     * @param buffer
     * @param newLimit
     * @return
     */
    static public java.nio.ByteBuffer advancingSlice(java.nio.ByteBuffer buffer, int newLimit) {
        // Make the new slice.
        java.nio.ByteBuffer newSlice = buffer.slice().order(java.nio.ByteOrder.LITTLE_ENDIAN);
        ((java.nio.Buffer) newSlice).limit(newLimit);

        // Advance the original.
        java.nio.Buffer buffer2 = buffer;
        buffer2.position(buffer2.position() + newLimit);
        
        return newSlice;
    }
}
