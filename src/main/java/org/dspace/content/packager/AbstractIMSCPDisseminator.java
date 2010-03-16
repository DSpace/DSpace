/*
 * AbstractIMSCPDisseminator
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
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageUtils;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

/**
 * Base class for package disseminator of the IMS Content Package spec,
 * or IMSCP.
 * <p>
 * For more information about IMSCP, see IMS Global Learning Consortium,
 * http://www.imsglobal.org/content/packaging/
 * <p>
 * This is a generic packager framework intended to be subclassed to create
 * disseminators for more specific IMSCP "profiles".   IMSCP is an
 * abstract and flexible framework that can encompass many
 * different kinds of metadata and inner package structures each with its
 * own expectations and restrictions.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.content.packager.IMSCPManifest
 */
public abstract class AbstractIMSCPDisseminator
    implements PackageDisseminator
{
    /** log4j category */
    private static Logger log = Logger.getLogger(org.dspace.content.packager.AbstractIMSCPDisseminator.class);

    /**
     * Export the object (Item, Collection, or Community) to a
     * package file on the indicated OutputStream.
     * Gets an exception of the object cannot be packaged or there is
     * a failure creating the package.
     * <p>
     * Get the manifest by either:
     *  1. Finding the manifest bitstream saved from the ingested package
     *     by the ingester that is a companion to the subclass doing the
     *     dissemination (preferred).
     *  2. Synthesizing a manifest for this item.  This may not be
     *     implemented in all subclasses, in which case the dissemination
     *     fails with an exception.
     * <p>
     * After that, all the content and metadata bitstreams are simply
     * added to the Zip file.
     *
     * @param context - DSpace context.
     * @param dso - DSpace object (item, collection, etc)
     * @param pkg - output stream on which to write package
     * @throws PackageValidationException if package cannot be created or there is
     *  a fatal error in creating it.
     */
    public void disseminate(Context context, DSpaceObject dso,
                            PackageParameters params, java.io.OutputStream pkg)
        throws PackageException, CrosswalkException,
               AuthorizeException, java.sql.SQLException, java.io.IOException
    {
        if (dso.getType() == Constants.ITEM)
        {
            IMSCPManifest manifest = null;
            try
            {
                manifest = (IMSCPManifest)getManifestClass().newInstance();
            }
            catch (Exception e)
            {
                // very unlikely, but do not lose exception, repackage it:
                throw new PackageException(e);
            }
            Item item = (Item)dso;
            java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(pkg);
            zip.setComment("IMSCP package created by DSpace for item "+
                item.getHandle());

            // write manifest first.
            java.util.zip.ZipEntry me = new java.util.zip.ZipEntry(manifest.MANIFEST_FILE);
            zip.putNextEntry(me);

            // find manifest by its special bitstream format:
            BitstreamFormat mbsf = manifest.getManifestBitstreamFormat(context);
            Bitstream manifestBs = null;
            if (mbsf != null)
                manifestBs = PackageUtils.getBitstreamByFormat(item, mbsf, Constants.METADATA_BUNDLE_NAME);
            if (manifestBs == null)
            {
                Document manifestDoc = makeManifest(context, item, params);
                XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
                outputPretty.output(manifestDoc, zip);
            }
            else
                Utils.copy(manifestBs.retrieve(), zip);
            zip.closeEntry();

            // copy all non-meta bitstreams into zip
            // also be sure not to write a duplicate of the manifest.
            Bundle bundles[] = item.getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                if (bundles[i].getName().equals(Constants.CONTENT_BUNDLE_NAME) ||
                    bundles[i].getName().equals(Constants.METADATA_BUNDLE_NAME))
                {
                    Bitstream[] bitstreams = bundles[i].getBitstreams();
                    for (int k = 0; k < bitstreams.length; k++)
                    {
                        String zname = bitstreams[k].getName();
                        if (!zname.equals(IMSCPManifest.MANIFEST_FILE))
                        {
                            java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry(zname);
                            ze.setSize(bitstreams[k].getSize());
                            zip.putNextEntry(ze);
                            Utils.copy(bitstreams[k].retrieve(), zip);
                            zip.closeEntry();
                        }
                    }
                }
            }
            zip.close();
        }
        else
            throw new PackageValidationException("Can only disseminate an Item now.");
    }

    /**
     * Return the MIME type.  If we implement the metadataOnly option,
     * it'll be an XML document; otherwise IMSCP packages are Zip files.
     */
    public String getMIMEType(PackageParameters params)
    {
        return (params != null && params.getProperty("metadataOnly") != null) ?
                "text/xml" : "application/zip";
    }

    /**
     * Create a manifest for this item as an XML document.
     * Subclass can thrown an exception if it does not implement this method.
     */
    abstract public Document makeManifest(Context context, Item item, PackageParameters params)
        throws PackageException;

     /**  Get flavor of manifest used by this disseminator.
      */
    abstract public Class getManifestClass();
}
