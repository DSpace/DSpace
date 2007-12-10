/*
 * X509Authentication.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.authenticate;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * Implicit authentication method that gets credentials from the X.509
 * client certificate supplied by the HTTPS client when connecting to
 * this server.  The email address in that certificate is taken as the
 * authenticated user name with no further checking, so be sure
 * your HTTP server (e.g. Tomcat) is configured correctly to accept only
 * client certificates it can validate.
 * <p>
 * See the <code>AuthenticationMethod</code> interface for more details.
 * <p>
 * <b>Configuration:</b><pre>
 *   authentication.x509.keystore.path = <em>path to Java keystore file</em>
 *   authentication.x509.keystore.password = <em>password to access the keystore</em>
 *   authentication.x509.ca.cert = <em>path to certificate file for CA whose client certs to accept.</em>
 *   authentication.x509.autoregister = <em>"true" if E-Person is created automatically for unknown new users.</em>
 * </pre>
 * Only one of the "<code>keystore.path</code>" or "<code>ca.cert</code>"
 * options is required.  If you supply a keystore, then all of the "trusted"
 * certificates in the keystore represent CAs whose client certificates will
 * be accepted.  The <code>ca.cert</code> option only allows a single CA to be named.
 * <p>
 * You can configure <em>both</em> a keystore and a CA cert, and both will
 * be used.
 * <p>
 * The <code>autoregister</code> configuration parameter determines what
 * the <code>canSelfRegister()</code> method returns.  It also allows an
 * EPerson record to be created automatically when the presented
 * certificate is acceptable but there is no corresponding EPerson.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class X509Authentication
    implements AuthenticationMethod
{

    /** log4j category */
    private static Logger log = Logger.getLogger(X509Authentication.class);

    /** public key of CA to check client certs against. */
    private static PublicKey caPublicKey = null;

    /** key store for CA certs if we use that */
    private static KeyStore caCertKeyStore = null;

    /**
     * Initialization:
     *  Set caPublicKey and/or keystore.  This loads the information
     *  needed to check if a client cert presented is valid and acceptable.
     */
    static
    {
        String keystorePath = ConfigurationManager.getProperty("authentication.x509.keystore.path");
        String keystorePassword = ConfigurationManager.getProperty("authentication.x509.keystore.password");
        String caCertPath = ConfigurationManager.getProperty("authentication.x509.ca.cert");

        // backward-compatible kludge
        if (caCertPath == null)
            caCertPath = ConfigurationManager.getProperty("webui.cert.ca");

        // First look for keystore full of trusted certs.
        if (keystorePath != null)
        {
            if (keystorePassword == null)
                keystorePassword = "";
            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new FileInputStream(keystorePath),
                                     keystorePassword.toCharArray());
                caCertKeyStore = ks;
            }
            catch (IOException e)
            {
                log.error("X509Authentication: Failed to load CA keystore, file="+
                            keystorePath+", error="+e.toString());
            }
            catch (GeneralSecurityException e)
            {
                log.error("X509Authentication: Failed to extract CA keystore, file="+
                            keystorePath+", error="+e.toString());
            }
        }

        // Second, try getting public key out of CA cert, if that's configured.
        if (caCertPath != null)
        {
            try
            {
                InputStream is = new BufferedInputStream(new FileInputStream(caCertPath));
                X509Certificate cert = (X509Certificate) CertificateFactory
                                       .getInstance("X.509").generateCertificate(is);
                if (cert != null)
                    caPublicKey = cert.getPublicKey();
            }
            catch (IOException e)
            {
                log.error("X509Authentication: Failed to load CA cert, file="+
                            caCertPath+", error="+e.toString());
            }
            catch (CertificateException e)
            {
                log.error("X509Authentication: Failed to extract CA cert, file="+
                            caCertPath+", error="+e.toString());
            }
        }
    }

    /**
     * Return the email address from <code>certificate</code>, or null
     * if an email address cannot be found in the certificate.
     * <p>
     * Note that the certificate parsing has only been tested with certificates
     * granted by the MIT Certification Authority, and may not work elsewhere.
     *
     * @param certificate -
     *            An X509 certificate object
     * @return - The email address found in certificate, or null if an email
     *         address cannot be found in the certificate.
     */
    private static String getEmail(X509Certificate certificate)
    {
        Principal principal = certificate.getSubjectDN();

        if (principal == null)
            return null;

        String dn = principal.getName();
        if (dn == null)
            return null;

        StringTokenizer tokenizer = new StringTokenizer(dn, ",");
        String token = null;
        while (tokenizer.hasMoreTokens())
        {
            int len = "emailaddress=".length();

            token = (String) tokenizer.nextToken();

            if (token.toLowerCase().startsWith("emailaddress="))
            {
                // Make sure the token actually contains something
                if (token.length() <= len)
                    return null;

                return token.substring(len).toLowerCase();
            }
        }

        return null;
    }

    /**
     * Verify CERTIFICATE against KEY. Return true if and only if CERTIFICATE is
     * valid and can be verified against KEY.
     *
     * @param certificate -
     *            An X509 certificate object
     * @param key -
     *            PublicKey to check the certificate against.
     * @return - True if CERTIFICATE is valid and can be verified against KEY,
     *         false otherwise.
     */
    private static boolean isValid(Context context,
                                   X509Certificate certificate)
    {
        if (certificate == null)
            return false;

        // This checks that current time is within cert's validity window:
        try
        {
            certificate.checkValidity();
        }
        catch (CertificateException e)
        {
            log.info(LogManager.getHeader(context, "authentication",
                    "X.509 Certificate is EXPIRED or PREMATURE: "+e.toString()));
            return false;
        }

        // Try CA public key, if available.
        if (caPublicKey != null)
        {
            try
            {
                certificate.verify(caPublicKey);
                return true;
            }
            catch (GeneralSecurityException e)
            {
                log.info(LogManager.getHeader(context, "authentication",
                        "X.509 Certificate FAILED SIGNATURE check: "+e.toString()));
            }
        }

        // Try it with keystore, if available.
        if (caCertKeyStore != null)
        {
            try
            {
                Enumeration ke = caCertKeyStore.aliases();

                while (ke.hasMoreElements())
                {
                    String alias = (String)ke.nextElement();
                    if (caCertKeyStore.isCertificateEntry(alias))
                    {
                        Certificate ca = caCertKeyStore.getCertificate(alias);
                        try {
                            certificate.verify(ca.getPublicKey());
                            return true;
                        }
                        catch (CertificateException ce)
                        {
                        }
                    }
                }
                log.info(LogManager.getHeader(context, "authentication",
                        "Keystore method FAILED SIGNATURE check on client cert."));
            }
            catch (GeneralSecurityException e)
            {
                log.info(LogManager.getHeader(context, "authentication",
                        "X.509 Certificate FAILED SIGNATURE check: "+e.toString()));
            }

        }
        return false;
    }

    /**
     * Predicate, can new user automatically create EPerson.
     * Checks configuration value.  You'll probably want this to
     * be true to take advantage of a Web certificate infrastructure
     * with many more users than are already known by DSpace.
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
    {
        return ConfigurationManager
            .getBooleanProperty("authentication.x509.autoregister");
    }

    /**
     *  Nothing extra to initialize.
     */
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson)
    {
    }

    /**
     * We don't use EPerson password so there is no reason to change it.
     */
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
    {
        return false;
    }

    /**
     * Returns true, this is an implicit method.
     */
    public boolean isImplicit()
    {
        return true;
    }

    /**
     * No special groups.
     */
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
        return new int[0];
    }

    /**
     * X509 certificate authentication. The client certificate
     * is obtained from the <code>ServletRequest</code> object.
     * <ul>
     * <li>If the certificate is valid, and corresponds to an existing EPerson,
     * and the user is allowed to login, return success.</li>
     * <li>If the user is matched but is not allowed to login, it fails.</li>
     * <li>If the certificate is valid, but there is no corresponding EPerson,
     * the <code>"authentication.x509.autoregister"</code>
     * configuration parameter is checked (via <code>canSelfRegister()</code>)
     * <ul>
     * <li>If it's true, a new EPerson record is created for the certificate, and
     * the result is success.</li>
     * <li>If it's false, return that the user was unknown.</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @return One of: SUCCESS, BAD_CREDENTIALS, NO_SUCH_USER, BAD_ARGS
     */
    public int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
    {
        // Obtain the certificate from the request, if any
        X509Certificate[] certs = null;
        if (request != null)
            certs = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");

        if ((certs == null) || (certs.length == 0))
            return BAD_ARGS;
        else
        {
            // We have a cert -- check it and get username from it.
            try
            {
                if (!isValid(context, certs[0]))
                {
                    log.warn(LogManager.getHeader(context, "authenticate",
                        "type=x509certificate, status=BAD_CREDENTIALS (not valid)"));
                    return BAD_CREDENTIALS;
                }

                // And it's valid - try and get an e-person
                String email = getEmail(certs[0]);
                EPerson eperson = null;
                if (email != null)
                    eperson = EPerson.findByEmail(context, email);
                if (eperson == null)
                {
                    // Cert is valid, but no record.
                    if (email != null && canSelfRegister(context, request, null))
                    {
                        // Register the new user automatically
                        log.info(LogManager.getHeader(context,
                                "autoregister", "from=x.509, email=" + email));

                        // TEMPORARILY turn off authorisation
                        context.setIgnoreAuthorization(true);
                        eperson = EPerson.create(context);
                        eperson.setEmail(email);
                        eperson.setCanLogIn(true);
                        AuthenticationManager.initEPerson(context,
                                request, eperson);
                        eperson.update();
                        try
                        {
                            context.commit();
                        }
                        catch (java.sql.SQLException sqle)
                        {
                            throw new RuntimeException(sqle);
                        }
                        context.setIgnoreAuthorization(false);
                        context.setCurrentUser(eperson);
                        return SUCCESS;
                    }
                    else
                    {
                        // No auto-registration for valid certs
                        log.warn(LogManager.getHeader(context, "authenticate",
                                "type=cert_but_no_record, cannot auto-register"));
                        return NO_SUCH_USER;
                    }
                }

                // make sure this is a login account
                else if (!eperson.canLogIn())
                {
                    log.warn(LogManager.getHeader(context, "authenticate",
                            "type=x509certificate, email="+email+", canLogIn=false, rejecting."));
                    return BAD_ARGS;
                }

                else
                {
                    log.info(LogManager.getHeader(context, "login",
                            "type=x509certificate"));
                    context.setCurrentUser(eperson);
                    return SUCCESS;
                }
            }
            catch (AuthorizeException ce)
            {
                log.warn(LogManager.getHeader(context, "authorize_exception",
                        ""), ce);
            }

            return BAD_ARGS;
        }
    }

    /**
     * Return null, since this is an implicit method with no login page.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @param response
     *  The HTTP response from the servlet method.
     *
     * @return fully-qualified URL
     */
    public String loginPageURL(Context context,
                            HttpServletRequest request,
                            HttpServletResponse response)
    {
        return null;
    }

    /**
     * Return null, since this is an implicit method with no login page.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @return Message key to look up in i18n message catalog.
     */
    public String loginPageTitle(Context context)
    {
        return null;
    }
}
