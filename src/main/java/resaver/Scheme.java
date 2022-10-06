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

import java.util.Objects;

/**
 * Describe an IString mapping.
 *
 * This exists solely for convenience, to avoid having to write
 * Map<IString, IString> repeatedly..
 *
 * @author Mark
 */
final public class Scheme extends java.util.HashMap<IString, IString> {

    /**
     * Creates a new empty Scheme.
     */
    public Scheme() {
        super();
    }

    /**
     * Creates a new Scheme containing the contents of an existing sScheme.
     *
     * @param m The existing Scheme whose contents should be copied.
     */
    public Scheme(java.util.Map<IString, IString> m) {
        super(m);
    }

    /**
     * @see java.util.HashMap#clone()
     * @return
     */
    @Override
    public Scheme clone() {
        return (Scheme) super.clone();
    }
    
    /**
     * @see Object#hashCode() 
     * @return 
     */
    @Override
    public int hashCode() {
        return Objects.hash(this);
        //return o.hashCode();
    }

    /**
     * @see Object#equals(java.lang.Object) 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        return Objects.equals(this, obj);        
    }
    
    // This is used to generate an identity hashcode rather than the value
    // hashcode that HashMap normally produces.
    //final private Object o = new Object();
    
}
