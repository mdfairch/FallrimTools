/*
 * Copyright 2020 Mark.
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
import java.util.Objects;
import java.util.stream.Stream;
import resaver.ess.Linkable;

/**
 *
 * @author Mark
 */
public class PapyrusContext extends resaver.ess.ESS.ESSContext {

    /**
     * Creates a new <code>PapyrusContext</code> from an existing
     * <code>ESSContext</code> and an instance of <code>Papyrus</code>.
     *
     * @param context
     * @param papyrus
     */
    public PapyrusContext(resaver.ess.ESS.ESSContext context, Papyrus papyrus) {
        super(context);
        this.PAPYRUS = Objects.requireNonNull(papyrus);
    }

    /**
     * Creates a new <code>PapyrusContext</code> from an existing
     * <code>PapyrusContext</code>.
     *
     * @param context
     */
    public PapyrusContext(PapyrusContext context) {
        super(context);
        this.PAPYRUS = Objects.requireNonNull(context.PAPYRUS);
    }

    /**
     * Reads an <code>EID</code> from a <code>ByteBuffer</code>. The size of the
     * <code>EID</code> is determined from the <code>ID64</code> flag of the
     * <code>Game</code> field of the relevant <code>ESS</code>.
     *
     * @param input The input stream.
     * @return The <code>EID</code>.
     */
    public EID readEID(ByteBuffer input) {
        return this.getGame().isID64()
                ? this.readEID64(input)
                : this.readEID32(input);
    }

    /**
     * Makes an <code>EID</code> from a <code>long</code>. The actual size of
     * the <code>EID</code> is determined from the <code>ID64</code> flag of the
     * <code>Game</code> field of the relevant <code>ESS</code>.
     *
     * @param val The id value.
     * @return The <code>EID</code>.
     */
    public EID makeEID(Number val) {
        return this.getGame().isID64()
                ? this.makeEID64(val.longValue())
                : this.makeEID32(val.intValue());
    }

    /**
     * Reads a four-byte <code>EID</code> from a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @return The <code>EID</code>.
     */
    public EID readEID32(ByteBuffer input) {
        return EID.read4byte(input, this.PAPYRUS);
    }

    /**
     * Reads an eight-byte <code>EID</code> from a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @return The <code>EID</code>.
     */
    public EID readEID64(ByteBuffer input) {
        return EID.read8byte(input, this.PAPYRUS);
    }

    /**
     * Makes a four-byte <code>EID</code> from an int.
     *
     * @param val The id value.
     * @return The <code>EID</code>.
     */
    public EID makeEID32(int val) {
        return EID.make4byte(val, this.PAPYRUS);
    }

    /**
     * Makes an eight-byte <code>EID</code> from a long.
     *
     * @param val The id value.
     * @return The <code>EID</code>.
     */
    public EID makeEID64(long val) {
        return EID.make8Byte(val, this.PAPYRUS);
    }

    /**
     * Shortcut for getStringTable().readRefID(input)
     *
     * @param input The input stream.
     * @return The new <code>TString</code>.
     * @throws PapyrusFormatException
     */
    public TString readTString(ByteBuffer input) throws PapyrusFormatException {
        return this.PAPYRUS.getStringTable().read(input);
    }

    /**
     * Shortcut for getStringTable().add(s)
     *
     * @param s The new <code>String</code>.
     * @return The new <code>TString</code>.
     */
    public TString addTString(String s) {
        return this.PAPYRUS.getStringTable().addString(s);
    }

    /**
     * Shortcut for getStringTable().get(s)
     *
     * @param index The index of the <code>TString</code>.
     * @return The <code>TString</code>.
     */
    public TString getTString(int index) {
        return this.PAPYRUS.getStringTable().get(index);
    }

    /**
     * Does a very general search for an ID.
     *
     * @param number The data to search for.
     * @return Any match of any kind.
     */
    @Override
    public Linkable broadSpectrumSearch(Number number) {
        HasID r1 = this.findAny(this.makeEID32(number.intValue()));
        if (r1 != null) {
            return r1;
        }

        HasID r2 = this.findAny(this.makeEID64(number.longValue()));
        if (r2 != null) {
            return r2;
        }

        Linkable r3 = super.broadSpectrumSearch(number);
        if (r3 != null) {
            return r3;
        }

        if (number.intValue() >= 0 && number.intValue() < this.PAPYRUS.getStringTable().size()) {
            TString s = this.PAPYRUS.getStringTable().get(number.intValue());
            Linkable r4 = this.findAny(s);
            if (r4 != null) {
                return r4;
            }
        }

        return null;
    }

    /**
     * Search for anything that has the specified name.
     * @param id
     * @return 
     */
    public Definition findAny(TString name) {
        return Stream.of(
                this.PAPYRUS.getScripts(),
                this.PAPYRUS.getStructs())
                .filter(c -> c.containsKey(name))
                .map(c -> c.get(name))
                .findAny().orElse(null);
    }

    /**
     * Search for anything that has the specified ID.
     * @param id
     * @return 
     */
    public HasID findAny(EID id) {
        if (this.PAPYRUS.getScriptInstances().containsKey(id)) {
            return this.PAPYRUS.getScriptInstances().get(id);
        } else if (this.PAPYRUS.getStructInstances().containsKey(id)) {
            return this.PAPYRUS.getStructInstances().get(id);
        } else if (this.PAPYRUS.getReferences().containsKey(id)) {
            return this.PAPYRUS.getReferences().get(id);
        } else if (this.PAPYRUS.getArrays().containsKey(id)) {
            return this.PAPYRUS.getArrays().get(id);
        } else if (this.PAPYRUS.getActiveScripts().containsKey(id)) {
            return this.PAPYRUS.getActiveScripts().get(id);
        } else if (this.PAPYRUS.getSuspendedStacks1().containsKey(id)) {
            return this.PAPYRUS.getSuspendedStacks1().get(id);
        } else if (this.PAPYRUS.getSuspendedStacks2().containsKey(id)) {
            return this.PAPYRUS.getSuspendedStacks2().get(id);
        } else if (this.PAPYRUS.getUnbinds().containsKey(id)) {
            return this.PAPYRUS.getUnbinds().get(id);
        } else {
            return null;
        }
    }

    public HasID findAll(EID id) {
        return Stream.of(this.PAPYRUS.getScriptInstances(),
                this.PAPYRUS.getStructInstances(),
                this.PAPYRUS.getReferences(),
                this.PAPYRUS.getArrays(),
                this.PAPYRUS.getActiveScripts(),
                this.PAPYRUS.getSuspendedStacks1(),
                this.PAPYRUS.getSuspendedStacks2(),
                this.PAPYRUS.getUnbinds())
                .filter(c -> c.containsKey(id))
                .map(c -> c.get(id))
                .findAny().orElse(null);
    }

    public Script findScript(TString name) {
        return this.PAPYRUS.getScripts().getOrDefault(name, null);
    }

    public Struct findStruct(TString name) {
        return this.PAPYRUS.getStructs().getOrDefault(name, null);
    }

    public ScriptInstance findScriptInstance(EID id) {
        return this.PAPYRUS.getScriptInstances().getOrDefault(id, null);
    }

    public StructInstance findStructInstance(EID id) {
        return this.PAPYRUS.getStructInstances().getOrDefault(id, null);
    }

    public Reference findReference(EID id) {
        return this.PAPYRUS.getReferences().getOrDefault(id, null);
    }

    public ArrayInfo findArray(EID id) {
        return this.PAPYRUS.getArrays().getOrDefault(id, null);
    }

    public ActiveScript findActiveScript(EID id) {
        return this.PAPYRUS.getActiveScripts().getOrDefault(id, null);
    }

    public GameElement findReferrent(EID id) {
        return this.PAPYRUS.findReferrent(id);
    }

    /**
     * @return The <code>Papyrus</code> itself. May not be full constructed.
     */
    protected Papyrus getPapyrus() {
        return this.PAPYRUS;
    }

    final private Papyrus PAPYRUS;

}
