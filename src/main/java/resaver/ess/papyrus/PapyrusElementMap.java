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

/**
 *
 * @author Mark
 * @param <T>
 */
public class PapyrusElementMap<T extends HasID> extends java.util.LinkedHashMap<EID, T> implements PapyrusElement {

    PapyrusElementMap(ByteBuffer input, PapyrusElementReader<T> reader) throws PapyrusElementException {
        try {
            int count = input.getInt();

            for (int i = 0; i < count; i++) {
                try {
                    T element = reader.read(input);
                    this.put(element.getID(), element);
                } catch (PapyrusFormatException ex) {
                    throw new ListException(i, count, ex);
                }
            }
        } catch (ListException ex) {
            throw new PapyrusElementException("Failed to read elements.", ex, this);
        }
    }

    PapyrusElementMap() {
    }

    @Override
    public int calculateSize() {
        return 4 + this.values().parallelStream().mapToInt(v -> v.calculateSize()).sum();
    }

    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.size());
        this.values().forEach(v -> v.write(output));
    }

    private static final long serialVersionUID = 1L;

    @FunctionalInterface
    interface PapyrusElementReader<T> {

        public T read(ByteBuffer input) throws PapyrusFormatException, PapyrusElementException;
    }

}
