/*
 * X509Manager.java
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
package org.dspace.app.webui.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.StringTokenizer;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * High-level manager for X509 certificates. Note that the String form (base64
 * encoding) for certs is very, very picky about whitespace, line endings and
 * such.
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class X509Manager
{
    /** The PublicKey of the Certificate Authority */
    private static PublicKey caPublicKey;

    /**
     * Return true if and only if CERTIFICATE is valid.
     * 
     * @param certificate -
     *            An X509 certificate object
     * @return - true if the certificate is valid, false otherwise
     * @exception CertificateException -
     *                If an error occurs
     */
    public static boolean isValid(X509Certificate certificate)
            throws CertificateException
    {
        initialize();

        return isValid(certificate, getCAPublicKey());
    }

    /**
     * Return the email address from CERTIFICATE, or null if an email address
     * cannot be found in the certificate.
     * 
     * Note that the certificate parsing has only been tested with certificates
     * granted by the MIT Certification Authority, and may not work elsewhere.
     * 
     * @param certificate -
     *            An X509 certificate object
     * @return - The email address found in certificate, or null if an email
     *         address cannot be found in the certificate.
     */
    public static String getEmail(X509Certificate certificate)
            throws AuthorizeException, SQLException
    {
        Principal principal = certificate.getSubjectDN();

        if (principal == null)
        {
            return null;
        }

        String dn = principal.getName();

        if (dn == null)
        {
            return null;
        }

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
                {
                    return null;
                }

                return token.substring(len).toLowerCase();
            }
        }

        return null;
    }

    /**
     * Return the eperson from CERTIFICATE, or null if the email in the
     * certificate doesn't correspond to an eperson.
     * 
     * Note that the certificate parsing has only been tested with certificates
     * granted by the MIT Certification Authority, and may not work elsewhere.
     * 
     * @param certificate -
     *            An X509 certificate object
     * @return - The email address found in certificate, or null if an email
     *         address cannot be found in the certificate.
     */
    public static EPerson getUser(Context context, X509Certificate certificate)
            throws AuthorizeException, SQLException
    {
        String email = getEmail(certificate);

        if (email != null)
        {
            return EPerson.findByEmail(context, email);
        }

        return null;
    }

    /**
     * Load the CA Public Key from a file We assume that it does not come from a
     * keystore
     * 
     * @exception CertificateException -
     *                If an error occurs
     */
    private static void initialize() throws CertificateException
    {
        String cert = ConfigurationManager.getProperty("webui.cert.ca");

        if (cert == null)
        {
            throw new CertificateException(
                    "Unable to initialize CA certificate: configuration property \"webui.cert.ca\" is not set");
        }

        try
        {
            InputStream is = new BufferedInputStream(new FileInputStream(cert));

            caPublicKey = getPublicKey(is);
        }
        catch (IOException e)
        {
            throw new CertificateException(
                    "Unable to initialize CA certificate: " + e);
        }
    }

    /**
     * Return the PublicKey that we use to validate X509 certs. For now, we
     * assume that there's only one key.
     * 
     * @return - The PublicKey that we use to validate X509 certs.
     * @exception CertificateException -
     *                If an error occurs
     */
    private static PublicKey getCAPublicKey() throws CertificateException
    {
        if (caPublicKey == null)
        {
            initialize();
        }

        return caPublicKey;
    }

    /**
     * Convert the stream containing certificate to an X509 certificate object.
     * 
     * @param stream -
     *            An InputStream containing an X509 certificate
     * @return - An X509 certificate object.
     * @exception CertificateException -
     *                If an error occurs
     */
    private static X509Certificate loadCertificate(InputStream stream)
            throws CertificateException
    {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(stream);
    }

    /**
     * Return the PublicKey read from STREAM.
     * 
     * @param stream -
     *            An InputStream containing an X509 certificate
     * @return - The public key from the certificate, or null
     * @exception CertificateException -
     *                If an error occurs
     */
    private static PublicKey getPublicKey(InputStream stream)
            throws CertificateException
    {
        X509Certificate cert = loadCertificate(stream);

        return ((cert == null) ? null : cert.getPublicKey());
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
    private static boolean isValid(X509Certificate certificate, PublicKey key)
    {
        if (certificate == null)
        {
            return false;
        }

        if (key == null)
        {
            return false;
        }

        try
        {
            certificate.checkValidity();
        }
        catch (CertificateExpiredException cee)
        {
            return false;
        }
        catch (CertificateNotYetValidException cnyve)
        {
            return false;
        }

        // Verify certificate
        try
        {
            certificate.verify(key);
        }
        catch (CertificateException ce)
        {
            return false;
        }
        catch (NoSuchAlgorithmException nsae)
        {
            return false;
        }
        catch (InvalidKeyException ike)
        {
            return false;
        }
        catch (NoSuchProviderException nspe)
        {
            return false;
        }
        catch (SignatureException se)
        {
            return false;
        }

        // Survived to this point?
        return true;
    }
}
