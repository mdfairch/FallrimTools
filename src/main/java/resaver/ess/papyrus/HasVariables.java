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

import java.util.List;

/**
 *
 * @author Mark Fairchild
 */
public interface HasVariables extends PapyrusElement {

    /**
     * @return The <code>Variable</code> <code>List</code> stored by the
     * <code>PapyrusElement</code>.
     */
    public List<Variable> getVariables();

    /**
     * @return The <code>MemberDesc</code> <code>List</code> for the
     * <code>PapyrusElement</code>.
     */
    public List<MemberDesc> getDescriptors();

    /**
     * Changes a <code>Variable</code>.
     *
     * @param index The index of the <code>Variable</code> to replace.
     * @param newVar The new <code>Variable</code>.
     */
    public void setVariable(int index, Variable newVar);

    /**
     * @return A new <code>List</code> made by pairing each
     * <code>Variable</code> with its corresponding <code>MemberDesc</code>.
     */
    /*default public java.util.List<Member> getMembers() {
        final List<MemberDesc> DESCRIPTORS = this.getDescriptors();
        final List<Variable> VARIABLES = this.getVariables();
        final List<Member> MEMBERS = new ArrayList<>();

        final Iterator<MemberDesc> ITER_DESC = DESCRIPTORS == null
                ? Collections.emptyListIterator()
                : DESCRIPTORS.iterator();

        final Iterator<Variable> ITER_VARS = VARIABLES == null
                ? Collections.emptyListIterator()
                : VARIABLES.iterator();

        while (ITER_DESC.hasNext() && ITER_VARS.hasNext()) {
            final MemberDesc DESCRIPTOR = ITER_DESC.next();
            final Variable VARIABLE = ITER_VARS.next();
            MEMBERS.add(new Member(DESCRIPTOR, VARIABLE));
        }

        while (ITER_DESC.hasNext()) {
            final MemberDesc DESCRIPTOR = ITER_DESC.next();
            MEMBERS.add(new Member(DESCRIPTOR, null));
        }

        while (ITER_VARS.hasNext()) {
            final Variable VARIABLE = ITER_VARS.next();
            MEMBERS.add(new Member(null, VARIABLE));
        }

        return MEMBERS;
    }*/

}
