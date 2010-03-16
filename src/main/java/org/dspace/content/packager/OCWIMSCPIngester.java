/*
 * OCWIMSCPIngester
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
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.license.CreativeCommons;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;

/**
 * Ingester for MIT OpenCourseware (OCW)/CWSpace profile of the IMS
 * Content Package spec, or IMSCP.
 * <p>
 * For more information about IMSCP, see IMS Global Learning
 * Consortium, http://www.imsglobal.org/content/packaging/
 * <p>
 *
 * Configuration keys:
 *  1. Plugin name of LOM ingest crosswalk
 *      cwspace.crosswalk.ingest.lom = <plugin name>
 *   e.g.
 *      cwspace.crosswalk.ingest.lom = OCW-LOM
 *
 *  2. URL of CC license to apply to ingested OCW courses
 *      cwspace.ocw.creativecommons.url = "http:..."
 *    e.g.
 *      cwspace.ocw.creativecommons.url = http://creativecommons.org/licenses/by-nc-sa/2.5/
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.content.packager.AbstractIMSCPIngester
 */
public class OCWIMSCPIngester extends AbstractIMSCPIngester
{
    /** log4j category */
    private static Logger log = Logger.getLogger(org.dspace.content.packager.OCWIMSCPIngester.class);

    /** SCORM ADLCP namespace, used for adlcp:location elements */
    private static final Namespace ADLCP_NS = Namespace
            .getNamespace("adlcp", "http://www.adlnet.org/xsd/adlcp_rootv1p2");

    /**
     * Returns specific subclass of manifest for this profile.
     * @return the class object.
     */
    public Class getManifestClass()
    {
        return OCWIMSCPManifest.class;
    }

    /**
     * Hook for subclass to modify the test of the package's
     * integrity, and add other tests. E.g. evaluate a PGP signature of
     * the manifest in a separate file.
     * <p>
     * The <code>packageFiles</code> contains "extra" files that were in
     * the package but were not referenced by the METS manifest (either as
     * content or metadata (mdRefs)).
     * The implementation of this method should look for any "extra" files
     * uses (e.g. a checksum or cryptographic signature for the manifest
     * itself) and remove them from the Set.
     * <p>
     * The <code>missingFiles</code> set is for
     * any files
     * referenced by the manifest but not found in the package.
     * The implementation can check it for "false positives", or add
     * other missing files it knows of.
     * <p>
     * If either  of the Sets <code>missingFiles</code>
     * or <code>packageFiles</code>
     * is not empty, the ingest will fail.
     *
     * @param packageFiles files in package but not referenced by METS
     * @param missingFiles files referenced by manifest but not in package
     *
     */
    public void checkPackageFiles(java.util.Set packageFiles, java.util.Set missingFiles,
                                           IMSCPManifest manifest)
        throws PackageValidationException, CrosswalkException
    {
    }

    /**
     * Crosswalk item's descriptive (and other Item-level) metadata
     * from the manifest.
     *
     * The element at /manifest/metadata/adlcp:location contains a URI
     * pointing to a file in the package which contains a LOM document
     * that applies to the entire course.  Read this and pass it to the
     * configured crosswalk plugin.  That is the sole source of Item-level
     * descriptive metadata.
     */
    public void crosswalk(Context context, Item item, IMSCPManifest manifest, boolean validate)
        throws PackageException, CrosswalkException,
               AuthorizeException, java.sql.SQLException, java.io.IOException
    {
        Element md = manifest.getMetadata();
        String loc = md.getChildTextNormalize("location", ADLCP_NS);
        if (loc == null)
            throw new PackageValidationException("Invalid OCW-IMSCP manifest: Cannot find item LOM metadata, no element at manifest/metadata/adlcp:location.");
        loc = manifest.getBase()+loc;

        Bundle bns[] = item.getBundles(Constants.METADATA_BUNDLE_NAME);
        if (bns != null && bns.length > 0)
        {
            Bitstream mbs = bns[0].getBitstreamByName(loc);
            if (mbs == null)
                throw new PackageValidationException("Invalid OCW-IMSCP manifest: LOM metadata file \""+loc+"\" does not seem to be in the package.");

            Document mdd = null;
            try
            {
                SAXBuilder parser = new SAXBuilder(validate);
                if (validate)
                    parser.setFeature("http://apache.org/xml/features/validation/schema", true);
                mdd = parser.build(mbs.retrieve());
            }
            catch (JDOMException je)
            {
                throw new MetadataValidationException("Error parsing or validating metadata section referenced by adlcp:location from file=\""+loc+"\", exception="+je.toString());
            }

            String xwalkName = ConfigurationManager.getProperty("cwspace.crosswalk.ingest.lom");
            if (xwalkName == null)
                throw new PackageException("Configuration error: no value for \"cwspace.crosswalk.ingest.lom\"");
            IngestionCrosswalk xwalk = (IngestionCrosswalk)PluginManager.getNamedPlugin(IngestionCrosswalk.class, xwalkName);
            if (xwalk == null)
                throw new PackageException("Configuration error: cannot load ingestion crosswalk for "+xwalkName);

            xwalk.ingest(context, item, mdd.getRootElement());
        }
        else
            throw new PackageValidationException("Unacceptable OCW-IMSCP manifest: Cannot find bitstream of LOM metadata: "+loc);
    }


    /**
     * Add license(s) to Item based on contents of METS and other policies.
     * The implementation of this method controls exactly what licenses
     * are added to the new item, including the DSpace deposit license.
     * It is given the collection (which is the source of a default deposit
     * license), an optional user-supplied deposit license (in the form of
     * a String), and the IMSCP manifest.
     * <p>
     * In this implementation we simply add the default deposit license.
     * OCW content is available under a specific Creative Commons license,
     * so if the URL for that license is configured, set that using the
     * DSpace Creative Commons licensing mechanism.
     *
     * @param context the DSpace context
     * @param collection DSpace Collection to which the item is being submitted.
     * @param license optional user-supplied Deposit License text (may be null)
     */
    public void addLicense(Context context, Collection collection,
                                    Item item, IMSCPManifest manifest,
                                    String license)
        throws PackageException, CrosswalkException,
               AuthorizeException, java.sql.SQLException, java.io.IOException
    {
        PackageUtils.addDepositLicense(context, license, item, collection);

        // if we find a fixed Creative Commons URL, add it:
        String ccURL = ConfigurationManager.getProperty("cwspace.ocw.creativecommons.url");
        if (ccURL != null)
            CreativeCommons.setLicense(context, item, ccURL);
    }

    /**
     * Hook for final "finishing" operations on the new Item.
     * This method is called when the new Item is otherwise complete and
     * ready to be returned.  The implementation should use this
     * opportunity to make whatever final checks and modifications are
     * necessary.
     * <p>
     * Set the primary bitstream, so when this item appears in the
     * DSpace Web UI, it will have a link to the single course home page
     * instead of appearing as a confusing list of dozens of separate
     * content files.  This also ensures the HTML is rendered through
     * the "html" servlet, which is the only way to get relative
     * references to links and stylesheets, etc. to work in the DSpace
     * Web UI.
     *
     * It is an error if the Item cannot be supplied with a primary
     * bitstream, because of the aforementioned rendering problems.
     *
     * @param context the DSpace context
     * @param item the item object.
     */
    public void finishItem(Context context, Item item, Collection collection)
        throws PackageException, CrosswalkException,
         AuthorizeException, java.sql.SQLException, java.io.IOException
    {
        // check for duplicate of this package already in archive
        OCWDuplicateCheck(context, item, collection);

        // make sure there is a Primary Bitstream in the contnet bundle:
        Bundle bns[] = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
        if (bns != null && bns.length > 0)
        {
            if (bns[0].getPrimaryBitstreamID() < 0)
                throw new PackageValidationException("Invalid IMSCP manifest: Could not determine \"primary bitstream\" (home page) from manifest.");
        }
        else
            throw new PackageValidationException("Invalid IMSCP package: no content bitstreams.");
    }

    /**
     * Hack to check for OCW submitting the same course package again to
     * the same collection.  Test works as follows:
     *
     *  1. Upon successful ingest set the metadata field dc.identifier.other
     *     to the MD5 checksum of the IMSCP manifest file.
     *
     *  2. At every ingest attempt, search for another Item in the target
     *     Collection with a matching identifier.  If one is found, abort
     *     the ingest and report that it is rejected as a duplicate.
     *
     *  NOTE: The dc.identifier.other value is actually set to
     *    "IMSCP-MD5-" followed by the MD5 checksum in hex; the prefix
     *    serves to remind observers what that value is there for.
     *
     *    We use the MD5 of the IMSCP manifest because we need some
     *    globally unique identifier which is inherent in the package, and
     *    is absolutely reliable as a means of detecting duplicates.  The
     *    descriptive metadata in the package is susceptible to error.
     *    Since course packages are made once by OCW and submitted, possibly,
     *    through multiple tries, we can rely on the manifest bitstream
     *    embedded in the package remaining the same through later attempts.
     *
     *  DEPENDENCIES:
     *    a. Assumes the default search index configuration includes a
     *       field "ID" whose values are dc.identifier.*
     *    b. There is a dc.identifier.other metadata field (part of
     *       DSpace default configuration)
     *    c. Bitstream's getChecksum() method returns the MD5 and not some
     *       other kind of checksum.  This is unlikely to change.
     *
     */
    private void OCWDuplicateCheck(Context context, Item item, Collection collection)
        throws PackageException, java.io.IOException, java.sql.SQLException
    {
        final String idPrefix = "IMSCP-MD5-";

        Bundle mdb[] = item.getBundles(Constants.METADATA_BUNDLE_NAME);
        if (mdb.length > 0)
        {
            Bitstream manifest = mdb[0].getBitstreamByName(IMSCPManifest.MANIFEST_FILE);
            if (manifest != null)
            {
                String uniqueId = idPrefix + manifest.getChecksum();
                log.debug("Looking for duplicate Item of IMSCP package ID="+uniqueId);

                QueryArgs qa = new QueryArgs();
                qa.setQuery(uniqueId);
                QueryResults qr = DSQuery.doQuery(context, qa, collection);
                if (qr.getHitCount() > 0)
                {
                    java.util.List hh = qr.getHitHandles();
                    String hdl = (String)hh.get(0);
                    throw new PackageException("Duplicate Item Rejected: There is apparently already a duplicate of this Item in the target collection, handle="+hdl);
                }
                else
                {
                     item.addMetadata(MetadataSchema.DC_SCHEMA,
                                      "identifier", "other", null, uniqueId);
                }
            }
        }
    }
}
