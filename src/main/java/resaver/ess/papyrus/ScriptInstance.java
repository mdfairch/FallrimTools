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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.logging.Logger;
import java.nio.ByteBuffer;
import java.util.Objects;
import resaver.ess.Element;
import resaver.ess.ESS;
import resaver.ess.Linkable;
import resaver.ess.Plugin;
import resaver.ess.RefID;

/**
 * Describes a script instance in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class ScriptInstance extends DefinedElement implements SeparateData, HasVariables {

    /**
     * Creates a new <code>ScriptInstances</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param scripts The <code>ScriptMap</code> containing the definitions.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     */
    ScriptInstance(ByteBuffer input, ScriptMap scripts, PapyrusContext context) throws PapyrusFormatException {
        super(input, scripts, context);
        this.UNKNOWN2BITS = input.getShort();
        this.UNKNOWN = input.getShort();
        this.REFID = context.readRefID(input);
        this.UNKNOWN_BYTE = input.get();
        if (context.getGame().isFO4()) {
            if ((this.UNKNOWN2BITS & 0x3) == 0x3) {
                this.UNKNOWN_FO_BYTE = input.get();
            } else {
                this.UNKNOWN_FO_BYTE = null;
            }
        } else {
            this.UNKNOWN_FO_BYTE = null;
        }
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        super.write(output);
        output.putShort(this.UNKNOWN2BITS);
        output.putShort(this.UNKNOWN);
        this.REFID.write(output);
        output.put(this.UNKNOWN_BYTE);
        if (null != this.UNKNOWN_FO_BYTE) {
            output.put(this.UNKNOWN_FO_BYTE);
        }
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
        this.data = new ScriptData(input, context);
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
        sum += 5;
        sum += this.REFID.calculateSize();
        if (null != this.UNKNOWN_FO_BYTE) {
            sum++;
        }

        sum += this.data == null ? 0 : this.data.calculateSize();
        return sum;
    }

    /**
     * @return The RefID of the papyrus element.
     */
    public RefID getRefID() {
        return this.REFID;
    }

    /**
     * @return The unknown short field; if it's -1, the RefID field may not be
     * valid.
     */
    public short getUnknown() {
        return this.UNKNOWN;
    }

    /**
     * @return The mystery flag; equivalent to <code>this.getUnknown==-1</code>.
     */
    public boolean isMysteryFlag() {
        return this.UNKNOWN == -1;
    }

    /**
     * @return The <code>ScriptData</code> for the instance.
     */
    public ScriptData getData() {
        return this.data;
    }

    /**
     * Sets the data field.
     *
     * @param newData The new value for the data field.
     */
    public void setData(ScriptData newData) {
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
     * Shortcut for getData().getState().
     *
     * @return The state of the script.
     */
    public TString getState() {
        return null != this.data ? this.data.getState() : null;
    }

    /**
     * @return Checks for a memberless error.
     */
    public boolean hasMemberlessError() {
        final List<MemberDesc> DESCS = null != this.getScript()
                ? this.getScript().getExtendedMembers()
                : Collections.emptyList();

        final List<Variable> VARS = this.getVariables();
        return VARS.isEmpty() && !DESCS.isEmpty();
    }

    /**
     * @return Checks for a definition error.
     */
    public boolean hasDefinitionError() {
        final List<MemberDesc> DESCS = null != this.getScript()
                ? this.getScript().getExtendedMembers()
                : Collections.emptyList();

        final List<Variable> VARS = this.getVariables();
        return DESCS.size() != VARS.size() && VARS.size() > 0;
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
        if (null == target || null == this.data) {
            return Linkable.makeLink("scriptinstance", this.getID(), this.toString());

        } else if (target instanceof Variable) {
            int index = this.getVariables().indexOf(target);
            if (index >= 0) {
                return Linkable.makeLink("scriptinstance", this.getID(), index, this.toString());
            } else {
                return Linkable.makeLink("scriptinstance", this.getID(), this.toString());
            }

        } else {
            return this.getVariables().stream()
                    .filter(var -> var.hasRef())
                    .filter(var -> var.getReferent() == target)
                    .map(var -> this.getVariables().indexOf(var))
                    .filter(index -> index >= 0)
                    .findFirst()
                    .map(index -> Linkable.makeLink("scriptinstance", this.getID(), index, this.toString()))
                    .orElse(Linkable.makeLink("scriptinstance", this.getID(), this.toString()));
        }
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();

        if (this.isUndefined()) {
            BUF.append("#").append(this.getScriptName()).append("#: ");
        } else {
            BUF.append(this.getScriptName()).append(": ");
        }

        if (this.isMysteryFlag()) {
            BUF.append("*");
        }

        BUF.append(this.REFID.toHex());
        BUF.append(" (").append(this.getID()).append(")");
        return BUF.toString();
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
            BUILDER.append(String.format("<html><h3>INSTANCE of %s</h3>", this.getScript().toHTML(this)));
        } else {
            BUILDER.append(String.format("<html><h3>INSTANCE of %s</h3>", this.getScriptName()));
        }

        final Plugin PLUGIN = this.REFID.PLUGIN;
        if (PLUGIN != null) {
            BUILDER.append(String.format("<p>This instance is attached to an object from %s.</p>", PLUGIN.toHTML(this)));
        } else if (this.REFID.getType() == RefID.Type.CREATED) {
            BUILDER.append("<p>This instance was created in-game.</p>");
        }

        if (this.isUndefined()) {
            BUILDER.append("<p><em>WARNING: SCRIPT MISSING!</em><br/>Remove Undefined Instances\" will delete this.</p>");
        }

        if (this.isUnattached()) {
            BUILDER.append("<p><em>WARNING: OBJECT MISSING!</em><br/>Selecting \"Remove Unattached Instances\" will delete this.</p>");
        }

        if (this.REFID.getType() == RefID.Type.CREATED && !save.getChangeForms().containsKey(this.REFID)) {
            BUILDER.append("<p><em>REFID POINTS TO NONEXISTENT CREATED FORM.</em><br/>Remove non-existent form instances\" will delete this. However, some mods create these instances deliberately. </p>");
        }

        if (null != analysis) {
            final SortedSet<String> PROVIDERS = analysis.SCRIPT_ORIGINS.get(this.getScriptName().toIString());
            if (null != PROVIDERS) {
                String probableProvider = PROVIDERS.last();
                BUILDER.append(String.format("<p>The script probably came from mod \"%s\".</p>", probableProvider));

                if (PROVIDERS.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    PROVIDERS.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }
            }
        }

        BUILDER.append("<p>");
        BUILDER.append(String.format("ID: %s<br/>", this.getID()));
        BUILDER.append(String.format("State: %s<br/>", this.getState()));

        boolean mysteryFlag = this.UNKNOWN == -1;

        if (save.getChangeForms().containsKey(this.REFID)) {
            BUILDER.append(String.format("RefID%s: %s<br/>", (mysteryFlag ? "@" : ""), this.REFID.toHTML(null)));
        } else {
            BUILDER.append(String.format("RefID%s: %s<br/>", (mysteryFlag ? "@" : ""), this.REFID.toString()));
        }

        BUILDER.append(String.format("Unknown2bits: %01X<br/>", this.UNKNOWN2BITS));
        BUILDER.append(String.format("UnknownShort: %04X<br/>", this.UNKNOWN));
        BUILDER.append(String.format("UnknownByte: %02x<br/>", this.UNKNOWN_BYTE));
        BUILDER.append("</p>");

        save.getPapyrus().printReferrents(this, BUILDER, "script instance");

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    /**
     * @return A flag indicating if the <code>ScriptInstance</code> is
     * unattached.
     *
     */
    public boolean isUnattached() {
        return this.REFID.isZero();
    }

    /**
     * @return A flag indicating that the <code>ScriptInstance</code> has a
     * canary variable.
     */
    public boolean hasCanary() {
        if (null == this.getScript()) {
            return false;
        }

        final List<MemberDesc> DESCS = this.getScript().getExtendedMembers();
        return DESCS.stream().anyMatch(isCanary);
    }

    /**
     * @return A flag indicating that the <code>ScriptInstance</code> has a
     * canary variable.
     */
    public int getCanary() {
        final List<Variable> MEMBERS = this.getVariables();
        if (null == this.getScript() || MEMBERS.isEmpty()) {
            return 0;
        }

        final List<MemberDesc> NAMES = this.getScript().getExtendedMembers();
        Optional<MemberDesc> canary = NAMES.stream().filter(isCanary).findFirst();

        if (canary.isPresent()) {
            Variable var = MEMBERS.get(NAMES.indexOf(canary.get()));
            if (var instanceof Variable.Int) {
                return ((Variable.Int) var).getValue();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * @return A flag indicating if the <code>ScriptInstance</code> is
     * undefined.
     *
     */
    @Override
    public boolean isUndefined() {
        if (null != this.getScript()) {
            return this.getScript().isUndefined();
        } else {
            return !Script.NATIVE_SCRIPTS.contains(this.getScriptName().toIString());
        }
    }

    final private short UNKNOWN2BITS;
    final private short UNKNOWN;
    final private RefID REFID;
    final private byte UNKNOWN_BYTE;
    final private Byte UNKNOWN_FO_BYTE;
    private ScriptData data;

    static final private java.util.function.Predicate<MemberDesc> isCanary = (desc -> desc.getName().equals("::iPapyrusDataVerification_var"));

    static final private Logger LOG = Logger.getLogger(ScriptInstance.class.getCanonicalName());

    /**
     * Describes a script instance's data in a Skyrim savegame.
     *
     * @author Mark Fairchild
     */
    final public class ScriptData implements PapyrusDataFor<ScriptInstance> {

        /**
         * Creates a new <code>ScriptData</code> by reading from a
         * <code>ByteBuffer</code>. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The <code>PapyrusContext</code> info.
         * @throws PapyrusElementException
         * @throws PapyrusFormatException
         */
        public ScriptData(ByteBuffer input, PapyrusContext context) throws PapyrusElementException, PapyrusFormatException {
            Objects.requireNonNull(input);
            Objects.requireNonNull(context);
            this.FLAG = input.get();
            this.STATE = context.readTString(input);
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
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            getID().write(output);
            output.put(this.FLAG);
            this.STATE.write(output);
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
            sum += this.STATE.calculateSize();
            sum += this.VARIABLES.stream().mapToInt(var -> var.calculateSize()).sum();
            return sum;
        }

        /**
         * @return The type of the script.
         */
        public TString getState() {
            return this.STATE;
        }

        /**
         * @see Object#toString()
         * @return
         */
        @Override
        public String toString() {
            final StringBuilder BUILDER = new StringBuilder();
            BUILDER.append("SCRIPTDATA\n");
            BUILDER.append(String.format("ID = %s\n", getID()));
            BUILDER.append(String.format("flag= %d\n", this.FLAG));
            BUILDER.append(String.format("type = %s\n", this.STATE));
            BUILDER.append(String.format("unknown1 = %d\n", this.UNKNOWN1));
            BUILDER.append(String.format("unknown2 = %d\n\n", this.UNKNOWN2));
            this.VARIABLES.forEach(var -> BUILDER.append(String.format("%s\n", var)));
            return BUILDER.toString();
        }

        //final private EID ID;
        final private byte FLAG;
        final private TString STATE;
        final private int UNKNOWN1;
        final private int UNKNOWN2;
        final private List<Variable> VARIABLES;

    }
}
