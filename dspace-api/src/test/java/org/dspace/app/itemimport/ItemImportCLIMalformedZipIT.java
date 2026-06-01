/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import org.apache.commons.io.file.PathUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Regression test for the destructive-cleanup bug in {@link ItemImportCLI}.
 *
 * When a zip package contains entry names whose bytes are not valid UTF-8
 * (e.g. a SAF zip created on Windows with Hungarian accents in filenames),
 * {@code java.util.zip.ZipFile} throws
 * {@code ZipException: invalid CEN header (bad entry name)} during unzip.
 * The original code left {@code sourcedir} pointing at the user-supplied
 * {@code --source} path, and the {@code finally} block then called
 * {@code FileUtils.deleteDirectory(sourcedir)} — destroying user data.
 *
 * The fix nulls {@code sourcedir} when {@code unzip()} throws, so the
 * cleanup block skips the destructive branch.
 */
public class ItemImportCLIMalformedZipIT extends AbstractIntegrationTestWithDatabase {

    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private Collection collection;
    private Path tempDir;
    private Path workDir;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .build();
        context.restoreAuthSystemState();

        tempDir = Files.createTempDirectory("safMalformedZipTest");
        java.io.File wd = new java.io.File(
                configurationService.getProperty("org.dspace.app.batchitemimport.work.dir"));
        if (!wd.exists()) {
            Files.createDirectories(wd.toPath());
        }
        workDir = wd.toPath();
    }

    @After
    @Override
    public void destroy() throws Exception {
        PathUtils.deleteOnExit(tempDir);
        if (Files.exists(workDir)) {
            try (var stream = Files.list(workDir)) {
                stream.forEach(PathUtils::deleteOnExit);
            }
        }
        super.destroy();
    }

    /**
     * Bug repro: import a malformed zip whose entry names are CP-852 bytes
     * (not valid UTF-8). With the Layer-1 fix in place, the user-supplied
     * source directory MUST survive the failed import.
     */
    @Test
    public void malformedZipMustNotDeleteSourceDir() throws Exception {
        // sandbox = the directory the user would pass as --source
        Path sandbox = Files.createDirectory(tempDir.resolve("sandbox"));
        Path sentinel = sandbox.resolve("DO_NOT_DELETE.txt");
        Files.writeString(sentinel, "this file must survive a failed import");

        Path malformedZip = sandbox.resolve("malformed.zip");
        writeMalformedZip(malformedZip);

        String[] args = new String[] {
            "import", "-a",
            "-e", admin.getEmail(),
            "-c", collection.getID().toString(),
            "-s", sandbox.toString(),
            "-m", tempDir.resolve("mapfile.out").toString(),
            "-z", "malformed.zip"
        };

        boolean threw = false;
        try {
            runDSpaceScript(args);
        } catch (Throwable expected) {
            threw = true;
        }

        if (!threw) {
            fail("Expected the import to fail on a malformed zip (invalid CEN header),"
                 + " but it returned normally.");
        }

        assertTrue("BUG: malformed zip triggered destructive cleanup —"
                   + " sandbox directory was deleted!",
                   Files.exists(sandbox));
        assertTrue("BUG: malformed zip triggered destructive cleanup —"
                   + " sentinel file inside sandbox was deleted!",
                   Files.exists(sentinel));
    }

    /**
     * Write a structurally valid zip whose single entry name uses raw CP-852
     * bytes (Hungarian "á" = 0xA0 and "ű" = 0xFB). These are not valid UTF-8
     * sequences, so {@link java.util.zip.ZipFile} (UTF-8 strict by default)
     * throws "invalid CEN header (bad entry name)" when opening it.
     *
     * Done by hand against the PKZIP spec so the test has no extra deps.
     */
    private static void writeMalformedZip(Path target) throws Exception {
        // "futás.pdf" — but the bytes are CP-852, NOT UTF-8.
        // f=0x66 u=0x75 t=0x74 á=0xA0 (cp852) s=0x73 . p d f
        byte[] badName = new byte[] {
            0x66, 0x75, 0x74, (byte) 0xA0, 0x73, 0x2E, 0x70, 0x64, 0x66
        };
        byte[] data = "%PDF-1.4\n%dummy payload\n".getBytes();
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        long crc = crc32.getValue();

        try (OutputStream raw = Files.newOutputStream(target);
             BufferedOutputStream buf = new BufferedOutputStream(raw);
             DataOutputStream out = new DataOutputStream(buf)) {

            int localOffset = 0;

            // ---- Local file header (PK\x03\x04) ----
            writeIntLE(out, 0x04034b50);
            writeShortLE(out, 20);          // version needed
            writeShortLE(out, 0);           // flags — bit 11 (UTF-8) NOT set
            writeShortLE(out, 0);           // compression: stored
            writeShortLE(out, 0);           // dos time
            writeShortLE(out, 0x21);        // dos date (1980-01-01)
            writeIntLE(out, (int) crc);
            writeIntLE(out, data.length);   // compressed size
            writeIntLE(out, data.length);   // uncompressed size
            writeShortLE(out, badName.length);
            writeShortLE(out, 0);           // extra field length
            out.write(badName);
            out.write(data);

            int cdOffset = 30 + badName.length + data.length;

            // ---- Central directory file header (PK\x01\x02) ----
            writeIntLE(out, 0x02014b50);
            writeShortLE(out, 20);          // version made by
            writeShortLE(out, 20);          // version needed
            writeShortLE(out, 0);           // flags
            writeShortLE(out, 0);           // compression
            writeShortLE(out, 0);           // dos time
            writeShortLE(out, 0x21);        // dos date
            writeIntLE(out, (int) crc);
            writeIntLE(out, data.length);
            writeIntLE(out, data.length);
            writeShortLE(out, badName.length);
            writeShortLE(out, 0);           // extra field
            writeShortLE(out, 0);           // file comment
            writeShortLE(out, 0);           // disk number
            writeShortLE(out, 0);           // internal attrs
            writeIntLE(out, 0);             // external attrs
            writeIntLE(out, localOffset);
            out.write(badName);

            int cdSize = 46 + badName.length;

            // ---- End of central directory (PK\x05\x06) ----
            writeIntLE(out, 0x06054b50);
            writeShortLE(out, 0);           // disk number
            writeShortLE(out, 0);           // disk with CD
            writeShortLE(out, 1);           // CD entries this disk
            writeShortLE(out, 1);           // total CD entries
            writeIntLE(out, cdSize);
            writeIntLE(out, cdOffset);
            writeShortLE(out, 0);           // zip comment length
        }
    }

    private static void writeIntLE(DataOutputStream out, int v) throws Exception {
        out.write(v & 0xff);
        out.write((v >>> 8) & 0xff);
        out.write((v >>> 16) & 0xff);
        out.write((v >>> 24) & 0xff);
    }

    private static void writeShortLE(DataOutputStream out, int v) throws Exception {
        out.write(v & 0xff);
        out.write((v >>> 8) & 0xff);
    }
}
