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
import java.util.function.Function;

/**
 * Look at what they need to mimic a fraction of Kotlin's power.
 * 
 * @author Mark Fairchild
 * @param <A>
 * @param <B>
 */
final public class Pair<A, B> {

    /**
     * Creates a pair with a single type for both fields, a specified value
     * in the first field, and <code>null</code> in the second field.
     * @param <A> The type for both fields. 
     * @param a The value for the first field.
     * @return The pair <code>(a,null)</code>.
     */
    static public <A> Pair<A, A> of(A a) {
        return new Pair<>(a, null);
    }

    /**
     * Creates a pair with specified values for both each field.
     * @param <A> The type for the first field.
     * @param <B> The type for the second field. 
     * @param a The value for the first field.
     * @param b The value for the second field.
     * @return The pair <code>(a,b)</code>.
     */
    static public <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    /**
     * Creates a function for generating instances of <code>Pair</code>
     * from source values.
     * @param <T> The type for the source value.
     * @param <A> The type for the first field.
     * @param <B> The type for the second field. 
     * @param f1 The function for generating the first field.
     * @param f2 The function for generating the second field.
     * @return A function <code>F(t)</code> such that 
     * <code>F(t) -> (f1(t), f2(t))</code>.
     */
    static public <A, B, T> Function<T, Pair<A, B>> mapper(Function<? super T, ? extends A> f1, Function<? super T, ? extends B> f2) {
        return t -> of(f1.apply(t), f2.apply(t));
    }
            
    /**
     * Creates a pair with specified values for both each field.
     * @param a The value for the first field.
     * @param b The value for the second field.
     */
    private Pair(A a, B b) {
        this.A = a;
        this.B = b;
    }

    /**
     * Produces a new <code>Pair</code> by applying a function to each field.
     * @param <C> The type of the first field in the result.
     * @param <D> The type of the second field in the result.
     * @param f1 The function for producing the first field in the result.
     * @param f2 The function for producing the second field in the result.
     * @return A new <code>Pair</code> of the form <code>(f1(A), f2(B))</code>.
     */
    public <C, D> Pair<C, D> map(Function<? super A, ? extends C> f1, Function<? super B, ? extends D> f2) {
        return Pair.of(f1.apply(this.A), f2.apply(this.B));
    }

    /**
     * @return A string representation of the <code>Pair</code>.
     */
    @Override
    public String toString() {
        //return "Pair{" + "A=" + A + ", B=" + B + '}';
        return String.format("(%s, %s)", A, B);
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

    /**
     * The first field.
     */
    final public A A;
    
    /**
     * The second field.
     */
    final public B B;
    
}
