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
package resaver;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * A case-insensitive version of the String class.
 *
 * IString has value semantics. It's <code>equals(obj)</code> method will return
 * true for any String or IString that contains a matching string.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class IString implements CharSequence, java.io.Serializable, Comparable<IString> {

    /**
     * A re-usable blank <code>IString</code>.
     */
    static public final IString BLANK = new IString();

    /**
     * Creates a new <code>IString</code> with a specified value.
     *
     * @param val The value to store, as a <code>String</code>.
     * @return The new <code>IString</code>.
     */
    static public IString get(String val) {
        //return CACHE.computeIfAbsent(val, v -> new IString(v.intern()));
        return CACHE.computeIfAbsent(val, v -> new IString(v));
        //return new IString(val);
    }

    /**
     * Creates a new <code>IString</code> with a specified value.
     *
     * @param val The value to store, as a <code>String</code>.
     */
    protected IString(String val) {
        this.STRING = Objects.requireNonNull(val);
        this.HASHCODE = this.STRING.toLowerCase().hashCode();
    }

    /**
     * Creates a new <code>IString</code> with a specified value.
     *
     * @param val The value to store, as a <code>String</code>.
     */
    protected IString(CharSequence val) {
        this.STRING = Objects.requireNonNull(val.toString());
        this.HASHCODE = this.STRING.toLowerCase().hashCode();
    }

    /**
     * Copy constructor.
     *
     * @param other The original <code>IString</code>.
     */
    protected IString(IString other) {
        Objects.requireNonNull(other);
        this.STRING = other.STRING;
        this.HASHCODE = other.HASHCODE;
    }

    /**
     * Creates a new blank <code>IString</code>.
     */
    private IString() {
        this("".intern());
    }

    /**
     * @return True if the <code>IString</code> is empty, false otherwise.
     * @see java.lang.String#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return this.STRING.isEmpty();
    }

    /**
     * @return The length of the <code>IString</code>.
     * @see java.lang.String#length()
     */
    @Override
    public int length() {
        return this.STRING.length();
    }

    /**
     * @see java.lang.String#charAt(int)
     */
    @Override
    public char charAt(int index) {
        return this.STRING.charAt(index);
    }

    /**
     * @see java.lang.String#subSequence(int, int)
     */
    @Override
    public IString subSequence(int start, int end) {
        return new IString(this.STRING.substring(start, end));

    }

    /**
     * @see java.lang.String#getBytes()
     * @return An array of bytes representing the <code>IString</code>.
     */
    public byte[] getUTF8() {
        return this.STRING.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the <code>String</code> value of the <code>IString</code>.
     *
     * @return
     */
    @Override
    public String toString() {
        return this.STRING;
    }

    /**
     * Calculates a case-insensitive hashcode for the <code>IString</code>.
     *
     * @see java.lang.String#hashCode()
     */
    @Override
    public int hashCode() {
        return this.HASHCODE;
    }

    /**
     * Tests for case-insensitive value-equality with another
     * <code>IString</code> or a <code>String</code>.
     *
     * @param obj The object to which to compare.
     * @see java.lang.String#equalsIgnoreCase(java.lang.String)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof IString) {
            return this.equals((IString) obj);
        } else if (obj instanceof String) {
            return this.equals((String) obj);
        } else {
            return super.equals(obj);
        }
    }

    /**
     * Tests for case-insensitive value-equality with a <code>String</code>.
     *
     * @param other The <code>String</code> to which to compare.
     * @return 
     * @see java.lang.String#equalsIgnoreCase(java.lang.String)
     */
    public boolean equals(String other) {
        return this.STRING.equalsIgnoreCase(other);
    }

    /**
     * Tests for case-insensitive value-equality with an <code>IString</code>.
     *
     * @param other The <code>IString</code> to which to compare.
     * @return <code>true</code> if the string are a case-insensitive match.
     * @see java.lang.String#equalsIgnoreCase(java.lang.String)
     */
    public boolean equals(IString other) {
        // IStrings end up in hashmaps a lot, so we cache their hashcodes.
        // Calculating the hashcodes is comparitively slow so this pays off.
        // And that provides a very convenient way to short-circuit this
        // equality.
        return this.HASHCODE != other.hashCode() 
                ? false
                : this.STRING.equalsIgnoreCase(other.STRING);
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * @param o
     * @return
     */
    @Override
    public int compareTo(IString o) {
        return compare(this, o);
    }

    /**
     * Performs case-insensitive regular-expression matching on the
     * <code>IString</code>.
     *
     * @param regex The regular expression.
     * @see java.lang.String#matches(java.lang.String)
     * @return True if the <code>IString</code> matches the regex, false
     * otherwise.
     */
    public boolean matches(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(this.STRING).matches();
    }

    /**
     * @see java.lang.String#format(java.lang.String, java.lang.Object...)
     * @param format The format string.
     * @param args The arguments to the format string.
     * @return A formatted <code>IString</code>
     *
     */
    static public IString format(String format, Object... args) {
        return new IString(String.format(format, args));
    }

    
    /**
     * Comparator for <code>IString</code> which ignores case.
     * @param s1 The first string
     * @param s2 The second string
     * @return The order code.
     */
    static public int compare(IString s1, IString s2) {
        return Objects.compare(s1.STRING, s2.STRING, String::compareToIgnoreCase);
    }
    
    final private String STRING;
    final private int HASHCODE;

    /**
     * Stores all <code>IString</code> instances for re-use, because they 
     * do get reused a LOT. This saves a massive amount of memory.
     */
    static private WeakHashMap<String, IString> CACHE = new WeakHashMap<>(60_000);

}
