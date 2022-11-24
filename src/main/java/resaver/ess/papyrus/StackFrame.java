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
import resaver.ess.AnalyzableElement;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import resaver.IString;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import resaver.Analysis;
import resaver.ess.ESS;
import resaver.ess.Element;
import resaver.ess.Flags;
import resaver.ess.Linkable;
import resaver.ess.WStringElement;
import resaver.pex.Opcode;

/**
 * Describes a stack frame in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class StackFrame implements PapyrusElement, AnalyzableElement, Linkable, HasVariables {

    /**
     * Creates a new <code>StackFrame</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param thread The <code>ActiveScript</code> parent.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     */
    public StackFrame(ByteBuffer input, ActiveScript thread, PapyrusContext context) throws PapyrusFormatException, PapyrusElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(thread);
        Objects.requireNonNull(context);

        int variableCount = input.getInt();
        if (variableCount < 0 || variableCount > 50000) {
            throw new PapyrusFormatException("Invalid variableCount " + variableCount);
        }

        this.THREAD = thread;
        this.FLAG = new Flags.Byte(input);
        this.FN_TYPE = Type.read(input);
        this.SCRIPTNAME = context.readTString(input);
        this.SCRIPT = context.findScript(this.SCRIPTNAME);

        this.BASENAME = context.readTString(input);
        this.EVENT = context.readTString(input);

        this.STATUS = (!this.FLAG.getFlag(0) && this.FN_TYPE == Type.NULL)
                ? Optional.of(context.readTString(input))
                : Optional.empty();

        this.OPCODE_MAJORVERSION = input.get();
        this.OPCODE_MINORVERSION = input.get();
        this.RETURNTYPE = context.readTString(input);
        this.FN_DOCSTRING = context.readTString(input);
        this.FN_USERFLAGS = new Flags.Int(input);
        this.FN_FLAGS = new Flags.Byte(input);

        int functionParameterCount = Short.toUnsignedInt(input.getShort());
        assert 0 <= functionParameterCount && functionParameterCount < 2048 : "Invalid functionParameterCount " + functionParameterCount;

        this.FN_PARAMS = new ArrayList<>(functionParameterCount);
        for (int i = 0; i < functionParameterCount; i++) {
            FunctionParam param = new FunctionParam(input, context);
            this.FN_PARAMS.add(param);
        }

        int functionLocalCount = Short.toUnsignedInt(input.getShort());
        assert 0 <= functionLocalCount && functionLocalCount < 2048 : "Invalid functionLocalCount " + functionLocalCount;

        this.FN_LOCALS = new ArrayList<>(functionLocalCount);
        for (int i = 0; i < functionLocalCount; i++) {
            FunctionLocal local = new FunctionLocal(input, context);
            this.FN_LOCALS.add(local);
        }

        int opcodeCount = Short.toUnsignedInt(input.getShort());
        assert 0 <= opcodeCount;

        try {
            this.CODE = new ArrayList<>(opcodeCount);
            for (int i = 0; i < opcodeCount; i++) {
                OpcodeData opcode = new OpcodeData(input, context);
                this.CODE.add(opcode);
            }
        } catch (ListException ex) {
            throw new PapyrusElementException("Failed to read StackFrame OpcodeData.", ex, this);
        }

        this.PTR = input.getInt();
        assert 0 <= this.PTR;

        this.OWNERFIELD = Variable.read(input, context);

        try {
            this.VARIABLES = Variable.readList(input, variableCount, context);
        } catch (ListException ex) {
            throw new PapyrusElementException("Faileed to read StackFrame variables.", ex, this);
        }

        if (this.OWNERFIELD instanceof Variable.Ref) {
            Variable.Ref ref = (Variable.Ref) this.OWNERFIELD;
            this.OWNER = ref.getReferent();
        } else {
            this.OWNER = null;
        }
    }

    /**
     * @see resaver.ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.VARIABLES.size());
        this.FLAG.write(output);
        this.FN_TYPE.write(output);
        this.SCRIPTNAME.write(output);
        this.BASENAME.write(output);
        this.EVENT.write(output);
        this.STATUS.ifPresent(s -> s.write(output));
        output.put(this.OPCODE_MAJORVERSION);
        output.put(this.OPCODE_MINORVERSION);
        this.RETURNTYPE.write(output);
        this.FN_DOCSTRING.write(output);
        this.FN_USERFLAGS.write(output);
        this.FN_FLAGS.write(output);

        output.putShort((short) this.FN_PARAMS.size());
        this.FN_PARAMS.forEach(param -> param.write(output));

        output.putShort((short) this.FN_LOCALS.size());
        this.FN_LOCALS.forEach(local -> local.write(output));

        output.putShort((short) this.CODE.size());
        this.CODE.forEach(opcode -> opcode.write(output));

        output.putInt(this.PTR);

        this.OWNERFIELD.write(output);
        this.VARIABLES.forEach(var -> var.write(output));
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 1;
        sum += this.FN_TYPE.calculateSize();
        sum += this.SCRIPTNAME.calculateSize();
        sum += this.BASENAME.calculateSize();
        sum += this.EVENT.calculateSize();
        sum += this.STATUS.map(s -> s.calculateSize()).orElse(0);
        sum += 2;
        sum += this.RETURNTYPE.calculateSize();
        sum += this.FN_DOCSTRING.calculateSize();
        sum += 5;

        sum += 2;
        sum += this.FN_PARAMS.parallelStream().mapToInt(param -> param.calculateSize()).sum();

        sum += 2;
        sum += this.FN_LOCALS.parallelStream().mapToInt(local -> local.calculateSize()).sum();

        sum += 2;
        sum += this.CODE.parallelStream().mapToInt(opcode -> opcode.calculateSize()).sum();

        sum += 4;

        sum += (null != this.OWNERFIELD ? this.OWNERFIELD.calculateSize() : 0);

        sum += 4;
        sum += this.VARIABLES.stream().mapToInt(var -> var.calculateSize()).sum();

        return sum;
    }

    /**
     * Replaces the opcodes of the <code>StackFrame</code> with NOPs.
     */
    void zero() {
        for (int i = 0; i < this.CODE.size(); i++) {
            this.CODE.set(i, OpcodeData.NOP);
        }
    }

    /**
     * @return The name of the script being executed.
     */
    public TString getScriptName() {
        return this.SCRIPTNAME;
    }

    /**
     * @return The script being executed.
     */
    public Script getScript() {
        return this.SCRIPT;
    }

    /**
     * @return The qualified name of the function being executed.
     */
    public IString getFName() {
        IString fname = IString.format("%s.%s", this.SCRIPTNAME, this.EVENT);
        return fname;
    }

    /**
     * @return The event name.
     */
    public TString getEvent() {
        return this.EVENT;
    }

    /**
     * @return The docstring.
     */
    public TString getDocString() {
        return this.FN_DOCSTRING;
    }

    /**
     * @return The status.
     */
    public TString getStatus() {
        return this.STATUS.orElse(null);
    }

    /**
     * @return The function parameter list.
     */
    public List<FunctionParam> getFunctionParams() {
        return Collections.unmodifiableList(this.FN_PARAMS);
    }

    /**
     * @return The function locals list.
     */
    public List<FunctionLocal> getFunctionLocals() {
        return Collections.unmodifiableList(this.FN_LOCALS);
    }

    /**
     * @return The function opcode data list.
     */
    public List<OpcodeData> getOpcodeData() {
        return Collections.unmodifiableList(this.CODE);
    }

    /**
     * @see HasVariables#getVariables()
     * @return
     */
    @Override
    public List<Variable> getVariables() {
        return this.VARIABLES == null 
                ? Collections.emptyList() 
                : Collections.unmodifiableList(this.VARIABLES);
    }

    /**
     * @see HasVariables#getDescriptors() 
     * @return 
     */
    @Override
    public List<MemberDesc> getDescriptors() {
        return Stream.concat(this.getFunctionParams().stream(), this.getFunctionLocals().stream()).collect(Collectors.toList());
    }

    /**
     * @see HasVariables#setVariable(int, resaver.ess.papyrus.Variable) 
     * @param index
     * @param newVar 
     */
    @Override
    public void setVariable(int index, Variable newVar) {
        if (this.VARIABLES == null) {
            throw new NullPointerException("The variable list is missing.");
        }
        if (index <= 0 || index >= this.VARIABLES.size()) {
            throw new IllegalArgumentException("Invalid variable index: " + index);
        }
        
        this.VARIABLES.set(index, newVar);
    }

    /**
     * @return The owner, which should be a form or an instance.
     */
    public Variable getOwner() {
        return OWNERFIELD;
    }

    /**
     * @return A flag indicating if the <code>StackFrame</code> is undefined.
     *
     */
    public boolean isUndefined() {
        if (this.isNative()) {
            return false;
        } else if (this.SCRIPT != null) {
            return this.SCRIPT.isUndefined();
        }
        return false;
    }

    /**
     * @return A flag indicating if the <code>StackFrame</code> is running a
     * static method.
     */
    public boolean isStatic() {
        return (null != this.FN_FLAGS ? this.FN_FLAGS.getFlag(0) : false);
    }

    /**
     * @return A flag indicating if the <code>StackFrame</code> is running a
     * native method.
     */
    public boolean isNative() {
        return (null != this.FN_FLAGS ? this.FN_FLAGS.getFlag(1) : false);
    }

    /**
     * @return A flag indicating if the <code>StackFrame</code> zeroed.
     *
     */
    public boolean isZeroed() {
        return !this.isNative() && null != this.CODE && !this.CODE.isEmpty()
                && this.CODE.stream().allMatch(op -> OpcodeData.NOP.equals(op));
    }

    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        assert null != this.THREAD;
        int frameIndex = this.THREAD.getStackFrames().indexOf(this);
        if (frameIndex < 0) {
            return "invalid";
        }

        if (null != target) {
            if (target instanceof Variable) {
                int varIndex = this.VARIABLES.indexOf(target);
                if (varIndex >= 0) {
                    return Linkable.makeLink("frame", this.THREAD.getID(), frameIndex, varIndex, this.toString());
                }

            } else {
                Optional<Variable> result = this.VARIABLES.stream()
                        .filter(v -> v.hasRef())
                        .filter(v -> v.getReferent() == target)
                        .findFirst();

                if (result.isPresent()) {
                    int varIndex = this.VARIABLES.indexOf(result.get());
                    if (varIndex >= 0) {
                        return Linkable.makeLink("frame", this.THREAD.getID(), frameIndex, varIndex, this.toString());
                    }
                }
            }
        }

        return Linkable.makeLink("frame", this.THREAD.getID(), frameIndex, this.toString());
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();
        BUF.append(this.isZeroed() ? "ZEROED " : "");
        BUF.append(this.isUndefined() ? "#" : "");
        BUF.append(this.BASENAME);
        BUF.append(this.isUndefined() ? "#." : ".");
        BUF.append(this.EVENT);
        BUF.append("()");

        if (this.isStatic()) {
            BUF.append(" ").append("static");
        }
        if (this.isNative()) {
            BUF.append(" ").append("native");
        }

        return BUF.toString();
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(resaver.Analysis analysis, ESS save) {
        final StringBuilder BUILDER = new StringBuilder();

        //BUILDER.append("<html><h3>STACKFRAME</h3>");
        BUILDER.append("<html><h3>STACKFRAME");
        if (this.isZeroed()) {
            BUILDER.append(" (ZEROED)");
        }
        BUILDER.append("<br/>");
        if (!this.RETURNTYPE.isEmpty() && !this.RETURNTYPE.equals("None")) {
            BUILDER.append(this.RETURNTYPE).append(" ");
        }
        if (this.isUndefined()) {
            BUILDER.append("#");
        }
        BUILDER.append(String.format("%s.%s()", this.SCRIPTNAME, this.EVENT));
        if (this.isStatic()) {
            BUILDER.append(" static");
        }
        if (this.isNative()) {
            BUILDER.append(" native");
        }
        BUILDER.append("</h3>");

        if (this.isZeroed()) {
            BUILDER.append("<p><em>WARNING: FUNCTION TERMINATED!</em><br/>This function has been terminated and all of its instructions erased.</p>");
        } else if (this.isUndefined()) {
            BUILDER.append("<p><em>WARNING: SCRIPT MISSING!</em><br/>Selecting \"Remove Undefined Instances\" will terminate the entire thread containing this frame.</p>");
        }

        /*if (null != analysis) {
            SortedSet<String> providers = analysis.FUNCTION_ORIGINS.get(this.getFName());
            if (null != providers) {
                String probablyProvider = providers.last();
                BUILDER.append(String.format("<p>Probably running code from mod %s.</p>", probablyProvider));

                if (providers.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    providers.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }
            }
        }*/
        if (this.OWNERFIELD instanceof Variable.Null) {
            BUILDER.append("<p>Owner: <em>UNOWNED</em></p>");
        } else if (null != this.OWNER) {
            BUILDER.append(String.format("<p>Owner: %s</p>", this.OWNER.toHTML(this)));
        } else if (this.isStatic()) {
            BUILDER.append("<p>Static method, no owner.</p>");
        } else {
            BUILDER.append(String.format("<p>Owner: %s</p>", this.OWNERFIELD.toHTML(this)));
        }

        BUILDER.append("<p>");
        BUILDER.append(String.format("Script: %s<br/>", (null == this.SCRIPT ? this.SCRIPTNAME : this.SCRIPT.toHTML(this))));
        BUILDER.append(String.format("Base: %s<br/>", this.BASENAME));
        BUILDER.append(String.format("Event: %s<br/>", this.EVENT));
        BUILDER.append(String.format("Status: %s<br/>", this.STATUS));
        BUILDER.append(String.format("Flag: %s<br/>", this.FLAG));
        BUILDER.append(String.format("Function type: %s<br/>", this.FN_TYPE));
        BUILDER.append(String.format("Function return type: %s<br/>", this.RETURNTYPE));
        BUILDER.append(String.format("Function docstring: %s<br/>", this.FN_DOCSTRING));
        BUILDER.append(String.format("%d parameters, %d locals, %d values.<br/>", this.FN_PARAMS.size(), this.FN_LOCALS.size(), this.VARIABLES.size()));
        BUILDER.append(String.format("Status: %s<br/>", this.STATUS));
        BUILDER.append(String.format("Function flags: %s<br/>", this.FN_FLAGS));
        BUILDER.append(String.format("Function user flags:<br/>%s", this.FN_USERFLAGS.toHTML()));
        BUILDER.append(String.format("Opcode version: %d.%d<br/>", this.OPCODE_MAJORVERSION, this.OPCODE_MINORVERSION));
        BUILDER.append("</p>");

        if (this.CODE.size() > 0) {
            BUILDER.append("<hr/><p>PAPYRUS BYTECODE:</p>");
            BUILDER.append("<code><pre>");
            List<OpcodeData> OPS = new ArrayList<>(this.CODE);
            OPS.subList(0, PTR).forEach(v -> BUILDER.append(String.format("   %s\n", v)));
            BUILDER.append(String.format("==><b>%s</b>\n", OPS.get(this.PTR)));
            OPS.subList(PTR + 1, this.CODE.size()).forEach(v -> BUILDER.append(String.format("   %s\n", v)));
            BUILDER.append("</pre></code>");
        } else {
            BUILDER.append("<p><em>Papyrus bytecode not available.</em></p>");
        }

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Analysis analysis, String mod) {
        Objects.requireNonNull(analysis);
        Objects.requireNonNull(mod);

        final SortedSet<String> OWNERS = analysis.SCRIPT_ORIGINS.get(this.SCRIPTNAME.toIString());
        if (null == OWNERS) {
            return false;
        }
        return OWNERS.contains(mod);
    }

    /**
     * Eliminates ::temp variables from an OpcodeData list.
     *
     * @param instructions
     * @param locals
     * @param types
     * @param terms
     */
    static String preMap(List<OpcodeData> instructions, List<MemberDesc> locals, List<MemberDesc> types, Map<Parameter, Parameter> terms, int ptr) {
        final StringBuilder BUF = new StringBuilder();

        for (int i = 0; i < instructions.size(); i++) {
            OpcodeData op = instructions.get(i);
            if (null == op) {
                continue;
            }

            ArrayList<Parameter> params = new ArrayList<>(op.getParameters());
            boolean del = makeTerm(op.getOpcode(), params, types, terms);
            if (del) {
                BUF.append(i == ptr ? "<b>==></b>" : "   ");
                BUF.append("<em><font color=\"lightgray\">");
                BUF.append(op.getOpcode());
                params.forEach(p -> BUF.append(", ").append(p.toValueString()));
                BUF.append("<font color=\"black\"></em>\n");
            } else {
                BUF.append(i == ptr ? "<b>==>" : "   ");
                BUF.append(op.getOpcode());
                params.forEach(p -> BUF.append(", ").append(p.toValueString()));
                BUF.append(i == ptr ? "</b>\n" : "\n");
            }
        }
        return BUF.toString();
    }

    /**
     *
     * @param op
     * @param args
     * @param types
     * @param terms
     * @return
     */
    static boolean makeTerm(Opcode op, List<Parameter> args, List<MemberDesc> types, Map<Parameter, Parameter> terms) {
        String term;
        String method, obj, dest, arg, prop, operand1, operand2;
        List<String> subArgs;
        WStringElement type;

        switch (op) {
            case IADD:
            case FADD:
            case STRCAT:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s + %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case ISUB:
            case FSUB:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s - %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case IMUL:
            case FMUL:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s * %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case IDIV:
            case FDIV:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s / %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case IMOD:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s %% %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case RETURN:
                replaceVariables(args, terms, -1);
                return false;

            case CALLMETHOD:
                replaceVariables(args, terms, 2);
                method = args.get(0).toValueString();
                obj = args.get(1).toValueString();
                subArgs = args
                        .subList(3, args.size())
                        .stream()
                        .map(v -> v.paren())
                        .collect(Collectors.toList());
                term = String.format("%s.%s%s", obj, method, paramList(subArgs));
                return processTerm(args, terms, 2, term);

            case CALLPARENT:
                replaceVariables(args, terms, 1);
                method = args.get(0).toValueString();
                subArgs = args
                        .subList(3, args.size())
                        .stream()
                        .map(v -> v.paren())
                        .collect(Collectors.toList());
                term = String.format("parent.%s%s", method, paramList(subArgs));
                return processTerm(args, terms, 1, term);

            case CALLSTATIC:
                replaceVariables(args, terms, 2);
                obj = args.get(0).toValueString();
                method = args.get(1).toValueString();
                subArgs = args
                        .subList(3, args.size())
                        .stream()
                        .map(v -> v.paren())
                        .collect(Collectors.toList());
                term = String.format("%s.%s%s", obj, method, paramList(subArgs));
                return processTerm(args, terms, 2, term);

            case NOT:
                replaceVariables(args, terms, 0);
                term = String.format("!%s", args.get(1).paren());
                return processTerm(args, terms, 0, term);

            case INEG:
            case FNEG:
                replaceVariables(args, terms, 0);
                term = String.format("-%s", args.get(1).paren());
                return processTerm(args, terms, 0, term);

            case ASSIGN:
                replaceVariables(args, terms, 0);
                term = String.format("%s", args.get(1));
                return processTerm(args, terms, 0, term);

            case CAST:
                replaceVariables(args, terms, 0);
                dest = args.get(0).toValueString();
                arg = args.get(1).paren();
                type = types.stream().filter(t -> t.getName().equals(dest)).findFirst().get().getType().toWString();

                if (type.equals("bool")) {
                    term = arg;
                } else {
                    term = String.format("(%s)%s", type, arg);
                }
                return processTerm(args, terms, 0, term);

            case PROPGET:
                replaceVariables(args, terms, 2);
                obj = args.get(1).toValueString();
                prop = args.get(0).toValueString();
                term = String.format("%s.%s", obj, prop);
                return processTerm(args, terms, 2, term);

            case PROPSET:
                replaceVariables(args, terms, -1);
                return false;

            case CMP_EQ:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s == %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case CMP_LT:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s < %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case CMP_LE:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s <= %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case CMP_GT:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s > %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case CMP_GE:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1).paren();
                operand2 = args.get(2).paren();
                term = String.format("%s >= %s", operand1, operand2);
                return processTerm(args, terms, 0, term);

            case ARR_CREATE:
                int size = args.get(1).getIntValue();
                dest = args.get(0).toValueString();
                type = types.stream().filter(t -> t.getName().equals(dest)).findFirst().get().getType().toWString();
                String subtype = type.toString().substring(0, type.length() - 2);
                term = String.format("new %s[%s]", subtype, size);
                return processTerm(args, terms, 0, term);

            case ARR_LENGTH:
                replaceVariables(args, terms, 0);
                term = String.format("%s.length", args.get(1));
                return processTerm(args, terms, 0, term);

            case ARR_GET:
                replaceVariables(args, terms, 0);
                operand1 = args.get(2).toValueString();
                operand2 = args.get(1).toValueString();
                term = String.format("%s[%s]", operand2, operand1);
                return processTerm(args, terms, 0, term);

            case ARR_SET:
                replaceVariables(args, terms, -1);
                return false;

            case JMPT:
            case JMPF:
                replaceVariables(args, terms, -1);
                return false;

            case JMP:
            case ARR_FIND:
            case ARR_RFIND:
            default:
                return false;
        }
    }

    /**
     * @param args
     * @param terms
     * @param destPos
     * @param positions
     */
    static boolean processTerm(List<Parameter> args, Map<Parameter, Parameter> terms, int destPos, String term) {
        if (destPos >= args.size() || !(args.get(destPos).getType() == Parameter.Type.IDENTIFIER)) {
            return false;
        }
        Parameter dest = args.get(destPos);

        if (!dest.isTemp()) {
            return false;
        }

        terms.put(dest, Parameter.createTerm(term));
        return true;
    }

    /**
     * Replaces certain variables with terms. In particular, all temp variables
     * and autovar names should be replaced.
     *
     * @param args
     * @param terms
     * @param exclude
     */
    static void replaceVariables(List<Parameter> args, Map<Parameter, Parameter> terms, int exclude) {
        for (int i = 0; i < args.size(); i++) {
            Parameter arg = args.get(i);
            if (terms.containsKey(arg) && i != exclude) {
                args.set(i, terms.get(arg));

            } else if (arg.isAutovar()) {
                final Matcher MATCHER = AUTOVAR_REGEX.matcher(arg.toValueString());
                MATCHER.matches();
                String prop = MATCHER.group(1);
                terms.put(arg, Parameter.createTerm(prop));
                args.set(i, terms.get(arg));
            }
        }
    }

    /**
     * Creates a function parameter list style string for a <code>List</code>.
     *
     * @param <T>
     * @param params
     * @return
     */
    static <T> String paramList(List<T> params) {
        return params.stream()
                .map(p -> p.toString())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    final private ActiveScript THREAD;
    final private Flags.Byte FLAG;
    final private Type FN_TYPE;
    final private TString SCRIPTNAME;
    final private Script SCRIPT;
    final private TString BASENAME;
    final private TString EVENT;
    final private Optional<TString> STATUS;
    final private byte OPCODE_MAJORVERSION;
    final private byte OPCODE_MINORVERSION;
    final private TString RETURNTYPE;
    final private TString FN_DOCSTRING;
    final private Flags.Int FN_USERFLAGS;
    final private Flags.Byte FN_FLAGS;
    final private List<FunctionParam> FN_PARAMS;
    final private List<FunctionLocal> FN_LOCALS;
    final private List<OpcodeData> CODE;
    final private int PTR;
    final private Variable OWNERFIELD;
    final private List<Variable> VARIABLES;
    final private DefinedElement OWNER;
    static final Pattern AUTOVAR_REGEX = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE);

}
