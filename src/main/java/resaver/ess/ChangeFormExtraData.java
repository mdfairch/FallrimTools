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
import resaver.ListException;

/**
 * Manages an extra data field from a change form.
 *
 * @author Mark Fairchild
 */
final public class ChangeFormExtraData extends GeneralElement {

    /**
     * Creates a new <code>ChangeFormExtraData</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>ESSContext</code> info.
     * @throws ElementException
     * 
     */
    public ChangeFormExtraData(ByteBuffer input, ESS.ESSContext context) throws ElementException {
        Objects.requireNonNull(input);
        
        try {
            final int COUNT = this.readVSVal(input, "EXTRA_DATA_COUNT").getValue();
            if (COUNT < 0) {
                throw new IllegalArgumentException("Negative array count: " + COUNT);
            } else if (COUNT > 1024) {
                throw new IllegalArgumentException("Excessive array count: " + COUNT);
            }

            final ChangeFormExtraDataData[] VAL = super.addValue("DATA", new ChangeFormExtraDataData[COUNT]);

            for (int i = 0; i < COUNT; i++) {
                try {
                    ChangeFormExtraDataData element = new ChangeFormExtraDataData(input, context);
                    VAL[i] = element;
                } catch (ElementException ex) {
                    VAL[i] = (ChangeFormExtraDataData) ex.getPartial();
                    throw new ListException(i, COUNT, ex);
                } catch (RuntimeException ex) {
                    throw new ListException(i, COUNT, ex);
                }
            }
            
        } catch (RuntimeException ex) {
            throw new ElementException("Error reading ExtraData", ex, this);
        }
    }

}
