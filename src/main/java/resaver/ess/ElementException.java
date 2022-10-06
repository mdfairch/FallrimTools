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
package resaver.ess;

/**
 *
 * @author Mark
 */
@SuppressWarnings("serial")
public class ElementException extends /*Runtime*/Exception {

    /**
     * Constructs an instance of <code>ElementException</code> with the
     * specified detail message, cause, and a partial <code>Element</code>.
     *
     * @param msg the detail message.
     * @param cause
     * @param partial
     *
     */
    public ElementException(String msg, Throwable cause, Element partial) {
        super(msg, cause);
        this.PARTIAL = partial;
    }

    /**
     * Constructs an instance of <code>ElementException</code> with the
     * specified detail message and a partial <code>Element</code>.
     *
     * @param msg the detail message.
     * @param partial
     *
     */
    public ElementException(String msg, Element partial) {
        super(msg);
        this.PARTIAL = partial;
    }

    public Element getPartial() {
        return this.PARTIAL;
    }

    final private Element PARTIAL;
}
