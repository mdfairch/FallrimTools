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

import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.event.TableModelListener;
import resaver.ess.papyrus.*;

/**
 * A table model for papyrus scripts.
 *
 * @author Mark Fairchild
 */
public class DefinitionTableModel implements javax.swing.table.TableModel {

    public DefinitionTableModel(Definition def) {        
        this.DEFINITION = Objects.requireNonNull(def);
    }

    @Override
    public int getRowCount() {
        return this.DEFINITION.getMembers().size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        assert 0 <= rowIndex && rowIndex < this.getRowCount();
        MemberDesc member = this.DEFINITION.getMembers().get(rowIndex);

        switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return member.getType();
            case 2:
                return member.getName();
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
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
    }

    final private Definition DEFINITION;
    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");
    static final private Class<?>[] COLUMNTYPES = new Class<?>[]{Integer.class, String.class, String.class};
    final private String[] COLUMNNAMES = new String[]{
        I18N.getString("VARTABLE_INDEX"), 
        I18N.getString("VARTABLE_TYPE"), 
        I18N.getString("VARTABLE_NAME"), 
    };
    
}
