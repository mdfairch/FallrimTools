/*
 * Copyright 2019 Mark.
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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.IntConsumer;

/**
 *
 * @author Mark
 */
@SuppressWarnings("serial")
public class ChangeFlagEditor extends JDialog {

    /**
     *
     * @param parent
     * @param mask
     * @param done
     */
    public ChangeFlagEditor(SaveWindow parent, int mask, IntConsumer done) {
        super(parent, "", true); //NOI18N
        this.FLAGS = new JButton[32];

        this.mask = mask;

        for (int i = 0; i < 32; i++) {
            this.FLAGS[i] = new JButton("  ");
        }

        this.maskField = new JFormattedTextField(new mf.Hex32Formatter());
        this.maskField.setValue(this.mask);
        this.maskField.setColumns(8);
        this.maskField.addPropertyChangeListener("value", e -> updateMask()); //NOI18N

        this.CANCEL = new JButton(I18N.getString("CHANGEFLAG_CANCEL"));
        super.setLayout(new BorderLayout());

        final JPanel TOP = new JPanel(new FlowLayout());
        final JPanel CENTER = new JPanel(new GridLayout(2, 32));
        final JPanel BOTTOM = new JPanel(new FlowLayout());
        super.add(TOP, BorderLayout.NORTH);
        super.add(BOTTOM, BorderLayout.SOUTH);
        super.add(CENTER, BorderLayout.CENTER);
        TOP.add(new JLabel(I18N.getString("CHANGEFLAG_MASK")));
        TOP.add(this.maskField);
        TOP.add(new JLabel(I18N.getString("CHANGEFLAG_TITLE_FILTER")));
        BOTTOM.add(this.CANCEL);

        for (int i = 0; i < 32; i++) {
            CENTER.add(new JLabel(Integer.toString(31 - i), JLabel.CENTER));
        }
        for (int i = 0; i < 32; i++) {
            JButton b = this.FLAGS[31 - i];
            CENTER.add(b);
            b.addActionListener(e -> flagToggle((JButton) e.getSource()));
        }

        this.CANCEL.addActionListener(e -> this.setVisible(false));
        super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.update();
    }

    /**
     *
     */
    private void update() {
        for (int i = 0; i < 32; i++) {
            JButton flag = this.FLAGS[i];
            flag.setText(this.getFlag(i) ? LABEL_SET : LABEL_UNSET);
        }
    }

    /**
     *
     */
    private void updateMask() {
        try {
            this.mask = (Integer) this.maskField.getValue();
            this.update();
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Illegal mask and/or field.", ex); //NOI18N
        }
    }

    /**
     *
     * @param b
     */
    private void flagToggle(JButton b) {
        int i = Arrays.asList(this.FLAGS).indexOf(b);
        this.setFlag(i, !this.getFlag(i));
    }

    /**
     *
     * @param i
     * @return
     */
    private boolean getFlag(int i) {
        int setBit = 0x1 << i;
        return (this.mask & setBit) != 0;
    }

    /**
     *
     * @param i
     * @param val
     */
    private void setFlag(int i, boolean newState) {
        //System.out.printf("button %d, %s\n", i, newState); //NOI18N
        JButton flag = this.FLAGS[i];
        int clearBit = ~(0x1 << i);
        int setBit = 0x1 << i;

        if (newState) {
            this.mask |= setBit;
            flag.setText(LABEL_SET);
        } else {
            this.mask &= clearBit;
            flag.setText(LABEL_UNSET);
        }

        this.maskField.setValue(this.mask);
    }

    @Override
    public String toString() {
        return String.format("%08x", this.mask); //NOI18N
    }

    private int mask;
    final private JFormattedTextField maskField;
    final private JButton[] FLAGS;
    final private JButton CANCEL;

    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");
    static final private String LABEL_UNSET = I18N.getString("CHANGEFLAG_FLAG_UNSET");
    static final private String LABEL_SET = I18N.getString("CHANGEFLAG_FLAG_SET");

    static enum State {
        UNSET, SET
    };

}
