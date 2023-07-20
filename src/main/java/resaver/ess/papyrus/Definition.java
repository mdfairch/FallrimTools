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
package resaver.ess.papyrus;

import java.util.List;
import java.util.Optional;
import resaver.ess.AnalyzableElement;
import resaver.ess.Linkable;

/**
 *
 * @author Mark Fairchild
 */
abstract public class Definition implements PapyrusElement, AnalyzableElement, Linkable {

    public Definition() {
        this.instanceCount = 0;
    }

    /**
     * @return The name of the papyrus element.
     */
    abstract public TString getName();

    /**
     * @return A flag indicating if the <code>Definition</code> is undefined.
     *
     */
    public boolean isUndefined() {
        return false;
    }

    /**
     * Increments the instance count.
     */
    void incrementInstanceCount() {
        this.instanceCount++;
    }

    /**
     * @return The instance count.
     */
    protected int getInstanceCount() {
        return this.instanceCount;
    }

    /**
     * @return The list of member descriptions.
     */
    abstract public List<MemberDesc> getMembers();

    private int instanceCount;
}
