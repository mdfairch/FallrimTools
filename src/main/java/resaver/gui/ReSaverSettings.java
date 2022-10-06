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

import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ResourceBundle;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import resaver.Game;

/**
 * Settings DialogBox.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class ReSaverSettings extends JDialog {

    public ReSaverSettings(SaveWindow parent, Game currentGame) {
        super(parent, true);
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setLocationRelativeTo(parent);

        final JTabbedPane PANE = new JTabbedPane();
        super.setContentPane(PANE);

        {
            final JPanel TAB = new JPanel();
            final JPanel LEFT = new JPanel();
            final JPanel RIGHT = new JPanel();
            TAB.setLayout(new BoxLayout(TAB, BoxLayout.LINE_AXIS));
            LEFT.setLayout(new BoxLayout(LEFT, BoxLayout.PAGE_AXIS));
            RIGHT.setLayout(new BoxLayout(RIGHT, BoxLayout.PAGE_AXIS));
            TAB.add(LEFT);
            TAB.add(RIGHT);
            
            PANE.add(I18N.getString("SETTINGS_GENERAL"), TAB);

            {
                final JCheckBox ALWAYSPARSEMODS = new JCheckBox(I18N.getString("SETTINGS_AUTO_PARSEPLUGINS"), PREFS.getBoolean("settings.alwaysParsePlugins", true)); //NOI18N
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(ALWAYSPARSEMODS);
                LEFT.add(PANEL);
                ALWAYSPARSEMODS.addActionListener(e -> PREFS.putBoolean("settings.alwaysParsePlugins", ALWAYSPARSEMODS.isSelected())); //NOI18N
            }

            {
                final JCheckBox ALWAYSPARSEFORMS = new JCheckBox(I18N.getString("SETTINGS_AUTO_PARSEFORMS"), PREFS.getBoolean("settings.alwaysParseForms", false)); //NOI18N
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(ALWAYSPARSEFORMS);
                LEFT.add(PANEL);
                ALWAYSPARSEFORMS.addActionListener(e -> PREFS.putBoolean("settings.alwaysParseForms", ALWAYSPARSEFORMS.isSelected())); //NOI18N
            }

            {
                final JCheckBox ALWAYSPARSEMODS = new JCheckBox(I18N.getString("SETTINGS_AUTO_PARSEPLUGINS"), PREFS.getBoolean("settings.alwaysParsePlugins", true)); //NOI18N
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(ALWAYSPARSEMODS);
                LEFT.add(PANEL);
                ALWAYSPARSEMODS.addActionListener(e -> PREFS.putBoolean("settings.alwaysParsePlugins", ALWAYSPARSEMODS.isSelected())); //NOI18N
            }

            {
                final JCheckBox INDISC = new JCheckBox(I18N.getString("SETTINGS_INDISCRIMINATE_PARSING"), PREFS.getBoolean("settings.parseIndiscriminate", false)); //NOI18N
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(INDISC);
                LEFT.add(PANEL);
                INDISC.addActionListener(e -> PREFS.putBoolean("settings.parseIndiscriminate", INDISC.isSelected())); //NOI18N
            }

            {
                final JCheckBox ALTSORT = new JCheckBox(I18N.getString("SETTINGS_ALTERNATE_SORTING"), PREFS.getBoolean("settings.altSort", true)); //NOI18N
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(ALTSORT);
                LEFT.add(PANEL);
                ALTSORT.addActionListener(e -> PREFS.putBoolean("settings.altSort", ALTSORT.isSelected())); //NOI18N
            }

            {
                final JCheckBox DARKTHEME = new JCheckBox(I18N.getString("SETTINGS_DARK"), PREFS.getBoolean("settings.darktheme", false)); //NOI18N
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(DARKTHEME);
                RIGHT.add(PANEL);
                DARKTHEME.addActionListener(e -> PREFS.putBoolean("settings.darktheme", DARKTHEME.isSelected())); //NOI18N
            }

            {
                final JCheckBox JAVAFX = new JCheckBox(I18N.getString("SETTINGS_USE_JAVAFX"), PREFS.getBoolean("settings.javafx", false)); //NOI18N
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(JAVAFX);
                RIGHT.add(PANEL);
                JAVAFX.addActionListener(e -> PREFS.putBoolean("settings.javafx", JAVAFX.isSelected())); //NOI18N
            }

            {
                final JLabel LABEL = new JLabel(I18N.getString("SETTINGS_FONT_SCALING"));
                final JFormattedTextField SCALEFIELD = new JFormattedTextField(PREFS.getFloat("settings.fontScale", 1.0f)); //NOI18N
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));

                LABEL.setLabelFor(SCALEFIELD);
                PANEL.add(LABEL);
                PANEL.add(SCALEFIELD);
                SCALEFIELD.setColumns(5);
                RIGHT.add(PANEL);

                SCALEFIELD.addActionListener(e -> {
                    Number n = (Number) SCALEFIELD.getValue();
                    float fontScale = Math.min(5.0f, Math.max(n.floatValue(), 0.5f));
                    PREFS.putFloat("settings.fontScale", fontScale); //NOI18N
                    SCALEFIELD.setValue(fontScale);

                    UIManager.getLookAndFeelDefaults().keySet().stream()
                            .filter(key -> key.toString().endsWith(".font")) //NOI18N
                            .forEach(key -> {
                                Font font = UIManager.getFont(key);
                                Font biggerFont = font.deriveFont(fontScale * font.getSize2D());
                                UIManager.put(key, biggerFont);
                            });
                });
            }

        }

        Game.VALUES.forEach((game) -> {
            JPanel TAB = new JPanel();
            TAB.setLayout(new BoxLayout(TAB, BoxLayout.PAGE_AXIS));
            PANE.add(game.NAME, TAB);

            {
                final JLabel LABEL = new JLabel(I18N.getString("SETTINGS_GAME_DIRECTORY"));
                final JTextField GAMEDIR = new JTextField(getFirst(Configurator.getGameDirectory(game), HOME).toString(), 50);
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                final JButton BUTTON = new JButton(I18N.getString("SETTINGS_SELECT"));
                LABEL.setLabelFor(GAMEDIR);
                GAMEDIR.setEditable(false);
                PANEL.add(LABEL);
                PANEL.add(GAMEDIR);
                PANEL.add(BUTTON);
                TAB.add(PANEL);

                BUTTON.addActionListener(e -> {
                    Path newPath = getFirst(Configurator.selectGameDirectory(parent, game), Configurator.getGameDirectory(game), HOME);
                    GAMEDIR.setText(newPath.toString());
                });
            }

            {
                final JLabel LABEL = new JLabel(I18N.getString("SETTINGS_MO2INI"));
                final JTextField MO2INI = new JTextField(getFirst(Configurator.getMO2Ini(game), HOME).toString(), 50);
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                final JButton BUTTON = new JButton(I18N.getString("SETTINGS_SELECT"));
                LABEL.setLabelFor(MO2INI);
                MO2INI.setEditable(false);
                PANEL.add(LABEL);
                PANEL.add(MO2INI);
                PANEL.add(BUTTON);
                TAB.add(PANEL);

                BUTTON.addActionListener(e -> {
                    Path newPath = getFirst(Configurator.selectMO2Ini(parent, game), Configurator.getMO2Ini(game), HOME);
                    MO2INI.setText(newPath.toString());
                });
            }

            {
                final JLabel LABEL = new JLabel(I18N.getString("SETTINGS_SAVEDIR"));
                final JTextField SAVEDIR = new JTextField(getFirst(Configurator.getSaveDirectory(game), HOME).toString(), 50);
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                final JButton BUTTON = new JButton(I18N.getString("SETTINGS_SELECT"));
                LABEL.setLabelFor(SAVEDIR);
                SAVEDIR.setEditable(false);
                PANEL.add(LABEL);
                PANEL.add(SAVEDIR);
                PANEL.add(BUTTON);
                TAB.add(PANEL);

                BUTTON.addActionListener(e -> {
                    Path newPath = getFirst(Configurator.selectSavefileDirectory(parent, game), Configurator.getSaveDirectory(game), HOME);
                    SAVEDIR.setText(newPath.toString());
                });
            }
        });

        if (currentGame != null) {
            PANE.setSelectedIndex(Game.VALUES.indexOf(currentGame));
        }
    }

    /**
     *
     * @param items
     * @return
     */
    static public Path getFirst(Path... items) {
        return Arrays.stream(items).filter(i -> i != null).findFirst().orElse(null);
    }

    static private final Path HOME = Paths.get(System.getProperty("user.home"), "appData", "local", "ModOrganizer"); //NOI18N
    static final private java.util.prefs.Preferences PREFS = java.util.prefs.Preferences.userNodeForPackage(resaver.ReSaver.class);
    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");

}
