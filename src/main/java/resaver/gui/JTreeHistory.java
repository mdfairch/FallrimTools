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

import java.awt.FlowLayout;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * A <code>JPanel</code> with two buttons that provides a history function for a
 * JTree.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class JTreeHistory extends JPanel {

    /**
     * Creates a new <code>JTreeHistory</code>.
     *
     * @param tree The <code>JTree</code> to be monitored.
     *
     */
    public JTreeHistory(JTree tree) {
        super(new FlowLayout());
        Objects.requireNonNull(tree);
        this.BTN_PREVSELECTION = new JButton(I18N.getString("HISTORY_BACK"));
        this.BTN_NEXTSELECTION = new JButton(I18N.getString("HISTORY_NEXT"));
        this.BTN_PREVSELECTION.setToolTipText(I18N.getString("HISTORY_BACK_TOOLTIP"));
        this.BTN_NEXTSELECTION.setToolTipText(I18N.getString("HISTORY_NEXT_TOOLTIP"));
        this.PREV_SELECTIONS = new LinkedList<>();
        this.NEXT_SELECTIONS = new LinkedList<>();
        this.LOCK = new ReentrantLock();
        this.TREE = tree;
        this.initComponents();
    }

    /**
     * Initialize the swing and AWT components.
     *
     */
    private void initComponents() {
        this.add(this.BTN_PREVSELECTION);
        this.add(this.BTN_NEXTSELECTION);
        this.BTN_PREVSELECTION.setEnabled(false);
        this.BTN_NEXTSELECTION.setEnabled(false);

        this.TREE.addTreeSelectionListener(e -> {
            if (!LOCK.isLocked()) {
                TreePath[] selections = TREE.getSelectionPaths();
                PREV_SELECTIONS.push(selections);
                NEXT_SELECTIONS.clear();
                while (PREV_SELECTIONS.size() > 100) {
                    PREV_SELECTIONS.removeLast();
                }

                BTN_PREVSELECTION.setEnabled(PREV_SELECTIONS.size() > 1);
                BTN_NEXTSELECTION.setEnabled(NEXT_SELECTIONS.size() > 0);

                if (selections != null && selections.length > 0) {
                    LOG.log(Level.INFO, MessageFormat.format("TreeSelection: selection = {0}", selections[0].getLastPathComponent()));
                } else {
                    LOG.log(Level.INFO, "TreeSelection: selection = null");
                }
            } else {
                LOG.log(Level.INFO, "TreeSelection: skipping");
            }
        });

        this.BTN_PREVSELECTION.addActionListener(e -> {
            if (PREV_SELECTIONS.size() < 1) {
                LOG.log(Level.WARNING, "Warning: history empty.");
                return;
            }
            if (!Arrays.equals(PREV_SELECTIONS.peek(), TREE.getSelectionPaths())) {
                LOG.log(Level.WARNING, "Warning: current path should equal top of previous stack.");
            }

            LOCK.lock();
            try {
                NEXT_SELECTIONS.push(PREV_SELECTIONS.pop());
                BTN_PREVSELECTION.setEnabled(PREV_SELECTIONS.size() > 1);
                BTN_NEXTSELECTION.setEnabled(NEXT_SELECTIONS.size() > 0);

                TreePath[] selections = PREV_SELECTIONS.peek();
                TREE.setSelectionPaths(selections);
                if (selections != null && selections.length > 0) {
                    TREE.scrollPathToVisible(selections[0]);
                    LOG.log(Level.INFO, "TreeSelection: prev = {0}", selections[0].getLastPathComponent());
                } else {
                    LOG.log(Level.INFO, "TreeSelection: prev = nothing");
                }
            } finally {
                LOCK.unlock();
            }
        });

        this.BTN_NEXTSELECTION.addActionListener(e -> {
            if (NEXT_SELECTIONS.isEmpty()) {
                LOG.log(Level.WARNING, "Warning: history empty.");
                return;
            }
            if (!Arrays.equals(PREV_SELECTIONS.peek(), TREE.getSelectionPaths())) {
                LOG.log(Level.WARNING, "Warning: current path should equal top of previous stack.");
            }

            LOCK.lock();
            try {
                PREV_SELECTIONS.push(NEXT_SELECTIONS.pop());
                BTN_PREVSELECTION.setEnabled(PREV_SELECTIONS.size() > 1);
                BTN_NEXTSELECTION.setEnabled(NEXT_SELECTIONS.size() > 0);

                TreePath[] selections = PREV_SELECTIONS.peek();
                TREE.setSelectionPaths(selections);
                if (selections != null && selections.length > 0) {
                    TREE.scrollPathToVisible(selections[0]);
                    LOG.log(Level.INFO, "TreeSelection: next = {0}", selections[0].getLastPathComponent());
                } else {
                    LOG.log(Level.INFO, "TreeSelection: next = nothing");
                }
            } finally {
                LOCK.unlock();
            }
        });
    }

    final private JButton BTN_PREVSELECTION;
    final private JButton BTN_NEXTSELECTION;
    final private LinkedList<TreePath[]> PREV_SELECTIONS;
    final private LinkedList<TreePath[]> NEXT_SELECTIONS;
    final private JTree TREE;
    final private ReentrantLock LOCK;

    static final private Logger LOG = Logger.getLogger(SaveWindow.class.getCanonicalName());
    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");

}
