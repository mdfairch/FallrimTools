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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import resaver.ess.ESS;
import resaver.ess.ModelBuilder;
import resaver.ess.SortingMethod;
import resaver.ess.papyrus.Worrier;

/**
 *
 * @author Mark Fairchild
 */
public class Opener extends SwingWorker<Opener.Result, Double> {

    final public class Result {
        public Result(ESS.Result result, Worrier worrier) {
            ESS = result.ESS;
            RESULT = result;
            WORRIER = worrier;
        }
        
        final public ESS ESS;
        final public ESS.Result RESULT;
        final public Worrier WORRIER;
    }
    
    /**
     *
     * @param window
     * @param savefile
     * @param sort
     * @param previousResult
     * @param doAfter
     * 
     */
    public Opener(SaveWindow window, Path savefile, SortingMethod sort, Optional<Result> previousResult, Runnable doAfter) {
        this.WINDOW = Objects.requireNonNull(window);
        this.SAVEFILE = Objects.requireNonNull(savefile);
        this.PREVIOUS = Objects.requireNonNull(previousResult);
        this.DOAFTER = Objects.requireNonNull(doAfter);
        this.SORT = Objects.requireNonNull(sort);
        Configurator.setPreviousSave(savefile);
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    protected Opener.Result doInBackground() throws Exception {
        if (!Configurator.validateSavegame(this.SAVEFILE)) {
            return null;
        }

        ProgressIndicator PROGRESS = this.WINDOW.createProgressIndicator("Opening");
        this.WINDOW.addWindowListener(this.LISTENER);
        this.WINDOW.clearESS();

        try {
            LOG.info("================"); //NOI18N
            LOG.log(Level.INFO, "Reading from savegame file \"{0}\".", this.SAVEFILE); //NOI18N

            final ProgressModel PM = new ProgressModel();
            final ModelBuilder MB = new ModelBuilder(PM, SORT, null);
            PROGRESS.setModel(PM);
            final ESS.Result ESS_RESULT = ESS.readESS(this.SAVEFILE, MB);
            final Worrier WORRIER = new Worrier(ESS_RESULT, PREVIOUS.map(p -> p.WORRIER));
            final Opener.Result RESULT = new Opener.Result(ESS_RESULT, WORRIER);
                    
            this.WINDOW.setESSResult(RESULT);
            SwingUtilities.invokeLater(DOAFTER);
            
            if (WORRIER.shouldWorry() || WORRIER.shouldDisableSaving()) {
                this.showErrors(WORRIER.getMessage().toString(), WORRIER.shouldWorry());
            }
            
            return RESULT;

        } catch (Exception | Error ex) {            
            final String MSG = MessageFormat.format(I18N.getString("OPENER_ERROR"), this.SAVEFILE.getFileName(), ex.getMessage());
            LOG.log(Level.SEVERE, MSG, ex);
            ex.printStackTrace(System.err);
            JOptionPane.showMessageDialog(this.WINDOW, MSG, I18N.getString("OPENER_ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
            return null;

        } finally {
            this.WINDOW.removeWindowListener(this.LISTENER);
            PROGRESS.stop();
        }
    }

    final private Path SAVEFILE;
    final private SaveWindow WINDOW;
    final private Optional<Result> PREVIOUS;
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
    
    private void showErrors(String worries, boolean shouldWorry) {
            new Thread((Runnable) java.awt.Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation")).start(); //NOI18N
            final String TITLE = I18N.getString("OPENER_SUCCESS");
            final JOptionPane JOP = new JOptionPane(new TextDialog(worries), shouldWorry ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
            final JDialog DIALOG = JOP.createDialog(this.WINDOW, TITLE);
            DIALOG.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
            DIALOG.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            if (!WINDOW.isFocused()) {
                new javax.swing.Timer(5000, e -> DIALOG.setVisible(false)).start();
            }
            DIALOG.setVisible(true);        
    }            

}
