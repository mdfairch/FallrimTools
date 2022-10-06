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
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class ChangeFormCollection extends java.util.ArrayList<ChangeForm> {

    public ChangeFormCollection(int expected) {
        super(expected);
    }

    /**
     * Finds the <code>ChangeForm</code> corresponding to a <code>RefID</code>.
     *
     * @param refID The <code>RefID</code>.
     * @return The corresponding <code>ChangeForm</code> or null if it was not
     * found.
     */
    public ChangeForm getChangeForm(RefID refID) {
        return this.stream()
                .filter(cf -> cf.getRefID().equals(refID))
                .findAny()
                .orElse(null);
    }

    /**
     * Finds the <code>ChangeForm</code> corresponding to a <code>RefID</code>.
     *
     * @param refID The <code>RefID</code>.
     * @return The corresponding <code>ChangeForm</code> or null if it was not
     * found.
     */
    public boolean containsKey(RefID refID) {
        return this.stream().anyMatch(cf -> cf.getRefID().equals(refID));
    }

}
