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
public enum ChangeFlagConstantsAchr implements ChangeFlagConstants {

    CHANGE_FORM_FLAGS(0),
    CHANGE_REFR_MOVE(1),
    CHANGE_REFR_HAVOK_MOVE(2),
    CHANGE_REFR_CELL_CHANGED(3),
    CHANGE_REFR_SCALE(4),
    CHANGE_REFR_INVENTORY(5),
    CHANGE_REFR_EXTRA_OWNERSHIP(6),
    CHANGE_REFR_BASEOBJECT(7),
    CHANGE_ACTOR_EXTRA_PACKAGE_DATA(9),
    CHANGE_LIFESTATE(10),
    CHANGE_ACTOR_EXTRA_MERCHANT_CONTAINER(11),
    CHANGE_ACTOR_EXTRA_DISMEMBERED_LIMBS(17),
    CHANGE_ACTOR_LEVELED_ACTOR(18),
    CHANGE_ACTOR_DISPOSITION_MODIFIERS(19),
    CHANGE_ACTOR_TEMP_MODIFIERS(20),
    CHANGE_ACTOR_DAMAGE_MODIFIERS(21),
    CHANGE_ACTOR_OVERRIDE_MODIFIERS(22),
    CHANGE_ACTOR_PERMANENT_MODIFIERS(23),
    CHANGE_REFR_PROMOTED(25),
    CHANGE_REFR_EXTRA_ACTIVATING_CHILDREN(26),
    CHANGE_REFR_LEVELED_INVENTORY(27),
    CHANGE_REFR_ANIMATION(28),
    CHANGE_REFR_EXTRA_ENCOUNTER_ZONE(29),
    CHANGE_REFR_EXTRA_CREATED_ONLY(30),
    CHANGE_REFR_EXTRA_GAME_ONLY(31);

    /**
     * Returns the flag position.
     *
     * @return
     */
    @Override
    public int getPosition() {
        return this.VAL;
    }

    private ChangeFlagConstantsAchr(int n) {
        this.VAL = n;
    }

    final private int VAL;
}
