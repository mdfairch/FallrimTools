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
public enum ChangeFlagConstantsQust implements ChangeFlagConstants {

    CHANGE_FORM_FLAGS(0),
    CHANGE_QUEST_FLAGS(1),
    CHANGE_QUEST_SCRIPT_DELAY(2),
    CHANGE_QUEST_ALREADY_RUN(26),
    CHANGE_QUEST_INSTANCES(27),
    CHANGE_QUEST_RUNDATA(28),
    CHANGE_QUEST_OBJECTIVES(29),
    CHANGE_QUEST_SCRIPT(30),
    CHANGE_QUEST_STAGES(31);

    /**
     * Returns the flag position.
     *
     * @return
     */
    @Override
    public int getPosition() {
        return this.VAL;
    }

    private ChangeFlagConstantsQust(int n) {
        this.VAL = n;
    }

    final private int VAL;
}
