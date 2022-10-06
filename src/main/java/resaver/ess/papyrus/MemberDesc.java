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
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Describes a script member in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
public class MemberDesc implements PapyrusElement, Comparable<MemberDesc> {

    /**
     * Creates a new <code>List</code> of <code>Variable</code> by reading from
     * a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @param count The number of variables.
     * @param context The <code>PapyrusContext</code> info.
     * @return The new <code>List</code> of <code>MemberDesc</code>.
     * @throws ListException
     */
    static public java.util.List<MemberDesc> readList(ByteBuffer input, int count, PapyrusContext context) throws ListException {
        final java.util.List<MemberDesc> DESCS = new java.util.ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            try {
                MemberDesc desc = new MemberDesc(input, context);
                DESCS.add(desc);
            } catch (PapyrusFormatException ex) {
                throw new ListException(i, count, ex);
            }
        }

        return DESCS;
    }

    /**
     * Creates a new <code>MemberData</code> by reading from a
     * <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     */
    public MemberDesc(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);
        this.NAME = context.readTString(input);
        this.TYPE = context.readTString(input);
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);
        this.NAME.write(output);
        this.TYPE.write(output);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return this.NAME.calculateSize() + this.TYPE.calculateSize();
    }

    /**
     * @return The ID of the papyrus element.
     */
    public TString getName() {
        return this.NAME;
    }

    /**
     * @return The type of the array.
     */
    public TString getType() {
        return this.TYPE;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return this.TYPE + " " + this.NAME;
    }

    @Override
    public int compareTo(MemberDesc o) {
        Objects.requireNonNull(o);
        return TString.compare(this.NAME, o.NAME);
    }

    final private TString NAME;
    final private TString TYPE;

}
