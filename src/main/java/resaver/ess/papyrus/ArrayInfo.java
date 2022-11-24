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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import resaver.Analysis;
import resaver.ess.ESS;
import resaver.ess.Element;
import resaver.ess.Linkable;

/**
 * Describes an array in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class ArrayInfo implements AnalyzableElement, Linkable, HasID, SeparateData, HasVariables {

    /**
     * Creates a new <code>ArrayInfo</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     */
    public ArrayInfo(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        this.ID = context.readEID(input);

        Type t = Type.read(input);
        if (!VALID_TYPES.contains(t)) {
            throw new PapyrusFormatException("Invalid ArrayInfo type: " + t);
        }
        this.TYPE = t;
        this.REFTYPE = this.TYPE.isRefType() ? context.readTString(input) : null;
        this.LENGTH = input.getInt();
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        this.ID.write(output);
        this.TYPE.write(output);

        if (null != this.REFTYPE) {
            this.REFTYPE.write(output);
        }

        output.putInt(this.LENGTH);
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
        this.data = new ArrayData(input, context);
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
        int sum = 5;
        sum += this.ID.calculateSize();
        if (null != this.REFTYPE) {
            sum += this.REFTYPE.calculateSize();
        }

        sum += this.data == null ? 0 : this.data.calculateSize();
        return sum;
    }

    /**
     * @return The ID of the papyrus element.
     */
    @Override
    public EID getID() {
        return this.ID;
    }

    /**
     * @return The type of the array.
     */
    public Type getType() {
        return this.TYPE;
    }

    /**
     * @return The reference type of the array.
     */
    public TString getRefType() {
        return this.REFTYPE;
    }

    /**
     * @return the length of the array.
     */
    public int getLength() {
        return this.LENGTH;
    }

    /**
     * @return Short string representation.
     */
    public String toValueString() {
        if (this.TYPE.isRefType()) {
            return this.REFTYPE + "[" + this.LENGTH + "]";
        } else if (this.TYPE == Type.NULL && !this.getVariables().isEmpty()) {
            Type t = this.getVariables().get(0).getType();
            return t + "[" + this.LENGTH + "]";
        } else {
            return this.TYPE + "[" + this.LENGTH + "]";
        }
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        if (null != target && null != this.data) {
            Optional<Variable> result = this.getVariables().stream()
                    .filter(v -> v.hasRef())
                    .filter(v -> v.getReferent() == target)
                    .findFirst();

            if (result.isPresent()) {
                int i = this.getVariables().indexOf(result.get());
                if (i >= 0) {
                    return Linkable.makeLink("array", this.ID, i, this.toString());
                }
            }
        }

        return Linkable.makeLink("array", this.ID, this.toString());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return this.toValueString() + " " + this.ID;
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

        BUILDER.append("<html><h3>ARRAY</h3>");

        List<DefinedElement> HOLDERS = save.getPapyrus().getContext().findReferees(this);

        if (null != analysis) {
            HOLDERS.forEach(owner -> {
                if (owner instanceof ScriptInstance) {
                    ScriptInstance instance = (ScriptInstance) owner;
                    SortedSet<String> mods = analysis.SCRIPT_ORIGINS.get(instance.getScriptName().toIString());
                    if (null != mods) {
                        String mod = mods.last();
                        TString type = instance.getScriptName();
                        BUILDER.append(String.format("<p>Probably created by script <a href=\"script://%s\">%s</a> which came from mod \"%s\".</p>", type, type, mod));
                    }
                }
            });
        }

        BUILDER.append("<p/>");
        BUILDER.append(String.format("<p>ID: %s</p>", this.getID()));
        BUILDER.append(String.format("<p>Content type: %s</p>", this.TYPE));

        if (this.TYPE.isRefType()) {
            final Script SCRIPT = save.getPapyrus().getScripts().get(this.REFTYPE);
            if (null != SCRIPT) {
                BUILDER.append(String.format("<p>Reference type: %s</p>", SCRIPT.toHTML(this)));
            } else {
                BUILDER.append(String.format("<p>Reference type: %s</p>", this.REFTYPE));
            }
        }

        BUILDER.append(String.format("<p>Length: %d</p>", this.getLength()));
        //BUILDER.append("</p>");

        if (HOLDERS.isEmpty()) {
            BUILDER.append("<p><em>NO OWNER FOUND.</em> But we didn't look very hard.</p>");
        } else {
            BUILDER.append("<p>Owners:</p><ul>");

            HOLDERS.stream().forEach(owner -> {
                if (owner instanceof Linkable) {
                    BUILDER.append(String.format("<li>%s %s", owner.getClass().getSimpleName(), ((Linkable) owner).toHTML(this)));
                } else if (owner != null) {
                    BUILDER.append(String.format("<li>%s %s", owner.getClass().getSimpleName(), owner));
                }
            });

            BUILDER.append("</ul>");
        }

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Analysis analysis, String mod) {
        Objects.requireNonNull(analysis);
        Objects.requireNonNull(mod);
        return false;
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
     * @return An empty <code>List</code>.
     */
    @Override
    public List<MemberDesc> getDescriptors() {
        return Collections.emptyList();
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

    final private EID ID;
    final private Type TYPE;
    final private TString REFTYPE;
    final private int LENGTH;
    private ArrayData data;

    static final private List<Type> VALID_TYPES = Arrays.asList(
            Type.NULL,
            Type.REF,
            Type.STRING,
            Type.INTEGER,
            Type.FLOAT,
            Type.BOOLEAN,
            Type.VARIANT,
            Type.STRUCT);

    /**
     * Describes array data in a Skyrim savegame.
     *
     * @author Mark Fairchild
     */
    final private class ArrayData implements PapyrusDataFor<ArrayInfo> {

        /**
         * Creates a new <code>ArrayData</code> by reading from a
         * <code>ByteBuffer</code>. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The <code>PapyrusContext</code> info.
         * @throws PapyrusElementException
         */
        public ArrayData(ByteBuffer input, PapyrusContext context) throws PapyrusElementException, PapyrusFormatException {
            Objects.requireNonNull(input);
            Objects.requireNonNull(context);

            try {
                int count = ArrayInfo.this.LENGTH;
                this.VARIABLES = Variable.readList(input, count, context);
            } catch (ListException ex) {
                throw new PapyrusElementException("Couldn't read Array variables.", ex, this);
            }
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            ID.write(output);
            this.VARIABLES.forEach(var -> var.write(output));
        }

        /**
         * @see resaver.ess.Element#calculateSize()
         * @return The size of the <code>Element</code> in bytes.
         */
        @Override
        public int calculateSize() {
            int sum = ID.calculateSize();
            sum += this.VARIABLES.stream().mapToInt(var -> var.calculateSize()).sum();
            return sum;
        }

        /**
         * @return String representation.
         */
        @Override
        public String toString() {
            return ID.toString() + this.VARIABLES;
        }

        //final private EID ID;
        final private List<Variable> VARIABLES;

    }
}
