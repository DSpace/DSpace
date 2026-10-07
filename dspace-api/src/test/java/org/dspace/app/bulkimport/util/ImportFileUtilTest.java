/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.junit.Test;

/**
 * Unit tests for {@link ImportFileUtil}, focused on the allowed-host validation that
 * guards URL-based (HTTP/HTTPS and FTP) file retrieval during bulk import.
 */
public class ImportFileUtilTest {

    private static final String REMOTE_CONTENT = "remote-file-content";

    /**
     * HTTP/HTTPS URLs must be validated against the configured allowed-host list. A host that
     * is not in the list must be rejected (returning an empty result and logging a warning)
     * without opening the stream.
     */
    @Test
    public void remoteFileFromDisallowedHostIsRejected() {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] { "allowed.example.com" });

        Optional<InputStream> result = importFileUtil.getInputStream("https://not-allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getWarningMessages(), hasItem(containsString(
            "Domain 'not-allowed.example.com' is not in the allowed list. "
                + "Path: https://not-allowed.example.com/file.pdf")));
    }

    /**
     * HTTP/HTTPS URLs whose host is in the configured allowed-host list must be retrieved: the
     * stream is returned and no rejection warning is logged.
     */
    @Test
    public void remoteFileFromAllowedHostIsAccepted() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] { "allowed.example.com" });

        Optional<InputStream> result = importFileUtil.getInputStream("https://allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(true));
        assertThat(IOUtils.toString(result.get(), StandardCharsets.UTF_8), is(REMOTE_CONTENT));
        assertThat(handler.getWarningMessages(), not(hasItem(containsString("is not in the allowed list"))));
    }

    /**
     * FTP URLs must be validated against the configured allowed-host list, exactly like
     * HTTP/HTTPS URLs. A host that is not in the list must be rejected (returning an empty
     * result and logging a warning) without opening any connection.
     */
    @Test
    public void ftpFileFromDisallowedHostIsRejected() {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] { "allowed.example.com" });

        Optional<InputStream> result = importFileUtil.getInputStream("ftp://not-allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getWarningMessages(), hasItem(containsString(
            "Domain 'not-allowed.example.com' is not in the allowed list. "
                + "Path: ftp://not-allowed.example.com/file.pdf")));
    }

    /**
     * Test double that supplies the allowed-host list directly and returns canned content for
     * allowed URLs, so both the validation and the allowed (positive) path can be exercised
     * without a running DSpace kernel / configuration service or any outbound network access.
     */
    private static final class TestableImportFileUtil extends ImportFileUtil {

        private final String[] allowedHosts;

        private TestableImportFileUtil(DSpaceRunnableHandler handler, String[] allowedHosts) {
            super(handler);
            this.allowedHosts = allowedHosts;
        }

        @Override
        protected String[] getAllowedIps() {
            return allowedHosts;
        }

        @Override
        protected InputStream openStream(URL url) {
            // Return canned content instead of performing a real network request, so the
            // allowed-host (positive) path can be unit-tested without outbound access.
            return new ByteArrayInputStream(REMOTE_CONTENT.getBytes(StandardCharsets.UTF_8));
        }
    }
}
