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

import java.util.Objects;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;
import resaver.ess.ChangeForm;
import resaver.ess.RefID;
import resaver.ess.Element;
import resaver.ess.Flags;
import resaver.ess.Linkable;

/**
 * Describes the FragmentTask field of an <code>ActiveScriptData</code>.
 *
 * @author Mark Fairchild
 */
final public class FragmentTask implements PapyrusElement, Linkable {

    /**
     * Creates a new <code>Unknown4</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param unknown3 The value of the unknown3 field.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     */
    public FragmentTask(ByteBuffer input, byte unknown3, PapyrusContext context) throws PapyrusFormatException {
        try {
            Objects.requireNonNull(input);
            Objects.requireNonNull(context);
            assert 1 <= unknown3 && unknown3 <= 3;

            if (unknown3 == 2) {
                this.DATA = new Type2(input, context);
                this.TYPECODE = null;
                this.TYPE = null;

            } else {
                this.TYPECODE = mf.BufferUtil.getLStringRaw(input);
                this.TYPE = FragmentType.valueOf(new String(this.TYPECODE, UTF_8));

                switch (this.TYPE) {
                    case QuestStage:
                        this.DATA = new QuestStage(input, context);
                        break;
                    case ScenePhaseResults:
                        this.DATA = new ScenePhaseResults(input, context);
                        break;
                    case SceneActionResults:
                        this.DATA = new SceneActionResults(input, context);
                        break;
                    case SceneResults:
                        this.DATA = new SceneResults(input, context);
                        break;
                    case TerminalRunResults:
                        this.DATA = new TerminalRunResults(input, context);
                        break;
                    case TopicInfo:
                        this.DATA = new TopicInfo(input, context);
                        break;
                    default:
                        throw new PapyrusFormatException("Unknown ActiveScript QuestData");
                }

            }

            this.VARIABLE = (unknown3 == 3 || unknown3 == 2)
                    ? Variable.read(input, context)
                    : null;

        } catch (PapyrusFormatException ex) {
            throw ex;
        }
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        assert null != output;
        assert null != this.TYPECODE || null != this.VARIABLE;

        // Corresponds to the unknown3 == 2 case.
        if (null != this.TYPECODE) {
            output.putInt(this.TYPECODE.length);
            output.put(this.TYPECODE);
        }

        this.DATA.write(output);

        if (null != this.VARIABLE) {
            this.VARIABLE.write(output);
        }
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 0;

        if (null != this.TYPECODE) {
            sum = 4 + this.TYPECODE.length;
        }

        sum += (null == this.DATA ? 0 : this.DATA.calculateSize());
        sum += (null == this.VARIABLE ? 0 : this.VARIABLE.calculateSize());
        return sum;
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        assert null != this.TYPECODE || null != this.VARIABLE;

        final StringBuilder BUILDER = new StringBuilder();
        BUILDER.append("UNK4:");

        if (null != this.TYPECODE) {
            BUILDER.append(this.TYPE);
        }

        BUILDER.append(this.DATA.toHTML(target));

        if (null != this.VARIABLE) {
            BUILDER.append(" (").append(this.VARIABLE.toHTML(null)).append(")");
        }

        return BUILDER.toString();
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder BUILDER = new StringBuilder();
        BUILDER.append("FragmentTask");

        if (null != this.TYPECODE) {
            BUILDER.append("(").append(this.TYPE).append("): ");
        }

        BUILDER.append(this.DATA);

        if (null != this.VARIABLE) {
            BUILDER.append("(").append(this.VARIABLE).append(")");
        }

        return BUILDER.toString();
    }

    final public byte[] TYPECODE;
    final public FragmentType TYPE;
    final public FragmentData DATA;
    final public Variable VARIABLE;
    //final public RefID QUESTID;
    //final public Byte BYTE;
    //final public Integer INT;
    //final public Integer UNKNOWN_4BYTES;
    //final public TString TSTRING;
    //final public Short STAGE;
    //final public ChangeForm FORM;

    static public enum FragmentType {
        QuestStage,
        ScenePhaseResults,
        SceneActionResults,
        SceneResults,
        TerminalRunResults,
        TopicInfo,
    }

    static public interface FragmentData extends Linkable, PapyrusElement {
    }

    /**
     * Stores the data for the other type of fragment.
     */
    static final public class Type2 implements FragmentData {

        public Type2(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
            if (context.getGame().isFO4()) {
                this.RUNNING_ID = context.readEID32(input);
                this.RUNNING = context.findActiveScript(this.RUNNING_ID);
            } else {
                this.RUNNING_ID = null;
                this.RUNNING = null;
            }
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            if (null != this.RUNNING_ID) {
                this.RUNNING_ID.write(output);
            }
        }

        /**
         * @see resaver.ess.Element#calculateSize()
         * @return The size of the <code>Element</code> in bytes.
         */
        @Override
        public int calculateSize() {
            return this.RUNNING_ID == null ? 0 : this.RUNNING_ID.calculateSize();
        }

        /**
         * @see resaver.ess.Linkable#toHTML(Element)
         * @param target A target within the <code>Linkable</code>.
         * @return
         */
        @Override
        public String toHTML(Element target) {
            final StringBuilder BUILDER = new StringBuilder();
            BUILDER.append("FragmentTask.Type2");

            if (null != this.RUNNING) {
                BUILDER.append(" ").append(this.RUNNING.toHTML(null));
            } else if (null != this.RUNNING_ID) {
                BUILDER.append(" ").append(this.RUNNING_ID);
            }

            return BUILDER.toString();
        }

        /**
         * @return String representation.
         */
        @Override
        public String toString() {
            final StringBuilder BUILDER = new StringBuilder();
            BUILDER.append("Type2");

            if (null != this.RUNNING_ID) {
                BUILDER.append(this.RUNNING_ID).append(" ");
            }

            return BUILDER.toString();
        }

        final public EID RUNNING_ID;
        final private ActiveScript RUNNING;
    }

    /**
     * Stores the data for a QuestStage fragment.
     */
    static final public class QuestStage implements FragmentData {

        public QuestStage(ByteBuffer input, PapyrusContext context) {
            this.QUESTID = context.readRefID(input);
            this.STAGE = input.getShort();
            this.FLAGS = Flags.readByteFlags(input);
            this.UNKNOWN_4BYTES = (context.getGame().isFO4() ? input.getInt() : null);
            this.QUEST = context.getChangeForm(this.QUESTID);
        }

        @Override
        public void write(ByteBuffer output) {
            this.QUESTID.write(output);
            output.putShort(this.STAGE);
            this.FLAGS.write(output);
            if (null != this.UNKNOWN_4BYTES) {
                output.putInt(this.UNKNOWN_4BYTES);
            }
        }

        @Override
        public int calculateSize() {
            int sum = 2;
            sum += this.QUESTID.calculateSize();
            sum += this.FLAGS.calculateSize();
            sum += this.UNKNOWN_4BYTES == null ? 0 : 4;
            return sum;
        }

        @Override
        public String toHTML(Element target) {
            final StringBuilder BUF = new StringBuilder();
            BUF.append(this.QUEST == null ? this.QUESTID : this.QUEST.toHTML(target));
            return BUF.append(" stage=")
                    .append(this.STAGE)
                    .append(" ").append(this.FLAGS)
                    .append(this.UNKNOWN_4BYTES)
                    .toString();
        }

        @Override
        public String toString() {
            final StringBuilder BUF = new StringBuilder();
            BUF.append(this.QUEST == null ? this.QUESTID : this.QUEST);
            return BUF.append(" stage=")
                    .append(this.STAGE)
                    .append(" ").append(this.FLAGS)
                    .append(this.UNKNOWN_4BYTES)
                    .toString();
        }

        final public RefID QUESTID;
        final public short STAGE;
        final public Flags.Byte FLAGS;
        final public Integer UNKNOWN_4BYTES;
        final public ChangeForm QUEST;

    }

    /**
     * Stores the data for a ScenePhaseResults fragment.
     */
    static final public class ScenePhaseResults implements FragmentData {

        public ScenePhaseResults(ByteBuffer input, PapyrusContext context) {
            this.QUESTID = context.readRefID(input);
            this.INT = input.getInt();
            this.UNKNOWN_4BYTES = (context.getGame().isFO4() ? input.getInt() : null);
            this.QUEST = context.getChangeForm(this.QUESTID);
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            this.QUESTID.write(output);
            output.putInt(this.INT);
            if (null != this.UNKNOWN_4BYTES) {
                output.putInt(this.UNKNOWN_4BYTES);
            }
        }

        @Override
        public int calculateSize() {
            int sum = 4;
            sum += this.QUESTID.calculateSize();
            sum += this.UNKNOWN_4BYTES == null ? 0 : 4;
            return sum;
        }

        @Override
        public String toHTML(Element target) {
            final StringBuilder BUF = new StringBuilder();
            BUF.append(this.QUEST == null ? this.QUESTID : this.QUEST.toHTML(target));
            return BUF.append(" stage=")
                    .append(this.INT)
                    .append(this.UNKNOWN_4BYTES)
                    .toString();
        }

        @Override
        public String toString() {
            final StringBuilder BUF = new StringBuilder();
            BUF.append(this.QUEST == null ? this.QUESTID : this.QUEST);
            return BUF.append(" stage=")
                    .append(this.INT)
                    .append(this.UNKNOWN_4BYTES)
                    .toString();
        }

        final public RefID QUESTID;
        final public int INT;
        final public Integer UNKNOWN_4BYTES;
        final public ChangeForm QUEST;

    }

    /**
     * Stores the data for a SceneActionResults fragment.
     */
    static final public class SceneActionResults implements FragmentData {

        public SceneActionResults(ByteBuffer input, PapyrusContext context) {
            this.QUESTID = context.readRefID(input);
            this.INT = input.getInt();
            this.UNKNOWN_4BYTES = (context.getGame().isFO4() ? input.getInt() : null);
            this.QUEST = context.getChangeForm(this.QUESTID);
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            this.QUESTID.write(output);
            output.putInt(this.INT);
            if (null != this.UNKNOWN_4BYTES) {
                output.putInt(this.UNKNOWN_4BYTES);
            }
        }

        @Override
        public int calculateSize() {
            int sum = 4;
            sum += this.QUESTID.calculateSize();
            sum += this.UNKNOWN_4BYTES == null ? 0 : 4;
            return sum;
        }

        @Override
        public String toHTML(Element target) {
            final StringBuilder BUF = new StringBuilder();
            BUF.append(this.QUEST == null ? this.QUESTID : this.QUEST.toHTML(target));
            return BUF.append(" stage=")
                    .append(this.INT)
                    .append(this.UNKNOWN_4BYTES)
                    .toString();
        }

        @Override
        public String toString() {
            final StringBuilder BUF = new StringBuilder();
            BUF.append(this.QUEST == null ? this.QUESTID : this.QUEST);
            return BUF.append(" stage=")
                    .append(this.INT)
                    .append(this.UNKNOWN_4BYTES)
                    .toString();
        }

        final public RefID QUESTID;
        final public int INT;
        final public Integer UNKNOWN_4BYTES;
        final public ChangeForm QUEST;

    }

    /**
     * Stores the data for a SceneResults fragment.
     */
    static final public class SceneResults implements FragmentData {

        public SceneResults(ByteBuffer input, PapyrusContext context) {
            this.QUESTID = context.readRefID(input);
            this.UNKNOWN_4BYTES = (context.getGame().isFO4() ? input.getInt() : null);
            this.QUEST = context.getChangeForm(this.QUESTID);
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            this.QUESTID.write(output);
            if (null != this.UNKNOWN_4BYTES) {
                output.putInt(this.UNKNOWN_4BYTES);
            }
        }

        @Override
        public int calculateSize() {
            int sum = this.QUESTID.calculateSize();
            sum += this.UNKNOWN_4BYTES == null ? 0 : 4;
            return sum;
        }

        @Override
        public String toHTML(Element target) {
            final StringBuilder BUF = new StringBuilder();
            BUF.append(this.QUEST == null ? this.QUESTID : this.QUEST.toHTML(target));
            return BUF.append(" stage=")
                    .append(this.UNKNOWN_4BYTES)
                    .toString();
        }

        @Override
        public String toString() {
            final StringBuilder BUF = new StringBuilder();
            BUF.append(this.QUEST == null ? this.QUESTID : this.QUEST);
            return BUF.append(" stage=")
                    .append(this.UNKNOWN_4BYTES)
                    .toString();
        }

        final public RefID QUESTID;
        final public Integer UNKNOWN_4BYTES;
        final public ChangeForm QUEST;

    }

    /**
     * Stores the data for a TerminalRunResults fragment.
     */
    static final public class TerminalRunResults implements FragmentData {

        public TerminalRunResults(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
            this.BYTE = input.get();
            this.INT = input.getInt();
            this.REFID = context.readRefID(input);
            this.TSTRING = context.readTString(input);
            this.FORM = context.getChangeForm(this.REFID);
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
            output.put(this.BYTE);
            output.putInt(this.INT);
            this.REFID.write(output);
            this.TSTRING.write(output);
        }

        @Override
        public int calculateSize() {
            int sum = 5;
            sum += this.REFID.calculateSize();
            sum += this.TSTRING.calculateSize();
            return sum;
        }

        @Override
        public String toHTML(Element target) {
            final StringBuilder BUF = new StringBuilder();
            return new StringBuilder()
                    .append(this.FORM == null ? this.REFID.toHTML(target) : this.FORM.toHTML(target))
                    .append(" ").append(this.TSTRING)
                    .append(String.format(" %08x %02x", this.INT, this.BYTE))
                    .toString();
        }

        @Override
        public String toString() {
            final StringBuilder BUF = new StringBuilder();
            return new StringBuilder()
                    .append(this.FORM == null ? this.REFID : this.FORM)
                    .append(" ").append(this.TSTRING)
                    .append(String.format(" %08x %02x", this.INT, this.BYTE))
                    .toString();
        }

        final byte BYTE;
        final int INT;
        final public RefID REFID;
        final public TString TSTRING;
        final public ChangeForm FORM;

    }

    /**
     * Stores the data for a SceneActionResults fragment.
     */
    static final public class TopicInfo implements FragmentData {

        public TopicInfo(ByteBuffer input, PapyrusContext context) {
        }

        /**
         * @see resaver.ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(ByteBuffer output) {
        }

        @Override
        public int calculateSize() {
            return 0;
        }

        @Override
        public String toHTML(Element target) {
            return "";
        }

        @Override
        public String toString() {
            return "";
        }


    }

}
