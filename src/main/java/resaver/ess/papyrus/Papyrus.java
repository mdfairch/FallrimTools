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

import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import resaver.ListException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.logging.Logger;
import resaver.ess.GlobalDataBlock;
import resaver.ess.ESS;
import resaver.ess.Element;
import java.util.stream.Collectors;
import resaver.ess.Linkable;
import resaver.ess.ModelBuilder;
import resaver.ess.PositionException;

/**
 * Describes a the data for a <code>GlobalData</code> when it is the Papyrus
 * script section.
 *
 * @author Mark Fairchild
 */
final public class Papyrus implements PapyrusElement, GlobalDataBlock {

    /**
     * Creates a new <code>Papyrus</code> by reading from a byte buffer.
     *
     * @param input The data.
     * @param context The <code>ESSContext</code> info.
     * @param model A <code>ModelBuilder</code>.
     *
     * @throws PapyrusException Thrown if the Papyrus block was partially
     * readRefID.
     *
     */
    public Papyrus(ByteBuffer input, ESS.ESSContext context, ModelBuilder model) throws PapyrusException {
        final mf.Counter SUM = new mf.Counter(input.capacity());
        SUM.addCountListener(sum -> {
            if (!this.truncated && sum != ((Buffer) input).position()) {
                throw new PositionException("Papyrus block", sum, ((Buffer) input).position());
            }
        });

        this.CONTEXT = new PapyrusContext(context, this);
        this.EIDS = new java.util.HashMap<>(100_000);

        try {
            if (input.limit() - input.position() < 7) {
                String msg = "The Papyrus block is missing. This can happen if Skyrim is running too many mods or too many scripts.\nUnfortunately, there is literally nothing I can do to help you with this.";
                throw new PapyrusException(msg, null, this);
            }

            // Read the header.            
            this.HEADER = input.getShort();
            SUM.click(2);

            // Read the string table.
            StringTable stringTable = null;
            try {
                stringTable = new StringTable(input, context);
            } catch (PapyrusElementException ex) {
                stringTable = (StringTable) ex.getPartial();
                throw new PapyrusException("Error reading StringTable.", ex, this);
            } finally {
                this.STRINGS = stringTable;
                model.addStringTable(stringTable);
            }

            SUM.click(this.STRINGS.calculateSize());

            int scriptCount = input.getInt();
            int structCount = context.getGame().isFO4() ? input.getInt() : 0;
            SUM.click(context.getGame().isFO4() ? 8 : 4);

            // Read the scripts.
            ScriptMap scriptMap = null;
            try {
                scriptMap = new ScriptMap(scriptCount, input, CONTEXT);
            } catch (PapyrusElementException ex) {
                scriptMap = (ScriptMap) ex.getPartial();
                throw new PapyrusException("Error reading Scripts.", ex, this);
            } finally {
                this.SCRIPTS = scriptMap;
                this.SCRIPTS.values().forEach(script -> script.resolveParent(this.SCRIPTS));
                model.addScripts(this.SCRIPTS);
                SUM.click(this.SCRIPTS.calculateSize() - 4);
            }

            // Read the structs.
            if (!context.getGame().isFO4()) {
                this.STRUCTS = null;
            } else {
                StructMap structMap = null;
                try {
                    structMap = new StructMap(structCount, input, CONTEXT);
                } catch (PapyrusElementException ex) {
                    structMap = (StructMap) ex.getPartial();
                    throw new PapyrusException("Error reading Structs.", ex, this);
                } finally {
                    this.STRUCTS = structMap;
                    model.addStructs(this.STRUCTS);
                    SUM.click(this.STRUCTS.calculateSize() - 4);
                }
            }

            // Read the script instance table.
            ScriptInstanceMap scriptInstanceMap = null;
            try {
                scriptInstanceMap = new ScriptInstanceMap(input, this.SCRIPTS, CONTEXT);
            } catch (PapyrusElementException ex) {
                scriptInstanceMap = (ScriptInstanceMap) ex.getPartial();
                throw new PapyrusException("Error reading ScriptInstances.", ex, this);
            } finally {
                this.SCRIPT_INSTANCES = scriptInstanceMap;
                SUM.click(this.SCRIPT_INSTANCES.calculateSize());
            }

            // Read the reference table.
            ReferenceMap referenceMap = null;
            try {
                referenceMap = new ReferenceMap(input, this.SCRIPTS, CONTEXT);
            } catch (PapyrusElementException ex) {
                referenceMap = (ReferenceMap) ex.getPartial();
                throw new PapyrusException("Error reading References.", ex, this);
            } finally {
                this.REFERENCES = referenceMap;
                SUM.click(this.REFERENCES.calculateSize());
            }

            // Read the struct body table.
            if (!context.getGame().isFO4()) {
                this.STRUCT_INSTANCES = null;
            } else {
                StructInstanceMap structInstanceMap = null;
                try {
                    structInstanceMap = new StructInstanceMap(input, this.STRUCTS, CONTEXT);
                } catch (PapyrusElementException ex) {
                    structInstanceMap = (StructInstanceMap) ex.getPartial();
                    throw new PapyrusException("Error reading StructInstances.", ex, this);
                } finally {
                    this.STRUCT_INSTANCES = structInstanceMap;
                    SUM.click(this.STRUCT_INSTANCES.calculateSize());
                }
            }

            // Read the array table.
            ArrayMap arrayMap = null;
            try {
                arrayMap = new ArrayMap(input, CONTEXT);
            } catch (PapyrusElementException ex) {
                arrayMap = (ArrayMap) ex.getPartial();
                throw new PapyrusException("Error reading ArrayInfos.", ex, this);
            } finally {
                this.ARRAYS = arrayMap;
                SUM.click(this.ARRAYS.calculateSize());
            }

            this.PAPYRUS_RUNTIME = CONTEXT.readEID32(input);
            SUM.click(4);

            // Read the active script table.
            ActiveScriptMap activeScriptMap = null;
            try {
                activeScriptMap = new ActiveScriptMap(input, CONTEXT);
            } catch (PapyrusElementException ex) {
                activeScriptMap = (ActiveScriptMap) ex.getPartial();
                throw new PapyrusException("Error reading ActiveScripts.", ex, this);
            } finally {
                this.ACTIVESCRIPTS = activeScriptMap;
                SUM.click(this.ACTIVESCRIPTS.calculateSize());
            }

            // Read the Script data table.
            try {
                SUM.unclick(this.SCRIPT_INSTANCES.calculateSize());
                int count = this.SCRIPT_INSTANCES.size();
                for (int i = 0; i < count; i++) {
                    try {
                        EID eid = CONTEXT.readEID(input);
                        ScriptInstance element = this.getScriptInstances().get(eid);
                        element.readData(input, CONTEXT);
                    } catch (NullPointerException | PapyrusFormatException | PapyrusElementException | BufferUnderflowException ex) {
                        throw new ListException(i, count, ex);
                    }
                }
                SUM.click(this.SCRIPT_INSTANCES.calculateSize());
            } catch (ListException ex) {
                throw new PapyrusException("Error reading ScriptInstance data.", ex, this);
            } finally {
                model.addScriptInstances(this.SCRIPT_INSTANCES);
            }

            // Read the reference data table.
            try {
                SUM.unclick(this.REFERENCES.calculateSize());
                int count = this.REFERENCES.size();
                for (int i = 0; i < count; i++) {
                    try {
                        EID eid = CONTEXT.readEID(input);
                        Reference element = this.getReferences().get(eid);
                        element.readData(input, CONTEXT);
                    } catch (NullPointerException | PapyrusFormatException | PapyrusElementException | BufferUnderflowException ex) {
                        throw new ListException(i, count, ex);
                    }
                }
                SUM.click(this.REFERENCES.calculateSize());
            } catch (ListException ex) {
                throw new PapyrusException("Error reading Reference data.", ex, this);
            } finally {
                model.addReferences(referenceMap);
            }

            // Read the struct data table.
            if (this.STRUCT_INSTANCES != null) {
                try {
                    SUM.unclick(this.STRUCT_INSTANCES.calculateSize());
                    int count = this.getStructInstances().size();
                    for (int i = 0; i < count; i++) {
                        try {
                            EID eid = CONTEXT.readEID(input);
                            StructInstance element = this.getStructInstances().get(eid);
                            element.readData(input, CONTEXT);
                        } catch (NullPointerException | PapyrusFormatException | PapyrusElementException | BufferUnderflowException ex) {
                            throw new ListException(i, count, ex);
                        }
                    }
                    SUM.click(this.STRUCT_INSTANCES.calculateSize());
                } catch (ListException ex) {
                    throw new PapyrusException("Error reading StructInstance data.", ex, this);
                } finally {
                    model.addStructInstances(this.STRUCT_INSTANCES);
                }
            }

            // Read the array data table.
            try {
                SUM.unclick(this.ARRAYS.calculateSize());
                int count = this.ARRAYS.size();
                for (int i = 0; i < count; i++) {
                    try {
                        EID eid = CONTEXT.readEID(input);
                        ArrayInfo element = this.getArrays().get(eid);
                        element.readData(input, CONTEXT);
                    } catch (NullPointerException | PapyrusFormatException | PapyrusElementException | BufferUnderflowException ex) {
                        throw new ListException(i, count, ex);
                    }
                }
                SUM.click(this.ARRAYS.calculateSize());
            } catch (ListException ex) {
                throw new PapyrusException("Error reading Array data.", ex, this);
            } finally {
                model.addArrays(arrayMap);
            }

            try {
                // Read the ActiveScript data table.
                try {
                    SUM.unclick(this.ACTIVESCRIPTS.calculateSize());
                    int count = this.ACTIVESCRIPTS.size();
                    for (int i = 0; i < count; i++) {
                        try {
                            EID eid = CONTEXT.readEID32(input);
                            ActiveScript element = this.getActiveScripts().get(eid);
                            element.readData(input, CONTEXT);
                        } catch (NullPointerException | PapyrusFormatException | PapyrusElementException | BufferUnderflowException ex) {
                            throw new ListException(i, count, ex);
                        }
                    }
                    SUM.click(this.ACTIVESCRIPTS.calculateSize());
                } catch (ListException ex) {
                    throw new PapyrusException("Error reading ActiveScript data.", ex, this);
                }

                // Read the function message table.
                int functionMessageCount = input.getInt();
                this.FUNCTIONMESSAGES = new ArrayList<>(functionMessageCount);
                try {
                    for (int i = 0; i < functionMessageCount; i++) {
                        try {
                            FunctionMessage message = new FunctionMessage(input, CONTEXT);
                            this.FUNCTIONMESSAGES.add(message);
                        } catch (NullPointerException | PapyrusElementException | BufferUnderflowException ex) {
                            throw new ListException(i, functionMessageCount, ex);
                        }
                    }
                    SUM.click(4 + this.FUNCTIONMESSAGES.parallelStream().mapToInt(v -> v.calculateSize()).sum());
                } catch (ListException ex) {
                    throw new PapyrusException("Failed to read FunctionMessage table.", ex, this);
                }

                // Read the first SuspendedStack table.
                SuspendedStackMap suspendStacks1 = null;
                try {
                    suspendStacks1 = new SuspendedStackMap(input, CONTEXT);
                } catch (PapyrusElementException ex) {
                    suspendStacks1 = (SuspendedStackMap) ex.getPartial();
                    throw new PapyrusException("Error reading SuspendedStacks1.", ex, this);
                } finally {
                    this.SUSPENDEDSTACKS1 = suspendStacks1;
                    SUM.click(this.SUSPENDEDSTACKS1.calculateSize());
                }

                // Read the second SuspendedStack table.
                SuspendedStackMap suspendStacks2 = null;
                try {
                    suspendStacks2 = new SuspendedStackMap(input, CONTEXT);
                } catch (PapyrusElementException ex) {
                    suspendStacks2 = (SuspendedStackMap) ex.getPartial();
                    throw new PapyrusException("Error reading SuspendedStacks2.", ex, this);
                } finally {
                    this.SUSPENDEDSTACKS2 = suspendStacks2;
                    SUM.click(this.SUSPENDEDSTACKS2.calculateSize());
                }
                
            } finally {
                model.addThreads(this.getActiveScripts());
                model.addFunctionMessages(this.getFunctionMessages());
                model.addSuspendedStacks1(this.getSuspendedStacks1());
                model.addSuspendedStacks2(this.getSuspendedStacks2());
            }

            // Read the "unknown" fields.
            this.UNK1 = input.getInt();
            SUM.click(4);

            this.UNK2 = this.UNK1 == 0 ? Optional.empty() : Optional.of(input.getInt());
            SUM.click(this.UNK2.isPresent() ? 4 : 0);

            int unknownCount = input.getInt();
            this.UNKS = new ArrayList<>(unknownCount);
            for (int i = 0; i < unknownCount; i++) {
                this.UNKS.add(CONTEXT.readEID(input));
            }

            model.addUnknownIDList(this.UNKS);
            SUM.click(4 + this.UNKS.parallelStream().mapToInt(v -> v.calculateSize()).sum());

            UnbindMap unbinds = null;
            try {
                unbinds = new UnbindMap(input, CONTEXT);
            } catch (PapyrusElementException ex) {
                unbinds = (UnbindMap) ex.getPartial();
                throw new PapyrusException("Error reading SuspendedStacks2.", ex, this);
            } finally {
                this.UNBINDMAP = unbinds;
                model.addUnbinds(unbinds);
                SUM.click(this.UNBINDMAP.calculateSize());
            }

            // For Skyrim, readRefID the save file version field.
            this.SAVE_FILE_VERSION = context.getGame().isSkyrim()
                    ? Optional.of(input.getShort())
                    : Optional.empty();

            Map<EID, SuspendedStack> stacks = this.getSuspendedStacks();
            this.getActiveScripts().values().forEach(script -> script.resolveStack(stacks, getFunctionMessages()));

            // Stuff the remaining data into a buffer.
            int remaining = input.limit() - input.position();
            this.ARRAYSBLOCK = new byte[remaining];
            input.get(this.ARRAYSBLOCK);

            if (((Buffer) input).position() != this.calculateSize()) {
                throw new IllegalStateException(String.format("pos = %d, calculated = %d", input.position(), this.calculateSize()));
            }

            // Read the "other" stuff.
            OtherData other = null;
            try {
                final ByteBuffer ARRAYSBUFFER = ByteBuffer.wrap(this.ARRAYSBLOCK);
                ARRAYSBUFFER.order(ByteOrder.LITTLE_ENDIAN);

                other = new OtherData(input, CONTEXT);
            } catch (PapyrusFormatException | BufferUnderflowException ex) {

            } finally {
                this.OTHER = other;
                model.addOtherData(other);
            }

        } catch (PapyrusException | java.nio.BufferUnderflowException ex) {
            this.truncated = true;
            final String MSG = String.format("Error while reading the Papyrus section of %s: %s.", context.getPath().getFileName(), ex.getMessage());
            throw new PapyrusException(MSG, ex, this);
            
        } finally {
            this.CONTEXT.buildCrossreferences();
        }
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        if (this.truncated) {
            throw new IllegalStateException("Papyrus is truncated. Cannot write.");
        }

        int startingPosition = ((Buffer) output).position();

        output.putShort(this.HEADER);

        // Write the string table.
        this.STRINGS.write(output);

        if (null != this.STRUCTS) {
            output.putInt(this.SCRIPTS.size());
            output.putInt(this.STRUCTS.size());
            this.SCRIPTS.write(output);
            this.STRUCTS.write(output);
        } else {
            output.putInt(this.SCRIPTS.size());
            this.SCRIPTS.write(output);
        }

        this.SCRIPT_INSTANCES.write(output);
        this.REFERENCES.write(output);

        if (this.STRUCT_INSTANCES != null) {
            this.STRUCT_INSTANCES.write(output);
        }

        this.ARRAYS.write(output);

        this.PAPYRUS_RUNTIME.write(output);
        this.ACTIVESCRIPTS.write(output);

        this.SCRIPT_INSTANCES.values().forEach(data -> data.getData().write(output));
        this.REFERENCES.values().forEach(ref -> ref.getData().write(output));

        if (this.STRUCT_INSTANCES != null) {
            this.STRUCT_INSTANCES.values().forEach(struct -> struct.writeData(output));
        }

        this.ARRAYS.values().forEach(info -> info.writeData(output));
        this.ACTIVESCRIPTS.values().forEach(script -> script.writeData(output));

        // Write the function message table and suspended stacks.
        output.putInt(this.FUNCTIONMESSAGES.size());
        this.FUNCTIONMESSAGES.forEach(message -> message.write(output));
        this.SUSPENDEDSTACKS1.write(output);
        this.SUSPENDEDSTACKS2.write(output);

        // Write the "unknown" fields.
        output.putInt(this.UNK1);
        this.UNK2.ifPresent(v -> output.putInt(v));
        output.putInt(this.UNKS.size());
        this.UNKS.forEach(id -> id.write(output));

        // Write the unbind map.
        this.UNBINDMAP.write(output);

        // Write the save file version field, if present.
        this.SAVE_FILE_VERSION.ifPresent(v -> output.putShort(v));

        // Write the remaining data.
        output.put(this.ARRAYSBLOCK);

        if (((Buffer) output).position() != startingPosition + this.calculateSize()) {
            throw new IllegalStateException(String.format("Actual = %d, calculated = %d", output.position(), this.calculateSize()));
        }
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 2; // HEADER
        sum += this.getStringTable().calculateSize();
        sum += this.getScripts().calculateSize();
        sum += this.STRUCTS == null ? 0 : this.getStructs().calculateSize();
        sum += this.getScriptInstances().calculateSize();
        sum += this.STRUCT_INSTANCES == null ? 0 : this.getStructInstances().calculateSize();
        sum += this.getReferences().calculateSize();
        sum += this.getArrays().calculateSize();
        sum += this.PAPYRUS_RUNTIME == null ? 0 : this.PAPYRUS_RUNTIME.calculateSize();
        sum += this.getActiveScripts().calculateSize();
        sum += 4 + this.getFunctionMessages().parallelStream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.getSuspendedStacks1().calculateSize();
        sum += this.getSuspendedStacks2().calculateSize();
        sum += 4; // UNK1
        sum += this.UNK2 != null && this.UNK2.isPresent() ? 4 : 0;
        sum += 4 + this.getUnknownIDList().parallelStream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.getUnbinds().calculateSize();
        sum += this.SAVE_FILE_VERSION != null && this.SAVE_FILE_VERSION.isPresent() ? 2 : 0;
        sum += this.ARRAYSBLOCK == null ? 0 : this.ARRAYSBLOCK.length;
        return sum;
    }

    /**
     * @return The header field.
     */
    public short getHeader() {
        return this.HEADER;
    }

    /**
     * @return Returns a new <code>PapyrusContext</code>.
     */
    public PapyrusContext getContext() {
        return this.CONTEXT;
    }

    /**
     * @return Accessor for the string table.
     */
    public StringTable getStringTable() {
        return this.STRINGS == null ? new StringTable() : this.STRINGS;
    }

    /**
     * @return Accessor for the list of scripts.
     */
    public ScriptMap getScripts() {
        return this.SCRIPTS == null ? new ScriptMap() : this.SCRIPTS;
    }

    /**
     * @return Accessor for the list of structdefs.
     */
    public StructMap getStructs() {
        return this.STRUCTS == null ? new StructMap() : this.STRUCTS;
    }

    /**
     * @return Accessor for the list of script instances.
     */
    public ScriptInstanceMap getScriptInstances() {
        return this.SCRIPT_INSTANCES == null ? new ScriptInstanceMap() : this.SCRIPT_INSTANCES;
    }

    /**
     * @return Accessor for the list of references.
     */
    public ReferenceMap getReferences() {
        return this.REFERENCES == null ? new ReferenceMap() : this.REFERENCES;
    }

    /**
     * @return Accessor for the list of structs.
     */
    public StructInstanceMap getStructInstances() {
        return this.STRUCT_INSTANCES == null ? new StructInstanceMap() : this.STRUCT_INSTANCES;
    }

    /**
     * @return Accessor for the list of arrays.
     */
    public ArrayMap getArrays() {
        return this.ARRAYS == null ? new ArrayMap() : this.ARRAYS;
    }

    /**
     * @return Accessor for the list of active scripts.
     */
    public ActiveScriptMap getActiveScripts() {
        return this.ACTIVESCRIPTS == null ? new ActiveScriptMap() : this.ACTIVESCRIPTS;
    }

    /**
     * @return Accessor for the list of function messages.
     */
    public List<FunctionMessage> getFunctionMessages() {
        return this.FUNCTIONMESSAGES == null ? Collections.emptyList() : this.FUNCTIONMESSAGES;
    }

    /**
     * @return Accessor for the combined list of suspended stacks.
     */
    public SuspendedStackMap getSuspendedStacks() {
        SuspendedStackMap s1 = this.getSuspendedStacks1();
        SuspendedStackMap s2 = this.getSuspendedStacks2();
        SuspendedStackMap combined = new SuspendedStackMap();
        combined.putAll(s1);
        combined.putAll(s2);
        return combined;
    }

    /**
     * @return Accessor for the first list of suspended stacks.
     */
    public SuspendedStackMap getSuspendedStacks1() {
        return this.SUSPENDEDSTACKS1 == null ? new SuspendedStackMap() : this.SUSPENDEDSTACKS1;
    }

    /**
     * @return Accessor for the second list of suspended stacks.
     */
    public SuspendedStackMap getSuspendedStacks2() {
        return this.SUSPENDEDSTACKS2 == null ? new SuspendedStackMap() : this.SUSPENDEDSTACKS2;
    }

    /**
     * @return Accessor for the queued unbinds list.
     */
    public UnbindMap getUnbinds() {
        return this.UNBINDMAP == null ? new UnbindMap() : this.UNBINDMAP;
    }

    /**
     * @return
     */
    public List<EID> getUnknownIDList() {
        return null != this.UNKS ? this.UNKS : Collections.emptyList();
    }

    /**
     * @return Accessor the "other" data.
     */
    public OtherData getOtherData() {
        return this.OTHER;
    }

    /**
     * @return Accessor for the Arrays block.
     */
    public ByteBuffer getArraysBlock() {
        final ByteBuffer BUFFER = ByteBuffer.wrap(this.ARRAYSBLOCK);
        BUFFER.order(ByteOrder.LITTLE_ENDIAN);
        return BUFFER;
    }

    /**
     * Find a <code>DefinedElement</code> by its <code>EID</code>.
     * <code>DefinedElement</code> includes <code>ScriptInstance</code>,
     * <code>StructInstance</code>, and <code>Reference</code>.
     *
     * @param id
     * @return
     */
    public DefinedElement findReferrent(EID id) {
        if (this.getScriptInstances().containsKey(id)) {
            return this.getScriptInstances().get(id);
        } else if (this.getReferences().containsKey(id)) {
            return this.getReferences().get(id);
        } else if (this.getStructInstances().containsKey(id)) {
            return this.getStructInstances().get(id);
        } else if (id.isZero()) {
            return null;
        } else {
            return null;
        }
    }

    /**
     * Counts the number of unattached instances, the
     * <code>ScriptInstance</code> objects whose refID is 0.
     *
     * @return The number of unattached instances.
     */
    public int countUnattachedInstances() {
        return (int) this.getScriptInstances().values().stream().filter(v -> v.isUnattached()).count();
    }

    /**
     * Counts the number of <code>ScriptInstance</code>, <code>Reference</code>,
     * and <code>ActiveScript</code> objects whose script is null.
     *
     * @return The number of undefined elements / undefined threads.
     */
    public int[] countUndefinedElements() {
        int count = 0;
        int threads = 0;

        count += this.getScripts().values().stream().filter(v -> v.isUndefined()).count();
        count += this.getScriptInstances().values().parallelStream().filter(v -> v.isUndefined()).count();
        count += this.getReferences().values().stream().filter(v -> v.isUndefined()).count();
        count += this.getStructs().values().stream().filter(v -> v.isUndefined()).count();
        count += this.getStructInstances().values().stream().filter(v -> v.isUndefined()).count();
        threads += this.getActiveScripts().values().stream().filter(v -> v.isUndefined() && !v.isTerminated()).count();
        return new int[]{count, threads};
    }

    /**
     * Removes all <code>ScriptInstance</code> objects whose refID is 0.
     *
     * @return The elements that were removed.
     */
    public Set<PapyrusElement> removeUnattachedInstances() {
        final Set<ScriptInstance> UNATTACHED = this.getScriptInstances().values()
                .stream()
                .filter(v -> v.isUnattached())
                .collect(Collectors.toSet());

        return this.removeElements(UNATTACHED);
    }

    /**
     * Removes all <code>ScriptInstance</code> objects whose script is null.
     * Also checks <code>ActiveScript</code>, <code>FunctionMessage</code>, and
     * <code>SuspendedStack</code>.
     *
     * @return The elements that were removed.
     */
    public Set<PapyrusElement> removeUndefinedElements() {
        final java.util.Set<PapyrusElement> REMOVED = new java.util.HashSet<>();

        REMOVED.addAll(this.removeElements(this.getScripts().values().stream().filter(v -> v.isUndefined()).collect(Collectors.toSet())));
        REMOVED.addAll(this.removeElements(this.getStructs().values().stream().filter(v -> v.isUndefined()).collect(Collectors.toSet())));
        REMOVED.addAll(this.removeElements(this.getScriptInstances().values().stream().filter(v -> v.isUndefined()).collect(Collectors.toSet())));
        REMOVED.addAll(this.removeElements(this.getReferences().values().stream().filter(v -> v.isUndefined()).collect(Collectors.toSet())));
        REMOVED.addAll(this.removeElements(this.getStructInstances().values().stream().filter(v -> v.isUndefined()).collect(Collectors.toSet())));

        this.getActiveScripts().values().stream()
                .filter(v -> v.isUndefined() && !v.isTerminated())
                .forEach(v -> {
                    v.zero();
                    REMOVED.add(v);
                });

        return REMOVED;
    }

    /**
     * Removes all <code>ScriptInstance</code> objects whose script is null.
     * Also checks <code>ActiveScript</code>, <code>FunctionMessage</code>, and
     * <code>SuspendedStack</code>.
     *
     * @return The elements that were removed.
     */
    public Set<ActiveScript> terminateUndefinedThreads() {
        final Set<ActiveScript> TERMINATED = this.getActiveScripts().values().stream()
                .filter(v -> v.isUndefined() && !v.isTerminated())
                .collect(Collectors.toSet());

        TERMINATED.forEach(v -> v.zero());
        return TERMINATED;
    }

    /**
     * Removes a <code>PapyrusElement</code> collection.
     *
     * @param elements The elements to remove.
     * @return The elements that were removed.
     *
     */
    public java.util.Set<PapyrusElement> removeElements(java.util.Collection<? extends PapyrusElement> elements) {
        if (elements == null || elements.contains(null)) {
            throw new NullPointerException("The set of elements to remove must not be null and must not contain null.");
        }

        final java.util.LinkedList<PapyrusElement> ELEMENTS = new java.util.LinkedList<>(elements);
        final java.util.Set<PapyrusElement> REMOVED = new java.util.HashSet<>(ELEMENTS.size());

        while (!ELEMENTS.isEmpty()) {
            final PapyrusElement ELEMENT = ELEMENTS.pop();

            if (ELEMENT instanceof Script) {
                final Definition DEF = (Definition) ELEMENT;
                ELEMENTS.addAll(this.getScriptInstances().values().parallelStream().filter(v -> v.getDefinition() == DEF).collect(Collectors.toSet()));
                REMOVED.add(this.getScripts().remove(DEF.getName()));

            } else if (ELEMENT instanceof Struct) {
                final Struct DEF = (Struct) ELEMENT;
                ELEMENTS.addAll(this.getStructInstances().values().parallelStream().filter(v -> v.getDefinition() == DEF).collect(Collectors.toSet()));
                REMOVED.add(this.getStructs().remove(DEF.getName()));

            } else if (ELEMENT instanceof ScriptInstance) {
                final ScriptInstance INSTANCE = (ScriptInstance) ELEMENT;
                if (this.getScriptInstances().containsKey(INSTANCE.getID())) {
                    REMOVED.add(this.getScriptInstances().remove(INSTANCE.getID()));
                }

            } else if (ELEMENT instanceof StructInstance) {
                final StructInstance STRUCT = (StructInstance) ELEMENT;
                if (this.getStructInstances().containsKey(STRUCT.getID())) {
                    REMOVED.add(this.getStructInstances().remove(STRUCT.getID()));
                }

            } else if (ELEMENT instanceof Reference) {
                final Reference REF = (Reference) ELEMENT;
                if (this.getReferences().containsKey(REF.getID())) {
                    REMOVED.add(this.getReferences().remove(REF.getID()));
                }

            } else if (ELEMENT instanceof ArrayInfo) {
                final ArrayInfo ARRAY = (ArrayInfo) ELEMENT;
                if (this.getArrays().containsKey(ARRAY.getID())) {
                    REMOVED.add(this.getArrays().remove(ARRAY.getID()));
                }

            } else if (ELEMENT instanceof ActiveScript) {
                final ActiveScript ACTIVE = (ActiveScript) ELEMENT;
                if (this.getActiveScripts().containsKey(ACTIVE.getID())) {
                    REMOVED.add(this.getActiveScripts().remove(ACTIVE.getID()));
                }

            } else if (ELEMENT instanceof SuspendedStack) {
                final SuspendedStack STACK = (SuspendedStack) ELEMENT;
                if (this.getSuspendedStacks1().containsKey(STACK.getID())) {
                    REMOVED.add(this.getSuspendedStacks1().remove(STACK.getID()));
                } else if (this.getSuspendedStacks2().containsKey(STACK.getID())) {
                    REMOVED.add(this.getSuspendedStacks2().remove(STACK.getID()));
                }

            } else {
                LOG.warning(String.format("Papyrus.removeElements: can't delete element: %s", ELEMENT));
            }

        }

        REMOVED.remove(null);
        return REMOVED;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return "Papyrus-" + super.toString();
    }

    /**
     * Searches for all <code>Linkable</code> elements that refer to the
     * specified ID.
     *
     * @param id
     * @return
     */
    private ReferrentMap findMatches(EID id) {
        final ReferrentMap REFERRENTS = new ReferrentMap();

        REFERRENTS.put(ScriptInstance.class, this.getScriptInstances().values().stream()
                .filter(v -> !id.equals(v.getID()))
                .filter(v -> v.getData() != null)
                .filter(v -> v.getVariables().stream().anyMatch(m -> m.hasRef(id)))
                .collect(Collectors.toSet()));

        REFERRENTS.put(Reference.class, this.getReferences().values().stream()
                .filter(v -> !id.equals(v.getID()))
                .filter(v -> v.getVariables().stream().anyMatch(m -> m.hasRef(id)))
                .collect(Collectors.toSet()));

        REFERRENTS.put(ArrayInfo.class, this.getArrays().values().stream()
                .filter(v -> v.getVariables().stream().anyMatch(m -> m.hasRef(id)))
                .collect(Collectors.toSet()));

        REFERRENTS.put(ActiveScript.class, this.getActiveScripts().values().stream()
                .filter(v -> !id.equals(v.getID()))
                .filter(v -> v.getAttached() == id)
                .collect(Collectors.toSet()));

        REFERRENTS.put(ActiveScript.class, this.getActiveScripts().values().stream()
                .flatMap(t -> t.getStackFrames().stream())
                .filter(s -> s.getOwner().hasRef(id))
                .collect(Collectors.toSet()));

        REFERRENTS.put(StackFrame.class, this.getActiveScripts().values().stream()
                .flatMap(t -> t.getStackFrames().stream())
                .filter(s -> s.getVariables().stream().anyMatch(m -> m.hasRef(id)))
                .collect(Collectors.toSet()));

        REFERRENTS.put(StructInstance.class, this.getStructInstances().values().stream()
                .filter(v -> !id.equals(v.getID()))
                .filter(v -> v.getVariables().stream().anyMatch(m -> m.hasRef(id)))
                .collect(Collectors.toSet()));

        return REFERRENTS;
    }

    /**
     * Searches for all <code>Linkable</code> elements that refer to the
     * specified ID and prints them to a <code>StringBuilder</code>.
     *
     * @param ref
     * @param builder
     * @param myName
     */
    public void printReferrents(DefinedElement ref, StringBuilder builder, String myName) {
        final ReferrentMap REFERENTS = this.findMatches(ref.getID());
        referrentsPrint(ref, builder, REFERENTS.get(ActiveScript.class), myName, "threads", "attached to");
        referrentsPrint(ref, builder, REFERENTS.get(StackFrame.class), myName, "stackframes", "with member data referring to");
        referrentsPrint(ref, builder, REFERENTS.get(ScriptInstance.class), myName, "instances", "with member data referring to");
        referrentsPrint(ref, builder, REFERENTS.get(Reference.class), myName, "references", "with member data referring to");
        referrentsPrint(ref, builder, REFERENTS.get(ArrayInfo.class), myName, "arrays", "with member data referring to");
        referrentsPrint(ref, builder, REFERENTS.get(StructInstance.class), myName, "structs", "with member data referring to");
    }

    /**
     * Helper for printReferrents.
     *
     * @param builder
     * @param ls
     * @param myname
     * @param lname
     * @param relationship
     */
    static private void referrentsPrint(Element ref, StringBuilder builder, Set<Linkable> ls, String myname, String lname, String relationship) {
        if (null == ls) {
            return;
        }

        if (ls.isEmpty()) {
            builder.append(String.format("<p>There are zero %s %s this %s.</p>", lname, relationship, myname));
        } else if (ls.size() == 1) {
            builder.append(String.format("<p>There is one %s %s this %s.</p>", lname, relationship, myname));
        } else {
            builder.append(String.format("<p>There are %s %s %s this %s.</p>", ls.size(), lname, relationship, myname));
        }

        if (!ls.isEmpty() && ls.size() < 50) {
            builder.append("<ul>");
            ls.forEach(i -> builder.append("<li>").append(i.toHTML(ref)).append("</a>"));
            builder.append("</ul>");
        }

    }

    /**
     * @return A flag indicating if the papyrus block has a truncation error.
     */
    public boolean isBroken() {
        return this.truncated || this.getStringTable().isTruncated();
    }

    /**
     * @return A flag indicating if the Papyrus block has a truncation error.
     */
    public boolean isTruncated() {
        return this.truncated;
    }

    private boolean truncated = false;
    final private PapyrusContext CONTEXT;
    final private short HEADER;
    final private EID PAPYRUS_RUNTIME;
    final private Optional<Short> SAVE_FILE_VERSION;
    final private int UNK1;
    final private Optional<Integer> UNK2;
    final private StringTable STRINGS;
    final private ScriptMap SCRIPTS;
    final private StructMap STRUCTS;
    final private ScriptInstanceMap SCRIPT_INSTANCES;
    final private ReferenceMap REFERENCES;
    final private StructInstanceMap STRUCT_INSTANCES;
    final private ArrayMap ARRAYS;
    final private ActiveScriptMap ACTIVESCRIPTS;
    final private List<FunctionMessage> FUNCTIONMESSAGES;
    final private SuspendedStackMap SUSPENDEDSTACKS1;
    final private SuspendedStackMap SUSPENDEDSTACKS2;
    final private UnbindMap UNBINDMAP;
    final private List<EID> UNKS;
    final private OtherData OTHER;
    final private byte[] ARRAYSBLOCK;
    final java.util.Map<Number, EID> EIDS;
    static final private Logger LOG = Logger.getLogger(Papyrus.class.getCanonicalName());

}
