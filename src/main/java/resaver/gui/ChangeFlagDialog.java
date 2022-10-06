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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Arrays;
import java.util.ResourceBundle;
import mf.BiIntConsumer;

/**
 *
 * @author Mark
 */
@SuppressWarnings("serial")
public class ChangeFlagDialog extends JDialog {

    /**
     *
     * @param parent
     * @param mask
     * @param filter
     * @param done
     */
    public ChangeFlagDialog(SaveWindow parent, int mask, int filter, BiIntConsumer done) {
        super(parent, "", true); //NOI18N
        this.FLAGS = new JButton[32];

        this.mask = mask;
        this.filter = filter;

        for (int i = 0; i < 32; i++) {
            this.FLAGS[i] = new JButton("  ");
        }

        this.maskField = new JFormattedTextField(new mf.Hex32Formatter());
        this.filterField = new JFormattedTextField(new mf.Hex32Formatter());

        this.maskField.setValue(this.mask);
        this.filterField.setValue(this.filter);
        this.FILTER = new JButton(I18N.getString("CHANGEFLAG_SET_FILTER"));
        this.CLEAR = new JButton(I18N.getString("CHANGEFLAG_CLEAR"));
        this.CANCEL = new JButton(I18N.getString("CHANGEFLAG_CANCEL"));

        this.maskField.setColumns(8);
        this.filterField.setColumns(8);
        this.maskField.addPropertyChangeListener("value", e -> updateMask()); //NOI18N
        this.filterField.addPropertyChangeListener("value", e -> updateFilter()); //NOI18N
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
        TOP.add(this.filterField);
        BOTTOM.add(this.FILTER);
        BOTTOM.add(this.CLEAR);
        BOTTOM.add(this.CANCEL);

        for (int i = 0; i < 32; i++) {
            CENTER.add(new JLabel(Integer.toString(31 - i), JLabel.CENTER));
        }
        for (int i = 0; i < 32; i++) {
            JButton b = this.FLAGS[31 - i];
            CENTER.add(b);
            b.addActionListener(e -> flagToggle((JButton) e.getSource()));
        }

        this.FILTER.addActionListener(e -> {
            this.setVisible(false);
            done.consume(this.mask, this.filter);
        });

        this.CANCEL.addActionListener(e -> this.setVisible(false));

        this.CLEAR.addActionListener(e -> {
            this.mask = 0;
            this.filter = 0;
            this.update();
        });

        super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.update();
    }

    /**
     *
     */
    private void update() {
        for (int i = 0; i < 32; i++) {
            JButton flag = this.FLAGS[i];
            
            switch (this.getFlag(i)) {
                case IGNORED:
                    flag.setText(LABEL_IGNORED);
                    break;
                case UNSET:
                    flag.setText(LABEL_UNSET);
                    break;
                case SET:
                    flag.setText(LABEL_SET);
                    break;
                default:
                    throw new IllegalStateException("Illegal value."); //NOI18N
            }
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
     */
    private void updateFilter() {
        try {
            this.filter = (Integer) this.filterField.getValue();
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

        switch (this.getFlag(i)) {
            case IGNORED:
                this.setFlag(i, State.UNSET);
                break;
            case UNSET:
                this.setFlag(i, State.SET);
                break;
            case SET:
                this.setFlag(i, State.IGNORED);
                break;
            default:
                throw new IllegalStateException("Illegal value."); //NOI18N
        }
    }

    /**
     *
     * @param i
     * @return
     */
    private State getFlag(int i) {
        int setBit = 0x1 << i;

        if ((this.mask & setBit) == 0) {
            return State.IGNORED;
        } else if ((this.filter & setBit) == 0) {
            return State.UNSET;
        } else {
            return State.SET;
        }
    }

    /**
     *
     * @param i
     * @param val
     */
    private void setFlag(int i, State newState) {
        //System.out.printf("button %d, %s\n", i, newState); //NOI18N
        JButton flag = this.FLAGS[i];
        int clearBit = ~(0x1 << i);
        int setBit = 0x1 << i;

        switch (newState) {
            case IGNORED:
                this.mask &= clearBit;
                this.filter &= clearBit;
                flag.setText(LABEL_IGNORED);
                break;
            case UNSET:
                this.mask |= setBit;
                this.filter &= clearBit;
                flag.setText(LABEL_UNSET);
                break;
            case SET:
                this.mask |= setBit;
                this.filter |= setBit;
                flag.setText(LABEL_SET);
                break;
            default:
                throw new IllegalStateException("Illegal flag value."); //NOI18N
        }

        this.maskField.setValue(this.mask);
        this.filterField.setValue(this.filter);
    }

    @Override
    public String toString() {
        return String.format("%08x / %08x", this.mask, this.filter); //NOI18N
    }

    private int mask;
    private int filter;
    final private JFormattedTextField maskField;
    final private JFormattedTextField filterField;
    final private JButton[] FLAGS;
    final private JButton FILTER;
    final private JButton CLEAR;
    final private JButton CANCEL;

    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");
    static final private String LABEL_IGNORED = I18N.getString("CHANGEFLAG_FLAG_IGNORED");
    static final private String LABEL_UNSET = I18N.getString("CHANGEFLAG_FLAG_UNSET");
    static final private String LABEL_SET = I18N.getString("CHANGEFLAG_FLAG_SET");

    static enum State {
        IGNORED, UNSET, SET
    };

}
