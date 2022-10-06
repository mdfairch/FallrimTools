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
package resaver.esp;

import java.util.Collection;
import java.util.Optional;
import resaver.IString;

/**
 *
 * @author Mark
 */
public class FieldList extends java.util.LinkedList<Field> {

    /*public boolean add(Field field) {
        if (this.containsKey(field.getCode())) {
            throw new IllegalStateException("OVERWRITING FIELD");
        }
        this.put(field.getCode(), field);
        return true;
    }

    public boolean addAll(Collection<? extends Field> fields) {
        fields.forEach(field -> this.add(field));
        return true;
    }
    
    public boolean addAll(FieldList fields) {
        fields.values().forEach(field -> this.add(field));
        return true;
    }
    
    public Field get(String key) {
        return super.get(IString.get(key));
    }
    
    public Optional<Field> getOpt(String key) {
        Field val = this.get(key);
        return val == null
                ? Optional.empty()
                : Optional.of(val);
    }*/

}
