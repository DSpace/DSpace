/*
 * IMSCPManifest
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

package org.dspace.content.packager;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.core.Context;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;

/**
 * Models the IMSCP Manifest document.  The expected lifecycle of this
 * object: create a new instance for each manifest, call parse() to read it,
 * and then make queries about its contents.
 * <p>
 * For more information about IMSCP, see IMS Global Learning Consortium,
 * http://www.imsglobal.org/content/packaging/
 * <p>
 * Note that this is an abstract class.  The IMSCP manifest is a pretty
 * abstract concept; in use it will be tightly profiled so the
 * interpretation belongs in a subclass customized for that profile.
 * <p>
 * @author Larry Stone
 * @version $Revision$
 */
public abstract class IMSCPManifest
{
    /** Filename of manifest, relative to package toplevel. */
    public static final String MANIFEST_FILE = "imsmanifest.xml";

    /** root element of parsed manifest. */
    protected Element manifest = null;

    /** IMSCP namespace of root, which can vary. */
    protected Namespace imscp_ns = null;

    /** log4j category */
    private static Logger log = Logger.getLogger(org.dspace.content.packager.IMSCPManifest.class);

    // cached value of xml:base
    private String base = null;

    // cache of all the content-related files in package
    private java.util.Set contentFiles = null;

    /**
     * Parse the manifest, store the results for later queries.
     * @param docStream input stream with XML document on it.
     * @param validate true to do XML Schema validation.
     */
    public void parse(java.io.InputStream docStream, boolean validate)
        throws MetadataValidationException, java.io.IOException
    {
        try
        {
            SAXBuilder builder = new SAXBuilder(validate);
            if (validate)
                builder.setFeature("http://apache.org/xml/features/validation/schema", true);
            Document mdoc = builder.build(docStream);
            manifest = mdoc.getRootElement();
            imscp_ns = manifest.getNamespace();
        }
        catch (JDOMException je)
        {
            throw new MetadataValidationException("IMSCP Manifest is invalid: "+je.toString());
        }
    }

    /**
     * Return value of manifest's xml:base attribute, or "" if there is
     * none so caller can use "+" to assemble pathnames.
     * @return base URI, if any, or empty string.
     */
    public String getBase()
    {
        if (base != null)
            return base;
        if (manifest != null)
        {
            base = manifest.getAttributeValue("base", Namespace.XML_NAMESPACE);
            if (base == null)
                base = "";
            return base;
        }
        else
            return "";
    }

    /**
     * Get the "metadata" element of the manifest, throw exception
     * if it is not available.
     * @return metadata element, never null.
     */
    public Element getMetadata()
        throws MetadataValidationException
    {
        Element md = null;
        if (manifest != null)
            md = manifest.getChild("metadata", imscp_ns);
        if (md == null)
            throw new MetadataValidationException("Manifest is invalid, missing \"metadata\" element.");
        return md;
    }

    /**
     * Sanity-check a newly-parsed manifest; separate operation so
     * subclasses can override it.  Subclass should call super.checkManifest(); first.
     * Return nothing, throw on any failures.
     */
    public void checkManifest()
        throws MetadataValidationException
    {
        if (manifest == null)
            throw new MetadataValidationException("IMSCP Manifest is missing or was not parsed.");

        if (!manifest.getName().equals("manifest"))
            throw new MetadataValidationException("Invalid IMSCP Manifest: Root element of Manifest is not \"manifest\"");
    }


    /**
     * Get local (package) files referenced by manifest as containing
     * content.  Includes all files that must be expected to be in package.
     * <p>
     * Collect set of files named in "resources" element.
     * They are either (a) resource@href  or (b) resource/file@href
     * NOTE: since paths are stored in a Set, it doesn't hurt to
     * make duplicate entries, so just grab all possible paths.
     * <p>
     * If a subclass overrides this method it should call the "super"
     * version to combine that list of files with the extra files
     * defined by its profile.
     * <p>
     * @return Set of String pathnames (never null).
     */
    public java.util.Set getContentFiles()
        throws PackageValidationException
    {
        if (contentFiles != null)
            return contentFiles;
        contentFiles = new java.util.HashSet();
        Element rs = manifest.getChild("resources", imscp_ns);
        if (rs == null)
            throw new PackageValidationException("Malformed IMSCP manifest: no \"resources\" element.");
        java.util.Iterator rsli = rs.getChildren("resource", imscp_ns).iterator();
        while (rsli.hasNext())
        {
            Element r = (Element)rsli.next();
            String hpath = r.getAttributeValue("href");
            if (hpath != null && !hpath.startsWith("http:") && !hpath.startsWith("https:"))
            {
                contentFiles.add(getBase()+hpath);
                log.debug("Got resource@href path="+hpath);
            }

            // add any values of resource/file@href:
            java.util.Iterator fli = r.getChildren("file", imscp_ns).iterator();
            while (fli.hasNext())
            {
                Element f = (Element)fli.next();
                hpath = f.getAttributeValue("href");
                if (hpath != null && !hpath.startsWith("http:") && !hpath.startsWith("https:"))
                {
                    contentFiles.add(getBase()+hpath);
                    log.debug("Got resource/file@href path="+hpath);
                }
            }
        }
        return contentFiles;
    }

    /**
     * Get local (package) files referenced by manifest as containing
     * metadata. E.g. LOM for specific learning objects.
     * Subclasses should override this method since the default
     * is to return an empty set.
     * @return Set of String pathnames (never null).
     */
    public java.util.Set getMetadataFiles()
        throws MetadataValidationException
    {
        return new java.util.HashSet();
    }

    /**
     *  Return the local path of the top-level "resource" in
     *  the default IMS CP organization, which becomes the primary bitstream
     *  in the ingested Item.
     *  @return path of primary bitstream (or null if none found).
     */
    abstract String getPrimaryBitstreamPath()
        throws MetadataValidationException;

    /**
     *  Return the special bitstream format used to identify this
     *  particular (sub)class of IMSCP manifest.
     *  @return special bitstream format marking manifest (never null).
     */
    abstract public BitstreamFormat getManifestBitstreamFormat(Context context)
        throws java.sql.SQLException, AuthorizeException, java.io.IOException;
}
