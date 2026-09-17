/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.secure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SecureFileAccessTest {

    private static final String PURPOSE = "test";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * An existing regular file directly beneath an allowed real directory
     * should be accepted and returned using its real path.
     */
    @Test
    public void validatePathForWriteAcceptsExistingFileUnderRealBase()
            throws Exception {
        Path basePath = createDirectory("base");
        Path filePath = Files.createFile(basePath.resolve("existing.txt"));

        Path result = SecureFileAccess.validatePathForWrite(
            filePath.toString(),
            List.of(basePath.toString()),
            PURPOSE
        );

        assertEquals(filePath.toRealPath(), result);
    }

    /**
     * A new file directly beneath an allowed real directory should be
     * accepted even though the file does not yet exist.
     */
    @Test
    public void validatePathForWriteAcceptsNonexistentFileUnderRealBase()
            throws Exception {
        Path basePath = createDirectory("base");
        Path filePath = basePath.resolve("new-file.txt");

        assertFalse(Files.exists(filePath));

        Path result = SecureFileAccess.validatePathForWrite(
            filePath.toString(),
            List.of(basePath.toString()),
            PURPOSE
        );

        assertEquals(basePath.resolve("new-file.txt"), result);
        assertFalse(Files.exists(filePath));
    }

    /**
     * A write target may contain multiple path elements that do not exist
     * yet. Validation must not require any of those elements to exist.
     */
    @Test
    public void validatePathForWriteAcceptsMultipleNonexistentPathElements()
            throws Exception {
        Path basePath = createDirectory("base");
        Path filePath = basePath.resolve("one")
                                .resolve("two")
                                .resolve("new-file.txt");

        assertFalse(Files.exists(filePath));

        Path result = SecureFileAccess.validatePathForWrite(
            filePath.toString(),
            List.of(basePath.toString()),
            PURPOSE
        );

        assertEquals(
            basePath.resolve("one/two/new-file.txt"),
            result
        );
        assertFalse(Files.exists(filePath));
    }

    /**
     * An existing file accessed through a symbolic-link alias of the allowed
     * base should be accepted.
     *
     * This represents paths such as:
     *
     * /var/folders/...              requested/configured spelling
     * /private/var/folders/...      real file location (on macOS, for example)
     */
    @Test
    public void validatePathForWriteAcceptsExistingFileThroughSymlinkedBase()
            throws Exception {
        Path rootPath = temporaryRoot();
        Path realBasePath = createDirectory("real-base");
        Path aliasPath = createSymbolicLinkOrSkip(
            rootPath.resolve("base-alias"),
            realBasePath
        );
        Path filePath = Files.createFile(
            realBasePath.resolve("existing.txt")
        );
        Path requestedPath = aliasPath.resolve("existing.txt");

        Path result = SecureFileAccess.validatePathForWrite(
            requestedPath.toString(),
            List.of(aliasPath.toString()),
            PURPOSE
        );

        assertEquals(filePath.toRealPath(), result);
    }

    /**
     * A nonexistent file accessed through a symbolic-link alias of the
     * allowed base should be accepted.
     *
     * This is the principal regression test for the macOS /var versus
     * /private/var problem.
     */
    @Test
    public void validatePathForWriteAcceptsNonexistentFileThroughSymlinkedBase()
            throws Exception {
        Path rootPath = temporaryRoot();
        Path realBasePath = createDirectory("real-base");
        Path aliasPath = createSymbolicLinkOrSkip(
            rootPath.resolve("base-alias"),
            realBasePath
        );
        Path requestedPath = aliasPath.resolve("new-file.txt");

        assertFalse(Files.exists(requestedPath));

        Path result = SecureFileAccess.validatePathForWrite(
            requestedPath.toString(),
            List.of(aliasPath.toString()),
            PURPOSE
        );

        assertEquals(
            realBasePath.resolve("new-file.txt"),
            result
        );
        assertFalse(Files.exists(requestedPath));
    }

    /**
     * An existing intermediate symbolic link must not permit a write outside
     * the allowed base.
     */
    @Test
    public void validatePathForWriteRejectsExistingFileThroughEscapingSymlink()
            throws Exception {
        Path basePath = createDirectory("base");
        Path outsidePath = createDirectory("outside");
        Path escapePath = createSymbolicLinkOrSkip(
            basePath.resolve("escape"),
            outsidePath
        );
        Path outsideFile = Files.createFile(
            outsidePath.resolve("existing.txt")
        );
        Path requestedPath = escapePath.resolve("existing.txt");

        assertEquals(outsideFile.toRealPath(), requestedPath.toRealPath());

        assertIllegalPath(requestedPath, List.of(basePath.toString()));
    }

    /**
     * An existing intermediate symbolic link must not permit a new file to
     * be written outside the allowed base.
     *
     * This is especially important because the final file cannot itself be
     * resolved with toRealPath().
     */
    @Test
    public void validatePathForWriteRejectsNonexistentFileThroughEscapingSymlink()
            throws Exception {
        Path basePath = createDirectory("base");
        Path outsidePath = createDirectory("outside");
        Path escapePath = createSymbolicLinkOrSkip(
            basePath.resolve("escape"),
            outsidePath
        );
        Path requestedPath = escapePath.resolve("new-file.txt");

        assertFalse(Files.exists(requestedPath));

        assertIllegalPath(requestedPath, List.of(basePath.toString()));
    }

    /**
     * A dangling symbolic link in the existing portion of a write path must
     * cause validation to fail.
     */
    @Test
    public void validatePathForWriteRejectsNonexistentFileThroughDanglingSymlink()
            throws Exception {
        Path rootPath = temporaryRoot();
        Path basePath = createDirectory("base");

        // Deliberately do not create this target.
        Path missingTarget = rootPath.resolve("missing-target");
        assertFalse(Files.exists(
            missingTarget,
            LinkOption.NOFOLLOW_LINKS
        ));

        // Create a "dangling link" to the missing target
        Path danglingLink = createSymbolicLinkOrSkip(
            basePath.resolve("dangling-link"),
            missingTarget
        );

        // Verify that the link entry exists and is actually a symbolic link,
        // even though following it would lead to a nonexistent target.
        BasicFileAttributes attributes = Files.readAttributes(
            danglingLink,
            BasicFileAttributes.class,
            LinkOption.NOFOLLOW_LINKS
        );
        assertTrue(attributes.isSymbolicLink());

        Path requestedPath = danglingLink.resolve("new-file.txt");

        // Path should be rejected (throwing an IOException) because the
        // non-existent target is outside of the base path
        assertThrows(
            IOException.class,
            () -> SecureFileAccess.validatePathForWrite(
                requestedPath.toString(),
                List.of(basePath.toString()),
                PURPOSE
            )
        );
    }

    /**
     * Lexical traversal using ".." must not permit a target outside the
     * allowed base.
     */
    @Test
    public void validatePathForWriteRejectsParentTraversalOutsideBase()
            throws Exception {
        Path basePath = createDirectory("base");
        Path requestedPath = basePath.resolve("..")
                                     .resolve("outside.txt");

        assertFalse(Files.exists(requestedPath));

        assertIllegalPath(requestedPath, List.of(basePath.toString()));
    }

    /**
     * Relative paths should be rejected (throwing an IOException)
     */
    @Test
    public void validatePathForWriteRejectsRelativePath()
            throws Exception {
        String relativePath = Path.of("relative", "new-file.txt").toString();

        IOException exception = assertThrows(
            IOException.class,
            () -> SecureFileAccess.validatePathForWrite(
                relativePath,
                List.of(temporaryRoot().toString()),
                PURPOSE
            )
        );

        assertEquals(
            "Absolute path required for I/O (%s): %s"
                .formatted(PURPOSE, relativePath),
            exception.getMessage()
        );
    }

    /**
     * Validation must continue through all configured base paths rather than
     * rejecting the candidate after it fails to match the first one.
     */
    @Test
    public void validatePathForWriteAcceptsSecondAllowedBase()
            throws Exception {
        Path firstBasePath = createDirectory("first-base");
        Path secondBasePath = createDirectory("second-base");
        Path filePath = secondBasePath.resolve("new-file.txt");

        assertFalse(Files.exists(filePath));

        Path result = SecureFileAccess.validatePathForWrite(
            filePath.toString(),
            List.of(
                firstBasePath.toString(),
                secondBasePath.toString()
            ),
            PURPOSE
        );

        assertEquals(
            secondBasePath.resolve("new-file.txt"),
            result
        );
    }

    private Path temporaryRoot() throws IOException {
        return temporaryFolder.getRoot().toPath().toRealPath();
    }

    private Path createDirectory(String name) throws IOException {
        return Files.createDirectory(temporaryRoot().resolve(name)).toRealPath();
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

    private void assertIllegalPath(
            Path filePath,
            List<String> allowedBasePaths
    ) {
        IOException exception = assertThrows(IOException.class,
            () -> SecureFileAccess.validatePathForWrite(filePath.toString(), allowedBasePaths, PURPOSE)
        );

        assertEquals("Illegal file path attempted for I/O (%s): %s".formatted(PURPOSE, filePath),
            exception.getMessage()
       );
    }
}