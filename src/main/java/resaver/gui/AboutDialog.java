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

import java.awt.Window;
import java.io.IOException;
import java.net.JarURLConnection;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 *
 * @author Mark
 */
public class AboutDialog {

    static public void show(Window window) {
        final StringBuilder BUF = new StringBuilder()
                .append(I18N.getString("ABOUT_WELCOME"))
                .append("\n\n")
                .append(AboutDialog.getVersion())
                .append("\n\n")
                .append(I18N.getString("ABOUT_COPYRIGHT"))
                .append("\n")
                .append(I18N.getString("ABOUT_LICENSE"))
                .append("\n\n")
                .append(MessageFormat.format(I18N.getString("ABOUT_OS_VER_ARCH"), System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch")))
                .append("\n")
                .append(MessageFormat.format(I18N.getString("ABOUT_MEMFREE"), Runtime.getRuntime().maxMemory() / 1073741824.0))
                .append("\n")
                .append(I18N.getString("ABOUT_JAVAPATH")).append(System.getProperty("java.home"))
                .append("\n")
                .append(I18N.getString("ABOUT_VENDOR")).append(System.getProperty("java.vendor"))
                .append("\n")
                .append(I18N.getString("ABOUT_JAVAVERSION")).append(System.getProperty("java.version"));

        final ImageIcon ICON = AboutDialog.getLogo();

        if (ICON == null) {
            JOptionPane.showMessageDialog(window, BUF.toString(), I18N.getString("ABOUT_TITLE"), 0);
        } else {
            JOptionPane.showMessageDialog(window, BUF.toString(), I18N.getString("ABOUT_TITLE"), 0, ICON);
        }
    }

    static public ImageIcon getLogo() {
        try {
            final java.net.URL URL = AboutDialog.class.getClassLoader().getResource(ICON_FILENAME);
            final java.awt.image.BufferedImage IMAGE = javax.imageio.ImageIO.read(URL);
            final ImageIcon ICON = new ImageIcon(IMAGE);
            return ICON;
        } catch (IOException | RuntimeException ex) {
            LOG.warning(MessageFormat.format(I18N.getString("ABOUT_RESOURCE_URL"), ICON_FILENAME));
            return null;
        }
    }

    static public CharSequence getVersion() {
        try {
            final java.net.URL RES = SaveWindow.class.getResource(SaveWindow.class.getSimpleName() + ".class");
            final java.net.JarURLConnection CONN = (JarURLConnection) RES.openConnection();
            final java.util.jar.Manifest MANIFEST = CONN.getManifest();
            final java.util.jar.Attributes ATTR = MANIFEST.getMainAttributes();
            return new StringBuilder()
                    .append(ATTR.getValue("Implementation-Version"))
                    .append('.')
                    .append(ATTR.getValue("Implementation-Build"))
                    .append(" (")
                    .append(ATTR.getValue("Built-Date"))
                    .append(")");
        } catch (IOException | ClassCastException ex) {
            return "(development version)";
        }

    }

    static final private Logger LOG = Logger.getLogger(AboutDialog.class.getCanonicalName());
    static final private String ICON_FILENAME = "CatsInSunbeam.jpg";
    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");
    
}
