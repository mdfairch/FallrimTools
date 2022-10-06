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

/**
 * Describes anything that can produce a block of HTML containing a link.
 *
 * @author Mark Fairchild
 */
public interface Linkable {

    /**
     * Creates an HTML link representation.
     *
     * @param target A target within the <code>Linkable</code>.
     * @return HTML link representation.
     */
    abstract public String toHTML(Element target);

    /**
     * Makes a link url in a standard way, with a target.
     * @param type
     * @param address
     * @param text
     * @return 
     */
    static public String makeLink(String type, Object address, String text) {
        return String.format("<a href=\"%s://%s\">%s</a>", type, address, text);
    }
    
    /**
     * Makes a link url in a standard way, with a target.
     * @param type
     * @param address
     * @param target
     * @param text
     * @return 
     */
    static public String makeLink(String type, Object address, int target, String text) {
        return String.format("<a href=\"%s://%s[%d]\">%s</a>", type, address, target, text);
    }

    /**
     * Makes a link url in a standard way, with two target2.
     * @param type
     * @param address
     * @param target1
     * @param target2
     * @param text
     * @return 
     */
    static public String makeLink(String type, Object address, int target1, int target2, String text) {
        return String.format("<a href=\"%s://%s[%d][%d]\">%s</a>", type, address, target1, target2, text);
    }
}
