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
package resaver.ess.papyrus;

/**
 * Stores a variable as well as it's descriptor.
 *
 * @author Mark Fairchild
 */
final public class Member {

    /**
     * Creates a new <code>Member</code>.
     *
     * @param desc The member's descriptor.
     * @param var The member's variable.
     */
    public Member(MemberDesc desc, Variable var) {
        this.DESC = desc;
        this.VAR = var;
    }

    /**
     * @return A flag indicating whether this member stores a player reference.
     */
    //public boolean isPlayerRef() {
    //    return this.VAR instanceof Variable.Ref
    //            && PLAYER_REFS.contains(this.DESC.getName().toIString());
    //}
    /**
     *
     * @return A flag indicating whether this member stores invalid data.
     */
    public boolean isInvalid() {
        return this.DESC == null || this.VAR == null;
    }

    final public MemberDesc DESC;
    final public Variable VAR;

    //static final private IString[] _PLAYER_REFS = new IString[]{
    //IString.get("player"),
    //IString.get("playerref"),
    //IString.get("::player_var"),
    //IString.get("::playerref_var"),};
    //static final public Collection<IString> PLAYER_REFS = Arrays.asList(_PLAYER_REFS);
}
