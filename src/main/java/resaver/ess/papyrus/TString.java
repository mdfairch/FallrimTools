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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import resaver.IString;
import resaver.ess.AnalyzableElement;
import resaver.ess.ESS;
import resaver.ess.Element;
import resaver.ess.Linkable;
import resaver.ess.WStringElement;

/**
 * A case-insensitive string with value semantics that reads and writes as an
 * index into a string table.
 *
 * @author Mark Fairchild
 */
abstract public class TString implements PapyrusElement, AnalyzableElement, Linkable {

    /**
     * Creates a new <code>TString</code> that is unindexed not part of a table.
     * It can only be used for comparisons.
     *
     * @param cs The contents for the new <code>TString</code>.
     * @return A new <code>TString</code>. It can't be used for anything except
     * comparison.
     */
    static public TString makeUnindexed(CharSequence cs) {
        return new TString(cs) {
            @Override
            public void write(ByteBuffer output) {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public int calculateSize() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }

    /**
     * Creates a new <code>TString</code> from a <code>WStringElement</code> and an
     * index.
     *
     * @param wstr The <code>WStringElement</code>.
     * @param index The index of the <code>TString</code>.
     */
    protected TString(WStringElement wstr, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Illegal index: " + index);
        }

        this.WSTR = Objects.requireNonNull(wstr);
        this.INDEX = index;
    }

    /**
     * Creates a new <code>TString</code> from a character sequence and an
     * index.
     *
     * @param cs The <code>CharSequence</code>.
     * @param index The index of the <code>TString</code>.
     */
    protected TString(CharSequence cs, int index) {
        this(new WStringElement(cs), index);
    }

    /**
     * Creates a new unindexed <code>TString</code> from a character sequence.
     *
     * @param cs The <code>CharSequence</code>.
     */
    protected TString(CharSequence cs) {
        this.WSTR = new WStringElement(cs);
        this.INDEX = -1;
    }

    /**
     * @see WStringElement#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    public void writeFull(ByteBuffer output) {
        this.WSTR.write(output);
    }

    /**
     * @see WStringElement#calculateFullSizecalculateFullSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    public int calculateFullSize() {
        return this.WSTR.calculateSize();
    }

    /**
     * @see IString#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        return this.WSTR.hashCode();
    }

    /**
     * Tests for case-insensitive value-equality with another
     * <code>TString</code>, <code>IString</code>, or <code>String</code>.
     *
     * @param obj The object to which to compare.
     * @see java.lang.String#equalsIgnoreCase(java.lang.String)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof TString) {
            TString other = (TString) obj;
            return this.INDEX == other.INDEX;
        } else {
            return this.WSTR.equals(obj);
        }
    }

    /**
     * Tests for case-insensitive value-equality with a <code>String</code>.
     *
     * @param obj The object to which to compare.
     * @see java.lang.String#equalsIgnoreCase(java.lang.String)
     * @return
     */
    public boolean equals(String obj) {
        return this.WSTR.equals(obj);
    }

    /**
     * Tests for case-insensitive value-equality with another
     * <code>TString</code>.
     *
     * @param other The <code>TString</code> to which to compare.
     * @return True if the strings have the same index, false otherwise.
     * @see java.lang.String#equalsIgnoreCase(java.lang.String)
     */
    public boolean equals(TString other) {
        Objects.requireNonNull(other);
        if (this.INDEX < 0 || other.INDEX < 0) {
            return this.WSTR.equals(other.WSTR);
        } else {
            return this.INDEX == other.INDEX;
        }
    }

    /**
     * Getter for the index field.
     *
     * @return
     */
    public int getIndex() {
        assert this.INDEX >= 0;
        return INDEX;
    }

    /**
     * @see java.lang.String#isEmpty()
     * @return
     */
    public boolean isEmpty() {
        return this.WSTR.isEmpty();
    }

    /**
     * @see AnalyzableElement#getInfo(Optional<resaver.Analysis>, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(Optional<resaver.Analysis> analysis, ESS save) {
        Objects.requireNonNull(save);

        final StringBuilder BUILDER = new StringBuilder();

        BUILDER.append("<html><h3>STRING</h3>");
        BUILDER.append(String.format("<p>Value: \"%s\".</p>", this));
        BUILDER.append(String.format("<p>Length: %d</p>", this.WSTR.length()));

        /*if (null != analysis) {
            final Map<String, Integer> OWNERS = analysis.STRING_ORIGINS.get(this.toIString());

            if (null != OWNERS) {
                int total = OWNERS.values().stream().mapToInt(k -> k).sum();

                BUILDER.append(String.format("<p>This string appears %d times in the script files of %d mods.</p><ul>", total, OWNERS.size()));
                OWNERS.forEach((mod, count) -> BUILDER.append(String.format("<li>String appears %d times in the scripts of mod \"%s\".", count, mod)));
                BUILDER.append("</ul>");

            } else {
                BUILDER.append("<p>String origin could not be determined.</p>");
            }
        }*/
        final Papyrus PAPYRUS = save.getPapyrus();
        final List<String> LINKS = new java.util.LinkedList<>();

        // Check definitions (Scripts and Structs).
        Stream.concat(
                PAPYRUS.getScripts().values().stream(),
                PAPYRUS.getStructs().values().stream()).parallel().forEach(def -> {

            if (this == def.getName()) {
                LINKS.add(def.toHTML(null));
            }
            
            List<MemberDesc> members = def.getMembers();           
            members.stream()
                    .filter(m -> this.equals(m.getName()))
                    .forEach(m -> LINKS.add(def.toHTML(m)));
        });

        /*
        // Check function messages.
        PAPYRUS.getSuspendedStacks().values().stream().parallel()
                .map(s -> s.getMessage())
                .forEach(stack -> {
                    stack.getMembers().stream().filter(var -> var.)
                });
        
        Stream.of(
                PAPYRUS.getFunctionMessages().stream().filter(m -> m.getMessage() != null).map(m -> Pair.make(m, m.getMessage())),
                PAPYRUS.getSuspendedStacks().values().stream().filter(s -> s.getMessage() != null).map(s -> Pair.make(s, s.getMessage())))
                .flatMap(i -> i);

        final Stream<Pair<? extends PapyrusElement, List<Variable>>> ELEMENTS = Stream.of(
                PAPYRUS.getScriptInstances().values().stream()
                        .map(i -> Pair.make(i, i.getData().getVariables())),
                PAPYRUS.getStructInstances().values().stream()
                        .map(s -> Pair.make(s, s.getMembers())),
                PAPYRUS.getReferences().values().stream()
                        .map(r -> Pair.make(r, r.getMembers())),
                PAPYRUS.getActiveScripts().values().stream()
                        .flatMap(t -> t.getStackFrames().stream())
                        .map(f -> Pair.make(f, f.getVariables())),
                PAPYRUS.getFunctionMessages().stream()
                        .filter(m -> m.getMessage() != null)
                        .map(m -> Pair.make(m, m.getMessage().getMembers())),
                PAPYRUS.getSuspendedStacks1().stream()
                        .filter(s -> s.getMessage() != null)
                        .map(s -> Pair.make(s, s.getMessage().getMembers())),
                PAPYRUS.getSuspendedStacks2().stream()
                        .filter(s -> s.getMessage() != null)
                        .map(s -> Pair.make(s, s.getMessage().getMembers())))
                .flatMap(i -> i);

        ELEMENTS.parallel().forEach(e -> {
            final PapyrusElement ELEMENT = e.A;
            final List<Variable> VARS = e.B;

            VARS.stream()
                    .filter(var -> var instanceof Variable.Str)
                    .map(var -> (Variable.Str) var)
                    .filter(var -> var.getValue().equals(this))
                    .forEach(var -> HOLDERS.add(Pair.make(var, ELEMENT)));
            VARS.stream()
                    .filter(var -> var instanceof Variable.Array)
                    .map(var -> (Variable.Array) var)
                    .filter(var -> var.getArray() != null)
                    .flatMap(var -> var.getArray().getMembers().stream())
                    .filter(var -> var.getType() == Type.STRING)
                    .map(var -> (Variable.Str) var)
                    .filter(var -> var.getValue().equals(this))
                    .forEach(var -> HOLDERS.add(Pair.make(var, ELEMENT)));
        });

        PAPYRUS.getActiveScripts().values().stream().forEach(thread -> {
            final Unknown4 U4 = thread.getUnknown4();
            if (U4 != null && U4.TSTRING != null && U4.TSTRING.equals(this)) {
                HOLDERS.add(Pair.make(U4, thread));
            }
        });

        PAPYRUS.getActiveScripts().values().stream().flatMap(thread -> thread.getStackFrames().stream())
                .parallel().forEach(frame -> {
                    if (Objects.equals(frame.getEvent(), this)
                            || Objects.equals(frame.getDocString(), this)
                            || Objects.equals(frame.getStatus(), this)) {
                        HOLDERS.add(Pair.make(frame));
                    }

                    Stream.concat(frame.getFunctionLocals().stream(), frame.getFunctionParams().stream())
                            .filter(member -> member.getName().equals(this))
                            .forEach(member -> HOLDERS.add(Pair.make(member, frame)));
                });

        MESSAGES
                .filter(p -> Objects.equals(p.B.getEvent(), this))
                .forEach(p -> HOLDERS.add(Pair.make(p.A, p.B)));
         */
        if (!LINKS.isEmpty()) {
            BUILDER.append(String.format("<p>This string occurs %d times in this save.</p>", LINKS.size()));
            LINKS.forEach(link -> BUILDER.append(link).append("<br/>"));
        }

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    /**
     * @return The <code>WStringElement</code> that that the <code>TString</code>
     * points to.
     */
    public WStringElement toWString() {
        return this.WSTR;
    }

    /**
     * @return The <code>WStringElement</code> that that the <code>TString</code>
     * points to.
     */
    public IString toIString() {
        return this.WSTR;
    }

    /**
     * @return The length of the <code>TString</code>.
     * @see java.lang.String#length()
     */
    public int length() {
        return this.WSTR.length();
    }

    /**
     *
     * @Override
     */
    @Override
    public String toString() {
        return this.WSTR.toString();
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        return Linkable.makeLink("string", this.INDEX, this.toString());
    }

    /**
     *
     * @param s1
     * @param s2
     * @return
     */
    static public int compare(TString s1, TString s2) {
        final WStringElement W1 = s1 != null ? s1.WSTR : null;
        final WStringElement W2 = s2 != null ? s2.WSTR : null;
        return WStringElement.compare(W1, W2);
    }

    final private int INDEX;
    final private WStringElement WSTR;

}
