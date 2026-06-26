/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid.client;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jakarta.inject.Named;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.app.util.XMLUtils;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Implementation of {@link GrobidClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 * @author Kim Shepherd
 *
 */
public class GrobidClientImpl implements GrobidClient {

    /**
     * HTTP connection pool for GROBID HTTP requests (supports proxies, for config {@see grobid.cfg})
     */
    @Autowired
    @Named("grobidHttpConnectionPoolService")
    protected HttpConnectionPoolService httpConnectionPoolService;

    private static final Logger LOG = LoggerFactory.getLogger(GrobidClientImpl.class);

    /**
     * Base URL of GROBID service. Set in constructor {@see spring-dspace-addon-import-services.xml}
     */
    private final String baseUrl;

    public GrobidClientImpl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public Optional<Document> retrieveHeaderDocument(InputStream inputStream) throws GrobidClientException {
        return retrieveHeaderDocument(inputStream, null);
    }

    /**
     * POST the input stream to configured GROBID service, parse the response as XML and return
     * a normalised {@link org.w3c.dom.Document} wrapped in Optional, or Optional.EMPTY if no
     * response content, or throw a GrobidClientException for any other error
     * @param inputStream       the PDF document
     * @param consolidateHeader the consolidate header parameter
     * @return optional (non-null) DOM Document, or Optional.EMPTY
     * @throws GrobidClientException on any HTTP error
     */
    @Override
    public Optional<Document> retrieveHeaderDocument(InputStream inputStream, ConsolidateHeaderEnum consolidateHeader)
            throws GrobidClientException {
        try  {
            CloseableHttpClient client = httpConnectionPoolService.getClient();
            HttpPost method = new HttpPost(baseUrl + "/api/processHeaderDocument");
            method.addHeader("Accept", "application/xml");

            // add multipart form data with application/xml header
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("input", inputStream);

            if (consolidateHeader != null) {
                builder.addTextBody("consolidateHeader", consolidateHeader.getValue());
            }

            HttpEntity entity = builder.build();
            method.setEntity(entity);

            HttpResponse response = client.execute(method);

            if (isNotSuccessful(response)) {
                throw new GrobidClientException(formatErrorMessage(response));
            }

            if (hasNoContent(response)) {
                LOG.warn("Cannot extract metadata from the document: GROBID returned NO CONTENT");
                return Optional.empty();
            }

            try (InputStream content = response.getEntity().getContent()) {
                DocumentBuilder documentBuilder = XMLUtils.getDocumentBuilder();
                Document document = documentBuilder.parse(content);
                // Normalize document
                document.normalizeDocument();
                return Optional.of(document);
            } catch (SAXException | ParserConfigurationException e) {
                throw new GrobidClientException(e);
            }
        } catch (IOException | UnsupportedOperationException e) {
            throw new GrobidClientException(e);
        }
    }

    private String formatErrorMessage(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        String message = format("An error occurs calling GROBID web services - Response status %s", statusCode);
        return getEntityContentString(response)
            .map(content -> message + " - " + content)
            .orElse(message);
    }

    private boolean isNotSuccessful(HttpResponse response) {
        return response.getStatusLine().getStatusCode() != HttpStatus.SC_OK;
    }

    private boolean hasNoContent(HttpResponse response) {
        return response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT;
    }

    private Optional<String> getEntityContentString(HttpResponse response) {
        try {
            return Optional.ofNullable(IOUtils.toString(response.getEntity().getContent(), defaultCharset()));
        } catch (UnsupportedOperationException | IOException e) {
            LOG.error("An error occurs reading HTTP response entity content", e);
            return Optional.empty();
        }
    }

}
