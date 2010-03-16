/*
 * OCWIMSCPManifest
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
import org.dspace.content.packager.PackageUtils;
import org.dspace.core.Context;

import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Namespace;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

/**
 * Models the MIT Open CourseWare (OCW) profile of IMSCP developed for CWSpace.
 * Includes constants for all the relevant namespaces, as well as
 * code to locate the "home page" to use as the primary content bitstream.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.content.packager.IMSCPManifest
 */
public class OCWIMSCPManifest extends IMSCPManifest
{
    /** log4j category */
    private static Logger log = Logger.getLogger(org.dspace.content.packager.OCWIMSCPManifest.class);

    /** CWSpace namespace. */
    public static final String CWSP_NS_URI = "http://www.dspace.org/xmlns/cwspace_imscp";
    public static final Namespace CWSP_NS = Namespace.getNamespace("cwsp", CWSP_NS_URI);

    /** Open CourseWare (OCW) namespace. */
    public static final String OCW_NS_URI = "http://ocw.mit.edu/xmlns/ocw_imscp";
    public static final Namespace OCW_NS = Namespace.getNamespace("ocw", OCW_NS_URI);

    /** ADLCP namespace. */
    public static final String ADLCP_NS_URI = "http://www.adlnet.org/xsd/adlcp_rootv1p2";
    public static final Namespace ADLCP_NS = Namespace.getNamespace("adlcp", ADLCP_NS_URI);

    /**  Learning Object Metadata (LOM) namespace. */
    public static final String LOM_NS_URI = "http://ocw.mit.edu/xmlns/LOM";
    public static final Namespace LOM_NS = Namespace.getNamespace("lom", LOM_NS_URI);

    /** for checking against children of <metadata> */
    private static final String MD_SCHEMA = "IMS Content";
    private static final String MD_SCHEMAVERSION = "1.1";

    /** for checking CWSPACE schema tags */
    private static final String CWSP_SCHEMA = "CWSpace";
    private static final String CWSP_SCHEMAVERSION = "0.1";

    /**
     * Short bitstream format name we assign to the Manifest
     * so we can find it later:
     */
    private static final String MANIFEST_FMT_NAME = "MIT OCW IMSCP Manifest";

    // cached set of metadata related files in package
    private java.util.Set metadataFiles = null;

    /**
     * Sanity-check the manifest.
     * Check profile/schema identification tags,
     * @throw MetadataValidationException for any fatal flaw in manifest.
     */
    public void checkManifest()
        throws MetadataValidationException
    {
        super.checkManifest();

        // check /manifest/metadata/schema and metadata/schemaversion:
        Element md = getMetadata();
        String schema = md.getChildTextNormalize("schema", imscp_ns);
        String schemaVersion = md.getChildTextNormalize("schemaversion", imscp_ns);
        if (schema == null || schemaVersion == null ||
            !schema.equals(MD_SCHEMA) || !schemaVersion.equals(MD_SCHEMAVERSION))
            throw new MetadataValidationException("Manifest is invalid, wrong or missing schema, schemaversion (expect \""+MD_SCHEMA+"\", "+MD_SCHEMAVERSION+")");

        // check /manifest/metadata/cwsp:packageMetadata/profile, profileVersion
        Element cwmd = md.getChild("packageMetadata", CWSP_NS);
        if (cwmd == null)
            throw new MetadataValidationException("Manifest is invalid, no cwsp:packageMetadata element in metadata.");
        String cwProfile = cwmd.getChildTextNormalize("profile", CWSP_NS);
        String cwProfileVersion = cwmd.getChildTextNormalize("profileVersion", CWSP_NS);
        if (cwProfile == null || cwProfileVersion == null ||
            !cwProfile.equals(CWSP_SCHEMA) || !cwProfileVersion.equals(CWSP_SCHEMAVERSION))
            throw new MetadataValidationException("Manifest is invalid, wrong or missing CWSpace profile, profileVersion (expect \""+CWSP_SCHEMA+"\", "+CWSP_SCHEMAVERSION+")");
    }

    /**
     * Returns all of the local files of metadata called for in manifest.
     * Grovel adlcp:location links out of metadata elements for entire
     * package and for each resource.
     */
    public java.util.Set getMetadataFiles()
        throws MetadataValidationException
    {
        if (metadataFiles != null)
            return metadataFiles;
        metadataFiles = new java.util.HashSet();

        // 1. add metadata/adlcp:location if there is one:
        Element md = getMetadata();
        String mdloc = md.getChildTextNormalize("location", ADLCP_NS);
        if (mdloc != null && !mdloc.startsWith("http:") && !mdloc.startsWith("https:"))
            metadataFiles.add(getBase() + mdloc);

        // 2. grovel resources/resource/metadata for adlcp:location
        Element rs = manifest.getChild("resources", imscp_ns);
        if (rs == null)
            throw new MetadataValidationException("Malformed IMSCP manifest: no \"resources\" element.");
        java.util.Iterator rsli = rs.getChildren("resource", imscp_ns).iterator();
        while (rsli.hasNext())
        {
            Element resource = (Element)rsli.next();

            java.util.Iterator mi = resource.getChildren("metadata", imscp_ns).iterator();
            while (mi.hasNext())
            {
                Element m = (Element)mi.next();
                String path = m.getChildTextNormalize("location", ADLCP_NS);
                if (path != null && !path.startsWith("http:") && !path.startsWith("https:"))
                {
                    metadataFiles.add(getBase() + path);
                    log.debug("Got metadata/adlcp:location path="+path);
                 }
            }
        }
        return metadataFiles;
    }

    /**
     *  Return the local path of the top-level "resource" in
     *  the default IMS CP organization, which becomes the primary bitstream
     *  in the ingested Item.
     *  <p>
     *  NOTE: It is a fatal error for OCW IMSCP if no PBS is found.
     *  <p>
     *  First find the desired organization element, which is at xpath
     *  <code>/manifest/organizations/organization@identifier</code>
     *  matching <code>/manifest/organizations@default</code>.  Its top
     *  level item has a <code>item@identifierref</code> that matches the resource with attribute:
     *  <code>/resources/resource/@identifier</code>
     *
     *  @return path -- never null (throws if nothing found).
     */
    public String getPrimaryBitstreamPath()
        throws MetadataValidationException
    {
        Element orgs = manifest.getChild("organizations", imscp_ns);
        if (orgs == null)
            throw new MetadataValidationException("Malformed IMSCP manifest: no \"organizations\" element.");
        String defaultOrg = orgs.getAttributeValue("default");

        Element firstItem  = null;

        // Get *either* organization whose identifier matches "default"
        // _or_ the first one if there is no default.
        try
        {
            String orgPath = "imscp:organization/imscp:item[1]" ;

            if(defaultOrg != null)
                orgPath = "imscp:organization[@identifier='"+defaultOrg+"']/imscp:item[1]";
            
            XPath orgXPath = XPath.newInstance(orgPath);
            orgXPath.addNamespace("imscp", imscp_ns.getURI());
            firstItem  = (Element)orgXPath.selectSingleNode(orgs);
        }
        catch (JDOMException je)
        {
            throw new MetadataValidationException(je);
        }

        if (firstItem == null)
            throw new MetadataValidationException("Malformed IMSCP manifest: Failed to locate \"home page\" for primary bitstream, because either default organization or its first item cannot be found. Check validity of organizations element.");
        else
        {
            String resId = firstItem.getAttributeValue("identifierref");
            if (resId != null)
            {
                try
                {
                    String resPath = "/imscp:manifest/imscp:resources/imscp:resource[@identifier='"+resId+"']/imscp:file/@href";
                    XPath resXPath = XPath.newInstance(resPath);
                    resXPath.addNamespace("imscp", imscp_ns.getURI());
                    Attribute href = (Attribute) resXPath.selectSingleNode(manifest);
                    if (href != null)
                        return getBase() + href.getValue();
                    else
                        throw new MetadataValidationException("Malformed IMSCP manifest: No resource found for indicated identifierref, XPath="+resPath);
                }
                catch (JDOMException je)
                {
                    throw new MetadataValidationException(je);
                }
            }
            else
                throw new MetadataValidationException("Malformed IMSCP manifest: first item in the default organization has no identifierref attribute: "+firstItem.toString());
        }
    }

    /**
     *  Return the special bitstream format used to identify this
     *  (sub)class of IMSCP manifest.
     *  @return special bitstream format for manifest.
     */
    public BitstreamFormat getManifestBitstreamFormat(Context context)
        throws java.sql.SQLException, AuthorizeException, java.io.IOException
    {
        return PackageUtils.findOrCreateBitstreamFormat(context,
             MANIFEST_FMT_NAME, "application/xml", MANIFEST_FMT_NAME);
    }

}
