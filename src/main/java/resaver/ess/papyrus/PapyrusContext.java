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
package resaver.ess.papyrus;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import resaver.ess.Element;
import resaver.ess.Linkable;
import resaver.ess.Plugin;

/**
 *
 * @author Mark
 */
public class PapyrusContext extends resaver.ess.ESS.ESSContext {

    /**
     * Creates a new <code>PapyrusContext</code> from an existing
     * <code>ESSContext</code> and an instance of <code>Papyrus</code>.
     *
     * @param context
     * @param papyrus
     */
    public PapyrusContext(resaver.ess.ESS.ESSContext context, Papyrus papyrus) {
        super(context);
        this.PAPYRUS = Objects.requireNonNull(papyrus);
        //this.REFEREES = new java.util.HashMap<>();
    }

    /**
     * Creates a new <code>PapyrusContext</code> from an existing
     * <code>PapyrusContext</code>.
     *
     * @param context
     */
    public PapyrusContext(PapyrusContext context) {
        super(context);
        this.PAPYRUS = Objects.requireNonNull(context.PAPYRUS);
        //this.REFEREES = new java.util.HashMap<>(context.REFEREES);
    }

    /**
     * Reads an <code>EID</code> from a <code>ByteBuffer</code>. The size of the
     * <code>EID</code> is determined from the <code>ID64</code> flag of the
     * <code>Game</code> field of the relevant <code>ESS</code>.
     *
     * @param input The input stream.
     * @return The <code>EID</code>.
     */
    public EID readEID(ByteBuffer input) {
        return this.getGame().isID64()
                ? this.readEID64(input)
                : this.readEID32(input);
    }

    /**
     * Makes an <code>EID</code> from a <code>long</code>. The actual size of
     * the <code>EID</code> is determined from the <code>ID64</code> flag of the
     * <code>Game</code> field of the relevant <code>ESS</code>.
     *
     * @param val The id value.
     * @return The <code>EID</code>.
     */
    public EID makeEID(Number val) {
        return this.getGame().isID64()
                ? this.makeEID64(val.longValue())
                : this.makeEID32(val.intValue());
    }

    /**
     * Reads a four-byte <code>EID</code> from a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @return The <code>EID</code>.
     */
    public EID readEID32(ByteBuffer input) {
        return EID.read4byte(input, this.PAPYRUS);
    }

    /**
     * Reads an eight-byte <code>EID</code> from a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @return The <code>EID</code>.
     */
    public EID readEID64(ByteBuffer input) {
        return EID.read8byte(input, this.PAPYRUS);
    }

    /**
     * Makes a four-byte <code>EID</code> from an int.
     *
     * @param val The id value.
     * @return The <code>EID</code>.
     */
    public EID makeEID32(int val) {
        return EID.make4byte(val, this.PAPYRUS);
    }

    /**
     * Makes an eight-byte <code>EID</code> from a long.
     *
     * @param val The id value.
     * @return The <code>EID</code>.
     */
    public EID makeEID64(long val) {
        return EID.make8Byte(val, this.PAPYRUS);
    }

    /**
     * Shortcut for getStringTable().readRefID(input)
     *
     * @param input The input stream.
     * @return The new <code>TString</code>.
     * @throws PapyrusFormatException
     */
    public TString readTString(ByteBuffer input) throws PapyrusFormatException {
        return this.PAPYRUS.getStringTable().read(input);
    }

    /**
     * Shortcut for getStringTable().add(s)
     *
     * @param s The new <code>String</code>.
     * @return The new <code>TString</code>.
     */
    public TString addTString(String s) {
        return this.PAPYRUS.getStringTable().addString(s);
    }

    /**
     * Shortcut for getStringTable().get(s)
     *
     * @param index The index of the <code>TString</code>.
     * @return The <code>TString</code>.
     */
    public TString getTString(int index) {
        return this.PAPYRUS.getStringTable().get(index);
    }

    /**
     * Does a very general search for an ID.
     *
     * @param number The data to search for.
     * @return Any match of any kind.
     */
    @Override
    public Linkable broadSpectrumSearch(Number number) {
        HasID r1 = this.findAny(this.makeEID32(number.intValue()));
        if (r1 != null) {
            return r1;
        }

        HasID r2 = this.findAny(this.makeEID64(number.longValue()));
        if (r2 != null) {
            return r2;
        }

        Linkable r3 = super.broadSpectrumSearch(number);
        if (r3 != null) {
            return r3;
        }

        if (number.intValue() >= 0 && number.intValue() < this.PAPYRUS.getStringTable().size()) {
            TString s = this.PAPYRUS.getStringTable().get(number.intValue());
            Linkable r4 = this.findAny(s);
            if (r4 != null) {
                return r4;
            }
        }

        return null;
    }

    /**
     * Search for anything that has the specified name.
     *
     * @param name
     * @return
     */
    public Definition findAny(TString name) {
        return Stream.of(
                this.PAPYRUS.getScripts(),
                this.PAPYRUS.getStructs())
                .filter(c -> c.containsKey(name))
                .map(c -> c.get(name))
                .findAny().orElse(null);
    }

    /**
     * Search for anything that has the specified ID.
     *
     * @param id
     * @return
     */
    public HasID findAny(EID id) {
        if (this.PAPYRUS.getScriptInstances().containsKey(id)) {
            return this.PAPYRUS.getScriptInstances().get(id);
        } else if (this.PAPYRUS.getStructInstances().containsKey(id)) {
            return this.PAPYRUS.getStructInstances().get(id);
        } else if (this.PAPYRUS.getReferences().containsKey(id)) {
            return this.PAPYRUS.getReferences().get(id);
        } else if (this.PAPYRUS.getArrays().containsKey(id)) {
            return this.PAPYRUS.getArrays().get(id);
        } else if (this.PAPYRUS.getActiveScripts().containsKey(id)) {
            return this.PAPYRUS.getActiveScripts().get(id);
        } else if (this.PAPYRUS.getSuspendedStacks1().containsKey(id)) {
            return this.PAPYRUS.getSuspendedStacks1().get(id);
        } else if (this.PAPYRUS.getSuspendedStacks2().containsKey(id)) {
            return this.PAPYRUS.getSuspendedStacks2().get(id);
        } else if (this.PAPYRUS.getUnbinds().containsKey(id)) {
            return this.PAPYRUS.getUnbinds().get(id);
        } else {
            return null;
        }
    }

    public HasID findAll(EID id) {
        return Stream.of(this.PAPYRUS.getScriptInstances(),
                this.PAPYRUS.getStructInstances(),
                this.PAPYRUS.getReferences(),
                this.PAPYRUS.getArrays(),
                this.PAPYRUS.getActiveScripts(),
                this.PAPYRUS.getSuspendedStacks1(),
                this.PAPYRUS.getSuspendedStacks2(),
                this.PAPYRUS.getUnbinds())
                .filter(c -> c.containsKey(id))
                .map(c -> c.get(id))
                .findAny().orElse(null);
    }

    void buildCrossreferences() {
        generalCrossReference = createSearchMap();
        search((a, b) -> generalCrossReference.computeIfAbsent(a, k -> createSearchSet()).add(b));
        pluginsCrossReference = crossReferencePlugins();
    }

    private Map<Element, Set<Element>> getCrossreference(boolean copy) {
        if (copy) {
            final Map<Element, Set<Element>> copiedCrossReference = copySearchMap(generalCrossReference);
            copiedCrossReference.replaceAll((k, v) -> copySearchSet(v));
            return copiedCrossReference;
        } else {
            return generalCrossReference;
        }
    }

    public void search(BiConsumer<Element, Element> consumer) {
        final mf.ConsoleProgress PROGRESS = new mf.ConsoleProgress();
        PROGRESS.toggleQuiet();

        PROGRESS.reset("ChangeForms", this.getESS().getChangeForms().size());
        this.getESS().getChangeForms().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(form -> {
            Plugin plugin = form.getRefID().PLUGIN;
            if (plugin != null) {
                consumer.accept(plugin, form);
            }
        });

        PROGRESS.reset("ScriptInstances", this.getPapyrus().getScriptInstances());
        PAPYRUS.getScriptInstances().values().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(instance -> {
            if (instance.getRefID() != null && instance.getRefID().PLUGIN != null) {
                Plugin plugin = instance.getRefID().PLUGIN;
                consumer.accept(plugin, instance);
            }

            if (instance.getScript() != null) {
                consumer.accept(instance, instance.getScript());
            }

            instance.getVariables().stream()
                    .filter(Objects::nonNull)
                    .filter(var -> var.hasRef())
                    .map(var -> var.getReferent())
                    .filter(Objects::nonNull)
                    .forEach(referent -> consumer.accept(instance, referent));
        });

        PROGRESS.reset("References", this.getPapyrus().getReferences());
        PAPYRUS.getReferences().values().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(instance -> {
            if (instance.getScript() != null) {
                consumer.accept(instance, instance.getScript());
            }

            instance.getVariables().stream()
                    .filter(Objects::nonNull)
                    .filter(var -> var.hasRef())
                    .map(var -> var.getReferent())
                    .filter(Objects::nonNull)
                    .forEach(referent -> consumer.accept(instance, referent));
        });

        PROGRESS.reset("StructInstances", this.getPapyrus().getStructInstances());
        PAPYRUS.getStructInstances().values().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(instance -> {
            if (instance.getStruct() != null) {
                consumer.accept(instance, instance.getStruct());
            }

            instance.getVariables().stream()
                    .filter(Objects::nonNull)
                    .filter(var -> var.hasRef())
                    .map(var -> var.getReferent())
                    .filter(Objects::nonNull)
                    .forEach(referent -> consumer.accept(instance, referent));
        });

        PROGRESS.reset("ActiveScripts", this.getPapyrus().getActiveScripts());
        PAPYRUS.getActiveScripts().values().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(thread -> {
            HasID attached = thread.getAttachedElement();
            if (attached != null) {
                consumer.accept(thread, attached);
            }

            thread.getStackFrames().stream().filter(Objects::nonNull).forEach(frame -> {
                Variable owner = frame.getOwner();
                if (owner != null && owner.hasRef() && owner.getReferent() != null) {
                    consumer.accept(thread, frame.getOwner());
                }

                if (frame.getScript() != null) {
                    consumer.accept(thread, frame.getScript());
                }

                frame.getVariables().stream()
                        .filter(Objects::nonNull)
                        .filter(var -> var.hasRef())
                        .map(var -> var.getReferent())
                        .filter(Objects::nonNull)
                        .forEach(referent -> consumer.accept(thread, referent));
            });
        });

        PROGRESS.reset("SuspendedStacks", this.getPapyrus().getSuspendedStacks());
        PAPYRUS.getSuspendedStacks().values().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(stack -> {
            FunctionMessageData data = stack.getMessage();
            consumer.accept(stack, stack.getScript());
            consumer.accept(stack, stack.getThread());

            if (data != null) {
                consumer.accept(stack, data);

                data.getVariables().stream()
                        .filter(Objects::nonNull)
                        .filter(var -> var.hasRef())
                        .map(var -> var.getReferent())
                        .filter(Objects::nonNull)
                        .forEach(referent -> consumer.accept(stack, referent));
            }
        });

        PROGRESS.reset("FunctionMessages", this.getPapyrus().getFunctionMessages());
        PAPYRUS.getFunctionMessages().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(msg -> {
            if (msg.hasMessage()) {
                FunctionMessageData data = msg.getMessage();
                if (data != null) {
                    consumer.accept(msg, data);

                    data.getVariables().stream()
                            .filter(Objects::nonNull)
                            .filter(var -> var.hasRef())
                            .map(var -> var.getReferent())
                            .filter(Objects::nonNull)
                            .forEach(referent -> consumer.accept(msg, referent));
                }
            }
        });

        PROGRESS.reset("Arrays", this.getPapyrus().getArrays());
        PAPYRUS.getArrays().values().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(array -> {
            if (array.getType().isRefType()) {
                array.getVariables().stream()
                        .filter(Objects::nonNull)
                        .filter(var -> var.hasRef())
                        .map(var -> var.getReferent())
                        .filter(Objects::nonNull)
                        .forEach(referent -> consumer.accept(array, referent));
            }
        });
    }

    private Set<Element> createSearchSet() {
        return new java.util.HashSet<>(30);
    }

    private Set<Element> copySearchSet(Set<Element> s) {
        Objects.requireNonNull(s);
        return new java.util.HashSet<>(s);
    }

    private Map<Element, Set<Element>> createSearchMap() {
        return new java.util.HashMap<>();
    }

    private Map<Element, Set<Element>> copySearchMap(Map<Element, Set<Element>> m) {
        Objects.requireNonNull(m);
        return new java.util.HashMap<>(m);
    }

    public List<DefinedElement> findReferees(Element element) {
        final Map<Element, Set<Element>> REACHABILITY = getCrossreference(false);

        final Set<Element> DIRECT = REACHABILITY.entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(element))
                .map(entry -> entry.getKey())
                .distinct()
                .collect(Collectors.toSet());

        final Set<Element> SECONDARY = REACHABILITY.entrySet()
                .stream()
                .filter(entry -> entry.getValue().stream().anyMatch(e -> DIRECT.contains(e)))
                .map(e -> e.getKey())
                .distinct()
                .collect(Collectors.toSet());

        final List<DefinedElement> REFEREES = Stream.concat(DIRECT.stream(), SECONDARY.stream())
                .filter(e -> e instanceof DefinedElement)
                .map(e -> (DefinedElement) e)
                .distinct()
                .collect(Collectors.toList());

        return REFEREES;
    }

    public Set<Element> getPluginReferences(Plugin plugin) {
        return pluginsCrossReference.getOrDefault(plugin, Collections.emptySet());
    }

    private Map<Plugin, Set<Element>> crossReferencePlugins() {
        //Map<Element, Set<Element>> REACHABILITY = createSearchMap();
        //search((a,b) -> REACHABILITY.computeIfAbsent(a, k -> createSearchSet()).add(b));
        final Map<Element, Set<Element>> REACHABILITY = getCrossreference(true);

        Set<Element> ELIMINATED = new java.util.HashSet<>(50_000);
        Plugin[] KERNEL = this.getESS().getPluginInfo()
                .stream()
                .filter(p -> REACHABILITY.containsKey(p))
                .toArray(l -> new Plugin[l]);

        int pass = 0;
        while (true) {
            pass++;
            //System.out.println("Cross-referencing plugins: pass " + pass);
            boolean removedIntersections = cleanReachability(REACHABILITY, ELIMINATED, KERNEL);
            boolean extendedReachability = extendReachability(REACHABILITY, ELIMINATED, KERNEL);
            if (!removedIntersections && !extendedReachability) {
                break;
            }
        }

        final mf.ConsoleProgress PROGRESS = new mf.ConsoleProgress();
        PROGRESS.toggleQuiet();

        PROGRESS.reset("Collecting plugin forms", this.getESS().getChangeForms());
        this.getESS().getChangeForms().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(form -> {
            Plugin p = form.getRefID().PLUGIN;
            if (p != null) {
                REACHABILITY.computeIfAbsent(p, k -> new java.util.HashSet<>()).add(form);
            }
        });

        PROGRESS.reset("Collecting plugin scripts", this.getPapyrus().getScriptInstances());
        PAPYRUS.getScriptInstances().values().stream().filter(PROGRESS::inc).filter(Objects::nonNull).forEach(instance -> {
            if (instance.getRefID() != null && instance.getRefID().PLUGIN != null) {
                Plugin p = instance.getRefID().PLUGIN;
                REACHABILITY.computeIfAbsent(p, k -> new java.util.HashSet<>()).add(instance);
            }
        });

        return REACHABILITY.entrySet()
                .stream()
                .filter(e -> e.getKey() instanceof Plugin)
                .collect(Collectors.toMap(e -> (Plugin) e.getKey(), e -> e.getValue()));
    }

    static private <T> boolean extendReachability(Map<T, Set<T>> reachability, Set<T> ignore, T[] kernel) {
        boolean extendedReach = false;

        final mf.ConsoleProgress PROGRESS = new mf.ConsoleProgress();
        PROGRESS.toggleQuiet();
        PROGRESS.reset("\tExtending reachability", kernel);
        
        for (T kern : kernel) {
            PROGRESS.inc();
            Set<T> reachable = reachability.get(kern);
            if (reachable != null) {
                Set<T> alsoReachable = reachable.parallelStream()
                        .flatMap(node -> reachability.getOrDefault(node, Collections.emptySet()).stream())
                        .filter(Objects::nonNull)
                        .filter(child -> !ignore.contains(child))
                        .filter(child -> !reachable.contains(child))
                        .collect(Collectors.toSet());

                reachable.addAll(alsoReachable);
                extendedReach = extendedReach || !alsoReachable.isEmpty();
            }
        }

        return extendedReach;
    }

    static private <T> boolean cleanReachability(Map<T, Set<T>> reachability, Set<T> eliminated, T[] kernel) {
        int intersections = 0;
        final java.util.HashSet<T> intersection = new java.util.HashSet<>(2000);

        final mf.ConsoleProgress PROGRESS = new mf.ConsoleProgress();
        PROGRESS.toggleQuiet();
        PROGRESS.reset("\tCleaning reachability", kernel);
        
        for (int i = 0; i < kernel.length; i++) {
            PROGRESS.inc();
            T kern1 = kernel[i];
            Set<T> reachableFrom1 = reachability.get(kern1);
            if (reachableFrom1 != null && !reachableFrom1.isEmpty()) {
                for (int j = i + 1; j < kernel.length; j++) {
                    T kern2 = kernel[j];
                    Set<T> reachableFrom2 = reachability.get(kern2);
                    if (reachableFrom2 != null && !reachableFrom2.isEmpty()) {
                        /*
                        intersection.clear();
                        if (reachableFrom1.size() < reachableFrom2.size()) {
                            intersection.addAll(reachableFrom1);
                            intersection.retainAll(reachableFrom2);
                        } else {
                            intersection.addAll(reachableFrom2);
                            intersection.retainAll(reachableFrom1);
                        }*/
                        intersect(reachableFrom1, reachableFrom2, intersection);

                        reachableFrom1.removeAll(intersection);
                        reachableFrom2.removeAll(intersection);
                        reachability.keySet().removeAll(intersection);
                        eliminated.addAll(intersection);
                        intersections += intersection.size();
                    }
                }
            }
        }

        return intersections > 0;
    }

    static private <T> void intersect(Set<T> a, Set<T> b, Set<T> intersection) {
        if (a.size() > b.size()) {
            intersect(b, a, intersection);
        } else {
            intersection.clear();
            
            for (T x : a) {
                if (b.contains(x)) 
                {
                    intersection.add(x);
                }
            }
        }
    }
    
    public Script findScript(TString name) {
        return this.PAPYRUS.getScripts().getOrDefault(name, null);
    }

    public Struct findStruct(TString name) {
        return this.PAPYRUS.getStructs().getOrDefault(name, null);
    }

    public ScriptInstance findScriptInstance(EID id) {
        return this.PAPYRUS.getScriptInstances().getOrDefault(id, null);
    }

    public StructInstance findStructInstance(EID id) {
        return this.PAPYRUS.getStructInstances().getOrDefault(id, null);
    }

    public Reference findReference(EID id) {
        return this.PAPYRUS.getReferences().getOrDefault(id, null);
    }

    public ArrayInfo findArray(EID id) {
        return this.PAPYRUS.getArrays().getOrDefault(id, null);
    }

    public ActiveScript findActiveScript(EID id) {
        return this.PAPYRUS.getActiveScripts().getOrDefault(id, null);
    }

    public DefinedElement findReferrent(EID id) {
        return this.PAPYRUS.findReferrent(id);
    }

    /**
     * @return The <code>Papyrus</code> itself. May not be full constructed.
     */
    protected Papyrus getPapyrus() {
        return this.PAPYRUS;
    }

    final private Papyrus PAPYRUS;
    //final private Map<Element, Set<Element>> REFEREES;

    private Map<Plugin, Set<Element>> pluginsCrossReference = null;
    private Map<Element, Set<Element>> generalCrossReference = null;

}
