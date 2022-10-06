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
package resaver.ess.papyrus;

import resaver.ListException;
import resaver.ess.AnalyzableElement;
import java.util.Optional;
import java.util.SortedSet;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import resaver.ess.Element;
import resaver.ess.ESS;
import resaver.ess.Linkable;

/**
 * Describes a reference in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class Reference extends GameElement implements SeparateData, HasVariables {

    /**
     * Creates a new <code>Reference</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param scripts The <code>ScriptMap</code> containing the definitions.
     * @param context The <code>PapyrusContext</code> info.
     */
    Reference(ByteBuffer input, ScriptMap scripts, PapyrusContext context) throws PapyrusFormatException {
        super(input, scripts, context);
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
     * @see SeparateData#readData(java.nio.ByteBuffer,
     * resaver.ess.papyrus.PapyrusContext)
     * @param input
     * @param context
     * @throws PapyrusElementException
     * @throws PapyrusFormatException
     */
    @Override
    public void readData(ByteBuffer input, PapyrusContext context) throws PapyrusElementException, PapyrusFormatException {
        this.data = new ReferenceData(input, context);
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
     * @return The <code>ReferenceData</code> for the instance.
     */
    public ReferenceData getData() {
        return this.data;
    }

    /**
     * Sets the data field.
     *
     * @param newData The new value for the data field.
     */
    public void setData(ReferenceData newData) {
        this.data = newData;
    }

    /**
     * @return The name of the corresponding <code>Script</code>.
     */
    public TString getScriptName() {
        return super.getDefinitionName();
    }

    /**
     * @return The corresponding <code>Script</code>.
     */
    public Script getScript() {
        assert super.getDefinition() instanceof Script;
        return (Script) super.getDefinition();
    }

    /**
     * @return A flag indicating if the <code>Reference</code> is undefined.
     *
     */
    @Override
    public boolean isUndefined() {
        if (null != this.getScript()) {
            return this.getScript().isUndefined();
        }

        return !Script.NATIVE_SCRIPTS.contains(this.getScriptName().toIString());
    }

    /**
     * @return The type of the reference.
     */
    public TString getType() {
        return null == this.data ? null : this.data.TYPE;
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
        return this.getScript().getExtendedMembers();
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
        if (null != target) {
            if (target instanceof Variable) {
                int i = this.getVariables().indexOf(target);
                if (i >= 0) {
                    return Linkable.makeLink("reference", this.getID(), i, this.toString());
                }

            } else {
                Optional<Variable> result = this.getVariables().stream()
                        .filter(v -> v.hasRef())
                        .filter(v -> v.getReferent() == target)
                        .findFirst();

                if (result.isPresent()) {
                    int i = this.getVariables().indexOf(result.get());
                    if (i >= 0) {
                        return Linkable.makeLink("reference", this.getID(), i, this.toString());
                    }
                }
            }
        }

        return Linkable.makeLink("reference", this.getID(), this.toString());
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
        if (null != this.getScript()) {
            BUILDER.append(String.format("<html><h3>REFERENCE of %s</h3>", this.getScript().toHTML(this)));
        } else {
            BUILDER.append(String.format("<html><h3>REFERENCE of %s</h3>", this.getScriptName()));
        }

        if (null != analysis) {
            SortedSet<String> providers = analysis.SCRIPT_ORIGINS.get(this.getScriptName().toIString());
            if (null != providers) {
                String probablyProvider = providers.last();
                BUILDER.append(String.format("<p>This script probably came from \"%s\".</p>", probablyProvider));

                if (providers.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    providers.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }
            }
        }

        BUILDER.append(String.format("<p>ID: %s</p>", this.getID()));
        BUILDER.append(String.format("<p>Type2: %s</p>", this.getType()));
        BUILDER.append(String.format("<p>Unknown1: %08x</p>", this.data.UNKNOWN1));
        BUILDER.append(String.format("<p>Unknown2: %08x</p>", this.data.UNKNOWN2));

        Linkable UNKNOWN1 = save.getPapyrus().getContext().broadSpectrumSearch(this.getData().UNKNOWN1);
        if (null != UNKNOWN1) {
            BUILDER.append("<p>Potential match for unknown1 found using general search:<br/>");
            BUILDER.append(UNKNOWN1.toHTML(this));
            BUILDER.append("</p>");
        }

        Linkable UNKNOWN2 = save.getPapyrus().getContext().broadSpectrumSearch(this.getData().UNKNOWN2);
        if (null != UNKNOWN2) {
            BUILDER.append("<p>Potential match for unknown2 found using general search:<br/>");
            BUILDER.append(UNKNOWN2.toHTML(this));
            BUILDER.append("</p>");
        }

        save.getPapyrus().printReferrents(this, BUILDER, "reference");

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    private ReferenceData data;

    final public class ReferenceData implements PapyrusDataFor<Reference> {

        /**
         * Creates a new <code>ReferenceData</code> by reading from a
         * <code>LittleEndianDataOutput</code>. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The <code>PapyrusContext</code> info.
         * @throws PapyrusElementException
         * @throws PapyrusFormatException
         */
        public ReferenceData(ByteBuffer input, PapyrusContext context) throws PapyrusElementException, PapyrusFormatException {
            this.FLAG = input.get();
            this.TYPE = context.readTString(input);
            this.UNKNOWN1 = input.getInt();
            this.UNKNOWN2 = ((this.FLAG & 0x04) != 0 ? input.getInt() : 0);

            try {
                int count = input.getInt();
                this.VARIABLES = Variable.readList(input, count, context);
            } catch (ListException ex) {
                throw new PapyrusElementException("Couldn't read struct variables.", ex, this);
            }
        }

        /**
         * @see resaver.ess.Element#write(ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            getID().write(output);
            output.put(this.FLAG);
            this.TYPE.write(output);
            output.putInt(this.UNKNOWN1);

            if ((this.FLAG & 0x04) != 0) {
                output.putInt(this.UNKNOWN2);
            }

            output.putInt(this.VARIABLES.size());
            this.VARIABLES.forEach(var -> var.write(output));
        }

        /**
         * @see resaver.ess.Element#calculateSize()
         * @return The size of the <code>Element</code> in bytes.
         */
        @Override
        public int calculateSize() {
            int sum = 9;
            sum += getID().calculateSize();
            sum += ((this.FLAG & 0x04) != 0 ? 4 : 0);
            sum += this.TYPE.calculateSize();
            sum += this.VARIABLES.parallelStream().mapToInt(var -> var.calculateSize()).sum();
            return sum;
        }

        final private byte FLAG;
        final private TString TYPE;
        final private int UNKNOWN1;
        final private int UNKNOWN2;
        final private List<Variable> VARIABLES;

    }
}
