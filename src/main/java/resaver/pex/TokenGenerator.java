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

import resaver.IString;

/**
 * Generates a sequence of identifiers. Every <code>TokenGenerator</code>
 * generates the same sequence.
 * 
 * @author Mark Fairchild
 */
final public class TokenGenerator implements Cloneable {

    /**
     * Creates a new <code>TokenGenerator</code>.
     * 
     */
    public TokenGenerator() {
        this.index = 0;
    }

    /**
     * Creates a copy of the <code>TokenGenerator</code>; it will return
     * the same sequence of identifiers as the original.
     * 
     * @return A copy of the <code>TokenGenerator</code>.
     */
    @Override
    public TokenGenerator clone() {
        TokenGenerator copy = new TokenGenerator();
        copy.index = this.index;
        return copy;
    }

    /**
     * Produces the next identifier in the sequence.
     * @return An identifier string.
     */
    public IString next() {
        return IString.format("%s%03d", PREFIX, this.index++);
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return String.format("TokenGenerator(%s) %d", PREFIX, this.index);
    }
    
    static final private String PREFIX = "QQZQQ";
    private int index;

}
