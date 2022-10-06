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

import java.util.List;
import java.util.Objects;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import resaver.ess.papyrus.*;


/**
 * A table model for <code>ArrayInfo</code>.
 * 
 * @author Mark Fairchild
 */
public class ArrayTableModel implements javax.swing.table.TableModel {

    /**
     * Creates a new <code>VariableTableModel</code>.
     * @param data The instance of <code>HasVariables</code>.
     */
    public ArrayTableModel(ArrayInfo data) {
        this.DATA = Objects.requireNonNull(data);
        this.LISTENERS = new java.util.LinkedList<>();
    }

    @Override
    public int getRowCount() {
        return this.DATA.getVariables().size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        assert 0 <= rowIndex && rowIndex < this.getRowCount();

        switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return this.DATA.getVariables().get(rowIndex).toTypeString();
            case 2:
                return this.DATA.getVariables().get(rowIndex);
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMNNAMES[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMNTYPES[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != 2) {
            return false;
        }

        assert 0 <= rowIndex && rowIndex < this.getRowCount();
        Variable var = this.DATA.getVariables().get(rowIndex);

        switch (var.getType()) {
            case STRING:
            case INTEGER:
            case FLOAT:
            case BOOLEAN:
            case REF:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!this.isCellEditable(rowIndex, columnIndex)) {
            throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
        } else if (!(aValue instanceof Variable)) {
            throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
        }

        this.DATA.setVariable(rowIndex, (Variable) aValue);
        this.fireTableCellUpdate(rowIndex, columnIndex);
    }

    public void fireTableCellUpdate(int row, int column) {
        TableModelEvent event = new TableModelEvent(this, row, row, column, TableModelEvent.UPDATE);
        this.LISTENERS.forEach(l -> l.tableChanged(event));
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        this.LISTENERS.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        this.LISTENERS.remove(l);
    }

    final private List<TableModelListener> LISTENERS;
    final private ArrayInfo DATA;
    final private String[] COLUMNNAMES = new String[]{"#", "Type", "Value"};
    static final private Class<?>[] COLUMNTYPES = new Class<?>[]{Integer.class, String.class, Variable.class};
}
