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
import resaver.ess.ESS;
import resaver.ess.Element;
import resaver.ess.Linkable;

/**
 * Describes an active script's stack in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class SuspendedStack implements PapyrusElement, AnalyzableElement, Linkable, HasID {

    /**
     * Creates a new <code>SuspendedStack</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     */
    public SuspendedStack(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException, PapyrusElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        this.ID = context.readEID32(input);
        this.FLAG = input.get();
        this.THREAD = context.findActiveScript(this.ID);

        if (this.FLAG == 0) {
            this.MESSAGE = null;
        } else {
            FunctionMessageData message = null;
            try {
                message = new FunctionMessageData(input, this, context);
            } catch (PapyrusElementException ex) {
                message = (FunctionMessageData) ex.getPartial();
                throw new PapyrusElementException("Failed to read message for SuspendedStack.", ex, this);
            } catch (PapyrusFormatException ex) {
                throw new PapyrusElementException("Failed to read message for SuspendedStack.", ex, this);
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
        this.ID.write(output);
        output.put(this.FLAG);
        assert this.FLAG > 0 || this.MESSAGE == null;

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
        int sum = 1;
        sum += this.ID.calculateSize();
        sum += this.MESSAGE == null ? 0 : this.MESSAGE.calculateSize();
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
        if (this.hasMessage()) {
            return this.MESSAGE.getScript();
        } else {
            return null;
        }
    }

    /**
     * @return The corresponding <code>ActiveScript</code>.
     */
    public ActiveScript getThread() {
        return this.THREAD;
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        if (null != target && this.hasMessage()) {
            Optional<Variable> result = this.MESSAGE.getVariables().stream()
                    .filter(v -> v.hasRef())
                    .filter(v -> v.getReferent() == target)
                    .findFirst();

            if (result.isPresent()) {
                int i = this.MESSAGE.getVariables().indexOf(result.get());
                if (i >= 0) {
                    return Linkable.makeLink("suspended", this.getID(), i, this.toString());
                }
            }
        }

        return Linkable.makeLink("suspended", this.getID(), this.toString());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();

        if (this.hasMessage()) {
            return this.MESSAGE.toString();

        } else if (this.THREAD != null) {
            return this.THREAD.toString();

        } else {
            BUF.append(this.ID);
        }

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
        BUILDER.append(String.format("<html><h3>SUSPENDEDSTACK</h3>"));

        if (this.THREAD != null) {
            BUILDER.append(String.format("<p>ActiveScript: %s</p>", this.THREAD.toHTML(this)));
        }

        if (this.hasMessage()) {
            if (null != analysis) {
                SortedSet<String> mods = analysis.SCRIPT_ORIGINS.get(this.MESSAGE.getScriptName().toIString());
                if (null != mods) {
                    BUILDER.append(String.format("<p>Probably running code from mod %s.</p>", mods.last()));
                }
            }

            BUILDER.append(String.format("<p>Function: %s</p>", this.MESSAGE.getFName()));
        }

        BUILDER.append(String.format("<p>Flag: %02x</p>", this.FLAG));

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
     * @return A flag indicating if the <code>SuspendedStack</code> is
     * undefined.
     *
     */
    public boolean isUndefined() {
        return this.hasMessage() && this.MESSAGE.isUndefined();
    }

    final private EID ID;
    final private byte FLAG;
    final private FunctionMessageData MESSAGE;
    final private ActiveScript THREAD;

}
