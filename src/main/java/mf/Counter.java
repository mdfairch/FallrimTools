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
package mf;

/**
 * A class for counting.
 *
 * @author Mark Fairchild
 */
final public class Counter {

    /**
     * Create a new <code>Counter</code> with the specified limit.
     *
     * @param newLimit The limit for the counter. Must be non-negative.
     */
    public Counter(int newLimit) {
        if (newLimit < 0) {
            throw new IllegalArgumentException("Limit must be non-negative: " + newLimit);
        }

        this.reset(newLimit);       
        this.percentListeners = null;
        this.countListeners = null;
    }

    /**
     * Increases the count by one. Any listeners will be notified.
     */
    public void click() {
        this.value++;

        if (this.percentListeners != null && !this.percentListeners.isEmpty()) {
            double percent = (double) this.value / (double) this.limit;
            this.percentListeners.forEach(l -> l.accept(percent));
        }
        if (this.countListeners != null && !this.countListeners.isEmpty()) {
            this.countListeners.forEach(l -> l.accept(this.value));
        }
    }

    /**
     * Increases the count by a non-negative value. Any listeners will be
     * notified.
     *
     * @param num The number of clicks. Must be non-negative.
     */
    public void click(int num) {
        if (num < 0) {
            throw new IllegalArgumentException("Clicks must be non-negative: " + num);
        }

        this.value += num;

        if (this.percentListeners != null && !this.percentListeners.isEmpty()) {
            double percent = this.getPercentage();
            this.percentListeners.forEach(l -> l.accept(percent));
        }
        if (this.countListeners != null && !this.countListeners.isEmpty()) {
            this.countListeners.forEach(l -> l.accept(this.value));
        }
    }

    /**
     * Decreases the count by one. The listeners will not be notified.
     *
     * @throws IllegalStateException Thrown if the count would become negative.
     */
    public void unclick() {
        if (this.value == 0) {
            throw new IllegalStateException("Counter can't be negative.");
        }

        this.value--;
    }

    /**
     * Decreases the count by a non-negative value. The listeners will not be
     * notified.
     *
     * @param num The number of clicks. Must be non-negative.
     * @throws IllegalStateException Thrown if the count would become negative.
     */
    public void unclick(int num) {
        if (num < 0) {
            throw new IllegalArgumentException("Clicks must be non-negative: " + num);
        } else if (this.value < num) {
            throw new IllegalStateException("Counter can't be negative.");
        }

        this.value -= num;
    }

    /**
     * Sets a new limit and resets the count to zero.
     *
     * @param newLimit The new limit. Must be non-negative.
     */
    public void reset(int newLimit) {
        if (newLimit < 0) {
            throw new IllegalArgumentException("Limit must be non-negative: " + newLimit);
        }
        
        this.value = 0;
        this.limit = newLimit;        
        int width = Integer.toString(this.limit).length();
        this.format = new StringBuilder().append('%').append(width).append("d/%").append(width).append('d').toString();
    }

    /**
     * Equivalent to calling click() and toString().
     *
     * @return The string representation.
     */
    public String eval() {
        this.click();
        return this.toString();
    }

    /**
     * @return The current count.
     */
    public int getCounter() {
        return this.value;
    }

    /**
     * @return The current count as a percentage of the limit.
     */
    public double getPercentage() {
        return (double) this.value / (double) this.limit;
    }

    /**
     * @return A string representation of the form <code>val/limit</code>.
     */
    @Override
    public String toString() {
        return String.format(this.format, this.value, this.limit);
    }

    /**
     * Adds a listener that will receive the current count whenever it
     * increases.
     *
     * @param listener The listener.
     */
    public void addCountListener(java.util.function.IntConsumer listener) {
        if (this.countListeners == null) {
            this.countListeners = new java.util.LinkedList<>();
        }
        this.countListeners.add(listener);
    }

    /**
     * Adds a listener that will receive the current count as a
     * percentage whenever it increases.
     *
     * @param listener The listener.
     */
    public void addPercentListener(java.util.function.DoubleConsumer listener) {
        if (this.percentListeners == null) {
            this.percentListeners = new java.util.LinkedList<>();
        }
        this.percentListeners.add(listener);
    }

    /**
     * Removes a listener.
     * @param listener The listener.
     */
    public void removeCountListener(java.util.function.IntConsumer listener) {
        if (this.countListeners != null) {
            this.countListeners.remove(listener);
        }
    }

    /**
     * Removes a listener.
     * @param listener The listener.
     */
    public void removePercentListener(java.util.function.DoubleConsumer listener) {
        if (this.percentListeners != null) {
            this.percentListeners.remove(listener);
        }
    }

    private int value;
    private int limit;
    private String format;
    private java.util.List<java.util.function.DoubleConsumer> percentListeners;
    private java.util.List<java.util.function.IntConsumer> countListeners;

}
