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
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.dspace.storage.secure.SecureFileAccess;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link ItemExportServiceImpl}
 */
public class ItemExportServiceImplTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
        Path sourceDir = temporaryFolder.newFolder("source").toPath().toRealPath();

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
                entries.put(entry.getName(), new String(input.readAllBytes(),UTF_8));
            }
        }

        // Verify that the Zip file only contained "contents.txt:
        assertEquals(Map.of("contents.txt", "test data"), entries);
    }
}