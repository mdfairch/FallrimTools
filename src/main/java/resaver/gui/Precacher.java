/*
 * Copyright 2016 Mark Fairchild.
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
import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import resaver.ess.ESS;

/**
 *
 * @author Mark Fairchild
 */
public class Precacher extends SwingWorker<Integer, Double> {

    /**
     *
     * @param window
     * @param save
     * @param progress
     */
    public Precacher(SaveWindow window, ESS save, Consumer<String> progress) {
        this.WINDOW = Objects.requireNonNull(window, "The window field must not be null."); //NOI18N
        this.SAVE = Objects.requireNonNull(save, "The save field must not be null."); //NOI18N
        this.PROGRESS = Objects.requireNonNull(progress);
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    protected Integer doInBackground() throws Exception {
        final mf.Timer TIMER = mf.Timer.startNew(I18N.getString("SCANNER_LOADPLUGINS"));
        this.WINDOW.addWindowListener(this.LISTENER);
        this.PROGRESS.accept("Precacher initialized.");
        LOG.info("Cross-referencing variables.");
        this.PROGRESS.accept("Cross-referencing variables.");
        this.SAVE.getPapyrus().getContext().precache();
        LOG.info(MessageFormat.format("Precaching completed, took {0}", TIMER.getFormattedTime()));
        this.WINDOW.removeWindowListener(this.LISTENER);
        return 1;
    }

    final private SaveWindow WINDOW;
    final private ESS SAVE;
    final private Consumer<String> PROGRESS;
    static final private Logger LOG = Logger.getLogger(Precacher.class.getCanonicalName());
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
