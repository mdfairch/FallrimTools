/*
 * Copyright 2016 Mark.
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Mark Fairchild
 */
final public class DisassemblyException extends Exception {

    /**
     * Creates a new instance of <code>DisassemblyException</code> with a
     * message, an empty partial disassembly, and no cause.
     *
     * @param message
     */
    public DisassemblyException(String message) {
        this(message, Collections.emptyList(), 0, null);
    }

    /**
     * Creates a new instance of <code>DisassemblyException</code> with a
     * message, a partial disassembly, and a cause.
     *
     * @param message The exception message.
     * @param partial The partial disassembly.
     * @param pdel The point-delta.
     * @param cause
     */
    public DisassemblyException(String message, List<String> partial, int pdel, Throwable cause) {
        super(message, cause);
        Objects.requireNonNull(partial);
        this.PARTIAL = Collections.unmodifiableList(new java.util.ArrayList<>(partial));
        this.PDEL = pdel;
    }

    /**
     * @return The partial disassembly.
     */
    public List<String> getPartial() {
        return this.PARTIAL;
    }

    /**
     * @return The pointer-delta.
     */
    public int getPtrDelta()    {
        return this.PDEL;
    }
    
    final private List<String> PARTIAL;
    final private int PDEL;
}
