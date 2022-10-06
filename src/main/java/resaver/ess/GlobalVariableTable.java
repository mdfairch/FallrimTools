/*
 * Copyright 2017 Mark Fairchild.
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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Objects;

/**
 *
 * @author Mark
 */
final public class GlobalVariableTable implements Element, GlobalDataBlock {

    /**
     * Creates a new <code>GlobalVariableTable</code>.
     *
     * @param input The input data.
     * @param context The <code>ESSContext</code> info.
     */
    public GlobalVariableTable(ByteBuffer input, ESS.ESSContext context) {
        this.COUNT = new VSVal(input);
        final int C = this.COUNT.getValue();
        this.VARIABLES = new java.util.ArrayList<>(C);

        for (int i = 0; i < C; i++) {
            GlobalVariable var = new GlobalVariable(input, context);
            this.VARIABLES.add(var);
        }
    }

    /**
     * Creates a new empty <code>GlobalVariableTable</code>.
     */
    public GlobalVariableTable() {
        this.COUNT = new VSVal(0);
        this.VARIABLES = Collections.emptyList();
    }
    
    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        this.COUNT.write(output);
        this.VARIABLES.forEach(var -> var.write(output));
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = this.COUNT.calculateSize();
        sum += this.VARIABLES.parallelStream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.COUNT);
        hash = 17 * hash + Objects.hashCode(this.VARIABLES);
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
        final GlobalVariableTable other = (GlobalVariableTable) obj;
        if (!Objects.equals(this.COUNT, other.COUNT)) {
            return false;
        }
        if (!Objects.equals(this.VARIABLES, other.VARIABLES)) {
            return false;
        }
        return true;
    }

    /**
     * @return The <code>GlobalVariable</code> list.
     */
    public java.util.List<GlobalVariable> getVariables() {
        return java.util.Collections.unmodifiableList(this.VARIABLES);
    }

    final private VSVal COUNT;
    final private java.util.List<GlobalVariable> VARIABLES;

}
