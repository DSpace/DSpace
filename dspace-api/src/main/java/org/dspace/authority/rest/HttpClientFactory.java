/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.rest;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class HttpClientFactory {

    public static HttpClient getNewHttpClient(boolean ignoreSSL) throws NoSuchAlgorithmException, KeyManagementException {
        HttpClient httpClient;
        if (ignoreSSL) {
            httpClient = isIgnoreSSLCertificate();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }
        return httpClient;
    }

    protected static HttpClient isIgnoreSSLCertificate() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        // set up a TrustManager that trusts everything
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                //                System.out.println("getAcceptedIssuers =============");
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs,
                                           String authType) {
                //                System.out.println("checkClientTrusted =============");
            }

            public void checkServerTrusted(X509Certificate[] certs,
                                           String authType) {
                //                System.out.println("checkServerTrusted =============");
            }
        }}, new SecureRandom());

        return HttpClientBuilder.create().setSslcontext(sslContext).build();

        /*
        HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        SSLSocketFactory sf = new SSLSocketFactory(sslContext);
        sf.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        Scheme httpsScheme = new Scheme("https", 443, sf);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(httpsScheme);


        return new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));
        */
    }
}
