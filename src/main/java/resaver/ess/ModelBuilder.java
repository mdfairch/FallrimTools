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
import java.util.prefs.Preferences;
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
     * @param progress
     */
    public ModelBuilder(ProgressModel progress) {
        progress.setMaximum(36);
        this.MODEL = new FilterTreeModel();
        this.EXECUTOR = java.util.concurrent.Executors.newFixedThreadPool(2);
        this.TASKS = java.util.Collections.synchronizedList(new ArrayList<>(15));
        this.PROGRESS = progress;
        this.ALT_SORT = Preferences.userNodeForPackage(resaver.ReSaver.class).getBoolean("settings.altSort", false);
    }

    /**
     * Add a <code>PluginInfo</code> to the model.
     *
     * @param plugins The <code>PluginInfo</code>.
     */
    public void addPluginInfo(PluginInfo plugins) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final ContainerNode NODE = new ContainerNode("Plugins (full)");
            NODE.addAll(plugins.getFullPlugins().stream().map(p -> new PluginNode(p)).collect(Collectors.toList()));
            PROGRESS.modifyValue(1);
            return NODE;
        }));

        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final ContainerNode NODE = new ContainerNode("Plugins (lite)");
            NODE.addAll(plugins.getLitePlugins().stream().map(p -> new PluginNode(p)).collect(Collectors.toList()));
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
            final ContainerNode NODE = new ContainerNode("Global Variables", gvt.getVariables()).sort(ALT_SORT);
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
                    .map(entry -> new ContainerNode(entry.getKey().toString(), entry.getValue()).sort(ALT_SORT))
                    .collect(Collectors.toList());

            final ContainerNode NODE = new ContainerNode("Strings");
            NODE.addAll(NODES).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("Script Definitions", script.values()).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("Struct Definitions", structs.values()).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("References", references.values()).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("Arrays", arrays.values()).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("QueuedUnbinds", unbinds.values()).sort(ALT_SORT);
            PROGRESS.modifyValue(1);
            return NODE;
        }));
    }

    /**
     * @param unknownIDs The <code>EID</code> list.
     */
    public void addUnknownIDList(List<EID> unknownIDs) {
        this.TASKS.add(this.EXECUTOR.submit(() -> {
            final ContainerNode NODE = new ContainerNode("Unknown ID List", unknownIDs).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("Animations", animations.getAnimations());
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
                    .map(entry -> new ContainerNode(entry.getKey().toString(), entry.getValue()).sort(ALT_SORT))
                    .collect(Collectors.toList());

            final ContainerNode NODE = new ContainerNode("Script Instances").addAll(NODES).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("Struct Instances", instances.values()).sort(ALT_SORT);
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
                        .map(entry -> new ContainerNode(entry.getKey()).addAll(entry.getValue()).sort(ALT_SORT))
                        .collect(Collectors.toList());

                final ContainerNode NODE = new ContainerNode("Active Scripts");
                NODE.addAll(NODE_GROUPS).sort(ALT_SORT);
                PROGRESS.modifyValue(1);
                return NODE;
            }));

        } else {
            this.TASKS.add(this.EXECUTOR.submit(() -> {
                final ContainerNode NODE = new ContainerNode("Active Scripts");
                NODE.addAll(threads.values().stream().map(t -> new ActiveScriptNode(t)).collect(Collectors.toList())).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("Function Messages");
            NODE.addAll(messages.stream().map(t -> new FunctionMessageNode(t)).collect(Collectors.toList())).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("Suspended Stacks 1");
            NODE.addAll(stacks.values().stream().map(t -> new SuspendedStackNode(t)).collect(Collectors.toList())).sort(ALT_SORT);
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
            final ContainerNode NODE = new ContainerNode("Suspended Stacks 2");
            NODE.addAll(stacks.values().stream().map(t -> new SuspendedStackNode(t)).collect(Collectors.toList())).sort(ALT_SORT);
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
                    .map(entry -> new ContainerNode(entry.getKey().toString(), entry.getValue()).sort(ALT_SORT))
                    .collect(Collectors.toList());

            final ContainerNode NODE = new ContainerNode("ChangeForms").addAll(NODES).sort(ALT_SORT);
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
                        OTHERDATA_NODES.add(new ContainerNode(key, Arrays.asList(array)));
                    }
                });
            }

            final ContainerNode NODE = new ContainerNode("Mystery Arrays").addAll(OTHERDATA_NODES).sort(ALT_SORT);
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

    static public FilterTreeModel createModel(ESS ess, ProgressModel progress) {
        Objects.requireNonNull(ess);
        Objects.requireNonNull(progress);
        
        final ModelBuilder MB = new ModelBuilder(progress);
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
    final private boolean ALT_SORT;

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
}
