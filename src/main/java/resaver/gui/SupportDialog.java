/*
 * Copyright 2023 Mark.
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import static j2html.TagCreator.*;
import java.awt.Insets;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 *
 * @author Mark
 */
public class SupportDialog {

    /**
     * Adapted from https://stackoverflow.com/questions/8348063/clickable-links-in-joptionpane
     * Author: Jean-Marc Astesana (https://stackoverflow.com/users/991850/jean-marc-astesana)
     * Date: 2023-09-07
     * 
     * @param window
     */
    static public void show(Window window) {
        String text = html(
                body(
                        //style().with(rawHtml(getStyle())),
                        p("FallrimTools and ReSaver take an extraordinary amount of time and effort to create and maintain,"),
                        p ("and I always try to help people with their savefile problems on NexusMods and Discord."),
                        p("If you'd like to contribute to that work, and help me to afford rent and food, that would be absolutely lovely. \u2665"),
                        p(a("You can support me and ReSaver on Patreon (patreon.com/user?u=18285974).").withHref("https://www.patreon.com/user?u=18285974")),
                        p(a("You can support me and ReSaver through PayPal (paypal.me/MarkDFSoftware).").withHref("https://www.paypal.me/MarkDFSoftware")),
                        p(a("You can support me and ReSaver through NexusMods (nexusmods.com/users/723902).").withHref("https://www.nexusmods.com/users/723902")),
                        p("Any support is welcome!")
                ).attr("style", getStyle())
        ).toString();
        
        JEditorPane pane = new JEditorPane("text/html", text);
        pane.setEditable(false);
        
        Insets margin = pane.getMargin();
        margin.left += 10;
        margin.right += 10;
        pane.setMargin(margin);
        
        pane.addHyperlinkListener((HyperlinkEvent e) -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (java.io.IOException|java.net.URISyntaxException ex) {
                    // why would this ever happen?
                }
            }
        });
        
        final ImageIcon ICON = SupportDialog.getLogo();

        if (ICON == null) {
            JOptionPane.showMessageDialog(window, pane, "Support", 0);
        } else {
            JOptionPane.showMessageDialog(window, pane, "Support", 0, ICON);
        }
    }

    /**
     * Taken from https://stackoverflow.com/questions/8348063/clickable-links-in-joptionpane
     * Author: Jean-Marc Astesana (https://stackoverflow.com/users/991850/jean-marc-astesana)
     * Date: 2023-09-07
     */
    static private String getStyle() {
        // for copying style
        JLabel label = new JLabel();
        java.awt.Font font = label.getFont();
        java.awt.Color color = label.getBackground();

        // create some css from the label's font
        return new StringBuilder("font-family:" + font.getFamily() + ";")
            .append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";")
            .append("font-size:" + font.getSize() + "pt;")
            .append("background-color: rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+");")
            .toString();
    }
    
    static public ImageIcon getLogo() {
        try {
            final java.net.URL URL = SupportDialog.class.getClassLoader().getResource(ICON_FILENAME);
            final java.awt.image.BufferedImage IMAGE = javax.imageio.ImageIO.read(URL);
            final ImageIcon ICON = new ImageIcon(IMAGE);
            return ICON;
        } catch (IOException | RuntimeException ex) {
            LOG.warning(MessageFormat.format(I18N.getString("ABOUT_RESOURCE_URL"), ICON_FILENAME));
            return null;
        }
    }

    static final private Logger LOG = Logger.getLogger(SupportDialog.class.getCanonicalName());
    static final private String ICON_FILENAME = "CatsInSunbeam.jpg";
    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");
    
}
