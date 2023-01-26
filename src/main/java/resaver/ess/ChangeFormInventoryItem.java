/*
 * Copyright 2017 Mark.
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

/**
 * Manages an inventory item.
 *
 * @author Mark Fairchild
 */
public class ChangeFormInventoryItem extends GeneralElement {

    /**
     * Creates a new <code>ChangeFormInventoryItem</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>ESSContext</code> info.
     * @throws ElementException
     * 
     */
    public ChangeFormInventoryItem(ByteBuffer input, ESS.ESSContext context) throws ElementException {
        
        try {
            this.ITEM = super.readRefID(input, "ITEM", context);
            this.COUNT = super.readInt(input, "COUNT");
            this.EXTRA = super.readElement(input, "EXTRA", in -> new ChangeFormExtraData(in, context));
        } catch (RuntimeException ex) {
            throw new ElementException("Error reading ChangeFormInventoryItem", ex, this);
        }
    }
    
    public String getName() {
        if (this.ITEM == null) {
            return "ITEM";
        } else {
            return this.ITEM.getName().orElse("ITEM");
        }
        
    }
    @Override
    protected String toStringFlat(String name) {
        return this.ITEM == null 
                ? "NULL"
                : new StringBuilder()
                        .append(this.ITEM.toHTML(null))
                        .append(" (count = ")
                        .append(this.COUNT)
                        .append(")")
                        .toString();
        
    }

    @Override
    protected String toStringStructured(String name, int level) {
        return new StringBuilder()
                .append(indent2(level))
                .append(toStringFlat(""))
                .append(this.EXTRA == null || this.EXTRA.isEmpty() ? "" : this.EXTRA.toStringStructured("", level))
                .toString();
    }
    
    
    final public RefID ITEM;
    final public int COUNT;
    final private ChangeFormExtraData EXTRA;
    

}
