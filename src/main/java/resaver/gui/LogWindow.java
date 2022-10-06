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

import static mf.TryCatch.Try;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Objects;
import javax.swing.*;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Displays progress messages in a JFrame. It receives messages via the Java
 * logging system.
 *
 * To use it, a <code>Handler</code> must be retrieved using the
 * <code>getHandler</code> method and attached to an instance of
 * <code>Logger</code>.
 *
 * @see java.util.logging.Logger
 * @see java.util.logging.Handler
 *
 * @author Mark
 */
public class LogWindow extends JScrollPane { 

    /**
     * Creates a new <code>LogWindow</code> with a default preferred size of
     * 480x400.
     */
    public LogWindow() {
        this.HANDLER = new LogWindowHandler();
        this.MESSAGES = new JTextArea();
        this.MESSAGES.setWrapStyleWord(true);
        this.MESSAGES.setLineWrap(true);
        super.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        super.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        super.setViewportView(this.MESSAGES);
        this.MESSAGES.setFont(this.MESSAGES.getFont().deriveFont(12.0f));
    }

    /**
     * @return A <code>Handler</code> for the Java logging system.
     */
    public Handler getHandler() {
        return this.HANDLER;
    }

    /**
     * This class handles the job of receiving log messages and displaying them.
     */
    private class LogWindowHandler extends Handler {

        public LogWindowHandler() {

        }

        @Override
        public void publish(LogRecord record) {
            final String rawMsg = record.getMessage() != null
                    ? record.getMessage()
                    : "NO MESSAGE";

            final String resMsg = record.getResourceBundle() != null
                    ? Try(() -> record.getResourceBundle().getString(rawMsg)).Catch(() -> rawMsg)
                    : rawMsg;
            
            final String paramMsg = record.getParameters() != null
                    ? MessageFormat.format(resMsg, record.getParameters())
                    : resMsg;
            
            
            String formatted = MessageFormat.format("[{0}.{1}] {2}", 
                    record.getSourceClassName(), 
                    record.getSourceMethodName(), 
                    paramMsg);
            
            LOGWRITER.println(formatted);
            
            if (record.getThrown() != null) {
                record.getThrown().printStackTrace(LOGWRITER);
            }
            
            LOGWRITER.flush();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    final private PrintWriter LOGWRITER = new PrintWriter(new Writer() {

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            MESSAGES.append(new String(cbuf, off, len));
        }

        @Override
        public void flush() {
            MESSAGES.setCaretPosition(LogWindow.this.MESSAGES.getDocument().getLength());
        }

        @Override
        public void close() {
        }
        
    });
    
    final private LogWindowHandler HANDLER;
    final private JTextArea MESSAGES;

}
