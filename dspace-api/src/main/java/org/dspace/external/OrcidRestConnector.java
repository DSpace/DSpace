/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;

/**
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class OrcidRestConnector {

    private static final Logger log = LogManager.getLogger(OrcidRestConnector.class);

    private final String url;

    public OrcidRestConnector(String url) {
        this.url = url;
    }

    public InputStream get(String path, String accessToken) throws OrcidConnectionException {
        String fullPath = url + '/' + trimSlashes(path);
        HttpGet httpGet = new HttpGet(fullPath);
        if (StringUtils.isNotBlank(accessToken)) {
            httpGet.addHeader("Content-Type", "application/vnd.orcid+xml");
            httpGet.addHeader("Authorization","Bearer " + accessToken);
        }
        try (CloseableHttpClient httpClient = DSpaceHttpClientFactory.getInstance().build()) {
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                if (!isSuccessful(httpResponse)) {
                    var statusCode = getStatusCode(httpResponse);
                    var reason = httpResponse.getStatusLine().getReasonPhrase();
                    var error = String.format("The request failed with:%d code, reason:%s ", statusCode, reason);
                    throw new OrcidConnectionException(error, statusCode);
                }
                try (InputStream responseStream = httpResponse.getEntity().getContent()) {
                    // Read all the content of the response stream into a byte array to prevent TruncatedChunkException
                    byte[] content = responseStream.readAllBytes();
                    return new ByteArrayInputStream(content);
                }
            }
        } catch (OrcidConnectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in rest connector for path: " + fullPath, e);
            throw new OrcidConnectionException("Failed to execute ORCID request: " + fullPath, 0, e);
        }
    }

    public static String trimSlashes(String path) {
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    private boolean isSuccessful(HttpResponse response) {
        int statusCode = getStatusCode(response);
        return statusCode >= 200 || statusCode <= 299;
    }

    private int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

}