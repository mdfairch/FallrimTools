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
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.List;
import resaver.IString;

/**
 * Describes script fragments for QUST records.
 *
 * @author Mark Fairchild
 */
public class FragmentScen extends FragmentBase {

    public FragmentScen(ByteBuffer input, ESPContext ctx) {
        this.UNKNOWN = input.get();
        this.FLAGS = input.get();

        if (ctx.GAME.isFO4()) {
            ctx.pushContext("FragmentScene");
            this.FILENAME = null;
            this.SCRIPT = new Script(input, ctx);
            ctx.PLUGIN_INFO.addScriptData(this.SCRIPT);
        } else {
            this.FILENAME = mf.BufferUtil.getUTF(input);
            this.SCRIPT = null;
            ctx.pushContext("FragmentScene:" + this.FILENAME);
        }

        this.FRAGMENTS = new java.util.ArrayList<>(1);
        this.PHASES = new java.util.ArrayList<>(1);

        int flagCount = FragmentBase.NumberOfSetBits(this.FLAGS);
        for (int i = 0; i < flagCount; i++) {
            Fragment fragment = new Fragment(input);
            this.FRAGMENTS.add(fragment);
        }

        int phaseCount = Short.toUnsignedInt(input.getShort());
        for (int i = 0; i < phaseCount; i++) {
            Phase phase = new Phase(input);
            this.PHASES.add(phase);
        }
    }

    @Override
    public void write(ByteBuffer output) {
        output.put(this.UNKNOWN);
        output.put(this.FLAGS);
        if (null != this.SCRIPT) {
            this.SCRIPT.write(output);
        }
        if (null != this.FILENAME) {
            output.put(this.FILENAME.getBytes(UTF_8));
        }

        this.FRAGMENTS.forEach(fragment -> fragment.write(output));
        output.putShort((short) this.PHASES.size());
        this.PHASES.forEach(phase -> phase.write(output));
    }

    @Override
    public int calculateSize() {
        int sum = 4;
        sum += (null != this.SCRIPT ? this.SCRIPT.calculateSize() : 0);
        sum += (null != this.FILENAME ? 2 + this.FILENAME.length() : 0);
        sum += this.FRAGMENTS.stream().mapToInt(v -> v.calculateSize()).sum();
        sum += this.PHASES.stream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
    }

    @Override
    public String toString() {
        if (null != this.SCRIPT) {
            return String.format("Scene: %s (%d, %d, %d frags, %d phases)", this.SCRIPT.NAME, this.FLAGS, this.UNKNOWN, this.FRAGMENTS.size(), this.PHASES.size());
        } else if (null != this.FILENAME) {
            return String.format("Scene: %s (%d, %d, %d frags, %d phases)", this.FILENAME, this.FLAGS, this.UNKNOWN, this.FRAGMENTS.size(), this.PHASES.size());
        } else {
            return String.format("Scene: (%d, %d, %d frags, %d phases)", this.FLAGS, this.UNKNOWN, this.FRAGMENTS.size(), this.PHASES.size());
        }
    }

    final private byte UNKNOWN;
    final private byte FLAGS;
    final private Script SCRIPT;
    final private String FILENAME;
    final private List<Fragment> FRAGMENTS;
    final private List<Phase> PHASES;

    /**
     *
     */
    public class Fragment implements Entry {

        public Fragment(ByteBuffer input) {
            this.UNKNOWN = input.get();
            this.SCRIPTNAME = IString.get(mf.BufferUtil.getUTF(input));
            this.FRAGMENTNAME = IString.get(mf.BufferUtil.getUTF(input));
        }

        @Override
        public void write(ByteBuffer output) {
            output.put(this.UNKNOWN);
            output.put(this.SCRIPTNAME.getUTF8());
            output.put(this.FRAGMENTNAME.getUTF8());
        }

        @Override
        public int calculateSize() {
            return 5 + this.SCRIPTNAME.length() + this.FRAGMENTNAME.length();
        }

        @Override
        public String toString() {
            return String.format("Frag %d %s[%s]", this.UNKNOWN, this.SCRIPTNAME, this.FRAGMENTNAME);
        }

        final private byte UNKNOWN;
        final private IString SCRIPTNAME;
        final private IString FRAGMENTNAME;
    }

    /**
     *
     */
    public class Phase implements Entry {

        public Phase(ByteBuffer input) {
            this.UNKNOWN1 = input.get();
            this.PHASE = input.getInt();
            this.UNKNOWN2 = input.get();
            this.SCRIPTNAME = IString.get(mf.BufferUtil.getUTF(input));
            this.FRAGMENTNAME = IString.get(mf.BufferUtil.getUTF(input));
        }

        @Override
        public void write(ByteBuffer output) {
            output.put(this.UNKNOWN1);
            output.putInt(this.PHASE);
            output.put(this.UNKNOWN2);
            output.put(this.SCRIPTNAME.getUTF8());
            output.put(this.FRAGMENTNAME.getUTF8());
        }

        @Override
        public int calculateSize() {
            return 10 + this.SCRIPTNAME.length() + this.FRAGMENTNAME.length();
        }

        @Override
        public String toString() {
            return String.format("Phase %d.%d.%d %s[%s]", this.PHASE, this.UNKNOWN1, this.UNKNOWN2, this.SCRIPTNAME, this.FRAGMENTNAME);
        }

        final private byte UNKNOWN1;
        final private int PHASE;
        final private byte UNKNOWN2;
        final private IString SCRIPTNAME;
        final private IString FRAGMENTNAME;
    }
}
