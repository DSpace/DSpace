/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.InputStream;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.Logger;

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
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(OrcidRestConnector.class);

    private String url;

    public OrcidRestConnector(String url) {
        this.url = url;
    }

    public InputStream get(String path, String accessToken) {
        InputStream result = null;
        path = trimSlashes(path);

        String fullPath = url + '/' + path;
        HttpGet httpGet = new HttpGet(fullPath);
        if (StringUtils.isNotBlank(accessToken)) {
            httpGet.addHeader("Content-Type", "application/vnd.orcid+xml");
            httpGet.addHeader("Authorization","Bearer " + accessToken);
        }
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse getResponse = httpClient.execute(httpGet);
            //do not close this httpClient
            result = getResponse.getEntity().getContent();
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
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


}
