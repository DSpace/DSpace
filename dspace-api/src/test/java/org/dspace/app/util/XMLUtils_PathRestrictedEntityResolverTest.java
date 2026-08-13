/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.dspace.app.util.XMLUtils.PathRestrictedEntityResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Unit tests for {@link XMLUtils.PathRestrictedEntityResolver}.
 *
 * These unit tests exercise the resolver in isolation without the full application.
 * Therefore, this PURPOSEFULLY doesn't extend AbstractUnitTest because it doesn't need a database setup.
 */
public class XMLUtils_PathRestrictedEntityResolverTest {

    @Rule
    public TemporaryFolder allowedFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder disallowedFolder = new TemporaryFolder();

    private Path allowedDir;
    private Path allowedFile;

    @Before
    public void setUp() throws IOException {
        allowedDir = allowedFolder.getRoot().toPath();
        allowedFile = allowedFolder.newFile("allowed.dtd").toPath();
    }

    @Test
    public void nullSystemId_returnsNull() throws Exception {
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver(allowedDir.toString());
        assertNull(resolver.resolveEntity("somePublicId", null));
    }

    @Test
    public void plainFilePath_withinAllowedDir_isResolved() throws Exception {
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver(allowedDir.toString());

        // systemId is a raw filesystem path (NOT a "file://" URI)
        String systemId = allowedFile.toString();
        assertFalse(systemId.startsWith("file://"));

        InputSource result = resolver.resolveEntity(null, systemId);
        // resolved successfully!
        assertNotNull(result);
    }

    @Test
    public void fileURI_withinAllowedDir_isResolved() throws Exception {
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver(allowedDir.toString());

        // systemId using "file://" prefix, e.g. file:///path/to/allowed.dtd
        String systemId = allowedFile.toUri().toString();
        assertTrue(systemId.startsWith("file://"));

        InputSource result = resolver.resolveEntity(null, systemId);
        // resolved successfully!
        assertNotNull(result);
    }

    @Test
    public void externalSystemId_isRejected() {
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver(allowedDir.toString());

        // systemId is an HTTP link
        SAXException ex = assertThrows(SAXException.class,
                                       () -> resolver.resolveEntity(null, "http://example.com/evil.dtd"));
        assertTrue(ex.getMessage().contains("External resources not allowed"));

        // systemId is an FTP link
        SAXException ex2 = assertThrows(SAXException.class,
                                       () -> resolver.resolveEntity(null, "ftp://example.com/evil.dtd"));
        assertTrue(ex2.getMessage().contains("External resources not allowed"));

        // systemId is an HTTPS link
        SAXException ex3 = assertThrows(SAXException.class,
                                        () -> resolver.resolveEntity(null, "https://example.com/evil.dtd"));
        assertTrue(ex3.getMessage().contains("External resources not allowed"));
    }

    @Test
    public void pathOutsideAllowedDir_isDenied() throws IOException {
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver(allowedDir.toString());

        Path outsideFile = disallowedFolder.newFile("disallowed.dtd").toPath();

        // systemId points at an existing file in a disallowed directory
        SAXException ex = assertThrows(SAXException.class,
                                       () -> resolver.resolveEntity(null, outsideFile.toString()));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    public void pathTraversalOutOfAllowedDir_isDenied() {
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver(allowedDir.toString());

        // systemId is an attempt at directory traversal attack starting from allowed directory
        String traversalAttempt = allowedDir.resolve("../../../etc/passwd").toString();

        SAXException ex = assertThrows(SAXException.class,
                                       () -> resolver.resolveEntity(null, traversalAttempt));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    public void noAllowedPaths_deniesEverything() {
        // No allowedBasePaths provided means that nothing will resolve
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver();

        SAXException ex = assertThrows(SAXException.class,
                                       () -> resolver.resolveEntity(null, allowedFile.toString()));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    public void multipleAllowedPaths_isResolvedForSecondPath() throws Exception {
        // Provide multiple allowedBatPaths
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver(
            disallowedFolder.getRoot().toString(), allowedDir.toString());

        // Ensure something in the *second* path will resolve correctly
        InputSource result = resolver.resolveEntity(null, allowedFile.toString());
        // resolved successfully!
        assertNotNull(result);
    }

    @Test
    public void withinAllowedDir_butFileDoesNotExist() {
        PathRestrictedEntityResolver resolver = new PathRestrictedEntityResolver(allowedDir.toString());

        // Specify a file that doesn't exist in an allowed directory
        Path missing = allowedDir.resolve("does-not-exist.dtd");

        // Resolve should throw a file not found error, even though it was requested from an allowed directory.
        SAXException ex = assertThrows(SAXException.class,
                                       () -> resolver.resolveEntity(null, missing.toString()));
        assertTrue(ex.getMessage().contains("File not found"));
    }
}
