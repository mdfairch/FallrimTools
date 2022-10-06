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
package resaver.pex;

import java.util.Collection;
import java.util.Objects;

/**
 * Stores stats about a script or group of scripts.
 *
 * @author Mark Fairchild
 */
final public class ScriptStats {

    /**
     * Creates a new <code>ModStats</code> with all counters set to zero.
     */
    public ScriptStats() {
        this.COUNTS = java.util.Collections.synchronizedMap(new java.util.EnumMap<>(Key.class));
    }

    /**
     * Returns the number of transient strings that can potentially be saved by
     * restringing.
     *
     * @return The number of local variables and function parameters.
     */
    public int getTransientStrings() {
        return this.getLocalVariables() + this.getParameters();
    }

    /**
     * Returns the number of permanent strings that can potentially be saved by
     * restringing.
     *
     * @return The number of non-conditional object variables.
     */
    public int getPermanentStrings() {
        return this.getObjectVariables() - this.getConditionals();
    }

    /**
     * Returns the value of the object variables counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    public int getObjectVariables() {
        return this.COUNTS.getOrDefault(Key.OBJECT_VARIABLE, 0);
    }

    /**
     * Returns the value of the autovars counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    public int getAutovars() {
        return this.COUNTS.getOrDefault(Key.AUTOVAR_VARIABLE, 0);
    }

    /**
     * Returns the value of the conditional variables counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    public int getConditionals() {
        return this.COUNTS.getOrDefault(Key.CONDITIONAL_VARIABLE, 0);
    }

    /**
     * Returns the value of the local variables counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    public int getLocalVariables() {
        return this.COUNTS.getOrDefault(Key.LOCAL_VARIABLE, 0);
    }

    /**
     * Returns the value of the parameters counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    public int getParameters() {
        return this.COUNTS.getOrDefault(Key.PARAMETERS, 0);
    }

    /**
     * Returns the value of the docstrings counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    public int getDocStrings() {
        return this.COUNTS.getOrDefault(Key.DOCSTRING, 0);
    }

    /**
     * Modifies the number of object variables.
     *
     * @param delta The change to the stat.
     */
    public void modObjectVariables(int delta) {
        int val = this.COUNTS.getOrDefault(Key.OBJECT_VARIABLE, 0);
        val += delta;
        this.COUNTS.put(Key.OBJECT_VARIABLE, val);
    }

    /**
     * Modifies the number of autovars.
     *
     * @param delta The change to the stat.
     */
    public void modAutoVariables(int delta) {
        int val = this.COUNTS.getOrDefault(Key.AUTOVAR_VARIABLE, 0);
        val += delta;
        this.COUNTS.put(Key.AUTOVAR_VARIABLE, val);
    }

    /**
     * Modifies the number of conditional autovars.
     *
     * @param delta The change to the stat.
     */
    public void modConditionals(int delta) {
        int val = this.COUNTS.getOrDefault(Key.CONDITIONAL_VARIABLE, 0);
        val += delta;
        this.COUNTS.put(Key.CONDITIONAL_VARIABLE, val);
    }

    /**
     * Modifies the number of local variables.
     *
     * @param delta The change to the stat.
     */
    public void modLocalVariables(int delta) {
        int val = this.COUNTS.getOrDefault(Key.LOCAL_VARIABLE, 0);
        val += delta;
        this.COUNTS.put(Key.LOCAL_VARIABLE, val);
    }

    /**
     * Modifies the number of parameters.
     *
     * @param delta The change to the stat.
     */
    public void modParameters(int delta) {
        int val = this.COUNTS.getOrDefault(Key.PARAMETERS, 0);
        val += delta;
        this.COUNTS.put(Key.PARAMETERS, val);
    }

    /**
     * Modifies the number of docstrings.
     *
     * @param delta The change to the stat.
     */
    public void modDocStrings(int delta) {
        int val = this.COUNTS.getOrDefault(Key.DOCSTRING, 0);
        val += delta;
        this.COUNTS.put(Key.DOCSTRING, val);
    }

    /**
     * Clears the stats.
     */
    public void clear() {
        this.COUNTS.clear();
    }

    /**
     * Adds the counts from one <code>ScriptStats</code> to another.
     *
     * @param other The counts to add.
     */
    public void add(ScriptStats other) {
        this.modAutoVariables(other.getAutovars());
        this.modConditionals(other.getConditionals());
        this.modDocStrings(other.getDocStrings());
        this.modLocalVariables(other.getLocalVariables());
        this.modObjectVariables(other.getObjectVariables());
        this.modParameters(other.getParameters());
    }

    /**
     *
     * @return
     */
    /*@Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();
        
        BUF.append(String.format("Processed %d scripts.\n", this.getScripts()));
        BUF.append(String.format("Renamed %d object variables.\n", this.getObjectVariables() - this.getConditionals()));
        BUF.append(String.format("%d of them were property autovariables.\n", this.getAutovars() - this.getConditionals()));
        BUF.append(String.format("Skipped %d conditional autovariables.\n", this.getConditionals()));
        BUF.append(String.format("Processed %d functions.\n", this.getFunctions()));
        BUF.append(String.format("Renamed %d function local variables.\n", this.getLocalVariables()));
        BUF.append(String.format("Renamed %d function parameters.\n", this.getParameters()));
        BUF.append(String.format("Stripped %d docstrings.\n", this.getDocStrings()));
        
        return BUF.toString();
    }*/
    final private java.util.Map<Key, Integer> COUNTS;

    static private enum Key {
        OBJECT_VARIABLE,
        AUTOVAR_VARIABLE,
        CONDITIONAL_VARIABLE,
        LOCAL_VARIABLE,
        PARAMETERS,
        DOCSTRING;
    }

    /**
     * Combines a <code>Collection</code> of <code>ScripStat> objects.
     *
     * @param statsGroup A <code>Collection</code> of stats.
     * @return A combined <code>ScriptStats</code> object containing the sum of
     * all the individual stats.
     */
    static public ScriptStats combine(Collection<ScriptStats> statsGroup) {
        Objects.requireNonNull(statsGroup);
        final ScriptStats COMBINED = new ScriptStats();

        for (ScriptStats stats : statsGroup) {
            COMBINED.modAutoVariables(stats.getAutovars());
            COMBINED.modConditionals(stats.getConditionals());
            COMBINED.modDocStrings(stats.getDocStrings());
            COMBINED.modLocalVariables(stats.getLocalVariables());
            COMBINED.modObjectVariables(stats.getObjectVariables());
            COMBINED.modParameters(stats.getParameters());
        }

        return COMBINED;
    }
}
