/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.file.PathUtils;
import org.dspace.AbstractDSpaceTest;
import org.dspace.app.itemexport.factory.ItemExportServiceFactory;
import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.secure.SecureFileAccess;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ItemExportServiceImpl}
 */
public class ItemExportServiceImplTest extends AbstractDSpaceTest {
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private Path tempDir;
    private Path workDir;

    @Before
    public void setUp() throws Exception {
        // Temporary directory to hold item export work and export directories
        tempDir = Files.createTempDirectory("tempItemExportServiceTest");

        // Create temporary workDir and set as the item export work directory
        workDir = Files.createDirectory(tempDir.resolve("workItemExportServiceTest"));
        configurationService.setProperty("org.dspace.app.itemexport.work.dir",
                new String[] { workDir.toString() });

        // Set tempDir/base as the only allowed item export directory
        Path baseDir = tempDir.resolve("base");
        configurationService.setProperty("org.dspace.app.itemexport.allowed.dir",
                new String[] { baseDir.toString() });
    }

    @After
    public void destroy() throws Exception {
        PathUtils.delete(tempDir);
    }

    /**
     * Verifies that "zipFiles" method excludes the "strTarget" path
     * from the Zip file based on file identity, instead of string comparison
     * of path names.
     *
     * This is necessary in the unusual case, where the "strTarget" is placed
     * in the directory, denoted by "strSource", that is being archived.
     * The "strTarget" parameter has typically been validated by the
     * "SecureFileAccess.validatePathForWrite" method, which may result in it
     * being lexically different from the "strSource" name, making string
     * comparison unreliable.
     */
    @Test
    public void zipFilesExcludesTargetBasedOnFileIdentity()
        throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source")).toRealPath();

        // Write a "contents.txt" file into "sourceDir"
        Files.writeString(sourceDir.resolve("contents.txt"), "test data");

        // Generate a "source/../source" path to "sourceDir". When normalized,
        // this is equivalent to the "real path" contained in "sourceDir"
        Path nonCanonicalSourceDir = sourceDir.resolve("..").resolve(sourceDir.getFileName());

        // Create the target file, "source/../source/out.zip_tmp" from the
        // non-canonical source directory
        Path nonCanonicalTarget = nonCanonicalSourceDir.resolve("out.zip_tmp");

        // Create the target file. Even though the target file will typically
        // be a Zip file, using a text file here, so that failing to exclude the
        // file will not lead to recursive growth of the file.
        Files.writeString(nonCanonicalTarget, "test target");

        Path validatedTarget = SecureFileAccess.validatePathForWrite(
            nonCanonicalTarget.toString(), List.of(sourceDir.toString()),
            "itemExportTest"
        );

        // Verify that "nonCanonicalTarget" and "validatedTarget" are lexically
        // distinct, but refer to the same file.
        assertNotEquals(nonCanonicalTarget.toString(), validatedTarget.toString());
        assertEquals(nonCanonicalTarget.toRealPath(), validatedTarget);

        // Create the zip file
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bos)) {
            new ItemExportServiceImpl().zipFiles(
                nonCanonicalSourceDir.toFile(),
                nonCanonicalSourceDir.toString(),
                validatedTarget.toString(),
                output
            );
        }

        // Extract contents of the Zip file
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(input.readAllBytes(), UTF_8));
            }
        }

        // Verify that the Zip file only contained "contents.txt".
        assertEquals(Map.of("contents.txt", "test data"), entries);
    }

    /**
     * A "happy path" test that verifies that a Zip archive file is created
     * with the expected contents and that the source directory is deleted.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void zipPublishesArchiveAndRemovesSource() throws Exception {
        Path base = Files.createDirectory(tempDir.resolve("base")).toRealPath();
        Path source = createSource(base);
        Path target = base.resolve("result.zip");

        ItemExportService itemExportService = ItemExportServiceFactory.getInstance()
            .getItemExportService();
        itemExportService.zip(source.toString(), target.toString());

        assertPublishedArchive(source, target);
    }

    /**
     * Verifies that zip method accepts a path with a symbolic link into the
     * allowed directories. Setup is:
     *
     * <pre>
     * tempDir/
     *   |- base/
     *   |   |- source/
     *   |        |- contents.txt
     *   |- alias -> base
     * </pre>
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void zipAcceptsSymlinkedDestinationToAllowedBase()
        throws Exception {
        Path root = tempDir.toRealPath();
        Path base = Files.createDirectory(root.resolve("base"));
        Path source = createSource(base);
        Path alias = createSymbolicLinkOrSkip(root.resolve("alias"), base);


        ItemExportService itemExportService = ItemExportServiceFactory.getInstance()
            .getItemExportService();
        itemExportService.zip(source.toString(), alias.resolve("result.zip").toString());

        assertPublishedArchive(source, base.resolve("result.zip"));
    }

    /**
     * Verifies that zip method accepts a path with a symbolic link into the
     * allowed directories (after validation, including resolving parent
     * references).
     *
     * Setup is:
     *
     * <pre>
     * tempDir/
     *   |- base/
     *   |   |- source/
     *   |   |    |- contents.txt
     *   |   |- link -> ../outside/target
     *   |- outside/
     *       |- target/
     * </pre>
     *
     * with the target being "base/link/../result.zip". After resolving
     * the parent references, the actual target directory after validation is
     * "tempDir/base/result.zip"
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void zipUsesValidatedTargetForFinalRename()
        throws Exception {
        Path root = tempDir.toRealPath();
        Path base = Files.createDirectory(root.resolve("base"));
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path outsideTarget = Files.createDirectory(outside.resolve("target"));
        Path source = createSource(base);
        Path link = createSymbolicLinkOrSkip(base.resolve("link"), outsideTarget);
        Path requestedTarget = link.resolve("..").resolve("result.zip");


        ItemExportService itemExportService = ItemExportServiceFactory.getInstance()
            .getItemExportService();
        itemExportService.zip(source.toString(), requestedTarget.toString());

        assertPublishedArchive(source, base.resolve("result.zip"));
        assertFalse(Files.exists(outside.resolve("result.zip"), LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(outside.resolve("result.zip_tmp"), LinkOption.NOFOLLOW_LINKS));
    }

    /**
     * Verifies that zip method rejects a path where an existing symbolic link
     * that points outside the allowed "base" export directory matches the
     * the temporary processing directory generated by the method.
     *
     * Setup is:
     *
     * <pre>
     * tempDir/
     *   |- base/
     *   |   |- source/
     *   |   |    |- contents.txt
     *   |   |- result.zip_tmp -> ../outside.txt
     *   |- outside.txt
     * </pre>
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void zipRejectsExistingTemporarySymlinkOutsideBaseBeforeMutation()
        throws Exception {
        Path root = tempDir.toRealPath();
        Path base = Files.createDirectory(root.resolve("base"));
        Path source = createSource(base);
        Path outsideFile = Files.writeString(root.resolve("outside.txt"), "do not change");
        Path target = base.resolve("result.zip");
        Path temporary = createSymbolicLinkOrSkip(base.resolve("result.zip_tmp"), outsideFile);

        ItemExportService itemExportService = ItemExportServiceFactory.getInstance()
            .getItemExportService();
        assertThrows(IOException.class,
            () -> itemExportService.zip(source.toString(), target.toString()));

        assertEquals("test data", Files.readString(source.resolve("contents.txt")));
        assertEquals("do not change", Files.readString(outsideFile));
        assertFalse(Files.exists(target, LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isSymbolicLink(temporary));
    }

    /**
     * Verifies that zip method rejects a path where an existing symbolic link
     * that points outside to a non-existing file outside the allowed "base"
     * export directory matches the the temporary processing directory generated
     * by the method.
     *
     * Setup is:
     *
     * <pre>
     * tempDir/
     *   |- base/
     *   |   |- source/
     *   |   |    |- contents.txt
     *   |   |- result.zip_tmp -> ../outside/missing.txt
     *   |- outside/
     *       |- <missing.txt does not exist>
     * </pre>
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void zipRejectsDanglingTemporarySymlinkBeforeMutation()
        throws Exception {
        Path root = tempDir.toRealPath();
        Path base = Files.createDirectory(root.resolve("base"));
        Path source = createSource(base);
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path outsideFile = outside.resolve("missing.txt"); // Path to non-existent file
        Path target = base.resolve("result.zip");
        Path temporary = createSymbolicLinkOrSkip(base.resolve("result.zip_tmp"), outsideFile);

        ItemExportService itemExportService = ItemExportServiceFactory.getInstance()
            .getItemExportService();
        assertThrows(NoSuchFileException.class,
            () -> itemExportService.zip(source.toString(), target.toString()));

        assertEquals("test data", Files.readString(source.resolve("contents.txt")));
        assertFalse(Files.exists(outsideFile, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(target, LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isSymbolicLink(temporary));
    }

    /**
     * Verifies that the target is a Zip file and contains the contents
     * created by the "createSource" method, and that the temporary archive that
     * was created during processing was deleted. Also verifies that the source
     * is deleted.
     *
     * @param source the Path of the source directory to export
     * @param target the Path to export the item to
     * @throws IOException if IO error
     */
    private void assertPublishedArchive(Path source, Path target)
        throws IOException {
        // Verify that the target file exists
        assertTrue(Files.isRegularFile(target));
        // Verify that source has been deleted
        assertFalse(Files.exists(source, LinkOption.NOFOLLOW_LINKS));
        // Verify that the temporary directory created during processing has
        // been deleted
        Path temporary = target.resolveSibling(target.getFileName() + "_tmp");
        assertFalse(Files.exists(temporary, LinkOption.NOFOLLOW_LINKS));

        // Verify that the contents of the Zip file match that created by the
        // "createSource" method
        try (ZipFile archive = new ZipFile(target.toFile())) {
            assertEquals(1, archive.size());
            ZipEntry entry = archive.getEntry("contents.txt");
            assertNotNull("Missing contents.txt in " + target, entry);
            try (InputStream input = archive.getInputStream(entry)) {
                assertEquals("test data", new String(input.readAllBytes(), UTF_8));
            }
        }
    }

    /**
     * Helper method that creates in the given base directory,
     * a "source" directory containing a "contents.txt" file with the text
     * "test data", to use in tests.
     *
     * @param base the Path to a directory to hold the "source" directory for
     * testing
     * @return the Path to the "source" directory.
     * @throws IOException if IO error
     */
    private Path createSource(Path base)
        throws IOException {
        Path source = Files.createDirectory(base.resolve("source"));
        Files.writeString(source.resolve("contents.txt"), "test data");
        return source;
    }

    /**
     * Creates a symbolic link, if supported by the operating system, otherwise
     * skips the calling test
     *
     * @param link the Path of the symbolic link to create
     * @param target the target of the symbolic link
     * @return the Path to the symbolic link
     */
    private Path createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException |
                 SecurityException |
                 IOException exception) {
            Assume.assumeNoException(
                "Symbolic links are unavailable on this filesystem",
                exception
            );

            throw new AssertionError(
                "Execution continued after a failed JUnit assumption",
                exception
            );
        }
    }
}
