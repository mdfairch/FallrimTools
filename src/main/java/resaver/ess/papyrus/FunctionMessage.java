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

import resaver.ess.AnalyzableElement;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.nio.ByteBuffer;
import resaver.Analysis;
import resaver.IString;
import resaver.ess.ESS;
import resaver.ess.Element;
import resaver.ess.Flags;
import resaver.ess.Linkable;

/**
 * Describes a function message in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class FunctionMessage implements PapyrusElement, AnalyzableElement, Linkable {//, HasID {

    /**
     * Creates a new <code>FunctionMessage</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusElementException
     */
    public FunctionMessage(ByteBuffer input, PapyrusContext context) throws PapyrusElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        this.FLAG = input.get();
        this.ID = this.FLAG <= 2 ? context.readEID32(input) : null;
        this.FLAGS = Flags.readByteFlags(input);

        this.THREAD = this.ID == null ? null : context.findActiveScript(this.ID);
        assert this.THREAD != null;

        if (this.FLAGS.FLAGS == 0) {
            this.MESSAGE = null;
        } else {
            FunctionMessageData message = null;
            try {
                message = new FunctionMessageData(input, this, context);

            } catch (PapyrusElementException ex) {
                message = (FunctionMessageData) ex.getPartial();
                throw new PapyrusElementException("Failed to read message for FunctionMessage.", ex, this);

            } catch (PapyrusFormatException ex) {
                throw new PapyrusElementException("Failed to read message for FunctionMessage.", ex, this);

            } finally {
                this.MESSAGE = message;
            }
        }
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.FLAG);

        if (this.ID != null) {
            this.ID.write(output);
        }

        this.FLAGS.write(output);

        if (this.MESSAGE != null) {
            this.MESSAGE.write(output);
        }
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 2;
        sum += this.ID == null ? 0 : this.ID.calculateSize();
        sum += this.MESSAGE == null ? 0 : this.MESSAGE.calculateSize();
        return sum;
    }

    /**
     * @return The ID of the papyrus element.
     */
    public EID getID() {
        return this.ID;
    }

    /**
     * @return The message field.
     */
    public boolean hasMessage() {
        return this.MESSAGE != null;
    }

    /**
     * @return The message field.
     */
    public FunctionMessageData getMessage() {
        return this.MESSAGE;
    }

    /**
     * @return The corresponding <code>Script</code>.
     */
    public Script getScript() {
        return this.hasMessage() ? this.MESSAGE.getScript() : null;
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        if (null != target && null != this.MESSAGE) {
            Optional<Variable> result = this.MESSAGE.getVariables().stream()
                    .filter(v -> v.hasRef())
                    .filter(v -> v.getReferent() == target)
                    .findFirst();

            if (result.isPresent()) {
                int i = this.MESSAGE.getVariables().indexOf(result.get());
                if (i >= 0) {
                    return Linkable.makeLink("message", this.ID, i, this.toString());
                }
            }
        }

        return Linkable.makeLink("message", this.ID, this.toString());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        if (this.MESSAGE != null) {
            TString scriptName = this.MESSAGE.getScriptName();

            if (this.isUndefined()) {
                if (this.FLAG <= 2) {
                    return "MSG #" + scriptName + "# (" + this.ID + ")";
                } else {
                    return "MSG #" + scriptName;
                }
            } else if (this.FLAG <= 2) {
                return "MSG " + scriptName + " (" + this.ID + ")";
            } else {
                return "MSG " + scriptName;
            }

        } else if (this.ID != null) {
            return "MSG " + this.FLAGS + "," + EID.pad8(this.FLAG) + " (" + this.ID + ")";

        } else {
            return "MSG " + this.FLAGS + "," + EID.pad8(this.FLAG);
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
        BUILDER.append(String.format("<html><h3>FUNCTIONMESSAGE</h3>"));

        if (null != analysis && null != this.MESSAGE) {
            IString scriptName = this.MESSAGE.getScriptName().toIString();
            SortedSet<String> mods = analysis.SCRIPT_ORIGINS.get(scriptName);
            if (null != mods) {
                String mod = mods.last();
                BUILDER.append(String.format("<p>Probably running code from mod %s.</p>", mod));
            }
        }

        BUILDER.append("<p>");
        if (this.MESSAGE != null) {
            BUILDER.append(String.format("Function: %s<br/>", this.MESSAGE.getFName()));
        }

        BUILDER.append(String.format("ID: %s<br/>", this.ID));

        if (this.THREAD != null) {
            BUILDER.append(String.format("ActiveScript: %s<br/>", this.THREAD.toHTML(null)));
        } else {
            BUILDER.append("ActiveScript: NONE<br/>");
        }

        BUILDER.append(String.format("Flag: %s<br/>", this.FLAGS));
        BUILDER.append(String.format("Unknown: %d<br/>", this.FLAG));
        BUILDER.append("</p>");

        if (this.hasMessage()) {
            BUILDER.append("<hr/>");
            BUILDER.append(this.getMessage().getInfo(analysis, save));
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
        return this.hasMessage() && this.MESSAGE.matches(analysis, mod);
    }

    /**
     * @return A flag indicating if the <code>FunctionMessage</code> is
     * undefined.
     *
     */
    public boolean isUndefined() {
        return this.hasMessage() && this.MESSAGE.isUndefined();
    }

    final private byte FLAG;
    final private EID ID;
    final private Flags.Byte FLAGS;
    final private FunctionMessageData MESSAGE;
    final private ActiveScript THREAD;

}
