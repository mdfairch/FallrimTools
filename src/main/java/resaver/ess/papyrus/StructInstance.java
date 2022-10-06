/*
 * Copyright 2016 Mark.
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
package resaver.ess.papyrus;

import resaver.ListException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import resaver.ess.AnalyzableElement;
import resaver.ess.ESS;
import resaver.ess.Element;
import resaver.ess.Flags;
import resaver.ess.Linkable;

/**
 *
 * @author Mark Fairchild
 */
public class StructInstance extends GameElement implements SeparateData, HasVariables {

    /**
     * Creates a new <code>Struct</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param structs The <code>StructMap</code> containing the definitions.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     */
    public StructInstance(ByteBuffer input, StructMap structs, PapyrusContext context) throws PapyrusFormatException {
        super(input, structs, context);
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        super.write(output);
    }

    /**
     * @see SeparateData#readData(java.nio.ByteBuffer, resaver.ess.ESS)
     * @param input
     * @param context
     * @throws PapyrusElementException
     * @throws PapyrusFormatException
     */
    @Override
    public void readData(ByteBuffer input, PapyrusContext context) throws PapyrusElementException, PapyrusFormatException {
        this.data = new StructData(input, context);
    }

    /**
     * @see SeparateData#writeData(java.nio.ByteBuffer)
     * @param output
     */
    @Override
    public void writeData(ByteBuffer output) {
        this.data.write(output);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = super.calculateSize();
        sum += this.data == null ? 0 : this.data.calculateSize();
        return sum;
    }

    /**
     * @return The name of the corresponding <code>Struct</code>.
     */
    public TString getStructName() {
        return super.getDefinitionName();
    }

    /**
     * @return The corresponding <code>Struct</code>.
     */
    public Struct getStruct() {
        assert super.getDefinition() instanceof Struct;
        return (Struct) super.getDefinition();
    }

    /**
     * @return A flag indicating if the <code>StructInstance</code> is
     * undefined.
     *
     */
    @Override
    public boolean isUndefined() {
        if (null != this.getStruct()) {
            return this.getStruct().isUndefined();
        }

        return false;
    }

    /**
     * @return The flag field.
     */
    public Flags.Byte getFlag() {
        return null == this.data ? null : this.data.FLAG;
    }

    /**
     * @see HasVariables#getVariables()
     * @return
     */
    @Override
    public List<Variable> getVariables() {
        return this.data == null 
                ? Collections.emptyList() 
                : Collections.unmodifiableList(this.data.VARIABLES);
    }

    /**
     * @see HasVariables#getDescriptors() 
     * @return 
     */
    @Override
    public List<MemberDesc> getDescriptors() {
        return this.getStruct().getMembers();
    }

    /**
     * @see HasVariables#setVariable(int, resaver.ess.papyrus.Variable) 
     * @param index
     * @param newVar 
     */
    @Override
    public void setVariable(int index, Variable newVar) {
        if (this.data == null || this.data.VARIABLES == null) {
            throw new NullPointerException("The variable list is missing.");
        }
        if (index <= 0 || index >= this.data.VARIABLES.size()) {
            throw new IllegalArgumentException("Invalid variable index: " + index);
        }
        
        this.data.VARIABLES.set(index, newVar);
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        if (null == target || null == this.data) {
            return Linkable.makeLink("structinstance", this.getID(), this.toString());

        } else if (target instanceof Variable) {
            int index = this.getVariables().indexOf(target);
            if (index >= 0) {
                return Linkable.makeLink("structinstance", this.getID(), index, this.toString());
            } else {
                return Linkable.makeLink("structinstance", this.getID(), this.toString());
            }

        } else {
            return this.getVariables().stream()
                    .filter(var -> var.hasRef())
                    .filter(var -> var.getReferent() == target)
                    .map(var -> this.getVariables().indexOf(var))
                    .filter(index -> index >= 0)
                    .findFirst()
                    .map(index -> Linkable.makeLink("structinstance", this.getID(), index, this.toString()))
                    .orElse(Linkable.makeLink("structinstance", this.getID(), this.toString()));
        }
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(resaver.Analysis analysis, ESS save) {
        final StringBuilder BUILDER = new StringBuilder();
        if (null != this.getStruct()) {
            BUILDER.append(String.format("<html><h3>STRUCTURE of %s</h3>", this.getStruct().toHTML(this)));
        } else {
            BUILDER.append(String.format("<html><h3>STRUCTURE of %s</h3>", this.getStructName()));
        }

        /*if (null != analysis) {
            SortedSet<String> providers = analysis.SCRIPT_ORIGINS.get(this.getScriptName());
            if (null != providers) {
                String probablyProvider = providers.last();
                BUILDER.append(String.format("<p>This struct probably came from \"%s\".</p>", probablyProvider));

                if (providers.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    providers.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }
            }
        }*/
        BUILDER.append(String.format("<p>ID: %s</p>", this.getID()));

        if (null == this.data) {
            BUILDER.append("<h3>DATA MISSING</h3>");
        } else {
            BUILDER.append(String.format("<p>Flag: %s</p>", this.getFlag()));
        }

        save.getPapyrus().printReferrents(this, BUILDER, "struct");

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    private StructData data;

    /**
     * Describes struct data in a Skyrim savegame.
     *
     * @author Mark Fairchild
     */
    final private class StructData implements PapyrusDataFor<StructInstance> {

        /**
         * Creates a new <code>StructData</code> by reading from a
         * <code>ByteBuffer</code>. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The <code>PapyrusContext</code> info.
         * @throws PapyrusElementException
         */
        public StructData(ByteBuffer input, PapyrusContext context) throws PapyrusElementException {
            Objects.requireNonNull(input);
            Objects.requireNonNull(context);

            this.FLAG = Flags.readByteFlags(input);

            try {
                int count = input.getInt();
                this.VARIABLES = Variable.readList(input, count, context);
            } catch (ListException ex) {
                throw new PapyrusElementException("Couldn't read struct variables.", ex, this);
            }
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            Objects.requireNonNull(output);
            getID().write(output);
            this.FLAG.write(output);
            output.putInt(this.VARIABLES.size());
            this.VARIABLES.forEach(var -> var.write(output));
        }

        /**
         * @see resaver.ess.Element#calculateSize()
         * @return The size of the <code>Element</code> in bytes.
         */
        @Override
        public int calculateSize() {
            int sum = 4;
            sum += this.FLAG.calculateSize();
            sum += getID().calculateSize();
            sum += this.VARIABLES.stream().mapToInt(var -> var.calculateSize()).sum();
            return sum;
        }

        /**
         * @return String representation.
         */
        @Override
        public String toString() {
            return getID().toString() + this.VARIABLES;
        }

        //final private EID ID;
        final private Flags.Byte FLAG;
        final private List<Variable> VARIABLES;

    }
}
