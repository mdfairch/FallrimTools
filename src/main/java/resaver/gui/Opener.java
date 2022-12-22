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

import java.awt.Dialog;
import resaver.ProgressModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import resaver.ess.ESS;
import resaver.ess.ModelBuilder;
import resaver.ess.papyrus.Worrier;
import static resaver.ess.ModelBuilder.SortingMethod;

/**
 *
 * @author Mark Fairchild
 */
public class Opener extends SwingWorker<ESS, Double> {

    /**
     *
     * @param window
     * @param savefile
     * @param sort
     * @param worrier
     * @param doAfter
     * 
     */
    public Opener(SaveWindow window, Path savefile, SortingMethod sort, Worrier worrier, Runnable doAfter) {
        this.WINDOW = Objects.requireNonNull(window);
        this.SAVEFILE = Objects.requireNonNull(savefile);
        this.WORRIER = worrier;
        this.DOAFTER = doAfter;
        this.SORT = Objects.requireNonNull(sort);
        Configurator.setPreviousSave(savefile);
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    protected ESS doInBackground() throws Exception {
        if (!Configurator.validateSavegame(this.SAVEFILE)) {
            return null;
        }

        this.WINDOW.getProgressIndicator().start(I18N.getString("OPENER_OPENING"));
        this.WINDOW.addWindowListener(this.LISTENER);
        this.WINDOW.clearESS();

        try {
            LOG.info("================"); //NOI18N
            LOG.log(Level.INFO, "Reading from savegame file \"{0}\".", this.SAVEFILE); //NOI18N

            final ProgressModel PROGRESS = new ProgressModel();
            final ModelBuilder MB = new ModelBuilder(PROGRESS, SORT, null);
            this.WINDOW.getProgressIndicator().setModel(PROGRESS);
            final ESS.Result RESULT = ESS.readESS(this.SAVEFILE, MB);
            
            WORRIER.check(RESULT);

            this.WINDOW.setESS(RESULT.SAVE_FILE, RESULT.ESS, RESULT.MODEL, WORRIER.shouldDisableSaving());

            if (this.DOAFTER != null) {
                SwingUtilities.invokeLater(DOAFTER);
            }
            
            if (WORRIER.shouldWorry() || WORRIER.shouldDisableSaving()) {
                new Thread((Runnable) java.awt.Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation")).start(); //NOI18N
                final String TITLE = I18N.getString("OPENER_SUCCESS");
                final JOptionPane JOP = new JOptionPane(new TextDialog(WORRIER.getMessage()), WORRIER.shouldWorry() ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                final JDialog DIALOG = JOP.createDialog(this.WINDOW, TITLE);
                DIALOG.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
                DIALOG.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                if (!WINDOW.isFocused()) {
                    new javax.swing.Timer(5000, e -> DIALOG.setVisible(false)).start();
                }
                DIALOG.setVisible(true);
            }
            
            return RESULT.ESS;

        } catch (Exception | Error ex) {            
            final String MSG = MessageFormat.format(I18N.getString("OPENER_ERROR"), this.SAVEFILE.getFileName(), ex.getMessage());
            LOG.log(Level.SEVERE, MSG, ex);
            ex.printStackTrace(System.err);
            JOptionPane.showMessageDialog(this.WINDOW, MSG, I18N.getString("OPENER_ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
            return null;

        } finally {
            this.WINDOW.removeWindowListener(this.LISTENER);
            this.WINDOW.getProgressIndicator().stop();
        }
    }

    final private Path SAVEFILE;
    final private SaveWindow WINDOW;
    final private Worrier WORRIER;
    final private Runnable DOAFTER;
    final private SortingMethod SORT;
    static final private Logger LOG = Logger.getLogger(Opener.class.getCanonicalName());
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
