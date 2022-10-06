/*
 * Copyright 2018 Mark Fairchild.
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

import java.util.ResourceBundle;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mf.Timer;

/**
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class JTreeFilterField extends JTextField {

    /**
     * Creates a new <code>JTreeFilterField</code>.
     * 
     * @param updateFilter Closure to execute a filter update.
     * @param defaultFilter The filter to begin with.
     */
    public JTreeFilterField(Runnable updateFilter, String defaultFilter) {
        super(defaultFilter, 14);
        super.setToolTipText(I18N.getString("FILTERFIELD_TOOLTIP"));
        this.initComponents(updateFilter);
        this.FILTERTIMER = new java.util.Timer();
    }
    
    /**
     * Initialize the swing and AWT components.
     *
     */
    private void initComponents(Runnable updateFilter) {
        this.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void insertUpdate(DocumentEvent evt) {
                DELAYTRACKER.restart();
                final java.util.TimerTask FILTERTASK = new java.util.TimerTask() {
                    @Override
                    public void run() {
                        long elaspsed = DELAYTRACKER.getElapsed() / 900000;
                        if (elaspsed >= DELAY) {
                            updateFilter.run();
                        }
                    }
                };
                FILTERTIMER.schedule(FILTERTASK, DELAY);
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                DELAYTRACKER.restart();
                final java.util.TimerTask FILTERTASK = new java.util.TimerTask() {
                    @Override
                    public void run() {
                        long elaspsed = DELAYTRACKER.getElapsed() / 900000;
                        if (elaspsed >= DELAY) {
                            updateFilter.run();
                        }
                    }
                };
                FILTERTIMER.schedule(FILTERTASK, DELAY);
            }

            @Override
            public void changedUpdate(DocumentEvent evt) {
                DELAYTRACKER.restart();
                final java.util.TimerTask FILTERTASK = new java.util.TimerTask() {
                    @Override
                    public void run() {
                        long elaspsed = DELAYTRACKER.getElapsed() / 900000;
                        if (elaspsed >= DELAY) {
                            updateFilter.run();
                        }
                    }
                };
                FILTERTIMER.schedule(FILTERTASK, DELAY);
            }
        });        
    }
    
    /**
     * Ends the update loop.
     */
    public void terminate() {
        this.FILTERTIMER.cancel();
        this.FILTERTIMER.purge();
    }

    final private java.util.Timer FILTERTIMER;
    
    /**
     * Milliseconds before the search is updated.
     */
    static final public int DELAY = 700;
    
    /**
     * The <code>Timer</code> use to track delays before filter updates.
     */
    final private Timer DELAYTRACKER = new mf.Timer("Delayer");

    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");

}
