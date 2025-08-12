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
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
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

    /**
     * log4j logger
     */
    private static final Logger log = LogManager.getLogger(OrcidRestConnector.class);

    private final String url;

    public OrcidRestConnector(String url) {
        this.url = url;
    }

    public InputStream get(String path, String accessToken) {
        CloseableHttpResponse getResponse = null;
        InputStream result = null;
        path = trimSlashes(path);

        String fullPath = url + '/' + path;
        HttpGet httpGet = new HttpGet(fullPath);
        if (StringUtils.isNotBlank(accessToken)) {
            httpGet.addHeader("Content-Type", "application/vnd.orcid+xml");
            httpGet.addHeader("Authorization","Bearer " + accessToken);
        }
        try (CloseableHttpClient httpClient = DSpaceHttpClientFactory.getInstance().build()) {
            getResponse = httpClient.execute(httpGet);
            try (InputStream responseStream = getResponse.getEntity().getContent()) {
                // Read all the content of the response stream into a byte array to prevent TruncatedChunkException
                byte[] content = responseStream.readAllBytes();
                result = new ByteArrayInputStream(content);
            }
        } catch (Exception e) {
            getGotError(e, fullPath);
        }

        return result;
    }

    protected void getGotError(Exception e, String fullPath) {
        log.error("Error in rest connector for path: " + fullPath, e);
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

    public static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
