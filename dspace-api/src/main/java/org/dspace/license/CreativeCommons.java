/*
 * CreativeCommons.java
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
package org.dspace.license;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;

public class CreativeCommons
{
    /**
     * The Bundle Name
     */
    public static final String CC_BUNDLE_NAME = "CC-LICENSE";

    private static final String CC_BS_SOURCE = "org.dspace.license.CreativeCommons";

    /**
     * Some BitStream Names (BSN)
     */
    private static final String BSN_LICENSE_URL = "license_url";

    private static final String BSN_LICENSE_TEXT = "license_text";

    private static final String BSN_LICENSE_RDF = "license_rdf";

    protected static Templates templates = null;
    
    private static boolean enabled_p;

    static
    {
        // we only check the property once
        enabled_p = ConfigurationManager
                .getBooleanProperty("webui.submit.enable-cc");

        if (enabled_p)
        {
            // if defined, set a proxy server for http requests to Creative
            // Commons site
            String proxyHost = ConfigurationManager
                    .getProperty("http.proxy.host");
            String proxyPort = ConfigurationManager
                    .getProperty("http.proxy.port");

            if ((proxyHost != null) && (proxyPort != null))
            {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", proxyPort);
            }
        }
        
        try
        {
            templates = TransformerFactory.newInstance().newTemplates(
                        new StreamSource(CreativeCommons.class
                                .getResourceAsStream("CreativeCommons.xsl")));
        }
        catch (TransformerConfigurationException e)
        {
            throw new RuntimeException(e.getMessage(),e);
        }
       
        
    }

    /**
     * Simple accessor for enabling of CC
     */
    public static boolean isEnabled()
    {
        return enabled_p;
    }

        // create the CC bundle if it doesn't exist
        // If it does, remove it and create a new one.
    private static Bundle getCcBundle(Item item)
        throws SQLException, AuthorizeException, IOException
    {
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);

        if ((bundles.length > 0) && (bundles[0] != null))
        {
            item.removeBundle(bundles[0]);
        }
        return item.createBundle(CC_BUNDLE_NAME);
    }


    /**
     * This is a bit of the "do-the-right-thing" method for CC stuff in an item
     */
    public static void setLicense(Context context, Item item,
            String cc_license_url) throws SQLException, IOException,
            AuthorizeException
    {
        Bundle bundle = getCcBundle(item);

        // get some more information
        String license_text = fetchLicenseText(cc_license_url);
        String license_rdf = fetchLicenseRDF(cc_license_url);
        
        // set the format
        BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(
                context, "License");

        // set the URL bitstream
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_URL, bs_format,
                cc_license_url.getBytes());

        // set the license text bitstream
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_TEXT, bs_format,
                license_text.getBytes());

        // set the RDF bitstream
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_RDF, bs_format,
                license_rdf.getBytes());
    }

    public static void setLicense(Context context, Item item,
                                  InputStream licenseStm, String mimeType)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle bundle = getCcBundle(item);

        // generic "License" format -- change for CC?
        BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(
                context, "License");

        Bitstream bs = bundle.createBitstream(licenseStm);
        bs.setSource(CC_BS_SOURCE);
        bs.setName((mimeType != null &&
                    (mimeType.equalsIgnoreCase("text/xml") ||
                     mimeType.equalsIgnoreCase("text/rdf"))) ?
                   BSN_LICENSE_RDF : BSN_LICENSE_TEXT);
        bs.setFormat(bs_format);
        bs.update();
    }

    public static void removeLicense(Context context, Item item)
            throws SQLException, IOException, AuthorizeException
    {
        // remove CC license bundle if one exists
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);

        if ((bundles.length > 0) && (bundles[0] != null))
        {
            item.removeBundle(bundles[0]);
        }
    }

    public static boolean hasLicense(Context context, Item item)
            throws SQLException, IOException
    {
        // try to find CC license bundle
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);

        if (bundles.length == 0)
        {
            return false;
        }

        // verify it has correct contents
        try
        {
            if ((getLicenseURL(item) == null) || (getLicenseText(item) == null)
                    || (getLicenseRDF(item) == null))
            {
                return false;
            }
        }
        catch (AuthorizeException ae)
        {
            return false;
        }

        return true;
    }

    public static String getLicenseURL(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(item, BSN_LICENSE_URL);
    }

    public static String getLicenseText(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(item, BSN_LICENSE_TEXT);
    }

    public static String getLicenseRDF(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get Creative Commons license RDF, returning Bitstream object.
     * @return bitstream or null.
     */
    public static Bitstream getLicenseRdfBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get Creative Commons license Text, returning Bitstream object.
     * @return bitstream or null.
     */
    public static Bitstream getLicenseTextBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_TEXT);
    }


    /**
     * Get a few license-specific properties. We expect these to be cached at
     * least per server run.
     */
    public static String fetchLicenseText(String license_url)
    {
        String text_url = license_url;
        byte[] urlBytes = fetchURL(text_url);

        return (urlBytes != null) ? new String(urlBytes) : "";
    }

    public static String fetchLicenseRDF(String license_url)
    {
        StringWriter result = new StringWriter();
        
        try
        {
            templates.newTransformer().transform(
                    new StreamSource(license_url + "rdf"),
                    new StreamResult(result)
                    );
        }
        catch (TransformerException e)
        {
            throw new RuntimeException(e.getMessage(),e);
        }

        return result.getBuffer().toString();
    }

    // The following two helper methods assume that the CC
    // bitstreams are short and easily expressed as byte arrays in RAM

    /**
     * This helper method takes some bytes and stores them as a bitstream for an
     * item, under the CC bundle, with the given bitstream name
     */
    private static void setBitstreamFromBytes(Item item, Bundle bundle,
            String bitstream_name, BitstreamFormat format, byte[] bytes)
            throws SQLException, IOException, AuthorizeException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Bitstream bs = bundle.createBitstream(bais);

        bs.setName(bitstream_name);
        bs.setSource(CC_BS_SOURCE);

        bs.setFormat(format);

        // commit everything
        bs.update();
    }

    /**
     * This helper method wraps a String around a byte array returned from the
     * bitstream method further down
     */
    private static String getStringFromBitstream(Item item,
            String bitstream_name) throws SQLException, IOException,
            AuthorizeException
    {
        byte[] bytes = getBytesFromBitstream(item, bitstream_name);

        if (bytes == null)
        {
            return null;
        }

        return new String(bytes);
    }

    /**
     * This helper method retrieves the bytes of a bitstream for an item under
     * the CC bundle, with the given bitstream name
     */
    private static Bitstream getBitstream(Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle cc_bundle = null;

        // look for the CC bundle
        try
        {
            Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);

            if ((bundles != null) && (bundles.length > 0))
            {
                cc_bundle = bundles[0];
            }
            else
            {
                return null;
            }
        }
        catch (Exception exc)
        {
            // this exception catching is a bit generic,
            // but basically it happens if there is no CC bundle
            return null;
        }

        return cc_bundle.getBitstreamByName(bitstream_name);
    }

    private static byte[] getBytesFromBitstream(Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException
    {
        Bitstream bs = getBitstream(item, bitstream_name);

        // no such bitstream
        if (bs == null)
        {
            return null;
        }

        // create a ByteArrayOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.copy(bs.retrieve(), baos);

        return baos.toByteArray();
    }

    /**
     * Fetch the contents of a URL
     */
    private static byte[] fetchURL(String url_string)
    {
        try
        {
            URL url = new URL(url_string);
            URLConnection connection = url.openConnection();
            byte[] bytes = new byte[connection.getContentLength()];

            // loop and read the data until it's done
            int offset = 0;

            while (true)
            {
                int len = connection.getInputStream().read(bytes, offset,
                        bytes.length - offset);

                if (len == -1)
                {
                    break;
                }

                offset += len;
            }

            return bytes;
        }
        catch (Exception exc)
        {
            return null;
        }
    }
}
