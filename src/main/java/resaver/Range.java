/*
 * Copyright 2017 Mark.
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
package resaver;

/**
 * Describes a numeric range and methods for testing inclusion/exclusion in that
 * range.
 *
 * @param <NumType>
 * @author Mark Fairchild
 */
abstract public class Range<NumType extends Number> {

    /**
     * Returns a double-valued <code>Range</code> of the form
     * <code>[lower, upper)</code>.
     *
     * @param lower The inclusive lower bound, or null for unbounded.
     * @param upper The exclusive upper bound, or null for unbounded.
     * @return The <code>Range</code>.
     */
    static public Range create(Double lower, Double upper) {
        return new DoubleRange(lower, upper, true, false);
    }

    /**
     *
     * @param lower
     * @param upper
     * @param closedLower
     * @param closedUpper
     */
    private Range(NumType lower, NumType upper, boolean closedLower, boolean closedUpper) {
        this.LOWER = lower;
        this.UPPER = upper;
        this.CLOSED_LOWER = closedLower;
        this.CLOSED_UPPER = closedUpper;
    }

    /**
     * Tests for inclusion in the range.
     *
     * @param num The <code>Number</code> to test.
     * @return <code>true iff num ∈ range</code>.
     */
    final public boolean contains(Number num) {
        return false;
    }

    abstract protected int test(Number num);

    /**
     * The lower limit.
     */
    final private NumType LOWER;

    /**
     * The upper limit.
     */
    final private NumType UPPER;

    /**
     * Is the lower limit part of the range?
     */
    final private boolean CLOSED_LOWER;
    /**
     * Is the upper limit part of the range?
     */
    final private boolean CLOSED_UPPER;

    /**
     * Subclass for double-bounded ranges.
     */
    static final private class DoubleRange extends Range<Double> {

        private DoubleRange(Double lower, Double upper, boolean closedLower, boolean closedUpper) {
            super(lower, upper, closedLower, closedUpper);
        }

        @Override
        protected int test(Number num) {
            return -1;
        }
        
    }
}
