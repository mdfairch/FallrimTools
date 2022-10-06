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

import resaver.ProgressModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import resaver.ess.ESS;

/**
 *
 * @author Mark Fairchild
 */
public class Saver extends SwingWorker<ESS, Double> {

    /**
     *
     * @param window
     * @param saveFile
     * @param save
     * @param doAfter
     *
     */
    public Saver(SaveWindow window, Path saveFile, ESS save, Runnable doAfter) {
        this.WINDOW = Objects.requireNonNull(window, "The window field must not be null."); //NOI18N
        this.SAVEFILE = Objects.requireNonNull(saveFile, "The saveFile field must not be null."); //NOI18N
        this.SAVE = Objects.requireNonNull(save, "The save field must not be null."); //NOI18N
        this.DOAFTER = doAfter;
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    protected ESS doInBackground() throws Exception {
        if (!Configurator.validWrite(this.SAVEFILE)) {
            return null;
        }

        this.WINDOW.getProgressIndicator().start(I18N.getString("SAVER_SAVING"));
        this.WINDOW.addWindowListener(this.LISTENER);

        try {
            LOG.info("================"); //NOI18N
            LOG.log(Level.INFO, "Writing to savegame file \"{0}\".", this.SAVEFILE); //NOI18N

            final ProgressModel MODEL = new ProgressModel();
            this.WINDOW.getProgressIndicator().setModel(MODEL);

            boolean watcherRunning = WINDOW.getWatcher().isRunning();
            WINDOW.getWatcher().stop();

            final ESS.Result RESULT = ESS.writeESS(this.SAVE, this.SAVEFILE, false);

            if (watcherRunning) {
                WINDOW.getWatcher().resume();
            }

            double time = RESULT.TIME_S;
            double size = RESULT.SIZE_MB;
            final StringBuilder MSG = new StringBuilder()
                    .append(I18N.getString("SAVER_SUCCESS"))
                    .append("\n") //NOI18N
                    .append(String.format(I18N.getString("SAVER_MB_PER_SECOND"), size, time));

            if (null != RESULT.BACKUP_FILE) {
                MSG.append("\n").append(MessageFormat.format(I18N.getString("SAVER_BACKUP"), RESULT.BACKUP_FILE));
            }
            if (RESULT.ESS.hasCosave()) {
                MSG.append("\n").append(MessageFormat.format(I18N.getString("SAVER_COSAVE"), RESULT.GAME.COSAVE_EXT.toUpperCase()));
            }

            final String TITLE = I18N.getString("SAVER_SUCCESS_TITLE");
            JOptionPane.showMessageDialog(this.WINDOW, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE);
            this.WINDOW.resetTitle(this.SAVEFILE);

            if (this.DOAFTER != null) {
                SwingUtilities.invokeLater(DOAFTER);
            }

            return this.SAVE;

        } catch (Exception | Error ex) {
            final String MSG = MessageFormat.format(I18N.getString("SAVER_ERROR"), this.SAVEFILE.getFileName(), ex.getMessage());
            LOG.log(Level.SEVERE, MSG, ex);
            JOptionPane.showMessageDialog(this.WINDOW, MSG, "Write Error", JOptionPane.ERROR_MESSAGE);
            return null;

        } finally {
            this.WINDOW.removeWindowListener(this.LISTENER);
            this.WINDOW.getProgressIndicator().stop();
        }
    }

    final private Path SAVEFILE;
    final private SaveWindow WINDOW;
    final private ESS SAVE;
    final private Runnable DOAFTER;

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
