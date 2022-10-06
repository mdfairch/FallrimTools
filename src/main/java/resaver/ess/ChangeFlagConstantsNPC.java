/*
 * Copyright 2019 Mark.
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
public enum ChangeFlagConstantsNPC implements ChangeFlagConstants {
    CHANGE_FORM_FLAGS(0), 
    CHANGE_ACTOR_BASE_DATA(1),
    UNK2(2),
    CHANGE_ACTOR_BASE_AIDATA(3),
    CHANGE_ACTOR_BASE_SPELLLIST(4),
    CHANGE_ACTOR_BASE_FULLNAME(5),
    CHANGE_ACTOR_BASE_FACTIONS(6),
    UNK7(7),
    UNK8(8),
    CHANGE_NPC_SKILLS(9),
    CHANGE_NPC_CLASS(10),
    CHANGE_NPC_FACE(11),
    CHANGE_DEFAULT_OUTFIT(12),
    CHANGE_SLEEP_OUTFIT(13),
    UNK14(14),
    UNK15(15),
    UNK16(16),
    UNK17(17),
    UNK18(18),
    UNK19(19),
    UNK20(20),
    UNK21(21),
    UNK22(22),
    UNK23(23),
    CHANGE_NPC_GENDER(24),
    CHANGE_NPC_RACE(25),
    UNK26(26),
    UNK27(27),
    UNK28(28),
    UNK29(29),
    UNK30(30),
    UNK31(31);
    
    /**
     * Returns the flag position.
     *
     * @return
     */
    @Override
    public int getPosition() {
        return this.VAL;
    }

    private ChangeFlagConstantsNPC(int n) {
        this.VAL = n;
    }
    
    final private int VAL;
}
