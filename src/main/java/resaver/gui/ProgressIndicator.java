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
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Displays a <code>JProgressBar</code> in a panel, while blocking the owner
 * window.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class ProgressIndicator extends JPanel {

    /**
     * Creates a new <code>ProgressIndicator</code>.
     *
     */
    public ProgressIndicator() {
        super.setLayout(new FlowLayout());
        this.LABEL = new JLabel(ResourceBundle.getBundle("Strings").getString("PROGRESSLABEL"));
        this.BAR = new JProgressBar();
        this.BAR.setIndeterminate(true);
        
        Dimension s = this.BAR.getPreferredSize();
        Dimension s_ = new Dimension(2 * s.width / 3, s.height);
        this.BAR.setPreferredSize(s_);
        this.LABEL.setVisible(false);
        this.BAR.setVisible(false);
        this.active = 0;
        super.add(this.LABEL);
        super.add(this.BAR);
    }

    /**
     * Sets the title and model of the <code>ProgressIndicator</code>..
     *
     * @param title The new title of the dialog.
     */
    synchronized public void start(String title) {
        this.start(title, null);
    }

    /**
     * Sets the title and model of the <code>ProgressIndicator</code>..
     *
     * @param title The new title of the dialog.
     * @param model The <code>ProgressModel</code> or null for indeterminate.
     */
    synchronized public void start(String title, ProgressModel model) {
        Objects.requireNonNull(title);
        this.LABEL.setText(title);
        this.setModel(model);
        
        if (this.active > 0) {
            this.active++;
        } else {
            this.active = 1;
            this.LABEL.setVisible(true);
            this.BAR.setVisible(true);
            startWaitCursor(this.getRootPane());            
        }
    }

    public void setModel(ProgressModel model) {
        if (model == null) {
            this.BAR.setIndeterminate(true);
            this.BAR.setModel(new ProgressModel(1));
        } else {
            this.BAR.setIndeterminate(false);
            this.BAR.setModel(model);
        }
    }

    public void clearModel() {
        this.setModel(null);
    }

    /**
     *
     */
    synchronized public void stop() {
        assert this.active > 0 : "Invalid call to ProgressIndicator.stop().";
        
        if (this.active > 0) {
            this.active--;
        }
        
        assert this.active >= 0 : "Invalid call to ProgressIndicator.stop().";
        
        if (this.active <= 0) {
            this.active = 0;
            this.LABEL.setVisible(false);
            this.BAR.setVisible(false);
            stopWaitCursor(this.getRootPane());
        }
    }

    /**
     *
     * @param component
     */
    static private void startWaitCursor(JComponent component) {
        RootPaneContainer root = ((RootPaneContainer) component.getTopLevelAncestor());
        root.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        root.getGlassPane().addMouseListener(NULLMOUSEADAPTER);
        root.getGlassPane().setVisible(true);
    }

    static private void stopWaitCursor(JComponent component) {
        RootPaneContainer root = ((RootPaneContainer) component.getTopLevelAncestor());
        root.getGlassPane().setCursor(Cursor.getDefaultCursor());
        root.getGlassPane().removeMouseListener(NULLMOUSEADAPTER);
        root.getGlassPane().setVisible(false);
    }

    static final private MouseAdapter NULLMOUSEADAPTER = new MouseAdapter() {
    };

    final private JLabel LABEL;
    final private JProgressBar BAR;
    private int active;

}
