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
package resaver.esp;

import java.nio.ByteBuffer;
import java.util.List;
import resaver.IString;

/**
 * FieldVMAD represents all a VMAD field.
 *
 * @author Mark Fairchild
 */
public class FieldVMAD implements Field {

    /**
     * Creates a new FieldVMAD by reading it from a LittleEndianInput.
     *
     * @param recordCode The record code.
     * @param fieldCode The field code, which must be "VMAD".
     * @param input The <code>ByteBuffer</code> to read.
     * @param big A flag indicating that this is a BIG field.
     * @param ctx
     */
    public FieldVMAD(RecordCode recordCode, IString fieldCode, ByteBuffer input, boolean big, ESPContext ctx) {
        assert input.hasRemaining();
        assert fieldCode.equals(IString.get("VMAD"));

        this.RECORDCODE = recordCode;
        this.CODE = fieldCode;
        this.VERSION = input.getShort();
        this.OBJFORMAT = input.getShort();
        this.SCRIPTS = new java.util.ArrayList<>(1);
        this.FRAGMENTS = new java.util.ArrayList<>(1);
        this.BIG = big;

        int scriptCount = Short.toUnsignedInt(input.getShort());

        for (int i = 0; i < scriptCount; i++) {
            Script script = new Script(input, ctx);
            ctx.PLUGIN_INFO.addScriptData(script);
        }

        int i = 0;
        while (input.hasRemaining()) {
            switch (recordCode) {
                case INFO:
                case PACK:
                    this.FRAGMENTS.add(new FragmentInfoPack(input, ctx));
                    break;
                case PERK:
                    this.FRAGMENTS.add(new FragmentPerk(input, ctx));
                    break;
                case QUST:
                    this.FRAGMENTS.add(new FragmentQust(input, ctx));
                    break;
                case SCEN:
                    this.FRAGMENTS.add(new FragmentScen(input, ctx));
                    break;
                case TERM:
                    this.FRAGMENTS.add(new FragmentTerm(input, ctx));
                    break;
                default:
                    throw new IllegalStateException("Unexpected fragment type: " + recordCode);
            }
            i++;
        }
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.CODE.getUTF8());

        output.putShort((short) (this.BIG ? 0 : this.calculateSize() - 6));
        output.putShort(this.VERSION);
        output.putShort(this.OBJFORMAT);
        output.putShort((short) this.SCRIPTS.size());
        this.SCRIPTS.forEach(script -> script.write(output));
        this.FRAGMENTS.forEach(fragment -> fragment.write(output));
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        int sum = 12;
        sum += this.SCRIPTS.stream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.FRAGMENTS.stream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
    }

    /**
     * Returns the field code.
     *
     * @return The field code.
     */
    @Override
    public IString getCode() {
        return this.CODE;
    }

    /**
     * Returns a String representation of the Field, which will just be the code
     * string.
     *
     * @return A string representation.
     *
     */
    @Override
    public String toString() {
        return this.getCode().toString();
    }

    final private RecordCode RECORDCODE;
    final private IString CODE;
    final private short VERSION;
    final private short OBJFORMAT;
    final private List<Script> SCRIPTS;
    final private List<FragmentBase> FRAGMENTS;
    final boolean BIG;

}
