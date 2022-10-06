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
package resaver;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import resaver.archive.ArchiveParser;
import resaver.esp.StringsFile;
import resaver.ess.PluginInfo;
import resaver.ess.Plugin;

/**
 * Describes a mod as a 4-tuple of a directory, a list of plugins, a list of
 * archives, and a list of script files.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class Mod implements java.io.Serializable {

    /**
     * Creates a new <code>Mod</code> from a <code>File</code> representing the
     * directory containing the mod's files. The directory will be scanned to
     * create lists of all the important files.
     *
     * @param game The game.
     * @param dir The directory containing the mod.
     * @return The <code>Mod</code>, or null if it couldn't be created.
     */
    static public Mod createMod(Game game, Path dir) {
        try {
            final Mod MOD = new Mod(game, dir);
            return MOD;
        } catch (FileNotFoundException ex) {
            LOG.warning(String.format("Couldn't read mod: %s\n%s", dir, ex.getMessage()));
            return null;
        } catch (IOException ex) {
            LOG.log(Level.WARNING, String.format("Couldn't read mod: %s", dir), ex);
            return null;
        }

    }

    /**
     * Creates a new <code>Mod</code> from a <code>File</code> representing the
     * directory containing the mod's files. The directory will be scanned to
     * create lists of all the important files.
     *
     * @param game The game.
     * @param dir The directory containing the mod.
     * @throws IllegalArgumentException Thrown if the directory doesn't exist,
     * isn't a directory, or isn't readable.
     * @throws IOException Thrown if there is a problem reading data.
     *
     */
    public Mod(Game game, Path dir) throws IOException {
        Objects.requireNonNull(game);
        Objects.requireNonNull(dir);
        this.DIRECTORY = dir;

        if (!Files.exists(dir)) {
            throw new FileNotFoundException("Directory doesn't exists: " + dir);
        }

        if (!Files.isDirectory(dir)) {
            throw new IOException("Directory isn't actual a directory: " + dir);
        }

        // Check if the parent directory is the game directory.
        final Path PARENT = dir.getParent();
        if (Files.list(PARENT).map(p -> p.getFileName()).anyMatch(GLOB_EXE::matches)) {
            this.MODNAME = PARENT.getFileName().toString();
        } else {
            this.MODNAME = dir.getFileName().toString();
        }

        this.SHORTNAME = (this.MODNAME.length() < 25 ? this.MODNAME : this.MODNAME.substring(0, 22) + "...");

        // Collect all files of relevance.
        this.SCRIPT_PATH = this.DIRECTORY.resolve(SCRIPTS_SUBDIR);
        this.STRING_PATH = this.DIRECTORY.resolve(STRINGS_SUBDIR);

        this.PLUGIN_FILES = Files.exists(this.DIRECTORY)
                ? Files.list(this.DIRECTORY).filter(GLOB_PLUGIN::matches).collect(Collectors.toList())
                : Collections.emptyList();

        this.ARCHIVE_FILES = Files.exists(this.DIRECTORY)
                ? Files.list(this.DIRECTORY).filter(GLOB_ARCHIVE::matches).collect(Collectors.toList())
                : Collections.emptyList();

        this.STRINGS_FILES = Files.exists(this.STRING_PATH)
                ? Files.walk(this.STRING_PATH).filter(GLOB_STRINGS::matches).collect(Collectors.toList())
                : Collections.emptyList();

        this.SCRIPT_FILES = Files.exists(this.SCRIPT_PATH)
                ? Files.walk(this.SCRIPT_PATH).filter(GLOB_SCRIPT::matches).collect(Collectors.toList())
                : Collections.emptyList();

        // Print out some status information.
        if (!Files.exists(this.DIRECTORY)) {
            LOG.warning(String.format("Mod \"%s\" doesn't exist.", this.MODNAME));

        } else {
            if (PLUGIN_FILES.isEmpty()) {
                LOG.fine(String.format("Mod \"%s\" contains no plugins.", this.MODNAME));
            } else {
                LOG.fine(String.format("Mod \"%s\" contains %d plugins.", this.MODNAME, PLUGIN_FILES.size()));
            }

            if (ARCHIVE_FILES.isEmpty()) {
                LOG.fine(String.format("Mod \"%s\" contains no archives.", this.MODNAME));
            } else {
                LOG.fine(String.format("Mod \"%s\" contains %d archives.", this.MODNAME, ARCHIVE_FILES.size()));
            }

            if (STRINGS_FILES.isEmpty()) {
                LOG.fine(String.format("Mod \"%s\" contains no loose localization files.", this.MODNAME));
            } else {
                LOG.fine(String.format("Mod \"%s\" contains %d loose localization files.", this.MODNAME, STRINGS_FILES.size()));
            }

            if (SCRIPT_FILES.isEmpty()) {
                LOG.fine(String.format("Mod \"%s\" contains no loose scripts.", this.MODNAME));
            } else {
                LOG.fine(String.format("Mod \"%s\" contains %d loose scripts.", this.MODNAME, SCRIPT_FILES.size()));
            }
        }
    }

    /**
     * An estimate of the total amount of data that would be scanned to read
     * this mod. The Math.sqrt operation is applied, to model the fact that
     * reading short files isn't much faster than reading long files.
     *
     * @return The size.
     */
    public double getSize() {
        if (null == this.size) {

            ToDoubleFunction<Path> toSize = f -> {
                try {
                    return Math.sqrt(Files.size(f));
                } catch (IOException ex) {
                    return 0;
                }
            };

            this.size = 0.0;
            this.size += this.SCRIPT_FILES.stream().mapToDouble(toSize).sum();
            this.size += this.STRINGS_FILES.stream().mapToDouble(toSize).sum();
            this.size += this.PLUGIN_FILES.stream().mapToDouble(toSize).sum();
            this.size += this.ARCHIVE_FILES.stream().mapToDouble(toSize).sum();
        }
        return this.size;
    }

    /**
     *
     * @return Returns true if the mod contains no plugins, archives, or loose
     * script files.
     */
    public boolean isEmpty() {
        return this.ARCHIVE_FILES.isEmpty() && this.PLUGIN_FILES.isEmpty() && this.SCRIPT_FILES.isEmpty();
    }

    /**
     * @return The directory storing the <code>Mod</code>.
     */
    public Path getDirectory() {
        return this.DIRECTORY;
    }

    /**
     * @return The number of archives.
     */
    public int getNumArchives() {
        return this.ARCHIVE_FILES.size();
    }

    /**
     * @return The number of loose script files.
     */
    public int getNumLooseScripts() {
        return this.SCRIPT_FILES.size();
    }

    /**
     * @return The number of loose script files.
     */
    public int getNumLooseStrings() {
        return this.STRINGS_FILES.size();
    }

    /**
     * @return The number of ESP/ESM files.
     */
    public int getNumESPs() {
        return this.PLUGIN_FILES.size();
    }

    /**
     * @return The name of the mod.
     */
    public String getName() {
        return this.MODNAME;
    }

    /**
     * @return The abbreviated name of the mod.
     */
    public String getShortName() {
        return this.SHORTNAME;
    }

    /**
     * @return A list of the names of the esp files in the mod.
     */
    public List<String> getESPNames() {
        final List<String> NAMES = new ArrayList<>(this.PLUGIN_FILES.size());
        this.PLUGIN_FILES.forEach(v -> NAMES.add(v.getFileName().toString()));
        return NAMES;
    }

    /**
     * Finds the <code>Plugin</code> corresponding to a
     * <code>StringsFile</code>.
     *
     * @param stringsFilePath
     * @param language
     * @param plugins
     * @return
     */
    private Plugin getStringsFilePlugin(Path stringsFilePath, String language, PluginInfo plugins) {
        final String SSREGEX = String.format("_%s\\.(il|dl)?strings", language);
        final String FILENAME = stringsFilePath.getFileName().toString();

        return Arrays.asList(
                Paths.get(FILENAME.replaceAll(SSREGEX, ".esm")),
                Paths.get(FILENAME.replaceAll(SSREGEX, ".esp")),
                Paths.get(FILENAME.replaceAll(SSREGEX, ".esl")))
                .stream()
                .filter(plugins.getPaths()::containsKey)
                .map(plugins.getPaths()::get)
                .findFirst().orElse(null);
    }

    /**
     * Reads the data for the <code>Mod</code>, consisting of
     * <code>StringsFile</code> objects and <code>PexFile</code> objects.
     *
     * @param language The language for the string tables.
     * @param plugins The <code>PluginInfo</code> from an <code>ESS</code>.
     * @return A <code>ModReadResults</code>.
     */
    public ModReadResults readData(PluginInfo plugins, String language) {
        Objects.requireNonNull(language);

        final String LANG = "_" + language.toLowerCase();
        final String GLOB = "glob:**" + LANG + ".*strings";
        final PathMatcher MATCHER = FS.getPathMatcher(GLOB);

        List<Path> ARCHIVE_ERRORS = new LinkedList<>();
        List<Path> STRINGSFILE_ERRORS = new LinkedList<>();

        // Read the archives.
        final List<StringsFile> STRINGSFILES = new LinkedList<>();
        final Map<Path, Path> SCRIPT_ORIGINS = new LinkedHashMap<>();

        this.ARCHIVE_FILES.forEach(archivePath -> {
            try (FileChannel channel = FileChannel.open(archivePath, StandardOpenOption.READ);
                    final ArchiveParser PARSER = ArchiveParser.createParser(archivePath, channel)) {

                final List<StringsFile> ARCHIVE_STRINGSFILES = new LinkedList<>();

                PARSER.getFiles(Paths.get("strings"), MATCHER).forEach((path, input) -> {
                    if (input.isPresent()) {
                        final Plugin PLUGIN = this.getStringsFilePlugin(path, language, plugins);
                        if (PLUGIN != null) {
                            try {
                                final StringsFile STRINGSFILE = StringsFile.readStringsFile(path, PLUGIN, input.get());
                                ARCHIVE_STRINGSFILES.add(STRINGSFILE);
                            } catch (java.nio.BufferUnderflowException ex) {
                                STRINGSFILE_ERRORS.add(archivePath.getFileName());
                            }
                        }
                    } else {
                        STRINGSFILE_ERRORS.add(archivePath.getFileName());
                    }
                });

                Map<Path, Path> ARCHIVE_SCRIPTS = PARSER.getFilenames(Paths.get("scripts"), GLOB_SCRIPT);

                SCRIPT_ORIGINS.putAll(ARCHIVE_SCRIPTS);
                STRINGSFILES.addAll(ARCHIVE_STRINGSFILES);

                int stringsCount = ARCHIVE_STRINGSFILES.stream().mapToInt(s -> s.TABLE.size()).sum();
                int scriptsCount = ARCHIVE_SCRIPTS.size();

                if (stringsCount > 0 || scriptsCount > 0) {
                    String fmt = "Read %5d scripts and %5d strings from %d stringsfiles in %s of \"%s\"";
                    String msg = String.format(fmt, scriptsCount, stringsCount, ARCHIVE_STRINGSFILES.size(), archivePath.getFileName(), this.SHORTNAME);
                    LOG.info(msg);
                }

            } catch (IOException ex) {
                ARCHIVE_ERRORS.add(archivePath.getFileName());
            }
        });

        // Read the loose stringtable files.
        final List<StringsFile> LOOSE_STRINGSFILES = this.STRINGS_FILES.stream()
                .filter(MATCHER::matches)
                .map(path -> {
                    try {
                        final Plugin PLUGIN = this.getStringsFilePlugin(path, language, plugins);
                        if (PLUGIN != null) {
                            final StringsFile STRINGSFILE = StringsFile.readStringsFile(path, PLUGIN);
                            return STRINGSFILE;
                        } else {
                            return null;
                        }
                    } catch (IOException ex) {
                        STRINGSFILE_ERRORS.add(path);
                        LOG.severe(String.format("Mod \"%s\": error while reading \"%s\".", this.SHORTNAME, path));
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());

        // Read the loose stringtable files.
        final Map<Path, Path> LOOSE_SCRIPTS = this.SCRIPT_FILES.stream()
                .filter(GLOB_SCRIPT::matches)
                .collect(Collectors.toMap(p -> p, p -> p.getFileName()));
        SCRIPT_ORIGINS.putAll(LOOSE_SCRIPTS);

        int stringsCount = LOOSE_STRINGSFILES.stream().mapToInt(s -> s.TABLE.size()).sum();
        int scriptsCount = LOOSE_SCRIPTS.size();

        if (stringsCount > 0 || scriptsCount > 0) {
            String fmt = "Read %5d scripts and %5d strings from %d stringsfiles in loose files of \"%s\"";
            String msg = String.format(fmt, scriptsCount, stringsCount, LOOSE_STRINGSFILES.size(), this.SHORTNAME);
            LOG.info(msg);
        }

        return new ModReadResults(SCRIPT_ORIGINS, STRINGSFILES, ARCHIVE_ERRORS, null, STRINGSFILE_ERRORS);
    }

    /**
     *
     * @return
     */
    public Analysis getAnalysis() {
        final Analysis ANALYSIS = new Analysis();
        ANALYSIS.MODS.add(this);
        ANALYSIS.ESPS.put(this.MODNAME, new TreeSet<>(this.getESPNames()));

        return ANALYSIS;
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        return this.MODNAME;
    }

    /**
     * @return A copy of the list of ESP files.
     */
    public List<Path> getESPFiles() {
        return new ArrayList<>(this.PLUGIN_FILES);
    }

    /**
     * @return A copy of the list of archive files.
     */
    public List<Path> getArchiveFiles() {
        return new ArrayList<>(this.ARCHIVE_FILES);
    }

    /**
     * @return A copy of the list of PEX files.
     */
    public List<Path> getPexFiles() {
        return new ArrayList<>(this.SCRIPT_FILES);
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        return this.DIRECTORY.hashCode();
    }

    /**
     * @see Object#equals(java.lang.Object)
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Mod other = (Mod) obj;
        return Objects.equals(this.DIRECTORY, other.DIRECTORY);
    }

    final private Path DIRECTORY;
    final private Path SCRIPT_PATH;
    final private Path STRING_PATH;
    final private String MODNAME;
    final private String SHORTNAME;

    final private List<Path> PLUGIN_FILES;
    final private List<Path> ARCHIVE_FILES;
    final private List<Path> SCRIPT_FILES;
    final private List<Path> STRINGS_FILES;
    transient Double size = null;

    static final private Path SCRIPTS_SUBDIR = Paths.get("scripts");
    static final private Path STRINGS_SUBDIR = Paths.get("strings");
    static final private Logger LOG = Logger.getLogger(Mod.class.getCanonicalName());

    static final private FileSystem FS = FileSystems.getDefault();
    static final public PathMatcher GLOB_CREATIONCLUB = FS.getPathMatcher("glob:**\\cc*.{esm,esp,esl,bsa,ba2}");
    static final public PathMatcher GLOB_INTEREST = FS.getPathMatcher("glob:**.{esm,esp,esl,bsa,ba2}");
    static final public PathMatcher GLOB_PLUGIN = FS.getPathMatcher("glob:**.{esm,esp,esl}");
    static final public PathMatcher GLOB_ARCHIVE = FS.getPathMatcher("glob:**.{bsa,ba2}");
    static final public PathMatcher GLOB_SCRIPT = FS.getPathMatcher("glob:**.pex");
    static final public PathMatcher GLOB_STRINGS = FS.getPathMatcher("glob:**.{strings,ilstrings,dlstrings}");
    static final public PathMatcher GLOB_ALL = FS.getPathMatcher("glob:**.{esm,esp,esl,bsa,ba2,pex,strings,ilstrings,dlstrings}");
    static final public PathMatcher GLOB_EXE = FS.getPathMatcher("glob:{skyrim.exe,skyrimse.exe,fallout4.exe}");

    /**
     * The status of an individual mod within a profile.
     */
    static public enum Status {
        CHECKED,
        UNCHECKED,
        DISABLED,
    }

    /**
     * Stores data relating strings, scripts, and functions to their
     *
     * @author Mark Fairchild
     */
    static public class Analysis implements java.io.Serializable {

        /**
         * List: (Mod name)
         */
        final public Set<Mod> MODS = new LinkedHashSet<>();

        /**
         * Map: (IString) -> File
         */
        //final public Map<IString, Path> SCRIPTS = new LinkedHashMap<>();
        final public Map<IString, File> SCRIPTS = new LinkedHashMap<>();

        /**
         * Map: (Mod name) -> (Lisp[ESP name])
         */
        final public Map<String, SortedSet<String>> ESPS = new LinkedHashMap<>();

        /**
         * Map: (IString) -> (List: (Mod name))
         */
        final public Map<IString, SortedSet<String>> SCRIPT_ORIGINS = new LinkedHashMap<>();

        /**
         * Map: (IString) -> (List: (Mod name))
         */
        final public Map<IString, SortedSet<String>> STRUCT_ORIGINS = new LinkedHashMap<>();

        /**
         * Merges analyses.
         *
         * @param sub
         * @return
         */
        public Analysis merge(Analysis sub) {
            this.MODS.addAll(sub.MODS);
            this.SCRIPTS.putAll(sub.SCRIPTS);

            sub.ESPS.forEach((name, list) -> {
                this.ESPS.merge(name, list, (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
            });

            sub.SCRIPT_ORIGINS.forEach((name, list) -> {
                this.SCRIPT_ORIGINS.merge(name, list, (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
            });

            sub.STRUCT_ORIGINS.forEach((name, list) -> {
                this.STRUCT_ORIGINS.merge(name, list, (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
            });

            return this;
        }
    }

    /**
     * An exception that indicates a stringtable file couldn't be read.
     */
    public class ModReadResults {

        /**
         * Creates a new <code>StringsReadError</code> with a list of
         * stringtable files and archive files that were corrupt.
         *
         * @param scripts
         * @param strings
         * @param archives The list of names of the archives that were
         * unreadable.
         * @param scriptNames The list of names of the script files that were
         * unreadable.
         * @param stringsNames The list of names of the stringtables that were
         * unreadable.
         * @see Exception#Exception()
         */
        private ModReadResults(Map<Path, Path> scriptOrigins, List<StringsFile> strings, List<Path> archiveErrors, List<Path> scriptErrors, List<Path> stringsErrors) {
            //super(String.format("Some data could not be read: %d archives, %d scripts, %d stringtables", archives.size(), scripts.size(), strings.size()));
            this.MOD = Mod.this;
            this.SCRIPT_ORIGINS = Collections.unmodifiableMap(scriptOrigins == null ? Collections.emptyMap() : scriptOrigins);
            this.STRINGSFILES = Collections.unmodifiableList(strings == null ? Collections.emptyList() : strings);
            this.ARCHIVE_ERRORS = Collections.unmodifiableList(archiveErrors == null ? Collections.emptyList() : archiveErrors);
            this.SCRIPT_ERRORS = Collections.unmodifiableList(scriptErrors == null ? Collections.emptyList() : scriptErrors);
            this.STRINGS_ERRORS = Collections.unmodifiableList(stringsErrors == null ? Collections.emptyList() : stringsErrors);
        }

        public Stream<Path> getErrorFiles() {
            return Arrays.asList(ARCHIVE_ERRORS, SCRIPT_ERRORS, STRINGS_ERRORS)
                    .stream()
                    .flatMap(v -> v.stream());
        }

        final public Mod MOD;
        final public Map<Path, Path> SCRIPT_ORIGINS;
        final public List<StringsFile> STRINGSFILES;
        final public List<Path> ARCHIVE_ERRORS;
        final public List<Path> SCRIPT_ERRORS;
        final public List<Path> STRINGS_ERRORS;
    }

}
