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
package resaver.ess.papyrus;

/**
 * An exception that stores a partially loaded Papyrus structure.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class PapyrusElementException extends Exception {

    /**
     * Constructs an instance of <code>PapyrusException</code> with the
     * specified detail message, cause, and a partial papyrus structure.
     *
     * @param msg the detail message.
     * @param cause
     * @param partial
     *
     */
    public PapyrusElementException(String msg, Throwable cause, PapyrusElement partial) {
        super(msg, cause);
        this.PARTIAL = partial;
    }

    public PapyrusElement getPartial() {
        return this.PARTIAL;
    }

    final private PapyrusElement PARTIAL;

}
