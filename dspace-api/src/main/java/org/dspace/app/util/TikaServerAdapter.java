/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.app.mediafilter.ExtractedTextHandler;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.util.MimeTypeUtils;

/**
 * Talk to a Tika Server instance.
 *
 * @author mwood
 */
public class TikaServerAdapter {
    private static final Logger log = LogManager.getLogger();

    private static final String C_TIKA_URL = "textextractor.tika.url";
    private static final String DEFAULT_TIKA_URL = "http://localhost:9998/";

    private TikaServerAdapter() {}

    /**
     * Ship bytes to Tika endpoint and return the result.
     *
     * @param path URL path to Tika endpoint ("/tika", "/detect" etc.)
     * @param source bytes for Tika to digest.
     * @param maxChars Tika should consider no more than this much input.
     * @return the result from Tika, or null if none.
     * @throws IOException if textextractor.tika.url is malformed, or passed through.
     */
    public static InputStream postStream(String path, InputStream source, int maxChars)
            throws IOException {
        // Where is our Tika Server?
        ConfigurationService cfg = DSpaceServicesFactory.getInstance()
                .getConfigurationService();
        String tikaServerUrl = cfg.getProperty(C_TIKA_URL, DEFAULT_TIKA_URL);
        URI tikaUri;
        try {
            tikaUri = new URI(tikaServerUrl).resolve(path);
        } catch (URISyntaxException e) {
            throw new IOException("Could not contact Tika server at " + tikaServerUrl, e);
        }

        HttpPost request = new HttpPost(tikaUri); // POST because bitstream may be huge
        request.addHeader(HttpHeaders.ACCEPT, MimeTypeUtils.TEXT_PLAIN_VALUE);
        request.addHeader("X-Tika-writelimit", String.valueOf(maxChars));

        InputStreamEntity sourceEntity = new InputStreamEntity(source);
        sourceEntity.setChunked(true); // Hint:  this may be large
        request.setEntity(sourceEntity);

        try (
                CloseableHttpClient client = DSpaceHttpClientFactory
                        .getInstance()
                        .build();
                CloseableHttpResponse response = client.execute(request);
                ) {
            StatusLine statusLine = response.getStatusLine();
            int status = statusLine.getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                if (null == entity) {
                    return null;
                }
                return new BufferedHttpEntity(entity).getContent(); // XXX result copied to memory!
            } else {
                log.error("Failed in Tika Server:  {}", statusLine.getReasonPhrase());
                return null;
            }

            // N.B. the entity must be buffered because we will close the
            // original here.
        }
    }

    /**
     * Ship bytes to Tika and pass the result to a callback.
     *
     * @param path URL path to Tika endpoint.
     * @param source data to ship to Tika.
     * @param maxChars Tika should consider no more than this much.
     * @param handler gadget to accept what Tika returns.
     * @throws java.lang.Exception
     */
    public static void postStream(String path, InputStream source, int maxChars,
            ExtractedTextHandler handler)
            throws Exception {
        // Where is our Tika Server?
        ConfigurationService cfg = DSpaceServicesFactory.getInstance()
                .getConfigurationService();
        String tikaServerUrl = cfg.getProperty(C_TIKA_URL,
                DEFAULT_TIKA_URL);

        URI tikaUri;
        try {
            tikaUri = new URI(tikaServerUrl).resolve(path);
        } catch (URISyntaxException e) {
            log.fatal(C_TIKA_URL + " = {} is ill-formed.  {}",
                    tikaServerUrl, e.getMessage());
            System.err.format(C_TIKA_URL + " = %s is ill-formed.  %s%n",
                    tikaServerUrl, e.getMessage());
            return;
        }
        InputStreamEntity sourceEntity = new InputStreamEntity(source);
        sourceEntity.setChunked(true); // Hint:  this may be large

        HttpPost request = new HttpPost(tikaUri); // POST because bitstream may be huge
        request.addHeader(HttpHeaders.ACCEPT, MimeTypeUtils.TEXT_PLAIN_VALUE);
        request.addHeader("X-Tika-writelimit", String.valueOf(maxChars));
        request.setEntity(sourceEntity);

        try (
                CloseableHttpClient client = DSpaceHttpClientFactory
                        .getInstance()
                        .build();
                CloseableHttpResponse response = client.execute(request);
                ) {
            StatusLine statusLine = response.getStatusLine();
            int status = statusLine.getStatusCode();
            if (status >= 200 && status < 300) { // Success?
                HttpEntity entity = response.getEntity();
                if (null == entity) {
                    log.info("Tika returned no result.  Status {}:  {}",
                            status, statusLine.getReasonPhrase());
                    return;
                }
                handler.handleStream(entity.getContent());
            } else {
                log.error("Failed to extract flat text:  {}", statusLine.getReasonPhrase());
            }
        }
    }
}
