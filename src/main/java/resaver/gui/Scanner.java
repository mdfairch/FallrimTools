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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import resaver.Analysis;
import resaver.Game;
import resaver.Mod;
import resaver.ResaverFormatting;
import resaver.esp.ESP;
import resaver.esp.PluginData;
import resaver.esp.PluginException;
import resaver.esp.StringsFile;
import resaver.ess.ESS;
import resaver.ess.Plugin;
import resaver.ess.PluginInfo;

/**
 *
 * @author Mark Fairchild
 */
public class Scanner extends SwingWorker<resaver.Analysis, Double> {

    /**
     *
     * @param window
     * @param save
     * @param gameDir
     * @param mo2Ini
     * @param doAfter
     * @param progress
     */
    public Scanner(SaveWindow window, ESS save, Path gameDir, Path mo2Ini, Runnable doAfter, Consumer<String> progress) {
        this.WINDOW = Objects.requireNonNull(window, "The window field must not be null."); //NOI18N
        this.SAVE = Objects.requireNonNull(save, "The save field must not be null."); //NOI18N
        this.GAME_DIR = Objects.requireNonNull(gameDir, "The game directory field must not be null."); //NOI18N
        this.MO2_INI = mo2Ini;
        this.DOAFTER = doAfter;
        this.PROGRESS = Objects.requireNonNull(progress);
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    protected resaver.Analysis doInBackground() throws Exception {
        final mf.Timer TIMER = mf.Timer.startNew(I18N.getString("SCANNER_LOADPLUGINS"));
        final Game GAME = this.SAVE.getHeader().GAME;
        this.WINDOW.addWindowListener(this.LISTENER);
        this.PROGRESS.accept(I18N.getString("SCANNER_INITIALIZING"));
        boolean hasModInfo = false;
        
        try {
            final PluginInfo PLUGINS = this.SAVE.getPluginInfo();
            LOG.info("Scanning plugins.");
            this.PROGRESS.accept(I18N.getString("SCANNER_LOG_SCANNING"));

            final List<Mod> MODS = new ArrayList<>(1024);
            final Mod CORE = new Mod(GAME, GAME_DIR.resolve("data")); //NOI18N
            MODS.add(CORE);

            if (null != this.MO2_INI) {
                this.PROGRESS.accept(I18N.getString("SCANNER_ANALYZINGMO2"));
                LOG.info("Checking Mod Organizer 2.");
                final java.util.List<Mod> MOMODS = Configurator.analyzeModOrganizer2(GAME, this.MO2_INI);
                MODS.addAll(MOMODS);
                hasModInfo = !MOMODS.isEmpty();
            }

            this.PROGRESS.accept(I18N.getString("SCANNER_ORGANIZING"));
            final Map<Path, Plugin> PLUGINPATHS = PLUGINS.getPaths();
            
            boolean indiscriminate = java.util.prefs.Preferences.userNodeForPackage(resaver.ReSaver.class).getBoolean("settings.parseIndiscriminate", false);
            
            final Map<Plugin, Path> PLUGINFILEMAP = MODS.stream()
                    .flatMap(mod -> mod.getESPFiles().stream())
                    .filter(path -> indiscriminate || PLUGINPATHS.containsKey(path.getFileName()))
                    .collect(Collectors.toMap(
                            path -> PLUGINPATHS.get(path.getFileName()),
                            path -> path,
                            (p1, p2) -> p2));

            final Map<Plugin, Mod> PLUGIN_MOD_MAP = new HashMap<>();

            if (indiscriminate) {
                MODS.stream().map(mod -> mod.getESPFiles().stream()
                        .collect(Collectors.toMap(
                                path -> PLUGINPATHS.containsKey(path.getFileName())
                                    ? PLUGINPATHS.get(path.getFileName())
                                    : Plugin.makeUnloadedPlugin(path.getFileName().toString()),
                                path -> mod,
                                (m1, m2) -> m2)))
                        .forEach(map -> PLUGIN_MOD_MAP.putAll(map));
            } else {
                MODS.stream().map(mod -> mod.getESPFiles().stream()
                        .filter(path -> PLUGINPATHS.containsKey(path.getFileName()))
                        .collect(Collectors.toMap(
                                path -> PLUGINPATHS.get(path.getFileName()),
                                path -> mod,
                                (m1, m2) -> m2)))
                        .forEach(map -> PLUGIN_MOD_MAP.putAll(map));
            }
            
//            try {
//                this.PROGRESS.accept(MessageFormat.format(I18N.getString("SCANNER_READ_STORED"), GAME.NAME));
//                final Analysis STORED = readAnalysis();
//                if (null != STORED) {
//                    this.PROGRESS.accept(MessageFormat.format(I18N.getString("SCANNER_VERIFY_STORED"), GAME.NAME));
//                    boolean valid = PLUGINFILEMAP
//                            .entrySet()
//                            .stream()
//                            .allMatch(e -> CheckPlugin(e.getKey(), e.getValue(), STORED));
//                    if (valid) {
//                        Log.info("The stored analysis was valid.");
//                        return STORED;
//                    } else {
//                        Log.info("The stored analysis was invalid, moving on.");                        
//                    }
//                }
//            } catch(RuntimeException ex) {
//                LOG.log(Level.WARNING, "Failed to read stored analysis.", ex);
//            }
            
            // The language. Eventually make this selectable?
            final String LANGUAGE = (GAME.isSkyrim() ? "english" : "en"); //NOI18N

            // Analyze scripts from mods. 
            final Mod.Analysis PROFILEANALYSIS = MODS.stream()
                    .map(mod -> mod.getAnalysis())
                    .reduce(new Mod.Analysis(), (a1, a2) -> a1.merge(a2));

            final List<Path> ERR_ARCHIVE = new LinkedList<>();
            final List<Path> ERR_SCRIPTS = new LinkedList<>();
            final List<Path> ERR_STRINGS = new LinkedList<>();

            List<StringsFile> STRINGSFILES = new ArrayList<>();
            Map<Path, Path> SCRIPT_ORIGINS = new LinkedHashMap<>();

            // Read StringsFiles and scripts.
            final mf.Counter COUNTER = new mf.Counter(MODS.size());

            for (Mod mod : MODS) {
                if (mod == CORE) {
                    COUNTER.click();
                    this.PROGRESS.accept(MessageFormat.format(I18N.getString("SCANNER_READING_MOD"), GAME.NAME));
                } else {
                    this.PROGRESS.accept(MessageFormat.format(I18N.getString("SCANNER_READING_MODN"), COUNTER.eval(), mod.getShortName()));
                }

                final Mod.ModReadResults RESULTS = mod.readData(PLUGINS, LANGUAGE);
                STRINGSFILES.addAll(RESULTS.STRINGSFILES);
                SCRIPT_ORIGINS.putAll(RESULTS.SCRIPT_ORIGINS);

                ERR_ARCHIVE.addAll(RESULTS.ARCHIVE_ERRORS);
                ERR_SCRIPTS.addAll(RESULTS.SCRIPT_ERRORS);
                ERR_STRINGS.addAll(RESULTS.STRINGS_ERRORS);

                final MessageFormat ERRMSG = new MessageFormat("Could not read {0} from {1}.");
                RESULTS.getErrorFiles().forEach(v -> LOG.warning(ERRMSG.format(new Object[] {v, mod})));
            }

            this.PROGRESS.accept(I18N.getString("SCANNER_COMBINING"));

            // Map plugins to their stringsfiles.
            Map<Plugin, List<StringsFile>> PLUGIN_STRINGS = STRINGSFILES.stream()
                    .collect(Collectors.groupingBy(stringsFile -> stringsFile.PLUGIN));

            // The master stringtable.
            final resaver.esp.StringTable STRINGTABLE = new resaver.esp.StringTable();
            PLUGINS.stream()
                    .filter(PLUGIN_STRINGS::containsKey)
                    .forEach(plugin -> STRINGTABLE.populateFromFiles(PLUGIN_STRINGS.get(plugin), plugin));

            // Create the database for plugin data.
            final List<String> MISSING_PLUGINS = java.util.Collections.synchronizedList(new java.util.LinkedList<>());
            final List<String> ERR_PLUGINS = java.util.Collections.synchronizedList(new java.util.LinkedList<>());

            final Map<Plugin, PluginData> PLUGIN_DATA = new HashMap<>();
            final Map<Plugin, Long> SIZES = new HashMap<>();

            COUNTER.reset(PLUGINS.getSize());

            for (Plugin plugin : PLUGINS.getAllPlugins()) {
                this.PROGRESS.accept(MessageFormat.format(I18N.getString("SCANNER_PARSING_PLUGIN"), COUNTER.eval(), plugin.indexName()));

                if (!PLUGINFILEMAP.containsKey(plugin)) {
                    MISSING_PLUGINS.add(plugin.NAME);
                    LOG.info(MessageFormat.format("Plugin {0} could not be found.", plugin));

                } else {
                    try {
                        final PluginData INFO = ESP.skimPlugin(PLUGINFILEMAP.get(plugin), GAME, plugin, PLUGINS);
                        PLUGIN_DATA.put(plugin, INFO);

                        final String MSG = String.format(I18N.getString("SCANNER_LOG_PLUGINDATA"), INFO.getNameCount(), INFO.getScriptDataSize() / 1024.0f, plugin.indexName());
                        LOG.info(MSG);

                        assert plugin != null;
                        assert INFO.getScriptDataSize() >= 0;
                        SIZES.put(plugin, INFO.getScriptDataSize());

                    } catch (ClosedByInterruptException ex) {
                        throw ex;
                    } catch (FileNotFoundException ex) {
                        MISSING_PLUGINS.add(plugin.NAME);
                        final String MSG = MessageFormat.format("Plugin missing: {0}.", plugin.indexName());
                        LOG.log(Level.WARNING, MSG, ex);                        
                    } catch (RuntimeException ex) {
                        ERR_PLUGINS.add(plugin.NAME);
                        final String MSG = MessageFormat.format("Error reading plugin: {0}.", plugin.indexName());
                        LOG.log(Level.WARNING, MSG, ex);
                        ex.printStackTrace(System.err);
                    } catch (PluginException ex) {
                        ERR_PLUGINS.add(ex.PLUGIN);
                        final String MSG = MessageFormat.format("Error reading plugin: {0}.", ex.CONTEXT);
                        LOG.log(Level.WARNING, MSG, ex);
                        ex.printStackTrace(System.err);                        
                    }
                }
            }

            this.PROGRESS.accept(I18N.getString("SCANNER_CREATING_ANALYSIS"));

            final resaver.Analysis ANALYSIS = new resaver.Analysis(PROFILEANALYSIS, PLUGIN_DATA, STRINGTABLE, hasModInfo);
            if (null != this.SAVE) {
                this.WINDOW.setAnalysis(ANALYSIS);
            }

            TIMER.stop();
            LOG.info(MessageFormat.format("Plugin scanning completed, took {0}", TIMER.getFormattedTime()));

            // Find the worst offenders for script data size.
            final List<Plugin> OFFENDERS = SIZES.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .map(entry -> entry.getKey())
                    .limit(3).collect(Collectors.toList());

            final StringBuilder BUF = new StringBuilder();
            double scriptDataSize = ANALYSIS.getScriptDataSize() / 1048576.0;

            if (scriptDataSize > 32.0) {
                final String MSG = String.format(I18N.getString("SCANNER_SCRIPTDATA"), scriptDataSize);
                BUF.append(MSG);

                OFFENDERS.stream().forEach(plugin -> {
                    final double SIZE = SIZES.get(plugin) / 1048576.0;
                    BUF.append(String.format(I18N.getString("SCANNER_SCRIPTDATA_LIST"), SIZE, plugin)).append("\n");
                });
            }

            final java.util.function.Function<Path, CharSequence> NAMER = p -> p.getFileName().toString();

            if (MISSING_PLUGINS.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_PLUGIN_MISSING"), MISSING_PLUGINS, 10));
            } else if (MISSING_PLUGINS.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_PLUGINS_MISSING"), MISSING_PLUGINS, 10));
            }

            if (ERR_PLUGINS.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_PLUGIN_READERROR"), ERR_PLUGINS, 10));
            } else if (ERR_PLUGINS.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_PLUGINS_READERROR"), ERR_PLUGINS, 10));
            }

            if (ERR_ARCHIVE.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_ARCHIVE_READERROR"), ERR_ARCHIVE, 3, NAMER));
            } else if (ERR_ARCHIVE.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_ARCHIVES_READERROR"), ERR_ARCHIVE, 3, NAMER));
            }

            if (ERR_STRINGS.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_STRINGSFILE_READERROR"), ERR_STRINGS, 3, NAMER));
            } else if (ERR_STRINGS.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_STRINGSFILES_READERROR"), ERR_STRINGS, 3, NAMER));
            }

            if (ERR_SCRIPTS.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_SCRIPT_READERROR"), ERR_SCRIPTS, 3, NAMER));
            } else if (ERR_SCRIPTS.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList(I18N.getString("SCANNER_SCRIPTS_READERROR"), ERR_SCRIPTS, 3, NAMER));
            }

            if (BUF.length() > 0) {
                JOptionPane.showMessageDialog(this.WINDOW, BUF.toString(), I18N.getString("SCANNER_DONE"), JOptionPane.INFORMATION_MESSAGE);
            }

            //writeAnalysis(ANALYSIS);
            
            return ANALYSIS;

        } catch (ClosedByInterruptException ex) {
            LOG.log(Level.SEVERE, I18N.getString("SCANNER_LOG_TERMINATED"), ex);
            return null;

        } catch (Exception | Error ex) {
            final String MSG = MessageFormat.format(I18N.getString("SCANNER_PLUGINS_ERROR"), ex.getMessage());
            LOG.log(Level.SEVERE, MSG, ex);
            JOptionPane.showMessageDialog(this.WINDOW, MSG, I18N.getString("SCANNER_PLUGINS_ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
            return null;

        } finally {
            this.WINDOW.removeWindowListener(this.LISTENER);

            if (this.DOAFTER != null) {
                SwingUtilities.invokeLater(DOAFTER);
            }

        }
    }

    final private SaveWindow WINDOW;
    final private ESS SAVE;
    final private Path GAME_DIR;
    final private Path MO2_INI;
    final private Runnable DOAFTER;
    final private Consumer<String> PROGRESS;
    static final private Logger LOG = Logger.getLogger(Scanner.class.getCanonicalName());
    static final private ResourceBundle I18N = ResourceBundle.getBundle("Strings");

    final private WindowAdapter LISTENER = new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            if (!isDone()) {
                cancel(true);
            }
        }
    };

    /**
     * Keep this around for debugging.
     */
    @SuppressWarnings("unused")
    private boolean CheckPlugin(Plugin plugin, Path path, Analysis analysis) {
        try {
            if (!analysis.ESP_INFOS.containsKey(plugin)) return false;
            long storedTimeStamp = analysis.ESP_INFOS.get(plugin).TIMESTAMP;
            long currentTimeStamp = Files.getLastModifiedTime(path).toMillis();
            return storedTimeStamp == currentTimeStamp;
        } catch (IOException ex) {
            return false;
        }
    }
}
