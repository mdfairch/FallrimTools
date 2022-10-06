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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import resaver.Game;

/**
 * Describes the table of file locations.
 *
 * @author Mark Fairchild
 */
public final class FileLocationTable implements Element {

    /**
     * Creates a new <code>FileLocationTable</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param game Specifies which format to use.
     * @throws IOException
     */
    public FileLocationTable(ByteBuffer input, Game game) throws IOException {
        this.GAME = Objects.requireNonNull(game);
        this.formIDArrayCountOffset = input.getInt();
        this.unknownTable3Offset = input.getInt();
        this.table1Offset = input.getInt();
        this.table2Offset = input.getInt();
        this.changeFormsOffset = input.getInt();
        this.table3Offset = input.getInt();
        this.TABLE1COUNT = input.getInt();
        this.TABLE2COUNT = input.getInt();
        int count = input.getInt();
        this.TABLE3COUNT = (this.GAME.isSkyrim() ? count + 1 : count);
        this.changeFormCount = input.getInt();
        this.UNUSED = new int[15];
        for (int i = 0; i < 15; i++) {
            this.UNUSED[i] = input.getInt();
        }
        this.t1size = table2Offset - table1Offset;
        this.t2size = changeFormsOffset - table2Offset;
        this.t3size = table3Offset - changeFormsOffset;
    }

    /**
     * Creates a new <code>FileLocationTable</code> by analyzing an
     * <code>ESS</code>.
     *
     * @param ess The <code>ESS</code> to rebuild for.
     */
    public FileLocationTable(ESS ess) {
        this.GAME = ess.getHeader().GAME;
        this.TABLE1COUNT = ess.getTable1().size();
        this.TABLE2COUNT = ess.getTable2().size();
        this.TABLE3COUNT = ess.getTable3().size();
        this.UNUSED = new int[15];
        Arrays.fill(this.UNUSED, 0);

        int table1Size = ess.getTable1().stream().mapToInt(v -> v.calculateSize()).sum();
        int table2Size = ess.getTable2().stream().mapToInt(v -> v.calculateSize()).sum();
        int table3Size = ess.getTable3().stream().mapToInt(v -> v.calculateSize()).sum();
        int changeFormsSize = ess.getChangeForms().parallelStream().mapToInt(v -> v.calculateSize()).sum();

        this.table1Offset = 0;
        this.table1Offset += ess.getHeader().calculateSize();
        this.table1Offset += 1;
        this.table1Offset += ess.getPluginInfo().calculateSize();
        this.table1Offset += this.calculateSize();
        if (null != ess.getVersionString()) {
            this.table1Offset += ess.getVersionString().length() + 2;
        }

        this.table2Offset = this.table1Offset + table1Size;
        this.changeFormCount = ess.getChangeForms().size();
        this.changeFormsOffset = this.table2Offset + table2Size;
        this.table3Offset = this.changeFormsOffset + changeFormsSize;
        this.formIDArrayCountOffset = this.table3Offset + table3Size;
        this.unknownTable3Offset = 0;
        this.unknownTable3Offset += this.formIDArrayCountOffset;
        this.unknownTable3Offset += 4 + 4 * ess.getFormIDs().length;
        this.unknownTable3Offset += 4 + 4 * ess.getVisitedWorldspaceArray().length;
    }

    /**
     * Rebuilds the file location table for an <code>ESS</code>.
     *
     * @param ess The <code>ESS</code> to rebuild for.
     */
    public void rebuild(ESS ess) {
        int table1Size = ess.getTable1().stream().mapToInt(v -> v.calculateSize()).sum();
        int table2Size = ess.getTable2().stream().mapToInt(v -> v.calculateSize()).sum();
        int table3Size = ess.getTable3().stream().mapToInt(v -> v.calculateSize()).sum();
        int changeFormsSize = ess.getChangeForms().parallelStream().mapToInt(v -> v.calculateSize()).sum();
        
        this.table1Offset = 0;
        this.table1Offset += ess.getHeader().calculateSize();
        this.table1Offset += 1;
        this.table1Offset += ess.getPluginInfo().calculateSize();
        this.table1Offset += this.calculateSize();
        if (null != ess.getVersionString()) {
            this.table1Offset += ess.getVersionString().length() + 2;
        }

        this.table2Offset = this.table1Offset + table1Size;
        this.changeFormCount = ess.getChangeForms().size();
        this.changeFormsOffset = this.table2Offset + table2Size;
        this.table3Offset = this.changeFormsOffset + changeFormsSize;
        this.formIDArrayCountOffset = this.table3Offset + table3Size;
        this.unknownTable3Offset = 0;
        this.unknownTable3Offset += this.formIDArrayCountOffset;
        this.unknownTable3Offset += 4 + 4 * ess.getFormIDs().length;
        this.unknownTable3Offset += 4 + 4 * ess.getVisitedWorldspaceArray().length;
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.formIDArrayCountOffset);
        output.putInt(this.unknownTable3Offset);
        output.putInt(this.table1Offset);
        output.putInt(this.table2Offset);
        output.putInt(this.changeFormsOffset);
        output.putInt(this.table3Offset);
        output.putInt(this.TABLE1COUNT);
        output.putInt(this.TABLE2COUNT);
        output.putInt(this.GAME.isSkyrim() ? this.TABLE3COUNT - 1 : this.TABLE3COUNT);
        output.putInt(this.changeFormCount);
        for (int i = 0; i < this.UNUSED.length; i++) {
            output.putInt(this.UNUSED[i]);
        }
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 100;
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + this.formIDArrayCountOffset;
        hash = 73 * hash + this.unknownTable3Offset;
        hash = 73 * hash + this.table1Offset;
        hash = 73 * hash + this.table2Offset;
        hash = 73 * hash + this.changeFormsOffset;
        hash = 73 * hash + this.table3Offset;
        hash = 73 * hash + this.TABLE1COUNT;
        hash = 73 * hash + this.TABLE2COUNT;
        hash = 73 * hash + this.TABLE3COUNT;
        hash = 73 * hash + this.changeFormCount;
        hash = 73 * hash + Arrays.hashCode(this.UNUSED);
        return hash;
    }

    /**
     * @see Object#equals(java.lang.Object)
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final FileLocationTable other = (FileLocationTable) obj;
        if (this.formIDArrayCountOffset != other.formIDArrayCountOffset) {
            return false;
        } else if (this.unknownTable3Offset != other.unknownTable3Offset) {
            return false;
        } else if (this.table1Offset != other.table1Offset) {
            return false;
        } else if (this.table2Offset != other.table2Offset) {
            return false;
        } else if (this.changeFormsOffset != other.changeFormsOffset) {
            return false;
        } else if (this.table3Offset != other.table3Offset) {
            return false;
        } else if (this.TABLE1COUNT != other.TABLE1COUNT) {
            return false;
        } else if (this.TABLE2COUNT != other.TABLE2COUNT) {
            return false;
        } else if (this.TABLE3COUNT != other.TABLE3COUNT) {
            return false;
        } else if (this.changeFormCount != other.changeFormCount) {
            return false;
        } else {
            return Objects.deepEquals(this.UNUSED, other.UNUSED);
        }
    }
    int t1size;
    int t2size;
    int t3size;
    int formIDArrayCountOffset;
    int unknownTable3Offset;
    int table1Offset;
    int table2Offset;
    int changeFormsOffset;
    int table3Offset;
    final int TABLE1COUNT;
    final int TABLE2COUNT;
    final int TABLE3COUNT;
    int changeFormCount;
    final int[] UNUSED;
    final Game GAME;

}
