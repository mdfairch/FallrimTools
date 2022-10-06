/*
 * Copyright 2020 Mark.
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

import javax.swing.JTextPane;
import javax.swing.event.HyperlinkListener;

/**
 * Displays HTML formatted text and supports a hyperlink listener.
 * 
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class InfoPane extends JTextPane {

    /**
     * @param text
     * @param listener
     */
    public InfoPane(String text, HyperlinkListener listener) {
        super.setEditable(false);
        super.setContentType("text/html");
        if (text != null) {
            this.setText(text);
        }
        if (listener != null) {
            super.addHyperlinkListener(listener);
        }
    }

    /**
     *
     * @param text
     *
     */
    @Override
    public void setText(String text) {
        super.setText(text);
        super.setCaretPosition(0);
    }
}
