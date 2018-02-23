/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.rest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dspace.authority.util.XMLUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.Scanner;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class RESTConnector {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(RESTConnector.class);

    private String url;

    public RESTConnector(String url) {
        this.url = url;
    }

    public Document get(String path) {
        Document document = null;

        InputStream result = null;
        path = trimSlashes(path);

        String fullPath = url + '/' + path;
        GetMethod httpGet = new GetMethod(fullPath);

        try {
            HttpClient httpclient = new HttpClient();

            httpclient.executeMethod(httpGet);
            //do not close this httpClient
            document = XMLUtils.convertStreamToXML(httpGet.getResponseBodyAsStream());

        } catch (Exception e) {
            httpGet.releaseConnection();
            getGotError(e, fullPath);
        }
        httpGet.releaseConnection();
        return document;
    }

    protected void getGotError(Exception e, String fullPath) {
        log.error("Error in rest connector for path: "+fullPath, e);
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
