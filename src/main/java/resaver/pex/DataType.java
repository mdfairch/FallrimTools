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

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.Objects;

/**
 * Describes the six datatypes that appear in PEX files.
 *
 * @author Mark Fairchild
 */
public enum DataType {
    NONE,
    IDENTIFIER,
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN;

    /**
     * Read a <code>DataType</code> from an input stream.
     *
     * @param input The input stream.
     * @return The <code>DataType</code>.
     */
    static DataType read(ByteBuffer input) throws IOException {
        Objects.requireNonNull(input);
        
        int index = Byte.toUnsignedInt(input.get());
        if (index < 0 || index >= VALUES.length) {
            throw new IOException("Invalid DataType.");
        }
        
        return VALUES[index];
    }

    static final private DataType[] VALUES = DataType.values();
}
