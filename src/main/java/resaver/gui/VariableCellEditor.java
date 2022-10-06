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

import java.awt.Component;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import resaver.ess.papyrus.EID;
import resaver.ess.papyrus.PapyrusContext;
import resaver.ess.papyrus.Variable;

/**
 * A <code>TableCellEditor</code> implementation for table cells that contain
 * <code>Variable</code> objects.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class VariableCellEditor extends AbstractCellEditor implements TableCellEditor {

    public VariableCellEditor(PapyrusContext context) {
        this.STR = new Str();
        this.INT = new Int();
        this.FLT = new Flt();
        this.BOOL = new Bool();
        this.REF = new Ref();
        this.subeditor = STR;
        this.CONTEXT = context;
    }

    @Override
    public Object getCellEditorValue() {
        return this.subeditor.getCellEditorValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value instanceof Variable.Str) {
            this.subeditor = this.STR;
            return this.subeditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        } else if (value instanceof Variable.Int) {
            this.subeditor = this.INT;
            return this.subeditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        } else if (value instanceof Variable.Flt) {
            this.subeditor = this.FLT;
            return this.subeditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        } else if (value instanceof Variable.Bool) {
            this.subeditor = this.BOOL;
            return this.subeditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        } else if (value instanceof Variable.Ref) {
            this.subeditor = this.REF;
            return this.subeditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        } else {
            throw new IllegalStateException();
        }

    }

    final private Str STR;
    final private Int INT;
    final private Flt FLT;
    final private Bool BOOL;
    final private Ref REF;
    private TableCellEditor subeditor;
    final private PapyrusContext CONTEXT;

    /**
     * Subclass that handles strings.
     */
    @SuppressWarnings("serial")
    final private class Str extends AbstractCellEditor implements TableCellEditor {

        public Str() {
            this.EDITER = new JTextField(10);
        }

        @Override
        public Variable.Str getCellEditorValue() {
            String text = this.EDITER.getText();
            return new Variable.Str(text, CONTEXT);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (!(value instanceof Variable.Str)) {
                return null;
            }

            this.var = (Variable.Str) value;
            this.EDITER.setText(var.getValue().toString());
            return this.EDITER;
        }

        private Variable.Str var;
        final private JTextField EDITER;
    }

    /**
     * Subclass that handles integers.
     */
    @SuppressWarnings("serial")
    final private class Int extends AbstractCellEditor implements TableCellEditor {

        public Int() {
            this.EDITER = new JFormattedTextField(NumberFormat.getIntegerInstance());
            this.EDITER.setColumns(5);
        }

        @Override
        public Variable.Int getCellEditorValue() {
            Number value = (Number) this.EDITER.getValue();
            if (null != value) {
                return new Variable.Int(value.intValue());
            } else {
                return null;
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Variable.Int) {
                Variable.Int var = (Variable.Int) value;
                this.EDITER.setValue(var.getValue());
                return this.EDITER;
            }

            return null;
        }

        final private JFormattedTextField EDITER;
    }

    /**
     * Subclass that handles floats.
     */
    @SuppressWarnings("serial")
    final private class Flt extends AbstractCellEditor implements TableCellEditor {

        public Flt() {
            this.EDITER = new JFormattedTextField(NumberFormat.getNumberInstance());
            this.EDITER.setColumns(5);
        }

        @Override
        public Variable.Flt getCellEditorValue() {
            Number value = (Number) this.EDITER.getValue();
            if (null != value) {
                return new Variable.Flt(value.floatValue());
            } else {
                return null;
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (!(value instanceof Variable.Flt)) {
                return null;
            }

            Variable.Flt var = (Variable.Flt) value;
            this.EDITER.setValue(var.getValue());
            return this.EDITER;
        }

        final private JFormattedTextField EDITER;
    }

    /**
     * Subclass that handles booleans.
     */
    @SuppressWarnings("serial")
    final private class Bool extends AbstractCellEditor implements TableCellEditor {

        public Bool() {
            this.EDITER = new JComboBox<>(new Boolean[]{Boolean.TRUE, Boolean.FALSE});
            this.EDITER.setPrototypeDisplayValue(Boolean.FALSE);
        }

        @Override
        public Variable.Bool getCellEditorValue() {
            Boolean value = (Boolean) this.EDITER.getSelectedItem();
            if (null != value) {
                return new Variable.Bool(value);
            } else {
                return null;
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (!(value instanceof Variable.Bool)) {
                return null;
            }

            Variable.Bool var = (Variable.Bool) value;
            this.EDITER.setSelectedItem(var.getValue());
            return this.EDITER;
        }

        final private JComboBox<Boolean> EDITER;
    }

    /**
     * Subclass that handles integers.
     */
    @SuppressWarnings("serial")
    final private class Ref extends AbstractCellEditor implements TableCellEditor {

        public Ref() {
            this.EDITER = new JFormattedTextField(FORMATTER);
            this.EDITER.setColumns(16);
        }

        @Override
        public Variable.Ref getCellEditorValue() {
            Long v = (Long) this.EDITER.getValue();
            Variable.Ref ref = this.original.derive(v, CONTEXT);
            return ref;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Variable.Ref) {
                this.original = (Variable.Ref) value;
                this.eid = this.original.getRef();
                this.EDITER.setValue(this.eid.longValue());
                return this.EDITER;
            }

            return null;
        }

        final private JFormattedTextField EDITER;
        private Variable.Ref original;
        private EID eid;

        final private JFormattedTextField.AbstractFormatter FORMATTER = new JFormattedTextField.AbstractFormatter() {
            @Override
            public Object stringToValue(String text) throws ParseException {
                try {
                    return Long.parseUnsignedLong(text, 16);
                } catch (NumberFormatException ex) {
                    throw new ParseException(text, 0);
                }
            }

            @Override
            public String valueToString(Object value) throws ParseException {
                if (null == value) {
                    return "";
                } else if (value instanceof Number) {
                    Number num = (Number) value;
                    if (eid.is4Byte()) {
                        return Integer.toHexString(num.intValue());
                    } else {
                        return Long.toHexString(num.longValue());
                    }
                } else {
                    throw new ParseException(value.toString(), 0);
                }
            }
        };

    }

}
