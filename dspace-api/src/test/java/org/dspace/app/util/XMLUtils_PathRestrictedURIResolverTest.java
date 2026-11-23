/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.dspace.app.util.XMLUtils.PathRestrictedURIResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link XMLUtils.PathRestrictedURIResolver}.
 *
 * These unit tests exercise the resolver in isolation without the full application.
 * Therefore, this PURPOSEFULLY doesn't extend AbstractUnitTest because it doesn't need a database setup.
 */
public class XMLUtils_PathRestrictedURIResolverTest {

    @Rule
    public TemporaryFolder allowedFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder disallowedFolder = new TemporaryFolder();

    private Path allowedDir;
    private Path allowedFile;
    private final String allowedFileName = "allowed.xsl";

    @Before
    public void setUp() throws IOException {
        allowedDir = allowedFolder.getRoot().toPath();
        allowedFile = allowedFolder.newFile(allowedFileName).toPath();
    }

    @Test
    public void absoluteFileHref_withinAllowedDir_isResolved() throws Exception {
        PathRestrictedURIResolver resolver = new PathRestrictedURIResolver(allowedDir.toString());

        // href is an absolute path via a file:// URI
        String href = allowedFile.toUri().toString();

        Source source = resolver.resolve(href, null);
        // resolved successfully!
        assertNotNull(source);

        // Also ensure an empty base works properly
        source = resolver.resolve(href, "");
        // resolved successfully!
        assertNotNull(source);
    }

    @Test
    public void relativeHref_resolvedAgainstBase_withinAllowedDir_isResolved() throws Exception {
        PathRestrictedURIResolver resolver = new PathRestrictedURIResolver(allowedDir.toString());

        // base points at the allowed directory
        String base = allowedDir.toUri().toString();
        // href is a relative URL to file based on the "base" path
        String href = allowedFileName;

        Source source = resolver.resolve(href, base);
        // resolved successfully!
        assertNotNull(source);
    }

    @Test
    public void externalResource_isRejected() {
        PathRestrictedURIResolver resolver = new PathRestrictedURIResolver(allowedDir.toString());

        // href is an HTTP URL
        TransformerException ex = assertThrows(TransformerException.class,
                                               () -> resolver.resolve("http://example.com/evil.xsl", null));
        assertTrue(ex.getMessage().contains("External resources not allowed"));

        // href is an HTTPS URL
        TransformerException ex2 = assertThrows(TransformerException.class,
                                               () -> resolver.resolve("https://example.com/evil.xsl", null));
        assertTrue(ex2.getMessage().contains("External resources not allowed"));

        // href is an FTP URL
        TransformerException ex3 = assertThrows(TransformerException.class,
                                                () -> resolver.resolve("ftp://example.com/evil.xsl", null));
        assertTrue(ex3.getMessage().contains("External resources not allowed"));
    }

    @Test
    public void hrefOutsideAllowedDir_isDenied() throws IOException {
        PathRestrictedURIResolver resolver = new PathRestrictedURIResolver(allowedDir.toString());

        // href points to an existing file in a disallowed folder
        Path outsideFile = disallowedFolder.newFile("disallowed.xsl").toPath();
        String href = outsideFile.toUri().toString();

        TransformerException ex = assertThrows(TransformerException.class,
                                               () -> resolver.resolve(href, null));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    public void relativeHref_traversingOutOfBase_isDenied() {
        PathRestrictedURIResolver resolver = new PathRestrictedURIResolver(allowedDir.toString());

        // href is an attempt at a directory traversal attack starting from allowed directory
        String base = allowedDir.toUri().toString();
        String href = "../../../../etc/passwd";

        TransformerException ex = assertThrows(TransformerException.class,
                                               () -> resolver.resolve(href, base));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    public void noAllowedPaths_deniesEverything() {
        // No allowedBasePaths provided means that nothing will resolve
        PathRestrictedURIResolver resolver = new PathRestrictedURIResolver();

        String href = allowedFile.toUri().toString();

        TransformerException ex = assertThrows(TransformerException.class,
                                               () -> resolver.resolve(href, null));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    public void multipleAllowedPaths_isResolvedForSecondPath() throws Exception {
        // Provide multiple allowedBatPaths
        PathRestrictedURIResolver resolver = new PathRestrictedURIResolver(
            disallowedFolder.getRoot().toString(), allowedDir.toString());

        // Ensure something in the *second* path will resolve correctly
        String href = allowedFile.toUri().toString();

        Source source = resolver.resolve(href, null);
        assertNotNull(source);
    }

    @Test
    public void withinAllowedDir_butFileDoesNotExist() {
        PathRestrictedURIResolver resolver = new PathRestrictedURIResolver(allowedDir.toString());

        // Specify a file that doesn't exist in an allowed directory
        String href = allowedDir.resolve("does-not-exist.xsl").toUri().toString();

        // Resolve should throw a file not found error, even though it was requested from an allowed directory.
        TransformerException ex = assertThrows(TransformerException.class,
                                               () -> resolver.resolve(href, null));
        assertTrue(ex.getMessage().contains("File not found"));
    }
}
