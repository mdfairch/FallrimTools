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
 * @param <TypeA>
 * @param <TypeB>
 */
final public class Pair<TypeA, TypeB> {

    static public <A> Pair<A, A> of(A a) {
        return new Pair<>(a, null);
    }

    static public <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    public Pair(TypeA a, TypeB b) {
        this.A = a;
        this.B = b;
    }

    public <C, D> Pair<C, D> map(java.util.function.Function<TypeA, C> f1, java.util.function.Function<TypeB, D> f2) {
        return Pair.of(f1.apply(this.A), f2.apply(this.B));
    }

    @Override
    public String toString() {
        return "Pair{" + "A=" + A + ", B=" + B + '}';
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
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        return Objects.equals(this.A, other.A) && Objects.equals(this.B, other.B);
    }

    final public TypeA A;
    final public TypeB B;
}
