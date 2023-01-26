/*
 * Copyright 2023 Mark Fairchild.
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
 * Interface for classes that store parsing data from plugins.
 * @author Mark Fairchild
 */
public interface PluginData {

    /**
     * Adds an entry for a record.
     *
     * @param formID The formId of the record.
     * @param code The record code.
     * @param fields The record's fields, which will be sampled for suitable
     * names.
     */
    default public void addRecord(int formID, RecordCode code, FieldList fields) {
        
    }

    /**
     * Add an entry for a VMAD field.
     * @param script 
     */
    default public void addScriptData(Script script) {
        
    }
    
}
