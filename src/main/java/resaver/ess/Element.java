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
import java.util.logging.Logger;
import resaver.ess.papyrus.ActiveScript;

/**
 * Describes a component of a Skyrim savegame.
 * 
 * @author Mark Fairchild
 */
public interface Element {

    /**
     * Write the <code>Element</code> to an output stream.
     * @param output The output stream.
     */
    public void write(ByteBuffer output);

    /**
     * @return The size of the <code>Element</code> in bytes.
     */
    public int calculateSize();
    
    /**
     * Just a shortcut for adding breakpoints.
     * @param condition The condition for which to break.
     */
    static public void conditionalBreakpoint(boolean condition) {
        if (condition) {
            Logger.getLogger(Element.class.getCanonicalName());
        }        
    }

    /**
     * Just a shortcut for adding breakpoints.
     * @param condition The condition for which to break.
     * @param action The thing to do.
     */
    static public void conditionalAction(boolean condition, Runnable action) {
        if (condition) {
            action.run();
            Logger.getLogger(Element.class.getCanonicalName());
        }        
    }
}
