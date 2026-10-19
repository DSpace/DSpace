/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.itemimport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link ItemImportServiceImpl}
 */
public class ItemImportServiceImplTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Verifies that the parent directories utilize the returned Path from
     * SecureFileAccess.validatePathForWrite for output, rather than the
     * path from the original Zip entry name
     */
    @Test
    public void unzipUsesValidatedParentForEntryContainingSymlinkAndParentReference()
            throws Exception {
        // Setup two directories:
        //   - <tmp>/destination - the "allowed" base directory for files
        //   - <tmp>/outside - directory that should not be accessible
        Path root = temporaryFolder.getRoot().toPath().toRealPath();
        Path destination = Files.createDirectory(root.resolve("destination"));
        Path outside = Files.createDirectory(root.resolve("outside"));

        // "archive.zip" file contains a "link/../created/file.txt" entry
        Path zipPath = root.resolve("archive.zip");
        createZip(zipPath, "link/../created/file.txt", "file contents");

        // Create a <tmp>/destination/archive.zip/ directory where files are
        // expected to be extracted to.
        Path extractionRoot = Files.createDirectories(
            destination.resolve(zipPath.getFileName())
        );

        // Create a symlink (<tmp>/destination/archive.zip/link) that links to
        // "outside", i.e.: <tmp>/destination/archive.zip/link -> <tmp>/outside
        Path symlinkTarget = Files.createDirectory(outside.resolve("target"));
        createSymbolicLinkOrSkip(
            extractionRoot.resolve("link"),
            symlinkTarget
        );

        ItemImportServiceImpl service = new ItemImportServiceImpl();

        // Extract the file
        service.unzip(zipPath.toFile(), destination.toString());

        // Verify that "file.txt" in the Zip file was extracted to
        // <tmp>/destination/archive.zip/created/file.txt,
        // *not* <tmp>/outside/created/file.txt
        // This is because <tmp>/destination/archive.zip/link/../created/file.txt
        // is normalized to <tmp>/destination/archive.zip/created before
        // resolving the symbolic link
        Path expectedFile =
            extractionRoot.resolve("created").resolve("file.txt");
        assertTrue(Files.isRegularFile(expectedFile));
        assertFalse(
            Files.exists(
                outside.resolve("created"),
                LinkOption.NOFOLLOW_LINKS
            )
        );
    }

    private void createZip(Path zipPath, String entryName, String contents)
        throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            output.putNextEntry(new ZipEntry(entryName));
            output.write(contents.getBytes(UTF_8));
            output.closeEntry();
        }
    }

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
