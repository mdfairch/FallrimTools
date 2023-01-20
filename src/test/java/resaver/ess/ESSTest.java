/*
 * Copyright 2017 Mark.
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
package resaver.ess;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import resaver.Game;
import resaver.ProgressModel;
import resaver.ess.papyrus.Papyrus;
import static resaver.ess.SortingMethod.*;


/**
 * Tests the read and write methods of the <code>ESS</code> class.
 *
 * @author Mark Fairchild
 */
//@DisplayName("Brute force read/write tests for ESS.")
public class ESSTest {

    final static public Path WORK_DIR = Paths.get(System.getProperty("user.dir"));
    final static public Path TESTSAVES_DIR = WORK_DIR.resolve("src/test/resources/TestSaves");
    static final private Logger LOG = Logger.getLogger(ESSTest.class.getCanonicalName());
    
    public ESSTest() {
        // Set up logging stuff.
        LOG.getParent().getHandlers()[0].setFormatter(new java.util.logging.Formatter() {
            @Override
            public String format(LogRecord record) {
                final java.util.logging.Level LEVEL = record.getLevel();
                final String MSG = record.getMessage();
                final String SRC = record.getSourceClassName() + "." + record.getSourceMethodName();
                final String LOG = String.format("%s: %s: %s\n", SRC, LEVEL, MSG);
                return LOG;
            }
        });

        LOG.getParent().getHandlers()[0].setLevel(Level.INFO);
    }

    private static Stream<Path> pathProvider() {
        java.util.List<Path> paths;
        
        try {
            paths = Files.walk(TESTSAVES_DIR)
                    .filter(p -> Game.FILTER_ALL.accept(p.toFile()))
                    .filter(Files::isReadable)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            System.out.println(paths);
        } catch (IOException ex) {
            System.out.println("Error while reading test files.");
            System.err.println(ex.getMessage());
            paths = Collections.emptyList();
        }

        return paths.stream();
    }

    /**
     * Test of readESS and writeESS methods, of class ESS.
     *
     * @param path
     */
    @DisplayName("Parameterized Test")
    @ParameterizedTest(name = "{index} => filename={0}")
    @MethodSource("pathProvider")    
    void readAndWriteSample(Path path) {
        System.out.printf("Testing %s)\n", WORK_DIR.relativize(path));
        
        try {
            ModelBuilder MODEL_ORIGINAL = new ModelBuilder(new ProgressModel(1));
            final ESS.Result IN_RESULT = ESS.readESS(path, MODEL_ORIGINAL);
            final ESS ORIGINAL = IN_RESULT.ESS;

            if (ORIGINAL.isTruncated() || ORIGINAL.getPapyrus().getStringTable().hasSTB()) {                
                System.out.println("\tCorruption detected");
                return;
            }
            
            System.out.printf("\tRead complete (%s)\n", ORIGINAL.getHeader().getCompression());
            final String EXT = "." + ORIGINAL.getHeader().GAME.SAVE_EXT;

            final Path F2 = Files.createTempFile("ess_test", EXT);
            ESS.writeESS(ORIGINAL, F2, true);
            System.out.println("\tWrite complete");

            ModelBuilder MODEL_RESAVE = new ModelBuilder(new ProgressModel(1));
            final ESS.Result OUT_RESULT = ESS.readESS(F2, MODEL_RESAVE);
            final ESS REWRITE = OUT_RESULT.ESS;

            verifyIdentical(ORIGINAL, REWRITE);
            assertEquals(ORIGINAL.getDigest(), REWRITE.getDigest(), "Verify that digests match for " + path);
            System.out.println("\tMatch");

        } catch (RuntimeException | IOException | ElementException ex) {
            System.err.println("Problem with " + path.getFileName() + "\n" + ex.getMessage());
            ex.printStackTrace(System.err);
            fail(path.getFileName().toString());
        }
    }
    
    public void verifyIdentical(ESS ess1, ESS ess2) {
        assertEquals(ess1.getFormVersion(), ess2.getFormVersion(), "Form version mismatch");
        assertEquals(ess1.getVersionString(), ess2.getVersionString(), "Version string mismatch");
        assertEquals(ess1.getPluginInfo(), ess2.getPluginInfo(), "PluginInfo mismatch");
        assertEquals(ess1.getFLT(), ess2.getFLT(), "FileLocationTable mismatch");
        assertArrayEquals(ess1.getFormIDs(), ess2.getFormIDs(), "FormID table mismatch");
        assertArrayEquals(ess1.getVisitedWorldspaceArray(), ess2.getVisitedWorldspaceArray(), "VisitedWorldspace table mismatch");
        assertArrayEquals(ess1.getUnknown3(), ess2.getUnknown3(), "Unknown3 table mismatch");
        assertArrayEquals(ess1.getCosave(), ess2.getCosave(), "SKSE CoSave mismatch");
        
        assertEquals(ess1.calculateBodySize(), ess2.calculateBodySize(), "Calculated body size mismatch");
        assertEquals(ess1.calculateSize(), ess2.calculateSize(), "Calculated total size mismatch");

        Header.verifyIdentical(ess1.getHeader(), ess2.getHeader());

        final java.util.Iterator<ChangeForm> ITER1 = ess1.getChangeForms().iterator();
        final java.util.Iterator<ChangeForm> ITER2 = ess2.getChangeForms().iterator();

        while (ITER1.hasNext() && ITER2.hasNext()) {
            final ChangeForm CF1 = ITER1.next();
            final ChangeForm CF2 = ITER2.next();
            ChangeForm.verifyIdentical(CF1, CF2);
        }

        if (ITER1.hasNext() != ITER2.hasNext()) {
            throw new IllegalStateException("Missing changeforms.");
        }

        final Papyrus PAP1 = ess1.getPapyrus();
        final Papyrus PAP2 = ess2.getPapyrus();

        if (PAP1.getHeader() != PAP2.getHeader()) {
            throw new IllegalStateException(String.format("Papyrus header mismatch: %d vs %d.", PAP1.getHeader(), PAP2.getHeader()));
        } else if (!PAP1.getStringTable().containsAll(PAP2.getStringTable())) {
            throw new IllegalStateException("StringTable mismatch.");
        } else if (!PAP2.getStringTable().containsAll(PAP1.getStringTable())) {
            throw new IllegalStateException("StringTable mismatch.");
        }

        final ByteBuffer BUF1 = ByteBuffer.allocate(PAP1.calculateSize());
        final ByteBuffer BUF2 = ByteBuffer.allocate(PAP2.calculateSize());
        PAP1.write(BUF1);
        PAP2.write(BUF2);

        if (!Arrays.equals(BUF1.array(), BUF2.array())) {
            throw new IllegalStateException("Papyrus mismatch.");
        }
    }    
}
