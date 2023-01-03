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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import resaver.Game;
import resaver.ess.ESS;
import resaver.ess.Header;
import static resaver.ResaverFormatting.makeHTMLList;
import resaver.ess.WStringElement;

/**
 *
 * @author Mark
 */
final public class Worrier {

    public Worrier() {
        this.message = null;
        this.shouldWorry = false;
        this.disableSaving = false;
        this.previousCanaries = null;
        this.previousNamespaces = null;
        this.previousESS = Optional.empty();
    }

    private CharSequence checkFatal(ESS.Result result) {
        final StringBuilder BUF = new StringBuilder();
        final ESS ESS = result.ESS;
        final Papyrus PAPYRUS = ESS.getPapyrus();

        // Check the first fatal condition -- truncation.
        if (ESS.isBroken()) {
            BUF.append("<p><em>THIS FILE IS TRUNCATED.</em> It is corrupted and can never be recovered, not even by the pure-hearted love of a Paraguay Jaguar.");

            if (PAPYRUS == null) {
                BUF.append("<br/><strong>No Papyrus section.</strong>");
                
            } else if (PAPYRUS.getStringTable().isTruncated()) {
                int missing = PAPYRUS.getStringTable().getMissingCount();
                BUF.append("<br/><strong>TRUNCATED STRING TABLE.</strong> ")
                        .append(missing)
                        .append(" strings missing. The cause of this is unknown, but sometimes involves the scripts that append to strings in a loop.");
            } else if (PAPYRUS.isBroken()) {
                BUF.append("<br/><strong>TRUNCATED PAPYRUS BLOCK.</strong> This is usually caused by too many scripts running at once, or recursive scripts without proper boundary conditions.");
            }
            
            if (Arrays.stream(ESS.getFormIDs()).anyMatch(i -> i == 0)) {
                int present = 0;
                while (present < ESS.getFormIDs().length && ESS.getFormIDs()[present] != 0) {
                    present++;
                }
                BUF.append("<br/><strong>TRUNCATED FORMID ARRAY</strong>. ")
                        .append(present).append('/').append(ESS.getFormIDs().length)
                        .append(" formIDs read. This is sometimes caused by updating mods without following their proper updating procedure.");
            }
            BUF.append("</p>");

            this.shouldWorry = true;
            this.disableSaving = true;
        }

        // Check the second fatal condition -- the string table bug.
        if (PAPYRUS != null && PAPYRUS.getStringTable().hasSTB()) {
            BUF.append("<p><em>THIS FILE HAS THE STRING-TABLE-BUG.</em> It is corrupted and can never be recovered, not even with lasers or cheetah blood.</p>");
            this.shouldWorry = true;
            this.disableSaving = true;
        }

        return BUF;
    }

    private CharSequence checkPerformance(ESS.Result result) {
        final StringBuilder BUF = new StringBuilder();

        double time = result.TIME_S;
        double size = result.SIZE_MB;

        BUF.append("<p>The savefile was successfully loaded.<ul>");
        BUF.append(String.format("<li>Read %1.1f mb in %1.1f seconds.</li>", size, time));
        if (result.ESS.hasCosave()) {
            BUF.append(String.format("<li>%s co-save was loaded.</li>", result.GAME.COSAVE_EXT.toUpperCase()));
        } else {
            BUF.append("<li>No co-save was found.</li>");
        }

        BUF.append("</ul></p>");
        return BUF;
    }

    private CharSequence checkNonFatal(ESS.Result result) {
        final StringBuilder BUF = new StringBuilder();
        final Papyrus PAPYRUS = result.ESS.getPapyrus();
        
        if (PAPYRUS == null) {
            return BUF;
        }

        int unattached = PAPYRUS.countUnattachedInstances();
        if (unattached > 0) {
            String msg = String.format("<p>There are %d unattached instances.</p>", unattached);
            BUF.append(msg);
            if (result.GAME != Game.FALLOUT4 || unattached > 2) {
                this.shouldWorry = true;
            }
        }

        int[] undefined = PAPYRUS.countUndefinedElements();

        if (undefined[0] > 0) {
            String msg = String.format("<p>There are %d undefined elements.</p>", undefined[0]);
            BUF.append(msg);
            this.shouldWorry = true;
        }

        if (undefined[1] > 0) {
            String msg = String.format("<p>There are %d undefined threads.</p>", undefined[1]);
            BUF.append(msg);
            this.shouldWorry = true;
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

            Script most1 = threads.get(0);
            Script most2 = frames.get(0);

            long numFrames = frameCounts.values().stream().mapToLong(v -> v).sum();

            if (numStacks > 200 || numFrames > 1000) {
                BUF.append(String.format("<p>There are %d stacks and %d frames, which probably indicates a problem.<br/>", numStacks, numFrames));
                BUF.append(String.format("%s occurs the most often as a stack frame (%d occurrences)</p>", most2.toHTML(null), frameCounts.get(most2)));
                //BUF.append(String.format("%s occurs the most often as a thread (%d occurrences)</p>", most1.toHTML(null), threadCounts.get(most1)));

                this.shouldWorry = true;

            } else if (numStacks > 50 || numFrames > 150) {
                BUF.append(String.format("<p>There are %d stacks and %d frames, which may indicate a problem.</p>", numStacks, numFrames));
                //BUF.append(String.format("%s occurs the most often (%d occurrences)</p>", most2.toHTML(null), frameCounts.get(most2)));

                this.shouldWorry = true;
            }

            List<ActiveScript> deep = PAPYRUS.getActiveScripts().values().stream()
                    .filter(thread -> thread.getStackFrames().size() >= 100)
                    .collect(Collectors.toList());
            deep.sort((a1, a2) -> Integer.compare(a2.getStackFrames().size(), a1.getStackFrames().size()));

            if (!deep.isEmpty()) {
                ActiveScript deepest = deep.get(0);
                int depth = deepest.getStackFrames().size();
                String msg = String.format("<p>There is a stack %d frames deep (%s).</p>", depth, deepest.toHTML(null));
                BUF.append(msg);
                this.shouldWorry = true;
            }
        }

        // Get a map of namespaces to scriptInstances in that namespace.
        Map<String, List<ScriptInstance>> currentNamespaces = PAPYRUS.getScriptInstances().values()
                .parallelStream()
                .filter(instance -> instance.getScriptName().toString().contains(":"))
                .collect(Collectors.groupingBy(instance -> instance.getScriptName().toString().split(":")[0]));

        Map<Script, Integer> currentCanaries = PAPYRUS.getScriptInstances().values()
                .parallelStream()
                .filter(instance -> instance.hasCanary())
                .collect(Collectors.toMap(instance -> instance.getScript(), instance -> instance.getCanary()));
       
        previousESS.ifPresent(previous -> {
            if (areProbablySequential(previous, result.ESS)) {
                Header H1 = previous.getHeader();
                Header H2 = result.ESS.getHeader();

                if (WStringElement.compare(H1.NAME, H2.NAME) == 0 && H1.FILETIME < H2.FILETIME) {
                    int previousSize = previous.calculateSize();
                    int currentSize = result.ESS.calculateSize();
                    double difference = 200.0 * (currentSize - previousSize) / (currentSize + previousSize);
                    if (difference < -5.0) {
                        String msg = String.format("<p>This savefile has %2.2f%% less papyrus data the previous one.</p>", -difference);
                        BUF.append(msg);
                        this.shouldWorry = true;
                    }
                }

                List<String> missingNamespaces = this.previousNamespaces.keySet().stream()
                        .filter(namespace -> !currentNamespaces.containsKey(namespace))
                        .filter(namespace -> this.previousNamespaces.get(namespace).stream()
                        .map(instance -> instance.getRefID())
                        .filter(refID -> !refID.isZero())
                        .anyMatch(refID -> result.ESS.getChangeForms().containsKey(refID)))
                        .collect(Collectors.toList());

                if (!missingNamespaces.isEmpty()) {
                    String msg = "This savefile has %d missing namespaces (the Canary error).";
                    BUF.append(makeHTMLList(msg, missingNamespaces, LIMIT, i -> i));
                    this.shouldWorry = true;
                }

                List<Script> canaryErrors = this.previousCanaries.keySet().stream()
                        .filter(script -> currentCanaries.containsKey(script))
                        .filter(script -> previousCanaries.get(script) != 0)
                        .filter(script -> currentCanaries.get(script) == 0)
                        .collect(Collectors.toList());

                if (!canaryErrors.isEmpty()) {
                    String msg = "This savefile has %d zeroed canaries.";
                    BUF.append(makeHTMLList(msg, canaryErrors, LIMIT, i -> i.toHTML(null)));
                    this.shouldWorry = true;
                }                
            }
        });
        
        List<ScriptInstance> memberless = PAPYRUS.getScriptInstances().values()
                .parallelStream()
                .filter(instance -> instance.hasMemberlessError())
                .collect(Collectors.toList());

        if (!memberless.isEmpty()) {
            String msg = "This savefile has %d script instances whose data is missing.";
            BUF.append(makeHTMLList(msg, memberless, LIMIT, i -> i.getScript().toHTML(null)));
            this.shouldWorry = true;
        }

        List<ScriptInstance> definitionErrors = PAPYRUS.getScriptInstances().values()
                .parallelStream()
                .filter(instance -> instance.hasDefinitionError())
                .collect(Collectors.toList());

        if (!definitionErrors.isEmpty()) {
            String msg = "This savefile has %d script instances with mismatched member data.";
            BUF.append(makeHTMLList(msg, definitionErrors, LIMIT, i -> i.getScript().toHTML(null)));
            this.shouldWorry = true;
        }

        this.previousNamespaces = currentNamespaces;
        this.previousCanaries = currentCanaries;
        this.previousESS = Optional.of(result.ESS);

        return BUF;
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
    
    public void check(ESS.Result result) {
        this.message = null;
        this.shouldWorry = false;
        this.disableSaving = false;

        final CharSequence PERFORMANCE = this.checkPerformance(result);
        final CharSequence FATAL = this.checkFatal(result);
        final CharSequence NONFATAL = this.checkNonFatal(result);

        final StringBuilder BUF = new StringBuilder();

        if (this.shouldDisableSaving()) {
            BUF.append("<h3>Serious problems were identified</h2><h3>Saving is disabled. Trust me, it's for your own good.</h3>");
            BUF.append(FATAL).append("<hr/>");
        }

        BUF.append(PERFORMANCE).append("<hr/>");

        if (this.shouldWorry()) {
            BUF.append(this.shouldDisableSaving()
                    ? "<h3>Additional problems were identified</h3>"
                    : "<h3>Potential problems were identified</h3>");
            BUF.append(NONFATAL).append("<hr/>");
        }

        this.message = BUF.toString();
    }

    public String getMessage() {
        return this.message;
    }

    public boolean shouldDisableSaving() {
        return this.disableSaving;
    }

    public boolean shouldWorry() {
        return this.shouldWorry;
    }

    private String message;
    private boolean shouldWorry;
    private boolean disableSaving;
    private Optional<ESS> previousESS;
    private Map<Script, Integer> previousCanaries;
    private Map<String, List<ScriptInstance>> previousNamespaces;

    //static final private Logger LOG = Logger.getLogger(Worrier.class.getCanonicalName());
    //static final private PathMatcher MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{fos,ess}");
    static final private int LIMIT = 12;
}
