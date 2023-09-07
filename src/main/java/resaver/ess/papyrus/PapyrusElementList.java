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

import resaver.ListException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author Mark
 * @param <T>
 */
public class PapyrusElementList<T extends HasID> extends java.util.ArrayList<T> implements PapyrusElement {

    PapyrusElementList(ByteBuffer input, PapyrusElementReader<T> reader) throws PapyrusElementException {
        try {
            int count = input.getInt();
            super.ensureCapacity(count);
            
            for (int i = 0; i < count; i++) {
                try {
                    T element = reader.read(input);
                    this.add(element);
                } catch (PapyrusFormatException ex) {
                    throw new ListException(i, count, ex);
                }
            }
        } catch (ListException ex) {
            throw new PapyrusElementException("Failed to read elements.", ex, this);
        }
    }

    PapyrusElementList() {
    }

    @Override
    public int calculateSize() {
        return 4 + this.parallelStream().mapToInt(v -> v.calculateSize()).sum();
    }

    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.size());
        this.forEach(v -> v.write(output));
    }

    public boolean containsKey(EID id) {
        return this.stream().anyMatch(e -> Objects.equals(id, e.getID()));
    }
            
    public T get(EID id) {
        Optional<T> val = this.stream().filter(e -> Objects.equals(id, e.getID())).findFirst();
        return val.orElse(null);
    }
            
    private static final long serialVersionUID = 1L;

    @FunctionalInterface
    interface PapyrusElementReader<T> {
        public T read(ByteBuffer input) throws PapyrusFormatException, PapyrusElementException;
    }

}
