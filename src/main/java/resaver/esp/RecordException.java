/*
 * Copyright 2022 Mark Fairchild.
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
package resaver.esp;

/**
 *
 * @author Mark
 */
public class RecordException extends ContextException {

    public RecordException(Throwable cause, RecordCode code, int formID, String context) {
        super(String.format("Error reading record: %s %s", code, formID), cause);
        this.CODE = code;
        this.FORMID = formID;
        this.CONTEXT = context;
    }

    public String getContext() {
        return CONTEXT;
    }

    final public RecordCode CODE;
    final public int FORMID;
    final public String CONTEXT;

}
