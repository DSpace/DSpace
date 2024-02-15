/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.stereotype.Component;

/**
 * Collection of utility methods for clarin customized operations
 *
 * @author Milan Majchrak (dspace at dataquest.sk)
 */
@Component
public class ClarinUtils {

    private ClarinUtils() {
    }

    /**
     * Disables SSL certificate validation for the given connection
     *
     * @param connection
     */
    public static void disableCertificateValidation(HttpsURLConnection connection) {
        try {
            // Create a TrustManager that trusts all certificates
            TrustManager[] trustAllCerts = { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                } }
            };

            // Install the TrustManager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            connection.setSSLSocketFactory(sslContext.getSocketFactory());

            // Set a HostnameVerifier that accepts all hostnames
            connection.setHostnameVerifier((hostname, session) -> true);

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Error disabling SSL certificate validation", e);
        }
    }
}
