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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Displays a modal dialog box with a message, blocking the UI until some
 * specified task is complete.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class ModalProgressDialog extends JDialog {

    /**
     * Create a new <code>ModalProgressDialog</code>.
     *
     * @param owner The owning <code>Frame</code>, which will be blocked.
     * @param title The title of the dialog.
     * @param task The task to perform while the owner is blocked.
     */
    public ModalProgressDialog(SaveWindow owner, String title, Runnable task) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.OWNER = owner;
        this.TOPPANEL = new JPanel();
        this.BOTTOMPANEL = new JPanel();
        this.LABEL = new JLabel(ResourceBundle.getBundle("Strings").getString("PROGRESSLABEL"));
        this.BAR = new JProgressBar();
        this.initComponents(task);
    }

    /**
     * Initialize the swing and AWT components.
     *
     */
    private void initComponents(Runnable task) {
        this.setPreferredSize(new Dimension(420, 100));
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.BAR.setPreferredSize(new Dimension(400, 30));
        this.TOPPANEL.add(this.LABEL);
        this.BOTTOMPANEL.add(this.BAR);

        this.setLayout(new BorderLayout());
        this.add(this.TOPPANEL, BorderLayout.PAGE_START);
        this.add(this.BOTTOMPANEL, BorderLayout.PAGE_END);

        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(this.getOwner());

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                Objects.requireNonNull(task);

                Runnable doThings = () -> {
                    task.run();
                    setVisible(false);
                };

                if (OWNER.isJavaFXAvailable()) {
                    try {
                        Class<?> PLATFORM = Class.forName("javafx.application.Platform"); //NOI18N
                        java.lang.reflect.Method RUNLATER = PLATFORM.getMethod("runLater", Runnable.class); //NOI18N
                        RUNLATER.invoke(null, doThings);
                    } catch (ReflectiveOperationException ex) {
                        SwingUtilities.invokeLater(doThings);
                    }
                } else {
                    SwingUtilities.invokeLater(doThings);
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {

            }

            @Override
            public void windowClosed(WindowEvent e) {
                dispose();
            }
        });
    }

    final private SaveWindow OWNER;
    final private JPanel TOPPANEL;
    final private JPanel BOTTOMPANEL;
    final private JLabel LABEL;
    final private JProgressBar BAR;

}
