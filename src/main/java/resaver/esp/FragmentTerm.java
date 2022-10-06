/*
 * Copyright 2017 Mark Fairchild.
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
 * Describes script fragments for QUST records.
 *
 * @author Mark Fairchild
 */
public class FragmentTerm extends FragmentBase {

    public FragmentTerm(ByteBuffer input, ESPContext ctx) {
        //input = <code>ByteBuffer</code>.debug(input);
        this.UNKNOWN = input.get();
        this.SCRIPT = new Script(input, ctx);
        ctx.PLUGIN_INFO.addScriptData(this.SCRIPT);

        int fragCount = input.getShort();
        this.FRAGMENTS = new java.util.ArrayList<>(fragCount);

        for (int i = 0; i < fragCount; i++) {
            Fragment fragment = new Fragment(input);
            this.FRAGMENTS.add(fragment);
        }
    }

    @Override
    public void write(ByteBuffer output) {
        output.put(this.UNKNOWN);
        this.SCRIPT.write(output);
        output.putShort((short) this.FRAGMENTS.size());

        this.FRAGMENTS.forEach(fragment -> fragment.write(output));
    }

    @Override
    public int calculateSize() {
        int sum = 3;
        sum += this.SCRIPT.calculateSize();
        sum += this.FRAGMENTS.stream().mapToInt(v -> v.calculateSize()).sum();
        return sum;
    }

    @Override
    public String toString() {
        return String.format("Term: %s (%d, %d fragments)", this.SCRIPT.NAME, this.UNKNOWN, this.FRAGMENTS.size());
    }

    final private byte UNKNOWN;
    final private Script SCRIPT;
    final private List<Fragment> FRAGMENTS;

    /**
     *
     */
    public class Fragment implements Entry {

        public Fragment(ByteBuffer input) {
            this.INDEX = input.getInt();
            this.UNKNOWN = input.get();
            this.SCRIPTNAME = IString.get(mf.BufferUtil.getUTF(input));
            this.FRAGMENTNAME = IString.get(mf.BufferUtil.getUTF(input));
        }

        @Override
        public void write(ByteBuffer output) {
            output.put((byte) this.INDEX);
            output.put(this.UNKNOWN);
            output.put(this.SCRIPTNAME.getUTF8());
            output.put(this.FRAGMENTNAME.getUTF8());
        }

        @Override
        public int calculateSize() {
            return 9 + this.SCRIPTNAME.length() + this.FRAGMENTNAME.length();
        }

        @Override
        public String toString() {
            return String.format("%d: %s [%s] (%d)", this.INDEX, this.SCRIPTNAME, this.FRAGMENTNAME, this.UNKNOWN);
        }

        final private int INDEX;
        final private byte UNKNOWN;
        final private IString SCRIPTNAME;
        final private IString FRAGMENTNAME;
    }

}
