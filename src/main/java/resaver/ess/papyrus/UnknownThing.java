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
package resaver.ess.papyrus;

import java.util.Objects;
import java.nio.ByteBuffer;

/**
 *
 * @author Mark Fairchild
 */
final public class UnknownThing implements PapyrusElement {

    /**
     * @param input The input stream.
     */
    public UnknownThing(ByteBuffer input) {
        this.VALUE = input.getInt();
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);
        output.putInt(this.VALUE);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 4;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return EID.pad8(this.VALUE);
    }

    final private int VALUE;
}
