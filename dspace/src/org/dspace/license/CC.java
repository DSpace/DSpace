/*
 * CC.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

/**
 * Class representing configuration and basic things for 
 * Creative Commons Licenses. Eventually this will do caching 
 * of such information instead of querying the CC site all the time.
 * <p>
 * This contains mostly static methods (we could build an object model, but
 * there's really no point in that for such simple things).
 *
 * @author   Ben Adida (ben@mit.edu)
 * @version  $Revision$
 */

import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.core.Utils;
import org.dspace.core.ConfigurationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.net.*;

public class CC
{
    /**
     * The Bundle Name
     */
    private static final String CC_BUNDLE_NAME = "CC";
    private static final String CC_BS_SOURCE = "org.dspace.content.CC";

    /**
     * Some BitStream Names (BSN)
     **/
    private static final String BSN_LICENSE_URL = "license_url";
    private static final String BSN_LICENSE_TEXT = "license_text";
    private static final String BSN_LICENSE_RDF = "license_rdf";

    private static boolean enabled_p;

    static {
	// we only check the property once
	enabled_p = ConfigurationManager.getBooleanProperty("cc.enabled");
    }

    /**
     * Simple accessor for enabling of CC 
     **/
    public static boolean isEnabled() {
	return enabled_p;
    }
    
    /**
     * This is a bit of the "do-the-right-thing" method
     * for CC stuff in an item
     */
    public static void setLicense(Context context, 
				    Item item, String cc_license_url) 
	throws SQLException, IOException, AuthorizeException 
    {
	// only if CC is enabled
	if (!CC.isEnabled())
	    return;

	// we create the CC bundle
	Bundle bundle = item.createBundle(CC_BUNDLE_NAME);

	// get some more information
	String license_text = fetchLicenseText(cc_license_url);
	String license_rdf = fetchLicenseRDF(cc_license_url);

	// here we need to transform the license_rdf into a document_rdf
	// first we find the beginning of "<License"
	int license_start = license_rdf.indexOf("<License");
	// the 10 is the length of the license closing tag.
	int license_end = license_rdf.indexOf("</License>") + 10;
	String document_rdf = "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"\n" + 
	    "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
	    "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" + 
	    "<Work rdf:about=\"\">\n" +
	    "<license rdf:resource=\"" + cc_license_url + "\">\n" + 
	    "</Work>\n\n" +
	    license_rdf.substring(license_start,license_end) +
	    "\n\n</rdf:RDF>";

	// set the format
        BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(
            context, "License");

	// set the URL bitstream
	setBitstreamFromBytes(item, bundle, BSN_LICENSE_URL, bs_format, cc_license_url.getBytes());

	// set the license text bitstream
	setBitstreamFromBytes(item, bundle, BSN_LICENSE_TEXT, bs_format, license_text.getBytes());
	
	// set the RDF bitstream
	setBitstreamFromBytes(item, bundle, BSN_LICENSE_RDF, bs_format, document_rdf.getBytes());
    }

    public static String getLicenseURL(Item item)
        throws SQLException, IOException, AuthorizeException
    {
	return getStringFromBitstream(item, BSN_LICENSE_URL);
    }

    public static String getLicenseText(Item item)
	throws SQLException, IOException, AuthorizeException
    {
	return getStringFromBitstream(item, BSN_LICENSE_TEXT);
    }

    public static String getLicenseRDF(Item item)
	throws SQLException, IOException, AuthorizeException
    {
	return getStringFromBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get a few license-specific properties. We expect these to
     * be cached at least per server run.
     */

    public static String fetchLicenseText(String license_url) {
	String text_url = license_url;
	return new String(fetchURL(text_url));
    }

    public static String fetchLicenseRDF(String license_url) {
	String rdf_url = license_url + "rdf";
	return new String(fetchURL(rdf_url));
    }

    // The following two helper methods assume that the CC
    // bitstreams are short and easily expressed as byte arrays in RAM

    /**
     * This helper method
     * takes some bytes and stores them as a bitstream
     * for an item, under the CC bundle, with the given bitstream name
     **/
    private static void setBitstreamFromBytes(Item item, Bundle bundle, String bitstream_name, BitstreamFormat format, byte[] bytes) 
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
     * This helper method wraps a String around a byte array
     * returned from the bitstream method further down
     **/
    private static String getStringFromBitstream(Item item, String bitstream_name) 
	throws SQLException, IOException, AuthorizeException
    {
	if (!CC.isEnabled())
	    return null;

	byte[] bytes = getBytesFromBitstream(item, bitstream_name);

	if (bytes == null) {
	    return "no bytes";
	}

	String value = new String(bytes);
	return value;
    }

    /**
     * This helper method
     * retrieves the bytes of a bitstream for an item under the CC bundle,
     * with the given bitstream name
     **/
    private static byte[] getBytesFromBitstream(Item item, String bitstream_name) 
	throws SQLException, IOException, AuthorizeException
    {
	Bundle cc_bundle = null;

	// look for the CC bundle
	try {
	    cc_bundle = item.getBundles(CC_BUNDLE_NAME)[0];
	} catch (Exception exc) {
	    // this exception catching is a bit generic,
	    // but basically it happens if there is no CC bundle
	    return "no such bundle".getBytes();
	}

	Bitstream bs = cc_bundle.getBitstreamByName(bitstream_name);

	// no such bitstream
	if (bs == null) {
	    return "no such bitstream".getBytes();
	}

	// create a ByteArrayOutputStream
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	Utils.copy(bs.retrieve(), baos);
	return baos.toByteArray();
    }


    /**
     * Fetch the contents of a URL
     **/
    private static byte[] fetchURL(String url_string) {
	try {
	    URL url = new URL(url_string);
	    URLConnection connection = url.openConnection();
	    byte[] bytes = new byte[connection.getContentLength()];
	    // loop and read the data until it's done
	    int offset = 0;
	    while (true) {
		int len = connection.getInputStream().read(bytes, offset, bytes.length - offset);
		if (len == -1)
		    break;
		offset += len;
	    }		

	    return bytes;
	} catch (Exception exc) {
	    return null;
	}
    }
}
