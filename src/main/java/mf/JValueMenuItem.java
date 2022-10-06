/*
 * Copyright 2018 Mark.
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
package mf;

import javax.swing.JMenuItem;

/**
 * A <code>JMenuItem</code> that has a parameter and whose display string is
 * formatted.
 *
 * @author Mark Fairchild
 * @since 2018-09-22
 */
public class JValueMenuItem<T> extends JMenuItem {

    public JValueMenuItem(String initialFormat, T initialValue) {
        super(String.format(initialFormat, initialValue));
        this.format = initialFormat;
        this.value = initialValue;
    }

    @Override
    public void setText(String newFormat) {
        this.format = newFormat;
        this.updateText();
    }

    public void setValue(T newValue) {
        this.value = newValue;
        this.updateText();
    }

    public T getValue() {
        return this.value;
    }

    private void updateText() {
        if (this.value == null) {
            final String formatted = String.format(this.format, "none");
            super.setText(formatted);
        } else {
            final String formatted = String.format(this.format, this.value);
            super.setText(formatted);
        }
    }

    /**
     *
     */
    @Override
    public void updateUI() {
        this.updateText();
        super.updateUI();
    }

    private String format;
    private T value;
}
