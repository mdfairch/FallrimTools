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
package resaver.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import resaver.ess.ESS;
import resaver.ess.papyrus.ActiveScript;
import resaver.ess.papyrus.Definition;
import resaver.ess.papyrus.Reference;
import resaver.ess.papyrus.ScriptInstance;
import resaver.ess.papyrus.Papyrus;
import resaver.ess.papyrus.PapyrusContext;
import resaver.ess.papyrus.PapyrusElement;
import resaver.ess.papyrus.Script;
import resaver.ess.papyrus.Struct;
import resaver.ess.papyrus.StructInstance;
import resaver.ess.papyrus.TString;

/**
 *
 * @author Mark
 */
public class BatchCleaner extends SwingWorker<Boolean, Double> {

    /**
     *
     * @param window
     * @param save
     */
    public BatchCleaner(SaveWindow window, ESS save) {
        this.WINDOW = Objects.requireNonNull(window, "The window field must not be null.");
        this.SAVE = Objects.requireNonNull(save, "The save field must not be null.");
        this.CONTEXT = this.SAVE.getPapyrus().getContext();
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    protected Boolean doInBackground() throws Exception {
        ProgressIndicator PROGRESS = this.WINDOW.createProgressIndicator("Batch cleaning");
        this.WINDOW.addWindowListener(this.LISTENER);

        try {
            String batch = null;

            // If no batch script was provided, throw up a dialog with a 
            // text area and let the user paste one in.
            if (null == batch) {
                final JTextArea TEXT = new JTextArea();
                TEXT.setColumns(50);
                TEXT.setRows(10);
                TEXT.setLineWrap(false);
                TEXT.setWrapStyleWord(false);

                final JScrollPane SCROLLER = new JScrollPane(TEXT);
                SCROLLER.setBorder(BorderFactory.createTitledBorder("Enter Scripts"));
                final String TITLE = "Batch Clean";
                int result = JOptionPane.showConfirmDialog(this.WINDOW, SCROLLER, TITLE, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (result == JOptionPane.CANCEL_OPTION) {
                    return false;
                }

                batch = TEXT.getText();
            }

            // If we still have no batch script, just exit.
            if (null == batch || batch.isEmpty()) {
                return false;
            }

            // Split the input into lines.
            final String[] LINES = batch.split("\n");
            final java.util.Set<Definition> CLEAN_NAMES = new java.util.TreeSet<>();

            // I had 99 problems, so I used regular expressions. Now I have 100 problems.
            // (script name)(optional .pex extension)(@@ followed by deletion prompt)
            final String PATTERN = "^([^\\.@\\s]+)(?:\\.pex)?(?:\\s*@@\\s*(.*))?";
            final Pattern REGEX = Pattern.compile(PATTERN, Pattern.CASE_INSENSITIVE);

            // Now iterate through the lines.
            for (String line : LINES) {
                // Match the regex.
                final Matcher MATCHER = REGEX.matcher(line);
                if (!MATCHER.find()) {
                    assert false;
                }

                // For debugging.
                java.util.List<String> groups = new java.util.LinkedList<>();
                for (int i = 0; i <= MATCHER.groupCount(); i++) {
                    groups.add(MATCHER.group(i));
                }
                System.out.printf("Groups = %d: %s\n", MATCHER.groupCount(), groups);

                // Retrieve group 1, the definition name.
                final String NAME = MATCHER.group(1).trim();
                final Definition DEF = this.CONTEXT.findAny(TString.makeUnindexed(NAME));

                if (DEF != null) {
                    // Group 2 is an optional deletion prompt.
                    if (null == MATCHER.group(2)) {
                        CLEAN_NAMES.add(DEF);
                        LOG.info(String.format("Definition present, adding to cleaning list: %s", DEF));

                    } else {
                        LOG.info(String.format("Definition present, prompting for deletion: %s", DEF));
                        final String PROMPT = MATCHER.group(2).trim();
                        final String MSG = String.format("Delete %s?\n%s", DEF, PROMPT);
                        final String TITLE = "Confirm";
                        int result = JOptionPane.showConfirmDialog(this.WINDOW, MSG, TITLE, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result == JOptionPane.OK_OPTION) {
                            CLEAN_NAMES.add(DEF);
                        } else if (result == JOptionPane.CANCEL_OPTION) {
                            return false;
                        }
                    }
                }
            }

            // If no scripts matched, abort.
            if (CLEAN_NAMES.isEmpty()) {
                final String MSG = "There were no matches.";
                final String TITLE = "No matches";
                JOptionPane.showMessageDialog(this.WINDOW, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            final StringBuilder BUF = new StringBuilder();
            BUF.append("The following scripts will be cleaned: \n\n");
            CLEAN_NAMES.forEach(v -> BUF.append(v).append('\n'));

            final JTextArea TEXT = new JTextArea(BUF.toString());
            TEXT.setColumns(40);
            TEXT.setEditable(false);
            final JScrollPane SCROLLER = new JScrollPane(TEXT);
            final String TITLE = "Batch Clean";

            int result = JOptionPane.showConfirmDialog(this.WINDOW, SCROLLER, TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return false;
            }

            final Papyrus PAPYRUS = this.SAVE.getPapyrus();
            
            final Set<ActiveScript> THREADS = CLEAN_NAMES.stream()
                    .filter(def -> (def instanceof Script))
                    .flatMap(def -> PAPYRUS.getActiveScripts().values().stream()
                            .filter(v -> v.hasScript((Script)def)))
                    .collect(Collectors.toSet());
            
            THREADS.forEach(t -> t.zero());
            final Set<PapyrusElement> REMOVED = this.SAVE.getPapyrus().removeElements(CLEAN_NAMES);
            this.WINDOW.deleteNodesFor(REMOVED);
            
            long scripts = REMOVED.stream().filter(v -> v instanceof Script).count();
            long scriptInstances = REMOVED.stream().filter(v -> v instanceof ScriptInstance).count();
            long structs = REMOVED.stream().filter(v -> v instanceof Struct).count();
            long structsInstances = REMOVED.stream().filter(v -> v instanceof StructInstance).count();
            long references = REMOVED.stream().filter(v -> v instanceof Reference).count();
            long threads = THREADS.size();

            final String MSG = String.format("Cleaned %d scripts and %d corresponding instances.\nCleaned %s structs and %d corresponding instances.\nCleaned %d references.\n%d threads were terminated.", scripts, scriptInstances, structs, structsInstances, references, threads);
            JOptionPane.showMessageDialog(this.WINDOW, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE);
            return true;

        } finally {
            this.WINDOW.removeWindowListener(this.LISTENER);
            PROGRESS.stop();
        }
    }

    final private SaveWindow WINDOW;
    final private ESS SAVE;
    final private PapyrusContext CONTEXT;
    
    static final private Logger LOG = Logger.getLogger(Saver.class.getCanonicalName());
    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");

    final private WindowAdapter LISTENER = new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            if (!isDone()) {
                cancel(true);
            }
        }
    };

}
