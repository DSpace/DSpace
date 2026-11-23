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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
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

    private static final int TIMEOUT_MS = 30_000;

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
    public void ftpFileFromDisallowedHostIsRejected() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        StubFtpUrlHandler ftpHandler = new StubFtpUrlHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] { "allowed.example.com" })
            .withUrl(newUrl("ftp://not-allowed.example.com/file.pdf", ftpHandler));

        Optional<InputStream> result = importFileUtil.getInputStream("ftp://not-allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getWarningMessages(), hasItem(containsString(
            "Domain 'not-allowed.example.com' is not in the allowed list. "
                + "Path: ftp://not-allowed.example.com/file.pdf")));
        assertThat(ftpHandler.getConnectionAttempts(), is(0));
    }

    /**
     * FTP URLs whose host is in the configured allowed-host list must be retrieved exactly like
     * HTTP/HTTPS URLs: the stream is returned, no rejection warning is logged, and the same
     * connect/read timeouts are applied to the underlying connection.
     */
    @Test
    public void ftpFileFromAllowedHostIsAccepted() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        StubFtpUrlHandler ftpHandler = new StubFtpUrlHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] { "allowed.example.com" })
            .withUrl(newUrl("ftp://allowed.example.com/file.pdf", ftpHandler));

        Optional<InputStream> result = importFileUtil.getInputStream("ftp://allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(true));
        assertThat(IOUtils.toString(result.get(), StandardCharsets.UTF_8), is(REMOTE_CONTENT));
        assertThat(handler.getWarningMessages(), not(hasItem(containsString("is not in the allowed list"))));
        assertThat(ftpHandler.getConnectionAttempts(), is(1));
        assertThat(ftpHandler.getConnection().getConnectTimeout(), is(TIMEOUT_MS));
        assertThat(ftpHandler.getConnection().getReadTimeout(), is(TIMEOUT_MS));
    }

    /**
     * The wildcard {@code *} re-enables FTP retrieval from any host, just like HTTP/HTTPS.
     */
    @Test
    public void ftpFileWithWildcardAllowlistIsAccepted() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        StubFtpUrlHandler ftpHandler = new StubFtpUrlHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] { "*" })
            .withUrl(newUrl("ftp://any.example.com/file.pdf", ftpHandler));

        Optional<InputStream> result = importFileUtil.getInputStream("ftp://any.example.com/file.pdf");

        assertThat(result.isPresent(), is(true));
        assertThat(IOUtils.toString(result.get(), StandardCharsets.UTF_8), is(REMOTE_CONTENT));
    }

    /**
     * A missing (null) {@code allowed.ips.import} disables FTP retrieval as well, so it is
     * rejected instead of throwing a {@link NullPointerException}.
     */
    @Test
    public void ftpFileWithNullAllowlistIsRejected() {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, null);

        Optional<InputStream> result = importFileUtil.getInputStream("ftp://allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getWarningMessages(), hasItem(containsString(
            "Remote retrieval is disabled: 'allowed.ips.import' is empty. "
                + "Rejected path: ftp://allowed.example.com/file.pdf")));
    }

    /**
     * When the FTP connection yields no stream, the failure must be logged as an error and an
     * empty result returned, matching the fail-safe behaviour of the other retrievals.
     */
    @Test
    public void ftpFileWithoutInputStreamIsRejected() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        StubFtpUrlHandler ftpHandler = new StubFtpUrlHandler().returningNullStream();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] { "allowed.example.com" })
            .withUrl(newUrl("ftp://allowed.example.com/file.pdf", ftpHandler));

        Optional<InputStream> result = importFileUtil.getInputStream("ftp://allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getErrorMessages(), hasItem(containsString(
            "Failed to obtain InputStream for FTP URL: ftp://allowed.example.com/file.pdf. "
                + "getInputStream() returned null")));
    }

    /**
     * An empty {@code allowed.ips.import} is the fail-closed default: the remote retrieval is
     * disabled, so the request is rejected with an explicit warning and the stream is never
     * opened.
     */
    @Test
    public void remoteFileWithEmptyAllowlistIsRejected() {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        TestableImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] {});

        Optional<InputStream> result = importFileUtil.getInputStream("https://allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getWarningMessages(), hasItem(containsString(
            "Remote retrieval is disabled: 'allowed.ips.import' is empty. "
                + "Rejected path: https://allowed.example.com/file.pdf")));
        assertThat(importFileUtil.getOpenStreamInvocations(), is(0));
    }

    /**
     * A missing (null) {@code allowed.ips.import} must be treated as an empty allow-list, so
     * the retrieval is rejected instead of throwing a {@link NullPointerException}.
     */
    @Test
    public void remoteFileWithNullAllowlistIsRejected() {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, null);

        Optional<InputStream> result = importFileUtil.getInputStream("https://allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getWarningMessages(), hasItem(containsString(
            "Remote retrieval is disabled: 'allowed.ips.import' is empty. "
                + "Rejected path: https://allowed.example.com/file.pdf")));
    }

    /**
     * The wildcard {@code *} explicitly re-enables remote retrieval from any host.
     */
    @Test
    public void remoteFileWithWildcardAllowlistIsAccepted() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] { "*" });

        Optional<InputStream> result = importFileUtil.getInputStream("https://any.example.com/file.pdf");

        assertThat(result.isPresent(), is(true));
        assertThat(IOUtils.toString(result.get(), StandardCharsets.UTF_8), is(REMOTE_CONTENT));
    }

    /**
     * Blank or null entries in the configured allow-list must be ignored: they must not cause a
     * {@link NullPointerException}, nor prevent the remaining configured hosts from matching.
     */
    @Test
    public void remoteFileWithBlankEntriesInAllowlistIsAccepted() {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(
            handler, new String[] { "allowed.example.com", null, "  " });

        Optional<InputStream> allowed = importFileUtil.getInputStream("https://allowed.example.com/file.pdf");
        Optional<InputStream> notAllowed = importFileUtil.getInputStream("https://not-allowed.example.com/file.pdf");

        assertThat(allowed.isPresent(), is(true));
        assertThat(notAllowed.isPresent(), is(false));
    }

    /**
     * The remote retrieval must only accept HTTP/HTTPS URLs: a different scheme must be
     * rejected before the allow-list is even consulted, without opening the stream.
     */
    @Test
    public void remoteFileWithNonHttpSchemeIsRejected() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        URL ftpUrl = new URL("ftp://allowed.example.com/file.pdf");
        TestableImportFileUtil importFileUtil =
            new TestableImportFileUtil(handler, new String[] { "allowed.example.com" }).withUrl(ftpUrl);

        Optional<InputStream> result = importFileUtil.getInputStream("http://allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getWarningMessages(), hasItem(containsString(
            "Scheme 'ftp' is not allowed for remote retrieval. Path: http://allowed.example.com/file.pdf")));
        assertThat(importFileUtil.getOpenStreamInvocations(), is(0));
    }

    /**
     * An empty {@code allowed.ips.import} disables FTP retrieval as well: the request is
     * rejected with an explicit warning without opening any connection.
     */
    @Test
    public void ftpFileWithEmptyAllowlistIsRejected() {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        ImportFileUtil importFileUtil = new TestableImportFileUtil(handler, new String[] {});

        Optional<InputStream> result = importFileUtil.getInputStream("ftp://allowed.example.com/file.pdf");

        assertThat(result.isPresent(), is(false));
        assertThat(handler.getWarningMessages(), hasItem(containsString(
            "Remote retrieval is disabled: 'allowed.ips.import' is empty. "
                + "Rejected path: ftp://allowed.example.com/file.pdf")));
    }

    /**
     * Test double that supplies the allowed-host list directly and returns canned content for
     * allowed URLs, so both the validation and the allowed (positive) path can be exercised
     * without a running DSpace kernel / configuration service or any outbound network access.
     */
    private static final class TestableImportFileUtil extends ImportFileUtil {

        private final String[] allowedHosts;

        private URL urlOverride;

        private int openStreamInvocations;

        private TestableImportFileUtil(DSpaceRunnableHandler handler, String[] allowedHosts) {
            super(handler);
            this.allowedHosts = allowedHosts;
        }

        private TestableImportFileUtil withUrl(URL url) {
            this.urlOverride = url;
            return this;
        }

        private int getOpenStreamInvocations() {
            return openStreamInvocations;
        }

        @Override
        protected String[] getAllowedIps() {
            return allowedHosts;
        }

        @Override
        protected URL getUrl(String path) throws IOException {
            return urlOverride != null ? urlOverride : super.getUrl(path);
        }

        @Override
        protected InputStream openStream(URL url) {
            // Return canned content instead of performing a real network request, so the
            // allowed-host (positive) path can be unit-tested without outbound access.
            openStreamInvocations++;
            return new ByteArrayInputStream(REMOTE_CONTENT.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static URL newUrl(String spec, URLStreamHandler handler) throws IOException {
        return new URL(null, spec, handler);
    }

    /**
     * FTP URL handler that serves canned content, so the FTP retrieval path can be exercised
     * without a running FTP server or any outbound network access. It also records how many
     * connections were attempted, which proves that rejected URLs never hit the network.
     */
    private static final class StubFtpUrlHandler extends URLStreamHandler {

        private URLConnection connection;

        private int connectionAttempts;

        private boolean nullStream;

        private StubFtpUrlHandler returningNullStream() {
            this.nullStream = true;
            return this;
        }

        private URLConnection getConnection() {
            return connection;
        }

        private int getConnectionAttempts() {
            return connectionAttempts;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            connectionAttempts++;
            connection = new StubUrlConnection(url, nullStream);
            return connection;
        }
    }

    private static final class StubUrlConnection extends URLConnection {

        private final boolean nullStream;

        private StubUrlConnection(URL url, boolean nullStream) {
            super(url);
            this.nullStream = nullStream;
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public InputStream getInputStream() {
            if (nullStream) {
                return null;
            }
            return new ByteArrayInputStream(REMOTE_CONTENT.getBytes(StandardCharsets.UTF_8));
        }
    }
}
