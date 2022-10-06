/*
 * Copyright 2018 Mark.
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
package resaver.ess;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Objects;
import resaver.ListException;

/**
 *
 * @author Mark
 */
public class AnimObjects implements GlobalDataBlock {

    /**
     * Creates a new <code>AnimObjects</code> by reading from a
     * <code>LittleEndianInput</code>. No error handling is performed.
     *
     * @param input The input data.
     * @param context The <code>ESSContext</code> info.
     * @throws ElementException
     * 
     */
    public AnimObjects(ByteBuffer input, ESS.ESSContext context) throws ElementException {
        try {
            final int COUNT = input.getInt();
            if (COUNT < 0 || COUNT > 1e6) {
                throw new IllegalArgumentException("AnimObject count was an illegal value: " + COUNT);
            }

            this.ANIMATIONS = new java.util.ArrayList<>(COUNT);

            for (int i = 0; i < COUNT; i++) {
                try {
                AnimObject var = new AnimObject(input, context);
                this.ANIMATIONS.add(var);
                } catch (ElementException ex) {
                    throw new ListException(i, COUNT, ex);
                }
            }
            
        } catch (BufferUnderflowException | ListException ex) {
            throw new ElementException("Error reading AnimObjects", ex, this);
        }
    }

    /**
     * Creates a new empty <code>AnimObjects</code>.
     */
    public AnimObjects() {
        this.ANIMATIONS = Collections.emptyList();
    }
    
    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.ANIMATIONS.size());
        this.ANIMATIONS.forEach(var -> var.write(output));
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 4;
        sum += this.ANIMATIONS.parallelStream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.ANIMATIONS);
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
        final AnimObjects other = (AnimObjects) obj;

        for (int i = 0; i < this.ANIMATIONS.size(); i++) {
            AnimObject ao1 = this.ANIMATIONS.get(i);
            AnimObject ao2 = other.ANIMATIONS.get(i);
            if (!ao1.equals(ao2)) {
                return ao1.equals(ao2);
            }
        }
        return Objects.equals(this.ANIMATIONS, other.ANIMATIONS);
    }

    /**
     * @return The <code>AnimObject</code> list.
     */
    public java.util.List<AnimObject> getAnimations() {
        return java.util.Collections.unmodifiableList(this.ANIMATIONS);
    }

    final private java.util.List<AnimObject> ANIMATIONS;

    /**
     *
     */
    static public class AnimObject extends GeneralElement {

        /**
         * Creates a new <code>AnimObject</code> by reading from a
         * <code>LittleEndianInput</code>. No error handling is performed.
         *
         * @param input The input data.
         * @param context The <code>ESSContext</code>.
         * @throws ElementException
         * 
         */
        public AnimObject(ByteBuffer input, ESS.ESSContext context) throws ElementException {
            try {
                ACTOR = super.readRefID(input, "ACTOR", context);
                ANIM = super.readRefID(input, "ANIMATION", context);
                FLAG = super.readByte(input, "FLAG");
            } catch (ElementException ex) {
                throw new ElementException("Error reading AnimObject", ex, this);
            }
        }
        
        @Override
        public String toString() {
            return new StringBuilder()
                    .append(ACTOR.getName().orElse(ACTOR.toString()))
                    .append(" - ")
                    .append(ANIM.getName().orElse(ANIM.toString()))
                    .append(" (")
                    .append(FLAG)
                    .append(")")
                    .toString();
        }
        
        final private RefID ACTOR;
        final private RefID ANIM;
        final private byte FLAG;
        
    }

}
