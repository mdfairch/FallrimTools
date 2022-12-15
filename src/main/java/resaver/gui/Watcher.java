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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import resaver.Game;
import resaver.ess.ModelBuilder.SortingMethod;
import resaver.ess.papyrus.Worrier;

/**
 *
 * @author Mark
 */
public class Watcher {

    /**
     *
     * @param window
     * @param worrier
     *
     */
    public Watcher(SaveWindow window, Worrier worrier) {
        this.WINDOW = Objects.requireNonNull(window);
        this.WORRIER = Objects.requireNonNull(worrier);
        this.worker = null;
        this.watchDirectories = null;

        this.WINDOW.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Watcher.this.stop();
            }
        });

    }


    synchronized public void start(Path... watchDirectories) {
        if (Arrays.stream(watchDirectories).allMatch(p -> Configurator.validDir(p))) {
            if (this.isRunning()) {
                this.stop();
            }
            this.watchDirectories = Arrays.asList(watchDirectories);
            this.worker = new WatchWorker();
            this.worker.execute();
        } else {
            throw new IllegalArgumentException("Invalid watch directory.");
        }
    }

    /**
     *
     */
    synchronized public void resume() {
        if (this.isRunning()) {
            return;
        }

        this.worker = new WatchWorker();
        this.worker.execute();
    }

    /**
     *
     */
    synchronized public void stop() {
        while (this.isRunning()) {
            this.worker.cancel(true);
        }
        this.worker = null;
    }

    /**
     *
     * @return
     */
    public boolean isRunning() {
        return this.worker != null && !this.worker.isDone();
    }

    /**
     *
     * @author Mark
     */
    class WatchWorker extends SwingWorker<List<Path>, Double> {

        /**
         *
         * @return @throws Exception
         */
        @Override
        synchronized protected List<Path> doInBackground() throws Exception {
            java.util.Set<Path> dirCollection = Stream.concat(
                    watchDirectories != null ? watchDirectories.stream() : Stream.empty(), 
                    Game.VALUES.stream().map(game -> Configurator.getSaveDirectory(game)))
                .filter(Files::exists)
                .collect(Collectors.toSet());

            final java.nio.file.FileSystem FS = java.nio.file.FileSystems.getDefault();

            try (final WatchService WATCHSERVICE = FS.newWatchService()) {
                Map<WatchKey, Path> REGKEYS = new HashMap<>();
                for (Path dir : dirCollection) {
                    LOG.info(String.format("WATCHER: initializing for %s", dir));
                    REGKEYS.put(dir.register(WATCHSERVICE, StandardWatchEventKinds.ENTRY_CREATE), dir);
                }

                while (true) {

                    final WatchKey EVENTKEY = WATCHSERVICE.take();
                    //final WatchKey EVENTKEY = WATCHSERVICE.poll(10, TimeUnit.SECONDS);
                    
                    if (EVENTKEY == null || !EVENTKEY.isValid()) {
                        LOG.info("INVALID EVENTKEY");
                        break;
                    }

                    for (WatchEvent<?> event : EVENTKEY.pollEvents()) {
                        if (event.kind() == java.nio.file.StandardWatchEventKinds.OVERFLOW) {
                            LOG.info("WATCHER OVERFLOW");
                            continue;
                        }

                        @SuppressWarnings("unchecked")
                        final Path NAME = ((WatchEvent<Path>) event).context();
                        final Path FULL = REGKEYS.get(EVENTKEY).resolve(NAME);

                        if (Files.exists(FULL) && MATCHER.matches(FULL)) {
                            LOG.info(String.format("WATCHER: Trying to open %s.", FULL));

                            for (int i = 0; i < 50 && !Files.isReadable(FULL); i++) {
                                LOG.info(String.format("Waiting for %s to be readable.", FULL));
                                this.wait(250);
                            }

                            if (Configurator.validateSavegame(FULL)) {
                                final Opener OPENER = new Opener(WINDOW, FULL, SortingMethod.ALPHA, WORRIER, null);
                                OPENER.execute();
                            } else {
                                LOG.info(String.format("WATCHER: Invalid file %s.", FULL));
                            }
                        }
                    }

                    if (!EVENTKEY.reset()) {
                        break;
                    }
                }
            } catch (InterruptedException | ClosedWatchServiceException ex) {
                LOG.info("WatcherService interrupted.");

            } catch (IOException ex) {
                final String MSG = String.format("Error.\n%s", ex.getMessage());
                JOptionPane.showMessageDialog(WINDOW, MSG, "Watch Error", JOptionPane.ERROR_MESSAGE);
                LOG.log(Level.SEVERE, "Watcher Error.", ex);

            } finally {
                return watchDirectories;
            }
        }
    }

    final private SaveWindow WINDOW;
    final private Worrier WORRIER;
    private WatchWorker worker;
    private List<Path> watchDirectories;
    static final private Logger LOG = Logger.getLogger(Opener.class.getCanonicalName());
    static final private PathMatcher MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{fos,ess}");

}
