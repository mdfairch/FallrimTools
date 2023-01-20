/*
 * Copyright 2018 Mark.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import resaver.ProgressModel;
import resaver.ess.papyrus.ActiveScript;
import resaver.ess.papyrus.ActiveScriptMap;
import resaver.ess.papyrus.ArrayMap;
import resaver.ess.papyrus.EID;
import resaver.ess.papyrus.FunctionMessage;
import resaver.ess.papyrus.OtherData;
import resaver.ess.papyrus.ScriptInstanceMap;
import resaver.ess.papyrus.Papyrus;
import resaver.ess.papyrus.PapyrusContext;
import resaver.ess.papyrus.ReferenceMap;
import resaver.ess.papyrus.ScriptInstance;
import resaver.ess.papyrus.ScriptMap;
import resaver.gui.FilterTreeModel;
import resaver.gui.FilterTreeModel.Node;
import resaver.ess.papyrus.StringTable;
import resaver.ess.papyrus.StructMap;
import resaver.ess.papyrus.StructInstanceMap;
import resaver.ess.papyrus.SuspendedStackMap;
import resaver.ess.papyrus.TString;
import resaver.ess.papyrus.UnbindMap;
import resaver.gui.FilterTreeModel.RootNode;
import resaver.gui.FilterTreeModel.ContainerNode;
import resaver.gui.FilterTreeModel.PluginNode;
import resaver.gui.FilterTreeModel.ActiveScriptNode;
import resaver.gui.FilterTreeModel.FunctionMessageNode;
import resaver.gui.FilterTreeModel.SuspendedStackNode;

/**
 *
 * @author Mark
 */
public class ModelBuilder {

    /**
     * Creates a new ModelBuilder with no sorting.
     * 
     * @param progress Used to send progress updates.
     */
    public ModelBuilder(ProgressModel progress) {
        this(progress, SortingMethod.NONE, null);
    }
    
    /**
     * Creates a new ModelBuilder with sorting.
     * 
     * @param progress Used to send progress updates.
     * @param sort How to sort nodes. MASS will be ignored. 
     */
    public ModelBuilder(ProgressModel progress, SortingMethod sort) {
        this(progress, sort, null);
    }
    
    /**
     * Creates a new ModelBuilder with mass sorting allowed.
     * 
     * @param progress Used to send progress updates.
     * @param sort How to sort nodes.
     * @param ess The savefile, for calculating node masses. Only required when sort=MASS.
     */
    public ModelBuilder(ProgressModel progress, SortingMethod sort, ESS ess) {
        Objects.requireNonNull(progress);
        Objects.requireNonNull(sort);
        
        progress.setMaximum(36);
        this.MODEL = new FilterTreeModel();
        this.EXECUTOR = java.util.concurrent.Executors.newFixedThreadPool(2);
        this.TASKS = java.util.Collections.synchronizedList(new ArrayList<>(15));
        this.PROGRESS = progress;
        
        switch (sort) {
            case NONE:
                this.COMPARE_NODES = null;
                this.COMPARE_ELEMENTS = null;
                break;
            case ALPHA:
                this.COMPARE_NODES = byName;
                this.COMPARE_ELEMENTS = byName;
                break;
            case SIZE:
                this.COMPARE_NODES = byCount;
                this.COMPARE_ELEMENTS = bySize;
                break;
            case MASS:
                if (ess != null) {
                    //Comparator<Object> massComparator = getByMass(ess);
                    this.COMPARE_NODES = getByMassNode(ess);
                    this.COMPARE_ELEMENTS = getByMassElement(ess);
                } else {
                    this.COMPARE_NODES = byName;
                    this.COMPARE_ELEMENTS = byName;
                }   break;
            default:
                this.COMPARE_NODES = byName;
                this.COMPARE_ELEMENTS = byName;
                break;
        }
    }

    /**
     * Add a <code>PluginInfo</code> to the model.
     *
     * @param plugins The <code>PluginInfo</code>.
     */
    public void addPluginInfo(PluginInfo plugins) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Plugins (full)");
            NODE.addAll(plugins.getFullPlugins().stream().map(p -> new PluginNode(p)).collect(Collectors.toList()));
            PROGRESS.modifyValue(1);
            NODE.sort();
            //System.out.println("Plugins-Full sorted.");
            PROGRESS.modifyValue(1);
            return NODE;
        }));

        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Plugins (lite)");
            NODE.addAll(plugins.getLitePlugins().stream().map(p -> new PluginNode(p)).collect(Collectors.toList()));
            PROGRESS.modifyValue(1);
            NODE.sort();
            //System.out.println("Plugins-Lite sorted.");
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>GlobalVariableTable</code> to the model.
     *
     * @param gvt The <code>GlobalVariableTable</code>.
     */
    public void addGlobalVariableTable(GlobalVariableTable gvt) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Global Variables", gvt.getVariables()).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>StringTable</code> to the model.
     *
     * @param table The <code>StringTable</code>.
     */
    public void addStringTable(StringTable table) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final Map<Character, List<TString>> DICTIONARY = table.stream()
                    .collect(Collectors.groupingBy(ALPHABETICAL));

            final List<Node> NODES = DICTIONARY.entrySet().stream()
                    .map(entry -> new GroupNode(entry.getKey().toString(), entry.getValue()).sort())
                    .collect(Collectors.toList());

            final GroupNode NODE = new GroupNode("Strings");
            NODE.addAll(NODES).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>ScriptMap</code> to the model.
     *
     * @param script The <code>ScriptMap</code>.
     */
    public void addScripts(ScriptMap script) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Script Definitions", script.values()).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>StructMap</code> to the model.
     *
     * @param structs The <code>StructMap</code>.
     */
    public void addStructs(StructMap structs) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Struct Definitions", structs.values()).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>ReferenceMap</code> to the model.
     *
     * @param references The <code>ReferenceMap</code>.
     */
    public void addReferences(ReferenceMap references) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("References", references.values()).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add an <code>ArrayMap</code> to the model.
     *
     * @param arrays The <code>ArrayMap</code>.
     */
    public void addArrays(ArrayMap arrays) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Arrays", arrays.values()).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>UnbindMap</code> to the model.
     *
     * @param unbinds The <code>UnbindMap</code>.
     */
    public void addUnbinds(UnbindMap unbinds) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("QueuedUnbinds", unbinds.values()).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * @param unknownIDs The <code>EID</code> list.
     */
    public void addUnknownIDList(List<EID> unknownIDs) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Unknown ID List", unknownIDs).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>GlobalVariableTable</code> to the model.
     *
     * @param animations The <code>GlobalVariableTable</code>.
     */
    public void addAnimations(AnimObjects animations) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Animations", animations.getAnimations());
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>ScriptInstanceMap</code> to the model.
     *
     * @param instances The <code>ScriptInstanceMap</code>.
     */
    public void addScriptInstances(ScriptInstanceMap instances) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final Map<Character, List<ScriptInstance>> DICTIONARY = instances.values().stream()
                    .collect(Collectors.groupingBy(ALPHABETICAL));

            final List<Node> NODES = DICTIONARY.entrySet().stream()
                    .map(entry -> new GroupNode(entry.getKey().toString(), entry.getValue()).sort())
                    .collect(Collectors.toList());

            final ContainerNode NODE = new GroupNode("Script Instances").addAll(NODES).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>StructInstanceMap</code> to the model.
     *
     * @param instances The <code>StructInstanceMap</code>.
     */
    public void addStructInstances(StructInstanceMap instances) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Struct Instances", instances.values()).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a <code>ActiveScriptMap</code> to the model.
     *
     * @param threads The <code>ActiveScriptMap</code>.
     */
    public void addThreads(ActiveScriptMap threads) {
        if (threads.size() > 1000) {
            this.TASKS.add(this.EXECUTOR.submit(() -> {
                final Map<String, List<ActiveScriptNode>> GROUPS = new java.util.LinkedHashMap<>();
                List<ActiveScriptNode> currentGroup = null;
                
                int index = 0;
                
                for (ActiveScript thread : threads.values()) {
                    if (currentGroup == null || currentGroup.size() >= 1000) {
                        currentGroup = new ArrayList<>(1000);
                        GROUPS.put(String.format("%d to %d", index, index+999), currentGroup);
                    }
                    
                    currentGroup.add(new ActiveScriptNode(thread));
                    index++;
                }
                
                final List<ContainerNode> NODE_GROUPS = GROUPS.entrySet().stream()
                        .map(entry -> new GroupNode(entry.getKey()).addAll(entry.getValue()).sort())
                        .collect(Collectors.toList());

                final GroupNode NODE = new GroupNode("Active Scripts");
                NODE.addAll(NODE_GROUPS).sort();
                PROGRESS.modifyValue(1);
                return NODE;
            }));

        } else {
            this.TASKS.add(this.EXECUTOR.submit(() -> {
                final GroupNode NODE = new GroupNode("Active Scripts");
                NODE.addAll(threads.values().stream().map(t -> new ActiveScriptNode(t)).collect(Collectors.toList())).sort();
                PROGRESS.modifyValue(1);
                return NODE;
            }));
        }
    }

    /**
     * Add a list of <code>FunctionMessage</code> to the model.
     *
     * @param messages The list of <code>FunctionMessage</code>.
     */
    public void addFunctionMessages(List<FunctionMessage> messages) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Function Messages");
            NODE.addAll(messages.stream().map(t -> new FunctionMessageNode(t)).collect(Collectors.toList())).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a list of <code>SuspendedStack</code> to the model.
     *
     * @param stacks The list of <code>SuspendedStack</code>.
     */
    public void addSuspendedStacks1(SuspendedStackMap stacks) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Suspended Stacks 1");
            NODE.addAll(stacks.values().stream().map(t -> new SuspendedStackNode(t)).collect(Collectors.toList())).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add a list of <code>SuspendedStack</code> to the model.
     *
     * @param stacks The list of <code>SuspendedStack</code>.
     */
    public void addSuspendedStacks2(SuspendedStackMap stacks) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final GroupNode NODE = new GroupNode("Suspended Stacks 2");
            NODE.addAll(stacks.values().stream().map(t -> new SuspendedStackNode(t)).collect(Collectors.toList())).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     *
     * @param changeForms
     */
    public void addChangeForms(ChangeFormCollection changeForms) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final Map<ChangeForm.Type, List<ChangeForm>> DICTIONARY = changeForms.stream().collect(Collectors.groupingBy(form -> form.getType()));

            final List<Node> NODES = DICTIONARY.entrySet().stream()
                    .map(entry -> new GroupNode(entry.getKey().toString(), entry.getValue()).sort())
                    .collect(Collectors.toList());

            final ContainerNode NODE = new GroupNode("ChangeForms").addAll(NODES).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * Add <code>OtherData</code> to the model.
     *
     * @param data The <code>OtherData</code>.
     */
    public void addOtherData(OtherData data) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final List<FilterTreeModel.Node> OTHERDATA_NODES = new ArrayList<>(10);

            if (data != null) {
                data.getValues().forEach((key, val) -> {
                    if (val instanceof Element[]) {
                        Element[] array = (Element[]) val;
                        OTHERDATA_NODES.add(new GroupNode(key.toString(), Arrays.asList(array)));
                    }
                });
            }

            final ContainerNode NODE = new GroupNode("Mystery Arrays").addAll(OTHERDATA_NODES).sort();
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     *
     * @param ess
     * @return
     */
    public FilterTreeModel finish(ESS ess) {
        try {
            this.EXECUTOR.shutdown();
            this.EXECUTOR.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            LOG.log(Level.SEVERE, "Model building was interrupted.", ex);
            return null;
        }

        if (!this.TASKS.stream().allMatch(task -> task.isDone())) {
            LOG.severe("Some tasks didn't finish.");
            return null;
        }

        // Populate the root elementNode.
        final ArrayList<FilterTreeModel.Node> ROOT_NODES = new ArrayList<>(15);

        this.TASKS.forEach(task -> {
            try {
                final Node NODE = task.get();
                ROOT_NODES.add(NODE);
                this.PROGRESS.modifyValue(1);
            } catch (InterruptedException | ExecutionException ex) {
                throw new IllegalStateException("ModelBuilding failed.", ex);
            }
        });

        final FilterTreeModel.Node ROOT = new RootNode(ess, ROOT_NODES);
        MODEL.setRoot(ROOT);
        return MODEL;
    }

    /**
     * Creates and populates a ModelBuilder all in one step.
     * 
     * @param progress
     * @param sort
     * @param ess
     * @return 
     */
    static public FilterTreeModel createModel(ProgressModel progress, SortingMethod sort, ESS ess) {
        Objects.requireNonNull(ess);
        Objects.requireNonNull(progress);

        
        final ModelBuilder MB = new ModelBuilder(progress, sort, ess);
        Papyrus papyrus = ess.getPapyrus();

        MB.addPluginInfo(ess.getPluginInfo());
        MB.addGlobalVariableTable(ess.getGlobals());

        MB.addChangeForms(ess.getChangeForms());
        MB.addStringTable(papyrus.getStringTable());
        MB.addScripts(papyrus.getScripts());

        if (ess.isFO4()) {
            MB.addStructs(papyrus.getStructs());
        }

        MB.addScriptInstances(papyrus.getScriptInstances());

        if (ess.isFO4()) {
            MB.addStructInstances(papyrus.getStructInstances());
        }

        MB.addReferences(papyrus.getReferences());
        MB.addArrays(papyrus.getArrays());
        MB.addThreads(papyrus.getActiveScripts());
        MB.addFunctionMessages(papyrus.getFunctionMessages());
        MB.addSuspendedStacks1(papyrus.getSuspendedStacks1());
        MB.addSuspendedStacks2(papyrus.getSuspendedStacks2());
        MB.addUnknownIDList(papyrus.getUnknownIDList());
        MB.addUnbinds(papyrus.getUnbinds());
        MB.addAnimations(ess.getAnimations());
        return MB.finish(ess);
    }

    final private FilterTreeModel MODEL;
    final private ExecutorService EXECUTOR;
    final private List<Future<Node>> TASKS;
    final private ProgressModel PROGRESS;
    final private Comparator<? super Node> COMPARE_NODES;
    final private Comparator<? super Element> COMPARE_ELEMENTS;

    /**
     * Maps a <code>TString</code> to a character.
     */
    static final Function<Element, Character> ALPHABETICAL = (v) -> {
        String str = v.toString();
        char firstChar = str.length() == 0 ? '0' : Character.toUpperCase(str.charAt(0));
        char category = Character.isLetter(firstChar) ? firstChar : '0';
        return category;
    };

    static final private Logger LOG = Logger.getLogger(ModelBuilder.class.getCanonicalName());



    static public Comparator<Node> byCount = (n1, n2) -> Integer.compare(n1.countLeaves(), n2.countLeaves());
    static public Comparator<Object> byName = (o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString());
    static public Comparator<Element> bySize = (e1, e2) -> Integer.compare(e1.calculateSize(), e2.calculateSize());
    
    
    final public Comparator<Element> getByMassElement(ESS ess) {
        resaver.ess.papyrus.PapyrusContext context = ess.getPapyrus().getContext();
        return (e1, e2) -> Integer.compare(getMass(e2, context), getMass(e1, context));
    }
    
    final public Comparator<Node> getByMassNode(ESS ess) {
        resaver.ess.papyrus.PapyrusContext context = ess.getPapyrus().getContext();
        return (n1, n2) -> Integer.compare(calculateMassN(n2, context), calculateMassN(n1, context));
    }
    
    //final Map<Node, Integer> massesNodes = new java.util.concurrent.ConcurrentHashMap<>(10000);
    final Map<Element, Integer> massesElements = new java.util.concurrent.ConcurrentHashMap<>(10000);
    
    final int getMass(Element element, PapyrusContext context) {
        return massesElements.computeIfAbsent(element, e -> calculateMassE(element, context));
    }
    
    //final int getMass(Node node, PapyrusContext context) {
    //    return massesNodes.computeIfAbsent(node, n -> calculateMassN(node, context));
    //}
    
    int calculateMassE(Element element, PapyrusContext context) {
        if (element instanceof Plugin) {
            Plugin plugin = (Plugin) element;
            Plugin.PluginMetrics metrics = plugin.createPluginMetrics(context.getESS(), null);
            System.out.printf("Calculated %06d bytes for %s", metrics.uniqueData, plugin.toString());
            return metrics.uniqueData;
            //java.util.Set<Element> uniqueRefs = context.getPluginReferences(plugin);
            //int size = uniqueRefs.stream().mapToInt(e -> e.calculateSize()).sum();
            //return size;
        } else if (element != null) {
            return element.calculateSize();
        } else {
            assert false : "NULL!";
            return 0;
        }
    }

    int calculateMassN(Node node, PapyrusContext context) {
        if (node == null) {
            assert false : "NULL!";
            return 0;
        }
        
        if (node.hasElement() && node.getElement() instanceof Plugin) {
            int k = 0;            
        }
        if (node.hasElement(Plugin.class)) {
            int k = 0;
        }
        
        int mass = 0;
        if (node.hasElement()) {
            mass += getMass(node.getElement(), context);
        }
        
        if (!node.isLeaf()) {
            mass += node.getChildren().stream().mapToInt(n -> calculateMassN(n, context)).sum();
        }
        
        return mass;
    }

    private class GroupNode extends FilterTreeModel.ContainerNode {
        
        GroupNode(String name, Collection<? extends Element> elements) {
            super(name, elements);
        }
        
        GroupNode(String name) {
            super(name);
        }
        /**
         * Sorts the children of the node.
         *
         * @param compareNodes 
         * @param compareElements 
         * @return The <code>Node</code> itself, to allow for chaining.
         * 
         */
        @Override
        public GroupNode sort() {
            if (COMPARE_ELEMENTS == null || COMPARE_ELEMENTS == null) {
                return this;
            }
            if (this.getName().contains("Plugin")) {
                int k = 0;
                System.out.println("\tSorting plugin group");
            }
            
            this.getChildren().sort((n1, n2) -> {
                if ((n1.hasElement() && n1.getElement() instanceof Plugin)
                        || (n2.hasElement() && n2.getElement() instanceof Plugin)) {
                    int k = 0;
                }
                if (n1.hasElement() && n2.hasElement()) {
                    Element e1 = n1.getElement();
                    Element e2 = n2.getElement();
                    return COMPARE_ELEMENTS.compare(e1, e2);
                } else {
                    return COMPARE_NODES.compare(n1, n2);
                }
            });

            return this;
        }

    
    }
}
