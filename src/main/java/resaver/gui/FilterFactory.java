/*
 * Copyright 2016 Mark.
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
package resaver.gui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import resaver.Mod;
import resaver.Analysis;
import resaver.ess.*;
import resaver.ess.papyrus.*;
import resaver.gui.FilterTreeModel.Node;
import static resaver.ess.ChangeFlagConstantsRefr.CHANGE_FORM_FLAGS;

/**
 *
 * @author Mark
 */
public class FilterFactory {

    static public enum ParseLevel { PARSED, PARTIAL, UNPARSED };
    
    public FilterFactory(ESS ess, Analysis analysis) {
        Objects.requireNonNull(ess);
        this.ANALYSIS = analysis;
        this.ESS = ess;
        this.CONTEXT = ess.getContext();
        this.FILTERS = new ArrayList<>(10);
        this.SUBFILTERS = new ArrayList<>(10);
    }
    
    public Predicate<Node> generate() {
        // AND the main filters together.
        Predicate<Node> filter = FILTERS.stream()
                .reduce((a, b) -> a.and(b))
                .orElse(null);
                
        // OR the subfilters together.
        Predicate<Node> subFilter = SUBFILTERS.stream()                
                .reduce((a, b) -> a.or(b))
                .orElse(null);
        
        if (filter != null && subFilter != null) {
            return filter.and(subFilter);
        } else if (filter != null) {
            return filter;
        } else if (subFilter != null) {
            return subFilter;
        } else {
            return null;
        }
    }
    
    /**
     * Add a plugin filter.
     * @param plugin The plugin to filter for.
     * @return
     */
    public FilterFactory addPluginFilter(Plugin plugin) {
        Objects.requireNonNull(plugin);
        //LOG.info(String.format("Filtering: plugin = \"%s\"", plugin));
        return addFilter(createPluginFilter(plugin));
    }
    
    /**
     * Add a mod filter.
     * @param mod The <code>Mod</code> to filter for.
     * @return
     */
    public FilterFactory addModFilter(Mod mod) {
        Objects.requireNonNull(mod);
        //LOG.info(String.format("Filtering: mod = \"%s\"", mod));
        return addFilter(createModFilter(mod));
    }
    
    /**
     * Add a mod filter.
     * @param regex The filter pattern.
     * @return
     */
    public FilterFactory addRegexFilter(String regex) {
        Objects.requireNonNull(regex);
        //LOG.info(String.format("Filtering: regex = \"%s\"", regex));
        return addFilter(createRegexFilter(regex));
    }
    
    /**
     * Add a changeflag filter.
     * @param mask A bitmask indicating which flags to filter.
     * @param filter A bitmask indicating the required value of masked flags.
     * @return
     */
    public FilterFactory addChangeFlagFilter(int mask, int filter) {
        return addFilter(createChangeFlagFilter(mask, filter));
    }
    
    /**
     * Add a changeformflag filter.
     * @param mask A bitmask indicating which flags to filter.
     * @param filter A bitmask indicating the required value of masked flags.
     * @return
     */
    public FilterFactory addChangeFormFlagFilter(int mask, int filter) {
        return addFilter(createChangeFormFlagFilter(mask, filter));
    }
    
    /**
     * Add a has-script filter.
     * @return
     */
    public FilterFactory addHasScriptFilter() {
        return addFilter(createHasScriptFilter());
    }
    
    /**
     * Add an unparsed data filter.
     * @param level
     * @return
     */
    public FilterFactory addUnparsedFilter(ParseLevel level) {
        return addFilter(createUnparsedDataFilter(level));
    }
    
    /**
     * Add an undefined element filter.
     * @return
     */
    public FilterFactory addUndefinedSubfilter() {
        return addSubFilter(createUndefinedFilter());
    }
    
    /**
     * Add an undefined element filter.
     * @return
     */
    public FilterFactory addUnattachedSubfilter() {
        
        return addSubFilter(createUnattachedFilter());
    }
    
    /**
     * Add a memberless filter.
     * @return
     */
    public FilterFactory addMemberlessSubfilter() {
        return addSubFilter(createMemberlessFilter());
    }
    
    /**
     * Add a canary filter.
     * @return
     */
    public FilterFactory addCanarySubfilter() {
        return addSubFilter(createCanaryFilter());
    }
    
    /**
     * Add a nullRef filter.
     * @return
     */
    public FilterFactory addNullRefSubfilter() {
        return addSubFilter(createNullRefFilter());
    }
    
    /**
     * Add a nonexistent element filter.
     * @return
     */
    public FilterFactory addNonexistentSubfilter() {
        return addSubFilter(createNonExistentFilter());
    }
    
    /**
     * Add a long-string filter.
     * @return
     */
    public FilterFactory addLongStringSubfilter() {
        return addSubFilter(createLongStringFilter());
    }
    
    /**
     * Add a deleted filter.
     * @return
     */
    public FilterFactory addDeletedSubfilter() {
        return addSubFilter(createDeletedFilter());
    }
    
    /**
     * Add a void filter.
     * @return
     */
    public FilterFactory addVoidSubfilter() {
        return addSubFilter(createVoidFilter());
    }
    
    private FilterFactory addFilter(Predicate<Node> filter) {
        FILTERS.add(filter);
        return this;
    }
    
    private FilterFactory addSubFilter(Predicate<Node> filter) {
        SUBFILTERS.add(filter);
        return this;
    }
    
    public FilterFactory addChangeFormContentFilter(String fieldCodes) {
        if (fieldCodes == null || fieldCodes.chars().allMatch(Character::isWhitespace)) {
            return this;
        } else {
            return addFilter(createChangeFormContentFilter(fieldCodes));
        }
    }
    
    final private Analysis ANALYSIS;
    final private ESS ESS;
    final private ESS.ESSContext CONTEXT;
    final private List<Predicate<Node>> FILTERS;
    final private List<Predicate<Node>> SUBFILTERS;
    
    /**
     * Create a mod filter.
     * @param plugins
     * @return
     */
    private Predicate<Node> createModFilter(Mod mod) {
        LOG.info(MessageFormat.format("Creating 'Mod' filter for {0}", mod));
        final PluginInfo PLUGINS = ESS.getPluginInfo();
        final String MODNAME = mod.getName();
        
        final Plugin[] MODPLUGINS = mod.getESPNames()
                .stream()
                .map(name -> PLUGINS.find(name))
                .filter(Objects::nonNull)
                .toArray(len -> new Plugin[len]);
        
        Predicate<Node> modFilter = node -> node.hasElement()
                && node.getElement() instanceof AnalyzableElement
                && ((AnalyzableElement) node.getElement()).matches(ANALYSIS, MODNAME);

        Predicate<Node> pluginFilter = createPluginFilter(MODPLUGINS);
        return modFilter.or(pluginFilter);        
    }
    
    /**
     * Create a plugin filter.
     * @param plugins
     * @return
     */
    private Predicate<Node> createPluginFilter(Plugin... plugins) {
        LOG.info(MessageFormat.format("Creating 'Plugin' filter for {0}", (Object[]) plugins));
        Objects.requireNonNull(plugins);
        //LOG.info(String.format("Filtering: plugins = \"%s\"", Arrays.toString(plugins)));

        Set<Plugin> pluginSet = new HashSet<>(Arrays.asList(plugins));
        
        return node -> {
            // If the node doesn't contain an element, it automatically fails.
            if (!node.hasElement()) {
                return false;

            } // Check if the element is the plugin itself.
            else if (node.getElement() instanceof Plugin) {
                return pluginSet.contains((Plugin) node.getElement());

            } // Check if the element is an instance with a matching refid.
            else if (node.getElement() instanceof ScriptInstance) {
                ScriptInstance instance = (ScriptInstance) node.getElement();
                RefID refID = instance.getRefID();
                return null != refID && refID.PLUGIN != null && pluginSet.contains(refID.PLUGIN);

            } // Check if the element is a ChangeForm with a matching refid.
            else if (node.getElement() instanceof ChangeForm) {
                ChangeForm form = (ChangeForm) node.getElement();
                RefID refID = form.getRefID();
                return null != refID && refID.PLUGIN != null && pluginSet.contains(refID.PLUGIN);

            } // If the element is not an instance, it automatically fails.
            return false;
        };
    }

    /**
     * Setup a regex setFilter.
     *
     * @param regex
     * @return
     */
    private Predicate<Node> createRegexFilter(String regex) {
        LOG.info("Creating 'Regex' filter.");
        if (!regex.isEmpty()) {
            try {
                LOG.info(String.format("Filtering: regex = \"%s\"", regex));
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                return node -> pattern.matcher(node.getName()).find();
            } catch (PatternSyntaxException ex) {
            }
        }

        return node -> true;
    }

    /**
     * Setup an undefined element setFilter.
     *
     * @return
     */
    private Predicate<Node> createUndefinedFilter() {
        LOG.info("Creating 'Undefined' filter.");
        return node -> {
            if (node.hasElement()) {
                Element e = node.getElement();
                if (e instanceof Script) {
                    return ((Script) e).isUndefined();
                } else if (e instanceof ScriptInstance) {
                    return ((ScriptInstance) e).isUndefined();
                } else if (e instanceof Reference) {
                    return ((Reference) e).isUndefined();
                } else if (e instanceof Struct) {
                    return ((Struct) e).isUndefined();
                } else if (e instanceof StructInstance) {
                    return ((StructInstance) e).isUndefined();
                } else if (e instanceof ActiveScript) {
                    return ((ActiveScript) e).isUndefined();
                } else if (e instanceof FunctionMessage) {
                    return ((FunctionMessage) e).isUndefined();
                } else if (e instanceof StackFrame) {
                    return ((StackFrame) e).isUndefined();
                } else if (e instanceof SuspendedStack) {
                    return ((SuspendedStack) e).isUndefined();
                }
            }
            return false;
        };
    }

    /**
     * Setup an unattached element setFilter.
     *
     * @return
     */
    private Predicate<Node> createUnattachedFilter() {
        LOG.info("Creating 'Unattached' filter.");
        return node -> {
            if (node.hasElement() && node.getElement() instanceof ScriptInstance) {
                return ((ScriptInstance) node.getElement()).isUnattached();
            }
            return false;
        };
    }

    /**
     * Setup an unattached element setFilter.
     *
     * @return
     */
    private Predicate<Node> createMemberlessFilter() {
        LOG.info("Creating 'Memberless' filter.");
        return node -> {
            if (node.hasElement() && node.getElement() instanceof ScriptInstance) {
                return ((ScriptInstance) node.getElement()).hasMemberlessError();
            }
            return false;
        };
    }

    /**
     * Setup an unattached element setFilter.
     *
     * @return
     */
    private Predicate<Node> createCanaryFilter() {
        LOG.info("Creating 'Canary' filter.");
        return node -> {
            if (node.hasElement() && node.getElement() instanceof ScriptInstance) {
                ScriptInstance instance = (ScriptInstance) node.getElement();
                return instance.hasCanary() && instance.getCanary() == 0;
            }
            return false;
        };
    }

    /**
     * Setup a nullref setFilter.
     *
     * @param context
     * @param analysis
     * @return
     */
    private Predicate<Node> createNullRefFilter() {
        LOG.info("Creating 'NullRef' filter.");
        return node -> {
            if (!node.hasElement() || !(node.getElement() instanceof ChangeForm)) {
                return false;
            }
            final ChangeForm FORM = (ChangeForm) node.getElement();
            if (FORM.getType() != ChangeForm.Type.FLST) {
                return false;
            }
            
            final ChangeFormData DATA = FORM.getData(ANALYSIS, CONTEXT, true);
            if (DATA == null || !(DATA instanceof ChangeFormFLST)) {
                return false;
            }

            return ((ChangeFormFLST) DATA).containsNullrefs();
        };
    }

    /**
     * Setup a non-existent element setFilter.
     *
     * @param ess The save file.
     * @return
     */
    private Predicate<Node> createNonExistentFilter() {
        LOG.info("Creating 'NonExistent' filter.");
        return node -> {
            if (node.hasElement() && node.getElement() instanceof ScriptInstance) {
                ScriptInstance instance = (ScriptInstance) node.getElement();
                RefID refID = instance.getRefID();
                return refID.getType() == RefID.Type.CREATED && !ESS.getChangeForms().containsKey(refID);
            }
            return false;
        };
    }

    /**
     * Setup a non-existent element setFilter.
     *
     * @return
     */
    private Predicate<Node> createLongStringFilter() {
        LOG.info("Creating 'LongString' filter.");
        return node -> {
            if (node.hasElement() && node.getElement() instanceof TString) {
                TString str = (TString) node.getElement();
                return str.length() >= 512;
            }
            return false;
        };
    }

    /**
     * Setup a deleted element setFilter.
     *
     * @param context
     * @param analysis
     * @return
     */
    private Predicate<Node> createDeletedFilter() {
        LOG.info("Creating 'Deleted' filter.");
        return node -> {
            if (!node.hasElement()) {
                return false;
            }
            if (!(node.getElement() instanceof ChangeForm)) {
                return false;
            }

            final ChangeForm FORM = (ChangeForm) node.getElement();

            if (!(FORM.getType() == ChangeForm.Type.ACHR || FORM.getType() == ChangeForm.Type.REFR)) {
                return false;
            }

            if (!FORM.getChangeFlags().getFlag(1) && !FORM.getChangeFlags().getFlag(3)) {
                return false;
            }

            final ChangeFormData DATA = FORM.getData(ANALYSIS, CONTEXT, true);

            if (DATA == null) {
                return false;
            }
            if (!(DATA instanceof GeneralElement)) {
                return false;
            }

            final GeneralElement ROOT = (GeneralElement) DATA;
            final Element MOVECELL = ROOT.getElement("MOVE_CELL");

            if (MOVECELL == null) {
                return false;
            }

            if (!(MOVECELL instanceof RefID)) {
                throw new IllegalStateException("MOVE_CELL was not a RefID: " + MOVECELL);
            }

            final RefID REF = (RefID) MOVECELL;
            return REF.FORMID == 0xFFFFFFFF;

        };
    }

    /**
     * Setup a deleted element setFilter.
     *
     * @param context
     * @param analysis
     * @return
     */
    private Predicate<Node> createVoidFilter() {
        LOG.info("Creating 'Void' filter.");
        return node -> {
            if (!node.hasElement()) {
                return false;
            }
            if (!(node.getElement() instanceof ChangeForm)) {
                return false;
            }

            final ChangeForm FORM = (ChangeForm) node.getElement();

            if (!(FORM.getType() == ChangeForm.Type.ACHR || FORM.getType() == ChangeForm.Type.REFR)) {
                return false;
            }

            final Flags.Int FLAGS = FORM.getChangeFlags();
            for (int i = 0; i <= 7; i++) {
                if (FLAGS.getFlag(i)) {
                    return false;
                }
            }

            final ChangeFormData DATA = FORM.getData(ANALYSIS, CONTEXT, true);

            if (DATA == null) {
                return false;
            }
            if (!(DATA instanceof GeneralElement)) {
                return false;
            }

            final GeneralElement ROOT = (GeneralElement) DATA;

            if (ROOT.getValues().isEmpty()) {
                return true;
            }

            if (ROOT.hasVal("INITIAL") && ROOT.count() <= 2) {
                GeneralElement initial = ROOT.getGeneralElement("INITIAL");
                if (initial.getValues().isEmpty()) {
                    if (ROOT.count() == 1) {
                        return true;
                    }

                    if (ROOT.hasVal("EXTRADATA")) {
                        final GeneralElement EXTRA = ROOT.getGeneralElement("EXTRADATA");
                        VSVal count = (VSVal) EXTRA.getVal("DATA_COUNT");
                        return count != null && count.getValue() == 0;
                    }
                }
            }

            return false;
        };
    }

    /**
     * Setup a ChangeFlag setFilter.
     *
     * @param mask
     * @param filter
     * @return
     */
    private Predicate<Node> createChangeFlagFilter(int mask, int filter) {
        LOG.info(MessageFormat.format("Creating 'ChangeFlag' filter {0} ({1})", new Flags.Int(filter), new Flags.Int(mask)));
        if (mask == 0) {
            return node -> true;
        } else {
            return node -> {
                if (!node.hasElement()) {
                    return false;
                }
                if (!(node.getElement() instanceof ChangeForm)) {
                    return false;
                }

                final ChangeForm FORM = (ChangeForm) node.getElement();

                final Flags.Int FLAGS = FORM.getChangeFlags();
                int flags = FLAGS.FLAGS;
                int filtered = (~filter) ^ flags;
                int masked = filtered | (~mask);
                return masked == -1;
            };
        }
    }

    /**
     * Setup a ChangeFormFlag setFilter.
     *
     * @param context
     * @param mask
     * @param filter
     * @return
     */
    private Predicate<Node> createChangeFormFlagFilter(int mask, int filter) {
        LOG.info(MessageFormat.format("Creating 'ChangeFormFlag' filter {0} ({1})", new Flags.Int(filter), new Flags.Int(mask)));
        if (mask == 0) {
            return node -> true;
        } else {
            return node -> {
                if (!node.hasElement()) {
                    return false;
                }
                if (!(node.getElement() instanceof ChangeForm)) {
                    return false;
                }

                final ChangeForm FORM = (ChangeForm) node.getElement();

                final Flags.Int FLAGS = FORM.getChangeFlags();
                if (!FLAGS.getFlag(CHANGE_FORM_FLAGS)) {
                    return false;
                }

                try {
                    ChangeFormData data = FORM.getData(ANALYSIS, CONTEXT, true);
                    if (!(data instanceof GeneralElement)) {
                        return false;
                    }
                    final GeneralElement DATA = (GeneralElement) data;

                    if (!DATA.hasVal(CHANGE_FORM_FLAGS)) {
                        return false;
                    }

                    final ChangeFormFlags CFF = (ChangeFormFlags) DATA.getElement(CHANGE_FORM_FLAGS);
                    int flags = CFF.getFlags().FLAGS;
                    int filtered = (~filter) ^ flags;
                    int masked = filtered | (~mask);
                    return masked == -1;
                    
                } catch (java.nio.BufferUnderflowException ex) {
                    return false;
                }
            };
        }
    }

    /**
     * Setup an unparsed data filter.
     * @return
     */
    private Predicate<Node> createUnparsedDataFilter(ParseLevel level) {
        LOG.info("Creating 'Unparsed' filter.");
        return node -> {
            if (!node.hasElement() || !(node.getElement() instanceof ChangeForm)) {
                return false;
            }
            final ChangeForm FORM = (ChangeForm) node.getElement();
            final ChangeFormData DATA = FORM.getData(ANALYSIS, CONTEXT, true);
            
            if (DATA == null || DATA instanceof ChangeFormDefault) {
                return level == ParseLevel.UNPARSED;
            } else if (DATA instanceof ChangeFormLeveled) {                
                return level == ParseLevel.PARSED;
            } else if (DATA instanceof ChangeFormFLST) {
                return level == ParseLevel.PARSED;
            } else if (DATA instanceof GeneralElement) {                    
                GeneralElement BODY = (GeneralElement) DATA;
                switch (level) {
                    case UNPARSED:
                        return BODY.count() == 0;
                    case PARTIAL:                    
                        return BODY.hasUnparsed();
                    default:
                        return !BODY.hasUnparsed();
                }
            } else {
                return level == ParseLevel.UNPARSED;
            }
        };
    }

    /**
     * Setup a changeform-has-script filter.
     * @return
     */
    private Predicate<Node> createHasScriptFilter() {
        LOG.info("Creating 'HasScript' filter.");
        final Set<RefID> CACHE = ESS.getPapyrus()
                .getScriptInstances()
                .values()
                .parallelStream()
                .map(i -> i.getRefID())
                .collect(Collectors.toSet());

        return node -> {
            if (!node.hasElement()) {
                return false;
            }
               
            final Element ELEMENT = node.getElement();
            if (ELEMENT instanceof ScriptInstance) {
                final ScriptInstance INSTANCE = (ScriptInstance) ELEMENT;
                return ESS.getChangeForms().containsKey(INSTANCE.getRefID());
                
            } else if (ELEMENT instanceof ChangeForm) {                
                final ChangeForm FORM = (ChangeForm) ELEMENT;
                return CACHE.contains(FORM.getRefID());
                
            } else {
                return false;
            }
        };
    }

    /**
     * Setup a ChangeForm contents filter.
     *
     * @param fieldCodes
     * @return
     */
    private Predicate<Node> createChangeFormContentFilter(String fieldCodesStringsString) {
        LOG.info("Creating 'ChangeFormContent' filter.");
        final String AND = ";";
        final String OR = ",";
        final String NOT = "!";

        if (fieldCodesStringsString == null || fieldCodesStringsString.chars().allMatch(Character::isWhitespace)) {
            return n -> true;
        }
        
        // Split the code in terms.
        String[] termCodes = fieldCodesStringsString.split(AND);
        if (termCodes == null || termCodes.length == 0) {
            return n -> true;
        }
        String[][][] terms = new String[termCodes.length][][];
        
        // Parse the terms into clauses.
        for (int termIndex = 0; termIndex < termCodes.length; termIndex++) {
            String termCode = termCodes[termIndex];
            String[] clauseCodes = termCode.split(OR);
            if (clauseCodes == null || clauseCodes.length == 0) {
                return n -> false;
            }
            terms[termIndex] = new String[clauseCodes.length][];
                   
            // Parse the clause into patterns.
            for (int clauseIndex = 0; clauseIndex < clauseCodes.length; clauseIndex++) {
                String clauseCode = clauseCodes[clauseIndex];
                String[] pattern = clauseCode.split("[\\/]");
                if (pattern == null || pattern.length < 1) {
                    return n-> false;
                }
                
                // Check for negation.
                String head = pattern[0];
                if (head.startsWith(NOT)) {
                    pattern[0] = head.replace(NOT, "");
                    terms[termIndex][clauseIndex] = prepend(NOT, pattern);
                } else {
                    terms[termIndex][clauseIndex] = pattern;
                }
            }
        }
             
        return node -> {
            try {
                if (node.hasElement() && node.getElement() instanceof ChangeForm) {
                    ChangeForm form = (ChangeForm) node.getElement();
                    ChangeFormData data = form.getData(ANALYSIS, CONTEXT, true);
                    if (data != null && data instanceof GeneralElement) {
                        return ((GeneralElement) data).searchMatches(terms);
                    }
                }

                return false;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        };
    }

    static private String[] prepend(String head, String[] tail) {
        String[] result = new String[tail.length + 1];
        result[0] = head;
        System.arraycopy(tail, 0, result, 1, tail.length);
        return result;
    }
    
    /**
     *
     */
    static final private Logger LOG = Logger.getLogger(FilterFactory.class.getCanonicalName());

}
