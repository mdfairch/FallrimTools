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

import java.util.ResourceBundle;

/**
 *
 * @author Mark
 */
final public class MemoryLabel extends javax.swing.JLabel {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new <code>MemoryLabel</code>.
     *
     */
    public MemoryLabel() {
        this.MEMTIMER = new java.util.Timer();
    }

    /**
     * Starts the update loop.
     */
    public void initialize() {
        this.MEMTIMER.schedule(TASK, 0, 500);
    }

    /**
     * Ends the update loop.
     */
    public void terminate() {
        this.MEMTIMER.cancel();
        this.MEMTIMER.purge();
    }

    /**
     * Updates the memory display field.
     */
    public void update() {
        final Runtime RT = Runtime.getRuntime();
        double GB = 1073741824.0;
        long MAX = RT.maxMemory();
        long PERCENT = 100 * RT.freeMemory() / MAX;
        String txt = String.format(FORMAT, PERCENT, MAX / GB);
        this.setText(txt);
    }

    final private java.util.Timer MEMTIMER;

    final private java.util.TimerTask TASK = new java.util.TimerTask() {
        @Override
        public void run() {
            update();
        }
    };

    static final private String FORMAT = ResourceBundle.getBundle("Strings").getString("MEMORYLABEL");
}
