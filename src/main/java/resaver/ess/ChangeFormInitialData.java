/*
 * Copyright 2017 Mark Fairchild.
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
import java.util.Objects;

/**
 * Manages the initial data field from a <code>ChangeForm</code>.
 *
 * @author Mark Fairchild
 */
class ChangeFormInitialData extends GeneralElement {

    /**
     * Creates a new <code>ChangeFormInitialData</code>.
     * @param input
     * @param initialType
     * @throws ElementException 
     * 
     */
    public ChangeFormInitialData(ByteBuffer input, int initialType, ESS.ESSContext context) throws ElementException {
        Objects.requireNonNull(input);
        switch (initialType) {
            case 1:
                super.readShort(input, "UNK");
                super.readByte(input, "CELLX");
                super.readByte(input, "CELLY");
                super.readInt(input, "UNK2");
                break;
            case 2:
                super.readShort(input, "UNK");
                super.readShort(input, "UNK1");
                super.readShort(input, "UNK2");
                super.readInt(input, "UNK3");
                break;
            case 3:
                super.readInt(input, "UNK");
                break;
            case 4:
                super.readRefID(input, "CELL", context);
                super.readFloats(input, "POS", 3);
                super.readFloats(input, "ROT", 3);
                break;
            case 5:
                super.readRefID(input, "CELL", context);
                super.readFloats(input, "POS", 3);
                super.readFloats(input, "ROT", 3);
                super.readByte(input, "UNK");
                super.readRefID(input, "BASE_OBJECT", context);
                break;
            case 6:
                super.readRefID(input, "CELL", context);
                super.readFloats(input, "POS", 3);
                super.readFloats(input, "ROT", 3);
                super.readRefID(input, "STARTING CELL", context);
                super.readShort(input, "UNK1");
                super.readShort(input, "UNK2");
                break;
            default:
        }
        
        INITIAL_TYPE = initialType;
    }
    
    final private int INITIAL_TYPE;
}
