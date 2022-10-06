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

/**
 * Keeps stats on what is done during remapping.
 *
 * @author Mark Fairchild
 */
final public class RemappingStats {

    /**
     * Creates a new <code>RemappingStats</code> with all counters set to zero.
     */
    public RemappingStats() {
        this.COUNTS = java.util.Collections.synchronizedMap(new java.util.EnumMap<>(Key.class));
    }

    /**
     * Returns the value of the script counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    public int getScripts() {
        return this.COUNTS.getOrDefault(Key.SCRIPT, 0);
    }

    /**
     * Returns the value of the object variables counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    public int getObjectVariables() {
        return this.COUNTS.getOrDefault(Key.OBJECT_VARIABLE, 0);
    }

    /**
     * Returns the value of the autovars counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    public int getAutovars() {
        return this.COUNTS.getOrDefault(Key.AUTOVAR_VARIABLE, 0);
    }

    /**
     * Returns the value of the conditional variables counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    public int getConditionals() {
        return this.COUNTS.getOrDefault(Key.CONDITIONAL_VARIABLE, 0);
    }

    /**
     * Returns the value of the functions counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    public int getFunctions() {
        return this.COUNTS.getOrDefault(Key.FUNCTIONS, 0);
    }

    /**
     * Returns the value of the local variables counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    public int getLocalVariables() {
        return this.COUNTS.getOrDefault(Key.LOCAL_VARIABLE, 0);
    }

    /**
     * Returns the value of the parameters counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    public int getParameters() {
        return this.COUNTS.getOrDefault(Key.PARAMETERS, 0);
    }

    /**
     * Returns the value of the docstrings counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    public int getDocStrings() {
        return this.COUNTS.getOrDefault(Key.DOCSTRING, 0);
    }

    /**
     * Increments the number of scripts.
     */
    public void incScripts() {
        int val = this.COUNTS.getOrDefault(Key.SCRIPT, 0);
        val++;
        this.COUNTS.put(Key.SCRIPT, val);
    }

    /**
     * Increments the number of object variables.
     */
    public void incObjectVariables() {
        int val = this.COUNTS.getOrDefault(Key.OBJECT_VARIABLE, 0);
        val++;
        this.COUNTS.put(Key.OBJECT_VARIABLE, val);
    }

    /**
     * Increments the number of autovars.
     */
    public void incAutoVariables() {
        int val = this.COUNTS.getOrDefault(Key.AUTOVAR_VARIABLE, 0);
        val++;
        this.COUNTS.put(Key.AUTOVAR_VARIABLE, val);
    }

    /**
     * Increments the number of conditional autovars.
     */
    public void incConditionals() {
        int val = this.COUNTS.getOrDefault(Key.CONDITIONAL_VARIABLE, 0);
        val++;
        this.COUNTS.put(Key.CONDITIONAL_VARIABLE, val);
    }

    /**
     * Increments the number of functions.
     */
    public void incFunctions() {
        int val = this.COUNTS.getOrDefault(Key.FUNCTIONS, 0);
        val++;
        this.COUNTS.put(Key.FUNCTIONS, val);
    }

    /**
     * Increments the number of local variables.
     */
    public void incLocalVariables() {
        int val = this.COUNTS.getOrDefault(Key.LOCAL_VARIABLE, 0);
        val++;
        this.COUNTS.put(Key.LOCAL_VARIABLE, val);
    }

    /**
     * Increments the number of parameters.
     */
    public void incParameters() {
        int val = this.COUNTS.getOrDefault(Key.PARAMETERS, 0);
        val++;
        this.COUNTS.put(Key.PARAMETERS, val);
    }

    /**
     * Increments the number of docstrings.
     */
    public void incDocStrings() {
        int val = this.COUNTS.getOrDefault(Key.DOCSTRING, 0);
        val++;
        this.COUNTS.put(Key.DOCSTRING, val);
    }

    /**
     * 
     * @return 
     */
    @Override
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
    }
    
    final private java.util.Map<Key, Integer> COUNTS;

    static private enum Key {
        SCRIPT,
        OBJECT_VARIABLE,
        AUTOVAR_VARIABLE,
        CONDITIONAL_VARIABLE,
        FUNCTIONS,
        LOCAL_VARIABLE,
        PARAMETERS,
        DOCSTRING;
    }
}
