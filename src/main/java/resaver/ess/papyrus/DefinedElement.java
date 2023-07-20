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

import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;
import resaver.ess.AnalyzableElement;
import resaver.ess.Linkable;

/**
 * <code>DefinedElement</code> is a superclass of <code>ScriptInstance</code>,
 * <code>Reference</code>, and <code>Struct</code>, for situations in which they
 * are interchangeable.
 *
 * @author Mark Fairchild
 */
abstract public class DefinedElement implements AnalyzableElement, Linkable, PapyrusElement, HasID {

    /**
     * Creates a new <code>DefinedElement</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param defs The list of definitions.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     */
    public DefinedElement(ByteBuffer input, Map<TString, ? extends Definition> defs, PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(defs);
        Objects.requireNonNull(context);

        this.ID = context.readEID(input);
        this.DEFINITION_NAME = context.readTString(input);
        this.DEFINITION = defs.getOrDefault(this.DEFINITION_NAME, null);
        this.DEFINITION.incrementInstanceCount();
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);
        this.ID.write(output);
        this.DEFINITION_NAME.write(output);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 0;
        sum += this.ID.calculateSize();
        sum += this.DEFINITION_NAME.calculateSize();
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
     * @return The name of the corresponding <code>Definition</code>.
     */
    public TString getDefinitionName() {
        return this.DEFINITION_NAME;
    }

    /**
     * @return The corresponding <code>Definition</code>.
     */
    public Definition getDefinition() {
        return this.DEFINITION;
    }

    /**
     * @return A flag indicating if the <code>DefinedElement</code> is undefined.
     *
     */
    abstract public boolean isUndefined();

    /**
     * @see AnalyzableElement#matches(Optional<resaver.Analysis>, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Optional<resaver.Analysis> analysis, String mod) {
        Objects.requireNonNull(analysis);
        Objects.requireNonNull(mod);

        return analysis
                .map(an -> an.SCRIPT_ORIGINS.get(this.DEFINITION_NAME.toIString()))
                .orElse(Collections.emptySortedSet())
                .contains(mod);
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();

        if (this.isUndefined()) {
            BUF.append("#").append(this.getDefinitionName()).append("# ");
        } else {
            BUF.append(this.getDefinitionName()).append(": ");
        }

        BUF.append(" (").append(this.getID()).append(")");
        return BUF.toString();
    }

    final private EID ID;
    final private TString DEFINITION_NAME;
    final private Definition DEFINITION;   
    
}
