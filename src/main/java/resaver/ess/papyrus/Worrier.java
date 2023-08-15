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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Function;
import resaver.Game;
import resaver.ess.ESS;
import resaver.ess.Header;
import static resaver.ResaverFormatting.makeHTMLList;
import resaver.ess.WStringElement;
import static j2html.TagCreator.*;
import j2html.tags.DomContent;
import java.util.HashMap;

/**
 *
 * @author Mark
 */
final public class Worrier {

    public Worrier(ESS.Result result, Optional<Worrier> previous) {
        this.shouldWorry = false;
        this.disableSaving = false;
        this.ESS = result.ESS;
        this.PAPYRUS = Optional.of(result.ESS).map(ess -> ess.getPapyrus()).orElse(null);
        
        // Get data from previous worrier for comparison.
        this.previousESS = previous.map(p -> p.ESS);
        this.previousCanaries = previous.map(p -> p.canaries).orElse(new HashMap<>());
        this.previousNamespaces = previous.map(p -> p.namespaces).orElse(new HashMap<>());
        
        // Find all the namespaces (if they exist)
        this.namespaces = PAPYRUS.getScriptInstances().values().parallelStream()
                .filter(instance -> instance.getScriptName().toString().contains(":"))
                .collect(Collectors.groupingBy(instance -> instance.getScriptName().toString().split(":")[0]));

        // Find all the canaries
        this.canaries = PAPYRUS.getScriptInstances().values().parallelStream()
                .filter(instance -> instance.hasCanary())
                .collect(Collectors.toMap(instance -> instance.getScript(), instance -> instance.getCanary()));      
        
        // Do the analysis.
        this.MESSAGE = this.check(result);
    }
   
    public DomContent getMessage() {
        return this.MESSAGE;
    }

    public boolean shouldDisableSaving() {
        return this.disableSaving;
    }

    public boolean shouldWorry() {
        return this.shouldWorry;
    }

    private void fatal(String tag, String msg) {
        msg(tag, text(msg), true);
    }
    
    private void warn(String tag, String msg) {
        msg(tag, text(msg), false);
    }
    
    private void msg(String tag, DomContent msg, boolean fatal) {
        //if (fatal) MESSAGES_FATAL.add(p(strong(em(tag)), msg));
        //else MESSAGES_WARNING.add(p(strong(tag), msg));
        if (fatal) MESSAGES_FATAL.add(div(h3(tag), msg, br()));
        else MESSAGES_WARNING.add(div(h3(tag), msg, br()));
    }
    
    private DomContent check(ESS.Result result) {
        MESSAGES_FATAL.clear();
        MESSAGES_WARNING.clear();
        
        this.shouldWorry = false;
        this.disableSaving = false;

        this.checkFatal();
        this.checkNonFatal(result);
        
        return div(
                this.shouldDisableSaving() 
                        ? p(h1("Serious problems were identified"), h2("Saving is disabled. Trust me, it's for your own good."), hr())
                        : emptyTag("IGNORE"),
                each(this.MESSAGES_FATAL, t->t),
                hr(),
                this.checkPerformance(result),
                hr(),
                this.shouldWorry()
                        ? h2(this.shouldDisableSaving() 
                                ? "Additional problems were identified"
                                : "Potential problems were identified")
                        : emptyTag("No worries"),
                hr(),
                each(this.MESSAGES_WARNING, t->t)
        );
    }

    private void checkFatal() {
        // Check the first fatal condition -- truncation.
        if (ESS.isBroken()) {
            this.shouldWorry = true;
            this.disableSaving = true;
            
            fatal("Broken savefile", "It is corrupted and can never be recovered, not even by the unhindered zeal of an Andean Mountain Tapir. ");

            if (ESS.isPluginOverflow()) {
                fatal("Plugin overflow", "This is caused by having exactly 255 or 256 plugins installed.");
            }

            if (ESS.isTruncated()) {
                fatal("Truncated file", "This is usually caused by too many scripts running at once, recursive scripts without proper boundary conditions, excessive size, or multithreading problems.");
            }
                
            if (PAPYRUS == null) {
                fatal("NO papyrus section", "The entire papyrus block is missing. This is sometimes caused by Papyrus being overloaded or by the game running too slowly to keep up with itself.");
                
            } else {
                if (PAPYRUS.getStringTable().isTruncated()) {
                    int missing = PAPYRUS.getStringTable().getMissingCount();
                    fatal("Truncated string-table", String.format("%d strings missing. The cause of this is unknown, but sometimes involves the scripts that append to strings in a loop.", missing));
                } 
                
                if (PAPYRUS.isTruncated()) {
                    fatal("Truncated papyrus block", "The Papyrus block is truncated (part of it is missing). This is usually caused by too many scripts running at once, recursive scripts without proper boundary conditions, excessive size, or multithreading problems. Sometimes it means that your savefile has data in it that ReSaver doesn't recognize.");
                }

                if (Arrays.stream(ESS.getFormIDs()).anyMatch(i -> i == 0)) {
                    long zeroes = Arrays.stream(ESS.getFormIDs()).filter(i -> i == 0).count();
                    long total = ESS.getFormIDs().length;
                    int present = 0;
                    while (present < ESS.getFormIDs().length && ESS.getFormIDs()[present] != 0) {
                        present++;
                    }
                    fatal("Truncated formID array", String.format("%d/%d formIDs read and %d null values in total. This is sometimes caused by updating mods without following their proper updating procedure.", present, total, zeroes));
                }
            }
        }

        // Check the second fatal condition -- the string table bug.
        if (PAPYRUS != null && PAPYRUS.getStringTable().hasSTB()) {
            this.shouldWorry = true;
            this.disableSaving = true;
            fatal("The string-table bug", "This usually only happens in Skyrim Legendary Edition without Meh321's patch, or in very very old versions of Fallout 4. It is a fatal error and the savefile can never be recovered, not even with lasers or cheetah blood.");
        }
    }

    private void checkNonFatal(ESS.Result result) {
        if (PAPYRUS == null) {
            this.shouldWorry = true;
            return;
        }

        int unattached = PAPYRUS.countUnattachedInstances();
        if (unattached > 0) {
            this.shouldWorry = (result.GAME != Game.FALLOUT4 || unattached > 2);
            warn("Unattached instances", String.format("There are %s script instances that are not attached to anything in-game. This is usually caused by uninstalling mods, or by updating mods that are not safe to update.", unattached));
        }

        int[] undefined = PAPYRUS.countUndefinedElements();

        if (undefined[0] > 0) {
            this.shouldWorry = true;
            warn("Undefined elements", String.format("There are %d elements whose definition is missing. This is usually caused by updating or uninstalling mods.", undefined[0]));
        }

        if (undefined[1] > 0) {
            this.shouldWorry = true;
            warn("Undefined threads", String.format("There are %d undefined threads. This is a serious problem, which is usually caused by updating or uninstalling mods.", undefined[1]));
        }

        long missingParents = PAPYRUS.getScripts().values().stream().filter(s -> s.isMissingParent()).count();
        if (missingParents > 0) {
            this.shouldWorry = true;
            warn("Missing parents", String.format("There are %s scripts with missing parents. This is usually caused by updating a mod to a new version that has major script changes.", missingParents));
        }

        long noParents = PAPYRUS.getScripts().values().stream().filter(s -> s.isNoParent()).count();
        if (noParents > 0) {
            this.shouldWorry = true;
            warn("No parents", String.format("There are %s scripts with no parent script. This is usually caused by updating a mod to a new version that has major script changes.", noParents));
        }

        int numStacks = PAPYRUS.getActiveScripts().size();

        Stream<Script> activeThreads = PAPYRUS.getActiveScripts().values().parallelStream()
                .filter(as -> as.hasStack() && !as.getStackFrames().isEmpty())
                .map(as -> as.getStackFrames().get(0))
                .map(f -> f.getScript());
                
        Stream<Script> activeStacks = PAPYRUS.getActiveScripts().values().parallelStream()
                .filter(as -> as.hasStack())
                .flatMap(as -> as.getStackFrames().stream())
                .map(f -> f.getScript());

        Stream<Script> suspendedThreads = PAPYRUS.getSuspendedStacks().values().parallelStream()
                .filter(ss -> ss.getScript() != null)
                .map(ss -> ss.getScript());

        Stream<Script> suspendedStacks = PAPYRUS.getSuspendedStacks().values().parallelStream()
                .filter(ss -> ss.getScript() != null)
                .map(ss -> ss.getScript());

        Map<Script, Long> threadCounts = Stream.concat(activeThreads, suspendedThreads)
                .filter(s -> s != null)
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()));
                
        Map<Script, Long> frameCounts = Stream.concat(activeStacks, suspendedStacks)
                .filter(s -> s != null)
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()));

        List<Script> threads = new ArrayList<>(threadCounts.keySet());
        List<Script> frames = new ArrayList<>(frameCounts.keySet());

        if (!frames.isEmpty()) {
            threads.sort((a, b) -> threadCounts.get(b).compareTo(threadCounts.get(a)));
            frames.sort((a, b) -> frameCounts.get(b).compareTo(frameCounts.get(a)));
            int numFrames = (int)frameCounts.values().stream().mapToLong(v -> v).sum();
            
            if (numStacks > 50 || numFrames > 150) {
                this.shouldWorry = true;
                warn("Stack count", String.format("There are %d stacks and %d frames. This may indicate a serious problem, but it can also just mean that Papyrus is overloaded because of low FPS or too many mods doing things at the same time.", numStacks, numFrames));

                if (numStacks > 200 || numFrames > 1000) {
                    if (frames.size() >= 1) {
                        warn("Frame count", String.format("%s occurs the most often as a stack frame (%d occurrences)</p>", frames.get(0).toHTML(null), frameCounts.get(frames.get(0))));
                    }
                    if (frames.size() >= 2) {
                        warn("Frame count", String.format("%s occurs the second most often as a stack frame (%d occurrences)</p>", frames.get(1).toHTML(null), frameCounts.get(frames.get(1))));
                    }
                }
            }

            List<ActiveScript> deep = PAPYRUS.getActiveScripts().values().stream()
                    .filter(thread -> thread.getStackFrames().size() >= 100)
                    .collect(Collectors.toList());
            deep.sort((a1, a2) -> Integer.compare(a2.getStackFrames().size(), a1.getStackFrames().size()));

            if (!deep.isEmpty()) {
                this.shouldWorry = true;
                ActiveScript deepest = deep.get(0);
                int depth = deepest.getStackFrames().size();
                warn("Stack depth", String.format("There is a stack %d frames deep (%s).", depth, deepest.toHTML(null)));
            }
        }

        previousESS.ifPresent(previous -> {
            if (areProbablySequential(previous, result.ESS)) {
                Header H1 = previous.getHeader();
                Header H2 = result.ESS.getHeader();

                if (WStringElement.compare(H1.NAME, H2.NAME) == 0 && H1.FILETIME < H2.FILETIME) {
                    int previousSize = previous.calculateSize();
                    int currentSize = result.ESS.calculateSize();
                    double difference = 200.0 * (currentSize - previousSize) / (currentSize + previousSize);
                    if (difference < -5.0) {
                        this.shouldWorry = true;
                        warn("Data drop", String.format("This savefile has %2.2f%% less papyrus data the previous one. This most often happens because of finishing a major quest or leaving a cell with lots of non-persistent objects. Sometimes it means that important data was truncated in a way that is not easily detectable.", -difference));
                    }
                }

                List<String> missingNamespaces = this.previousNamespaces.keySet().stream()
                        .filter(namespace -> !namespaces.containsKey(namespace))
                        .filter(namespace -> this.previousNamespaces.get(namespace).stream()
                        .map(instance -> instance.getRefID())
                        .filter(refID -> !refID.isZero())
                        .anyMatch(refID -> result.ESS.getChangeForms().containsKey(refID)))
                        .collect(Collectors.toList());

                if (!missingNamespaces.isEmpty()) {
                    this.shouldWorry = true;
                    msg("Canary error", makeHTMLList("This savefile has missing namespaces. This can be caused by uninstalling or updating mods, or by what Kinggath named the 'canary error'.", missingNamespaces, LIMIT), false);
                }

                List<mf.Pair<Script,Integer>> canaryErrors = this.previousCanaries.keySet().stream()
                        .filter(script -> canaries.containsKey(script))
                        .filter(script -> previousCanaries.get(script) != 0)
                        .filter(script -> canaries.get(script) == 0)
                        .map(mf.Pair.mapper(s->s, s->previousCanaries.get(s)))
                        .collect(Collectors.toList());

                if (!canaryErrors.isEmpty()) {
                    this.shouldWorry = true;
                    msg("Canary error", makeHTMLList("This savefile has zeroed canaries. The cause of this seems to be related to script memory limits but it is not clear; Kinggath named it the 'canary error' if you want to search for more information.", canaryErrors, LIMIT, CanaryErrorFormatter), false);
                }                
            }
        });
        
        List<ScriptInstance> memberless = PAPYRUS.getScriptInstances().values()
                .parallelStream()
                .filter(instance -> instance.hasMemberlessError())
                .collect(Collectors.toList());

        if (!memberless.isEmpty()) {
            this.shouldWorry = true;
            msg("Missing member data", makeHTMLList("This savefile has zeroed canaries. The cause of this seems to be related to script memory limits but it is not clear; Kinggath named it the 'canary error' if you want to search for more information.", memberless, LIMIT, MemberlessFormatter), false);
        }

        List<ScriptInstance> definitionErrors = PAPYRUS.getScriptInstances().values()
                .parallelStream()
                .filter(instance -> instance.hasDefinitionError())
                .collect(Collectors.toList());

        if (!definitionErrors.isEmpty()) {
            this.shouldWorry = true;
            msg("Mismatched member data", makeHTMLList("This savefile has script instances with mismatched member data. This is usually caused by updating mods that weren't designed to be safe to update.", definitionErrors, LIMIT, DefinitionErrorFormatter), false);
        }
    }

    private DomContent checkPerformance(ESS.Result result) {
        return p(text("The savefile was successfully loaded."), 
            ul(
                    li(String.format("Read %1.1f mb in %1.1f seconds.", result.TIME_S, result.SIZE_MB)),
                    li(result.ESS.hasCosave() 
                            ? String.format("%s co-save was loaded.", result.GAME.COSAVE_EXT.toUpperCase())
                            : "No co-save was found.")
                    
            )
        );
    }

    static private boolean areProbablySequential(ESS prev, ESS next) {
        Objects.requireNonNull(prev);
        Objects.requireNonNull(next);
        
        final Header H1 = prev.getHeader();
        final Header H2 = next.getHeader();
        
        return H1.GAME == H2.GAME
                && H1.NAME.equals(H2.NAME)
                && H2.SAVENUMBER > H1.SAVENUMBER
                && H2.SAVENUMBER - H1.SAVENUMBER < 10;
    }
    
    private boolean shouldWorry;
    private boolean disableSaving;
    final private DomContent MESSAGE;
    final private List<DomContent> MESSAGES_FATAL = new ArrayList<>();
    final private List<DomContent> MESSAGES_WARNING = new ArrayList<>();
    
    final private ESS ESS;
    final private Papyrus PAPYRUS;
    final private Map<Script, Integer> canaries;
    final private Map<String, List<ScriptInstance>> namespaces;
    
    private Optional<ESS> previousESS;
    final private Map<Script, Integer> previousCanaries;
    final private Map<String, List<ScriptInstance>> previousNamespaces;

    static final private int LIMIT = 12;
    
    static final private Function<mf.Pair<Script,Integer>,String> CanaryErrorFormatter = i -> MessageFormat.format("{0} ({1}->0)", i.A.toHTML(null), i.B);    
    static final private Function<ScriptInstance,String> MemberlessFormatter = i -> MessageFormat.format("{0} ({1})", i.toHTML(null), i.getScript().getExtendedMembers().size());
    static final private Function<ScriptInstance,String> DefinitionErrorFormatter = i -> MessageFormat.format("{0}", i.toHTML(null));

}
