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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.*;
import resaver.ess.AnalyzableElement;
import resaver.ess.papyrus.*;

/**
 * Describes a JTable specialized for displaying variable tables.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class VariableTable extends JTable {

    /**
     * Creates a new <code>VariableTable</code>.
     *
     * @param window
     */
    public VariableTable(SaveWindow window) {
        this.WINDOW = Objects.requireNonNull(window);
        this.MI_FIND = new JMenuItem(I18N.getString("VARTABLE_FIND"), KeyEvent.VK_F);
        this.TABLE_POPUP_MENU = new JPopupMenu(I18N.getString("VARTABLE_MENU"));
        this.initComponent();
    }

    /**
     * Initializes the table's components.
     */
    private void initComponent() {
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getModel().addTableModelListener(e -> this.WINDOW.setModified());
        this.TABLE_POPUP_MENU.add(this.MI_FIND);

        this.MI_FIND.addActionListener(e -> {
            int viewRow = getSelectedRow();
            int modelRow = convertRowIndexToModel(viewRow);
            int column = getModel().getColumnCount() - 1;
            Object o = getModel().getValueAt(modelRow, column);
            assert o instanceof Variable;
            Variable var = (Variable) o;

            if (var.hasRef() && !var.getRef().isZero()) {
                if (var instanceof Variable.Array) {
                    Variable.Array arr = (Variable.Array) var;
                    this.WINDOW.findElement(arr.getArray());
                } else {
                    this.WINDOW.findElement(var.getReferent());
                }
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = rowAtPoint(e.getPoint());
                    int col = columnAtPoint(e.getPoint());
                    setRowSelectionInterval(row, row);

                    int modelRow = convertRowIndexToModel(row);
                    int column = getModel().getColumnCount() - 1;
                    Object o = getModel().getValueAt(modelRow, column);
                    assert o instanceof Variable;
                    Variable var = (Variable) o;

                    if (var.hasRef() && !var.getRef().isZero()) {
                        TABLE_POPUP_MENU.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    /**
     *
     * @param index
     */
    public void scrollSelectionToVisible(int index) {
        if (index < 0) {
            return;
        }

        this.getSelectionModel().setSelectionInterval(index, index);
        if (!(this.getParent() instanceof JViewport)) {
            return;
        }

        final JViewport PARENT = (JViewport) this.getParent();
        final Rectangle CELL_RECTANGLE = this.getCellRect(index, 0, true);
        final Point POINT = PARENT.getViewPosition();
        CELL_RECTANGLE.setLocation(CELL_RECTANGLE.x - POINT.x, CELL_RECTANGLE.y - POINT.y);
        this.scrollRectToVisible(CELL_RECTANGLE);
    }

    /**
     *
     * @param element
     * @return
     */
    public boolean isSupported(AnalyzableElement element) {
        return element instanceof ScriptInstance
                || element instanceof StructInstance
                || element instanceof Struct
                || element instanceof StackFrame
                || element instanceof ArrayInfo
                || element instanceof FunctionMessageData
                || element instanceof SuspendedStack
                || element instanceof FunctionMessage
                || element instanceof Reference;
    }

    /**
     * Clears the table.
     */
    public void clearTable() {
        this.setModel(new DefaultTableModel());
    }

    /**
     * Displays a <code>AnalyzableElement</code> using an appropriate model.
     *
     * @param element The <code>PapyrusElement</code> to display.
     * @param context The <code>PapyrusContext</code> info.
     */
    public void displayElement(AnalyzableElement element, PapyrusContext context) {
        if (element instanceof ArrayInfo) {
            this.displayArray((ArrayInfo) element, context);

        } else if (element instanceof HasVariables) {
            this.displayVariableTable((HasVariables) element, context);

        } else if (element instanceof Definition) {
            this.displayDefinition((Definition) element, context);

        } else if (element instanceof FunctionMessageData) {
            this.displayVariableTable((FunctionMessageData) element, context);

        } else if (element instanceof SuspendedStack) {
            SuspendedStack stack = (SuspendedStack) element;
            if (stack.hasMessage()) {
                this.displayVariableTable(stack.getMessage(), context);
            } else {
                this.clearTable();
            }
        } else if (element instanceof FunctionMessage) {
            FunctionMessage fn = (FunctionMessage) element;
            if (fn.hasMessage()) {
                this.displayVariableTable(fn.getMessage(), context);
            } else {
                this.clearTable();
            }
        } else {
            this.clearTable();
        }
    }

    /**
     * Displays a <code>Definition</code> using an appropriate model.
     *
     * @param def The <code>Definition</code> to display.
     * @param context The <code>PapyrusContext</code> info.
     */
    private void displayDefinition(Definition def, PapyrusContext context) {
        this.setDefaultRenderer(Variable.class, new VariableCellRenderer());
        this.setDefaultEditor(Variable.class, new VariableCellEditor(context));
        this.setModel(new DefinitionTableModel(def));
        this.getColumn(this.getColumnName(0)).setMinWidth(25);
        this.getColumn(this.getColumnName(0)).setMaxWidth(25);
    }

    /**
     * Displays a <code>ScriptInstance</code> using an appropriate model.
     *
     * @param instance The <code>PapyrusElement</code> to display.
     * @param context The <code>PapyrusContext</code> info.
     */
    private void displayVariableTable(HasVariables instance, PapyrusContext context) {
        this.setDefaultRenderer(Variable.class, new VariableCellRenderer());
        this.setDefaultEditor(Variable.class, new VariableCellEditor(context));
        this.setModel(new VariableTableModel(instance));
        this.getColumn(this.getColumnName(0)).setMinWidth(25);
        this.getColumn(this.getColumnName(0)).setMaxWidth(25);
        this.getColumn(this.getColumnName(1)).setMinWidth(120);
        this.getColumn(this.getColumnName(1)).setMaxWidth(120);
    }

    /**
     * Displays an <code>ArrayInfo</code> using an appropriate model.
     *
     * @param array The <code>PapyrusElement</code> to display.
     * @param context The <code>PapyrusContext</code> info.
     */
    private void displayArray(ArrayInfo array, PapyrusContext context) {
        this.setDefaultRenderer(Variable.class, new VariableCellRenderer());
        this.setDefaultEditor(Variable.class, new VariableCellEditor(context));
        this.setModel(new ArrayTableModel(array));
        this.getColumn(this.getColumnName(0)).setMinWidth(25);
        this.getColumn(this.getColumnName(0)).setMaxWidth(25);
        this.getColumn(this.getColumnName(1)).setMinWidth(120);
        this.getColumn(this.getColumnName(1)).setMaxWidth(120);
    }

    final private JPopupMenu TABLE_POPUP_MENU;
    final private JMenuItem MI_FIND;
    final private SaveWindow WINDOW;
    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");

}
