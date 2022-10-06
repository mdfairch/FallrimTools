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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import resaver.ess.papyrus.Variable;

/**
 * Renderer for cells showing variables, to allow hot-linking of references.
 *
 * @author Mark Fairchld
 */
@SuppressWarnings("serial")
final class VariableCellRenderer extends DefaultTableCellRenderer {

    public VariableCellRenderer() {
        this.DEFAULT_COLOR = super.getForeground();
        this.INVALID_COLOR = Color.RED;
        this.NULL_COLOR = Color.BLUE;
        this.DEFAULT_FONT = super.getFont();
        this.INVALID_FONT = super.getFont().deriveFont(Font.ITALIC);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Variable) {
            final Variable VAR = (Variable) value;
            final String STR = ((Variable) value).toValueString();
            final Component C = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            C.setForeground(this.DEFAULT_COLOR);
            C.setFont(this.DEFAULT_FONT);

            if (VAR instanceof Variable.Ref) {
                Variable.Ref REF = (Variable.Ref) VAR;
                if (REF.isNull()) {
                    C.setForeground(this.NULL_COLOR);
                } else if (null == REF.getReferent()) {
                    C.setForeground(this.INVALID_COLOR);
                    C.setFont(this.INVALID_FONT);
                }
            }

            return C;

        } else {
            final Component C = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            C.setForeground(this.DEFAULT_COLOR);
            C.setFont(this.DEFAULT_FONT);
            return C;
        }
    }

    final private Color DEFAULT_COLOR;
    final private Color INVALID_COLOR;
    final private Color NULL_COLOR;
    final private Font DEFAULT_FONT;
    final private Font INVALID_FONT;
}
