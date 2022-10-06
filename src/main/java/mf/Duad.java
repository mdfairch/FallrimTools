/*
 * Copyright 2018 Mark Fairchild.
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
package mf;

import java.util.Objects;

/**
 *
 * @author Mark Fairchild
 * @param <T>
 */
final public class Duad<T> {

    static public <T> Duad<T> make(T a, T b) {
        return new Duad<>(a, b);
    }

    public Duad(T a, T b) {
        this.A = a;
        this.B = b;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append('(')
                .append(A)
                .append(", ")
                .append(B)
                .append(')')
                .toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.A);
        hash = 97 * hash + Objects.hashCode(this.B);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Duad<?> other = (Duad<?>) obj;
        return Objects.equals(this.A, other.A) && Objects.equals(this.B, other.B);
    }

    final public T A, B;
}
