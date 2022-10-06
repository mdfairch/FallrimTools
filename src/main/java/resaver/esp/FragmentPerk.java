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
 * Describes script fragments for PERK records.
 *
 * @author Mark Fairchild
 */
public class FragmentPerk extends FragmentBase {

    public FragmentPerk(ByteBuffer input, ESPContext ctx) {
        try {
            this.UNKNOWN = input.get();

            if (ctx.GAME.isFO4()) {
                ctx.pushContext("FragmentPerk");
                this.FILENAME = null;
                this.SCRIPT = new Script(input, ctx);
                ctx.PLUGIN_INFO.addScriptData(this.SCRIPT);
            } else {
                this.FILENAME = mf.BufferUtil.getUTF(input);
                this.SCRIPT = null;
                ctx.pushContext("FragmentPerk:" + this.FILENAME);
            }

            int fragmentCount = Short.toUnsignedInt(input.getShort());
            this.FRAGMENTS = new java.util.ArrayList<>(fragmentCount);
            for (int i = 0; i < fragmentCount; i++) {
                Fragment fragment = new Fragment(input);
                this.FRAGMENTS.add(fragment);
            }

        } finally {
            ctx.popContext();
        }
    }

    @Override
    public void write(ByteBuffer output) {
        output.put(this.UNKNOWN);
        if (null != this.SCRIPT) {
            this.SCRIPT.write(output);
        }
        if (null != this.FILENAME) {
            output.put(this.FILENAME.getBytes(UTF_8));
        }

        output.putShort((short) this.FRAGMENTS.size());
        this.FRAGMENTS.forEach(fragment -> fragment.write(output));
    }

    @Override
    public int calculateSize() {
        int sum = 3;
        sum += (null != this.SCRIPT ? this.SCRIPT.calculateSize() : 0);
        sum += (null != this.FILENAME ? 2 + this.FILENAME.length() : 0);
        sum += this.FRAGMENTS.stream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
    }

    @Override
    public String toString() {
        if (null != this.SCRIPT) {
            return String.format("Perk: %s (%d, %d frags)", this.SCRIPT.NAME, this.UNKNOWN, this.FRAGMENTS.size());
        } else if (null != this.FILENAME) {
            return String.format("Perk: %s (%d, %d frags)", this.FILENAME, this.UNKNOWN, this.FRAGMENTS.size());
        } else {
            return String.format("Perk: (%d, %d frags)", this.UNKNOWN, this.FRAGMENTS.size());
        }
    }

    final private byte UNKNOWN;
    final private String FILENAME;
    final private Script SCRIPT;
    final private List<Fragment> FRAGMENTS;

    /**
     *
     */
    public class Fragment implements Entry {

        public Fragment(ByteBuffer input) {
            this.INDEX = Short.toUnsignedInt(input.getShort());
            this.UNKNOWN1 = input.getShort();
            this.UNKNOWN2 = input.get();
            this.SCRIPTNAME = IString.get(mf.BufferUtil.getUTF(input));
            this.FRAGMENTNAME = IString.get(mf.BufferUtil.getUTF(input));
        }

        @Override
        public void write(ByteBuffer output) {
            output.putShort((short) this.INDEX);
            output.putShort(this.UNKNOWN1);
            output.put(this.UNKNOWN2);
            output.put(this.SCRIPTNAME.getUTF8());
            output.put(this.FRAGMENTNAME.getUTF8());
        }

        @Override
        public int calculateSize() {
            return 9 + this.SCRIPTNAME.length() + this.FRAGMENTNAME.length();
        }

        final private int INDEX;
        final private short UNKNOWN1;
        final private byte UNKNOWN2;
        final private IString SCRIPTNAME;
        final private IString FRAGMENTNAME;
    }
}
