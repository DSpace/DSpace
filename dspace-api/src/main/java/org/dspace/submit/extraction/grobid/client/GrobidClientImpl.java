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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.dspace.submit.extraction.grobid.TEI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link GrobidClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GrobidClientImpl implements GrobidClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidClientImpl.class);

    private final String baseUrl;

    public GrobidClientImpl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public TEI processHeaderDocument(InputStream inputStream) {
        return processHeaderDocument(inputStream, null);
    }

    @Override
    public TEI processHeaderDocument(InputStream inputStream, ConsolidateHeaderEnum consolidateHeader) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost method = new HttpPost(baseUrl + "/api/processHeaderDocument");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("input", inputStream);

            if (consolidateHeader != null) {
                builder.addTextBody("consolidateHeader", consolidateHeader.getValue());
            }

            HttpEntity entity = builder.build();
            method.setEntity(entity);

            HttpResponse response = client.execute(method);

            if (isNotSuccessfully(response)) {
                throw new GrobidClientException(formatErrorMessage(response));
            }

            return unmarshall(response.getEntity(), TEI.class);

        } catch (IOException | UnsupportedOperationException | JAXBException e) {
            throw new GrobidClientException(e);
        }
    }

    private String formatErrorMessage(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        String message = format("An error occurs calling GROBID web services - Response status %s", statusCode);
        return getEntityContent(response)
            .map(content -> message + " - " + content)
            .orElse(message);
    }

    @SuppressWarnings("unchecked")
    private <T> T unmarshall(HttpEntity entity, Class<T> clazz)
        throws JAXBException, UnsupportedOperationException, IOException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        return (T) context.createUnmarshaller().unmarshal(entity.getContent());
    }

    private boolean isNotSuccessfully(HttpResponse response) {
        return response.getStatusLine().getStatusCode() != HttpStatus.SC_OK;
    }

    private Optional<String> getEntityContent(HttpResponse response) {
        try {
            return Optional.ofNullable(IOUtils.toString(response.getEntity().getContent(), defaultCharset()));
        } catch (UnsupportedOperationException | IOException e) {
            LOGGER.error("An error occurs reading HTTP response entity content", e);
            return Optional.empty();
        }
    }

}
