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
package resaver.ess;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import static resaver.ResaverFormatting.zeroPad6;
import static resaver.ResaverFormatting.zeroPad8;

/**
 * Describes 3-byte formIDs from Skyrim savegames.
 *
 * @author Mark Fairchild
 */
final public class RefID implements Element, Linkable, Comparable<RefID> {

    /**
     * Creates a new <code>RefID</code> directly.
     *
     * @param newData
     * @param ess The savefile for context.
     */
    RefID(int newData, ESS ess) {
        this.DATA = newData;

        if (this.isZero()) {
            this.FORMID = 0;
            this.PLUGIN = null;
        } else {
            final PluginInfo PLUGINS = ess.getPluginInfo();

            switch (this.getType()) {
                case DEFAULT:
                    this.PLUGIN = PLUGINS.getFullPlugins().get(0);
                    this.FORMID = this.getValPart();
                    break;
                case CREATED:
                    this.FORMID = 0xFF000000 | this.getValPart();
                    this.PLUGIN = PluginInfo.Created;
                    break;
                case FORMIDX:
                    assert this.getValPart() > 0 : "Invalid form index: " + this.getValPart();
                    final int FORM_INDEX = this.getValPart() - 1;

                    if (FORM_INDEX < ess.getFormIDs().length) {
                        this.FORMID = ess.getFormIDs()[FORM_INDEX];
                        this.PLUGIN = ess.getPluginFor(this.FORMID);
                    } else {
                        this.FORMID = -1;
                        this.PLUGIN = null;
                    }
                    break;
                default:
                    this.FORMID = 0;
                    this.PLUGIN = null;
                    break;
            }
        }

        this.name = ess.getAnalysis() == null ? null : ess.getAnalysis().getName(this.PLUGIN, this.FORMID);
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put((byte) (this.DATA >> 16));
        output.put((byte) (this.DATA >> 8));
        output.put((byte) (this.DATA >> 0));
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 3;
    }

    /**
     * @return The type of RefID.
     */
    public Type getType() {
        int index = 0x3 & this.DATA >>> 22;
        switch (index) {
            case 0:
                return Type.FORMIDX;
            case 1:
                return Type.DEFAULT;
            case 2:
                return Type.CREATED;
            case 3:
            default:
                return Type.INVALID;
        }
    }

    /**
     * @return The value portion of the RefID, which is guaranteed to be non-negative.
     */
    private int getValPart() {
        return this.DATA & 0x3FFFFF;
    }

    /**
     * @return The name field, if any.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(this.name);
    }

    /**
     * @return A flag indicating if the RefID is zero.
     */
    public boolean isZero() {
        return this.getValPart() == 0;
    }

    /**
     * @return A flag indicating that the RefID is not zero and is not of 
     * type INVALID.
     */
    public boolean isValid() {
        return this.getType() != Type.INVALID && !this.isZero();
    }
    
    /**
     * Adds the EDID/FULL field for the RefID.
     *
     * @param analysis The analysis data.
     */
    public void addNames(resaver.Analysis analysis) {
        this.name = analysis.getName(this.PLUGIN, this.FORMID);
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        if (this.FORMID == 0) {
            return this.getType() + ":" + zeroPad6(this.DATA);
        } else if (null != this.name) {
            return zeroPad8(this.FORMID) + " (" + this.name + ")";
        } else {
            return zeroPad8(this.FORMID);
        }
    }

    /**
     * @return The hexadecimal representation of the refID.
     */
    public String toRaw() {
        return String.format("%06x", this.DATA);
    }
    
    /**
     * @return The XEdit-style plugin:formid representation.
     */
    public String toHex() {
        final StringBuilder BUF = new StringBuilder();
        
        if (PLUGIN != null) {
            BUF.append(this.PLUGIN.NAME).append(":");
        }
        
        if (FORMID != 0) {
            BUF.append(zeroPad8(this.FORMID));
        } else {
            BUF.append(zeroPad6(this.DATA));
        }
        
        return BUF.toString();
    }
    
    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        return Linkable.makeLink("refid", this.toRaw(), this.toString());
    }

    @Override
    public int compareTo(RefID other) {
        Objects.requireNonNull(other);
        return Integer.compareUnsigned(this.DATA, other.DATA);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.DATA);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else {
            return obj instanceof RefID && ((RefID) obj).DATA == this.DATA;
        }
    }

    public boolean equals(int other) {
        if (this.FORMID != 0) {
            return this.FORMID == other;
        } else {
            return this.DATA == other;
        }
    }

    final private int DATA;
    final public int FORMID;
    final public Plugin PLUGIN;
    private String name;

    /**
     * The four types of RefIDs.
     */
    public enum Type {
        FORMIDX, DEFAULT, CREATED, INVALID;
    }

}
