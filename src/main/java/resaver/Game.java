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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Mark Fairchild
 */
public enum Game {

    SKYRIM_LE("Skyrim Legendary Edition",
            "Skyrim Savefile",
            "ess",
            "skse",
            Paths.get("skyrim"),
            Paths.get("Skyrim/Saves"),
            Paths.get("tesv.exe"),
            "Unofficial Skyrim Legendary Edition Patch.esp"),
    SKYRIM_SE("Skyrim Special Edition",
            "Skyrim SE Savefile",
            "ess",
            "skse",
            Paths.get("skyrim special edition"),
            Paths.get("Skyrim Special Edition/Saves"),
            Paths.get("skyrimse.exe"),
            "Unofficial Skyrim Special Edition Patch.esp"),
    SKYRIM_SW("Skyrim Switch Edition",
            "Skyrim Switch Savefile",
            "sav0",
            "skse",
            Paths.get("skyrim switch edition"),
            Paths.get("Skyrim SW/Saves"),
            Paths.get("SkyrimSE.exe")),
    SKYRIM_VR("Skyrim VR Edition",
            "Skyrim VR Savefile",
            "ess",
            "skse",
            Paths.get("Elderscroll SkyrimVR"),
            Paths.get("Skyrim VR/Saves"),
            Paths.get("SkyrimVR.exe")),
    FALLOUT4("Fallout 4",
            "Fallout 4 Savefile",
            "fos",
            "f4se",
            Paths.get("fallout 4"),
            Paths.get("fallout4/Saves"),
            Paths.get("fallout4.exe"),
            "Unofficial Fallout 4 Patch.esp"),
    FALLOUT_VR("Fallout 4 VR",
            "Fallout 4 VR Savefile",
            "ess",
            "skse",
            Paths.get("Fallout 4 VR"),
            Paths.get("Fallout4VR/Saves"),
            Paths.get("fallout4vr.exe"));

    /**
     * A filename filter for all of the games.
     */
    static final public FileNameExtensionFilter FILTER_ALL = new FileNameExtensionFilter("Bethesda Savefiles", "ess", "fos", "sav0");

    /**
     * Cached list version of the values.
     */
    static public List<Game> VALUES = java.util.Collections.unmodifiableList(java.util.Arrays.asList(Game.values()));

    /**
     * The name of the supported game.
     */
    final public String NAME;

    /**
     * The file extension for the game's savefiles.
     */
    final public String SAVE_EXT;

    /**
     * The file extension for the game's cosaves.
     */
    final public String COSAVE_EXT;

    /**
     * The name of the game directory.
     */
    final public Path GAME_DIRECTORY;

    /**
     * A <code>Path</code> to the default location for savefiles.
     */
    final public Path SAVE_DIRECTORY;

    /**
     * The name of the game executable.
     */
    final public Path EXECUTABLE;

    /**
     * An FX_FILTER, for dialog boxes that choose a savefile.
     */
    final public FileNameExtensionFilter FILTER;

    /**
     * Names of unofficial patches.
     */
    final public List<String> PATCH_NAMES;

    /**
     * A <code>PathMatcher</code> that matches savefile names.
     */
    final private PathMatcher SAVE_MATCHER;

    /**
     * A glob for the game's savefiles.
     */
    final public String SAVE_GLOB;

    /**
     * A glob for the game's cosaves.
     */
    final public String COSAVE_GLOB;

    /**
     * Test if a savefile matches.
     *
     * @param path
     * @return
     */
    public boolean testFilename(Path path) {
        return this.SAVE_MATCHER.matches(path.getFileName());
    }

    /**
     * @return Flag indicating whether the game has a 64bit IDs.
     */
    public boolean isID64() {
        return !this.isSLE();
    }

    /**
     * @return Flag indicating whether the game is Fallout 4.
     */
    public boolean isFO4() {
        switch (this) {
            case FALLOUT4:
            case FALLOUT_VR:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return Flag indicating whether the game is an edition of Skyrim.
     *
     */
    public boolean isSkyrim() {
        switch (this) {
            case SKYRIM_LE:
            case SKYRIM_SW:
            case SKYRIM_SE:
            case SKYRIM_VR:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return Flag indicating whether the game is Skyrim Legendary Edition.
     */
    public boolean isSLE() {
        return this == SKYRIM_LE;
    }

    /**
     * Creates a new <code>Game</code> for the specified extension.
     *
     * @param gameName The game's name.
     * @param saveName The name for savefiles.
     * @param saveExt The file extension for savefiles.
     * @param cosaveExt The file extension for co-saves.
     * @param gameDir The default name of the game's directory.
     * @param exe The filename of the game's executable.
     */
    private Game(String gameName, String saveName, String saveExt, String cosaveExt, Path gameDir, Path saveDir, Path exe, String... patchNames) {
        this.NAME = gameName;
        this.GAME_DIRECTORY = gameDir;
        this.SAVE_EXT = saveExt;
        this.COSAVE_EXT = cosaveExt;
        this.SAVE_DIRECTORY = saveDir;

        this.EXECUTABLE = exe;
        this.FILTER = new FileNameExtensionFilter(saveName, saveExt);
        this.PATCH_NAMES = Collections.unmodifiableList(Arrays.asList(patchNames));
        this.SAVE_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*." + this.SAVE_EXT);
        this.SAVE_GLOB = "**." + SAVE_EXT;
        this.COSAVE_GLOB = "**." + COSAVE_EXT;
    }

};
