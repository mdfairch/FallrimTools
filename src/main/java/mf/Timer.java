/*
 * Copyright 2016 Mark Fairchild.
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

import java.util.Map;
import java.util.Objects;

/**
 * A simple stopwatch class.
 *
 * @author Mark Fairchild
 */
final public class Timer {

    /**
     * Creates a <code>Timer</code>, starts it, and then returns it. For convenience.
     * @param name The name of the <code>Timer</code>.
     * @return The newly created <code>Timer</code>.
     */
    static public Timer startNew(String name) {
        Timer t = new Timer(name);
        t.start();
        return t;
    }
    
    /**
     * Creates a new <code>Timer</code> with the specified name.
     *
     * @param name The name of the <code>Timer</code>.
     *
     */
    public Timer(String name) {
        if (null == name || name.isEmpty()) {
            this.NAME = null;
        } else if (name.length() > 100) {
            this.NAME = name.substring(0, 100).trim();
        } else {
            this.NAME = name.trim();
        }

        this.reset();
    }

    /**
     * Resets the <code>Timer</code>. If the <code>Timer</code> is running, it
     * will be stopped.
     */
    public void reset() {
        this.totalElapsed_ = 0;
        this.initialTime_ = 0;
        this.running_ = false;
    }

    /**
     * Starts the <code>Timer</code>. If the <code>Timer</code> is already
     * running, this method has no effect.
     */
    public void start() {
        if (this.running_) {
            return;
        }

        this.initialTime_ = System.nanoTime();
        this.running_ = true;
    }

    /**
     * This method is equivalent to:      <code>
     * Timer.reset();
     * Timer.start();
     * </code>
     *
     */
    public void restart() {
        this.reset();
        this.start();
    }

    /**
     * Stops the <code>Timer</code>. If the <code>Timer</code> isn't running,
     * this method has no effect.
     */
    public void stop() {
        if (!this.running_) {
            return;
        }

        long finalTime = System.nanoTime();
        this.running_ = false;
        this.totalElapsed_ += (finalTime - this.initialTime_);
    }

    /**
     * Checks if the <code>Timer</code> is running.
     *
     * @return A flag indicating the the <code>Timer</code> is running.
     */
    public boolean isRunning() {
        return this.running_;
    }

    /**
     * Returns the amount of time that has elapsed while the <code>Timer</code>
     * was running, in nanoseconds.
     *
     * @return The total amount of time elapsed between calls to
     * <code>start()</code> and <code>stop()</code>.
     */
    public long getElapsed() {
        if (this.running_) {
            long finalTime = System.nanoTime();
            return this.totalElapsed_ + (finalTime - this.initialTime_);
        } else {
            return this.totalElapsed_;
        }
    }

    /**
     * Returns the name of the <code>Timer</code>.
     *
     * @return The name of the <code>Timer</code>.
     */
    public String getName() {
        return this.NAME;
    }

    /**
     * Stores a record of the current elapsed time. If there is already a record
     * with the specified name, the new value will be added to it.
     *
     * @param recordName The record name under which to make the record.
     */
    public void record(String recordName) {
        Objects.requireNonNull(recordName);
        long elapsed = this.getElapsed();

        if (null == this.records) {
            this.records = new java.util.LinkedHashMap<>();
        }

        this.records.merge(recordName, elapsed, Long::sum);
    }

    /**
     * Stores a record of the current elapsed time and resets the timer.
     *
     * @see Timer#record(java.lang.String)
     * @see Timer#reset()
     * @param recordName The record name under which to make the record.
     */
    public void recordRestart(String recordName) {
        this.record(recordName);
        this.restart();
    }

    /**
     * Returns the map of recorded times, or null if no records have been
     * recorded.
     *
     * @return A map of record name to elapsed times or null if no records have
     * been recorded.
     *
     */
    public Map<String, Long> getRecords() {
        if (null == this.records) {
            return null;
        } else {
            return java.util.Collections.unmodifiableMap(this.records);
        }
    }

    /**
     * Returns the elapsed time as a string with a unit.
     *
     * @return A string of the form "<ELAPSED TIME> <UNIT>".
     *
     */
    public String getFormattedTime() {
        long elapsed = this.getElapsed();

        if (elapsed < MICROSECOND) {
            return String.format("%d ns", elapsed);
        } else if (elapsed < MILLISECOND) {
            float microseconds = elapsed / MICROSECOND;
            return String.format("%1.1f us", microseconds);
        } else if (elapsed < SECOND) {
            float milliseconds = elapsed / MILLISECOND;
            return String.format("%1.1f ms", milliseconds);
        } else if (elapsed < MINUTE) {
            float seconds = elapsed / SECOND;
            return String.format("%1.1f s", seconds);
        } else {
            float seconds = elapsed / MINUTE;
            return String.format("%1.1f m", seconds);
        }
    }

    /**
     * Returns a pretty-print representation of the <code>Timer</code>.
     *
     * @return A string of the form "Timer <NAME>: <ELAPSED TIME> ns (running)".
     */
    @Override
    public String toString() {
        final StringBuilder SB = new StringBuilder();
        SB.append("Timer ");
        SB.append(this.getName());
        SB.append(": ");

        long elapsed = this.getElapsed();
        if (elapsed < MICROSECOND) {
            SB.append(String.format("%d ns", elapsed));
        } else if (elapsed < MILLISECOND) {
            float microseconds = elapsed / MICROSECOND;
            SB.append(String.format("%.2f us", microseconds));
        } else if (elapsed < SECOND) {
            float milliseconds = elapsed / MILLISECOND;
            SB.append(String.format("%.2f ms", milliseconds));
        } else if (elapsed < MINUTE) {
            float seconds = elapsed / SECOND;
            SB.append(String.format("%.2f s", seconds));
        } else {
            float seconds = elapsed / MINUTE;
            SB.append(String.format("%.2f m", seconds));
        }

        if (this.running_) {
            SB.append(" (running)");
        }

        return SB.toString();
    }

    final private String NAME;
    private long totalElapsed_;
    private long initialTime_;
    private boolean running_;
    private java.util.Map<String, Long> records;

    static final private float MICROSECOND = 1.0e3f;
    static final private float MILLISECOND = 1.0e6f;
    static final private float SECOND = 1.0e9f;
    static final private float MINUTE = 6.0e10f;
}
