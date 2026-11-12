/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.itemimport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
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
        createZip(zipPath, Map.of("link/../created/file.txt", "file contents"));

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
        String sourceDir = service.unzip(zipPath.toFile(), destination.toString());

        // Verify that "file.txt" in the Zip file was extracted to
        // <tmp>/destination/archive.zip/created/file.txt,
        // *not* <tmp>/outside/created/file.txt
        // This is because <tmp>/destination/archive.zip/link/../created/file.txt
        // is normalized to <tmp>/destination/archive.zip/created before
        // resolving the symbolic link
        Path extractedFile =
            extractionRoot.resolve("created").resolve("file.txt");
        assertTrue(Files.isRegularFile(extractedFile));
        assertEquals("file contents", Files.readString(extractedFile));
        // Verify that "unzip" returns the validated path to the extraction root
        assertEquals(extractionRoot.toRealPath(), Path.of(sourceDir));
        assertFalse(
            Files.exists(
                outside.resolve("created"),
                LinkOption.NOFOLLOW_LINKS
            )
        );
    }

    @Test
    public void unzipReturnsImportRoot() throws Exception {
        // Imported Zip files may contain an optional outer "wrapper" directory,
        // such as "SimpleArchiveFormat", that the directories of the items
        // being imported. The ItemImportServiceImpl.unzip method handles
        // this optional directory internally, and returns an "import root"
        // that enables callers to assume that the directory contains the
        // items to be imported as immediate child subdirectories.
        //
        // The "assertImportRoot" method verifies that the expected relative
        // directory is returned as part of the "import root" determined by
        // the ItemImportServiceImpl.unzip method

        // import root contains "SimpleArchiveFormat" when item directories are
        // contained within it.
        assertImportRoot("SimpleArchiveFormat",
            "SimpleArchiveFormat/item1/contents", "SimpleArchiveFormat/item2/contents"
        );

        // import root does not contain wrapper when no wrapper is present
        assertImportRoot("", "item1/contents", "item2/contents");

        // import root contains wrapper, regardless of files in root of Zip file
        assertImportRoot("SimpleArchiveFormat", "README", "SimpleArchiveFormat/item1/contents");
        // .. and order of entries doesn't matter
        assertImportRoot("SimpleArchiveFormat", "SimpleArchiveFormat/item1/contents", "README");

        // Properly handles an empty archive
        assertImportRoot("");

        // No wrapper when Zip file contains file, but no directories
        assertImportRoot("", "contents");


        // import root is normalized, prior to wrapper detection
        assertImportRoot("", "top/../created/file.txt");
        assertImportRoot("SimpleArchiveFormat", "discard/../SimpleArchiveFormat/item1/contents");
    }

    /**
     * Constructs a Zip file containing file entries with the given entryNames,
     * and asserts that the import root returned by the
     * ItemImportServiceImpl.unzip method includes the expected "wrapper"
     * directory
     *
     * @param expectedWrapper the directory name of the "wrapper" directory in
     * the Zip file, or an empty string if no wrapper is expected.
     * @param entryNames the relative file paths of Zip entries in to create
     * in the Zip file.
     */
    private void assertImportRoot(String expectedWrapper, String... entryNames) throws Exception {
        // In a temporary directory, creates an "archive.zip" Zip file with files corresponding to the entryNames
        Path root = temporaryFolder.newFolder().toPath().toRealPath();
        Path destination = Files.createDirectory(root.resolve("destination"));
        Path zipPath = root.resolve("archive.zip");
        Map<String, String> entries = new LinkedHashMap<>();
        for (String name : entryNames) {
            entries.put(name, "contents: " + name);
        }
        createZip(zipPath, entries);

        // Unzip the archive.zip file, and get the validated location of the unzip,
        String unzipLocation = new ItemImportServiceImpl().unzip(zipPath.toFile(), destination.toString());

        // Verify that the unzipLocation matches the expected location with the expected wrapper
        Path extractionRoot = destination.resolve("archive.zip").toRealPath();
        assertEquals(extractionRoot.resolve(expectedWrapper).toRealPath(), Path.of(unzipLocation));
        assertTrue(Path.of(unzipLocation).startsWith(extractionRoot));
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            Path expectedFile = extractionRoot.resolve(entry.getKey()).normalize();
            assertEquals(entry.getValue(), Files.readString(expectedFile));
        }
    }

    /**
     * Creates a Zip file at the given zipPath, containing entries from the
     * given Map.
     * @param zipPath the Path of the Zip file to create
     * @param entries a Map of ZipEntry paths and content to include in the
     * Zip file.
     */
    private void createZip(Path zipPath, Map<String, String> entries) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(UTF_8));
                output.closeEntry();
            }
        }
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
