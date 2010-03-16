/*
 * AbstractIMSCPIngester
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
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageUtils;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Base class for package ingester of the IMS Content Package spec, or
 * IMSCP.
 * <p>
 * For more information about IMSCP, see IMS Global Learning Consortium,
 * http://www.imsglobal.org/content/packaging/
 * <p>
 * This is a generic packager framework intended to be subclassed to create
 * ingesters for more specific IMSCP "profiles".   IMSCP is an
 * abstract and flexible framework that can encompass many
 * different kinds of metadata and inner package structures each with its
 * own expectations and restrictions.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.content.packager.IMSCPManifest
 */
public abstract class AbstractIMSCPIngester
       implements PackageIngester
{
    /** log4j category */
    private static Logger log = Logger.getLogger(org.dspace.content.packager.AbstractIMSCPIngester.class);

    // default choice whether to validate the manifest
    private static final boolean validateDefault = false;

    /**
     * Create a new DSpace item out of an IMSCP content package.
     * All contents are dictated by the IMSCP manifest.
     * Package is a ZIP archive, all files relative to top level
     * and the manifest (as per spec) in imsmanifest.xml.
     *
     * @param context - DSpace context.
     * @param collection - collection under which to create new item.
     * @param pkg - input stream containing package to ingest.
     * @param license - may be null, which takes default license.
     * @return workspace item created by ingest.
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     */
    public WorkspaceItem ingest(Context context, Collection collection,
                                java.io.InputStream pkg, PackageParameters params,
                                String license)
        throws PackageException, CrosswalkException,
               AuthorizeException, java.sql.SQLException, java.io.IOException
    {
        java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(pkg);
        java.util.HashMap fileIdToBitstream = new java.util.HashMap();
        WorkspaceItem wi = null;
        boolean success = false;
        java.util.HashSet packageFiles = new java.util.HashSet();

        boolean validate = params.getBooleanProperty("validate", validateDefault);

        try
        {
            /* 1. Read all the files in the Zip into bitstreams first,
             *  because we only get to take one pass through a Zip input
             *  stream.  Give them temporary bitstream names corresponding
             *  to the same names they had in the Zip, since those MUST
             *  match the URL references in <Flocat> and <mdRef> elements.
             *  ..Along the way, parse and sanity-check the manifest too.
             */
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
            wi = WorkspaceItem.create(context, collection, false);
            Item item = wi.getItem();
            Bundle contentBundle = item.createBundle(Constants.CONTENT_BUNDLE_NAME);
            Bundle mdBundle = item.createBundle(Constants.METADATA_BUNDLE_NAME);
            java.util.zip.ZipEntry ze;

            while ((ze = zip.getNextEntry()) != null)
            {
                if (ze.isDirectory())
                    continue;
                Bitstream bs = null;
                String fname = ze.getName();
                if (fname.equals(manifest.MANIFEST_FILE))
                {
                    bs = mdBundle.createBitstream(new PackageUtils.UnclosableInputStream(zip));
                    bs.setName(fname);
                    bs.setFormat(manifest.getManifestBitstreamFormat(context));
                    manifest.parse(bs.retrieve(), validate);
                }
                else
                {
                    bs = contentBundle.createBitstream(new PackageUtils.UnclosableInputStream(zip));
                    // name is needed for guessing format..
                    bs.setName(fname);
                    bs.setFormat(FormatIdentifier.guessFormat(context, bs));
                }
                packageFiles.add(fname);
                bs.setSource(fname);
                bs.update();
            }
            zip.close();

            // ensure manifest was read and that it passes sanity check.
            manifest.checkManifest();

            /* 2. Grovel a file list out of IMSCP Manifest and compare
             *  it to the files in package, as an integrity test.  Extra
             *  package files are allowed but if a file mentioned in the
             *  manifest is missing, it is a fatal error.
             *  At the end of this stage, packageFiles _should_ be empty
             *  (contains only files not mentioned in manifest), and
             *  missingFiles _must_ be empty (it contains manifest files
             *  not found in the package).
             */

            // Compare manifest files with the ones found in package:
            //  a. Start with content files (mentioned in <resource> elements)
            java.util.HashSet missingFiles = new java.util.HashSet();
            for (java.util.Iterator mi = manifest.getContentFiles().iterator(); mi.hasNext(); )
            {
                String path = (String)mi.next();
                if (packageFiles.contains(path))
                    packageFiles.remove(path);
                else
                    missingFiles.add(path);
            }

            //  b. Process files mentioned in <adlcp:location>s - check and move
            //     to METADATA bundle.
            for (java.util.Iterator mi = manifest.getMetadataFiles().iterator(); mi.hasNext(); )
            {
                String path = (String)mi.next();
                if (packageFiles.contains(path))
                    packageFiles.remove(path);
                else
                    missingFiles.add(path);

                // if there is a bitstream with that name in the Content
                // bundle, move it to the Metadata bundle:
                Bitstream mdbs = contentBundle.getBitstreamByName(path);
                if (mdbs != null)
                {
                    mdBundle.addBitstream(mdbs);
                    contentBundle.removeBitstream(mdbs);
                }
            }

            // KLUDGE: make sure Manifest file doesn't get flagged as
            // extra, since it won't be mentioned in the manifest.
            if (packageFiles.contains(IMSCPManifest.MANIFEST_FILE))
                packageFiles.remove(IMSCPManifest.MANIFEST_FILE);

            // Give subclass a chance to refine the lists of in-package
            // and missing files, delete extraneous files, etc.
            checkPackageFiles(packageFiles, missingFiles, manifest);

            // Any missing file is a fatal error:
            if (!missingFiles.isEmpty())
            {
                StringBuffer msg = new StringBuffer("Package is unacceptable: it is missing these files listed in Manifest:");
                for (java.util.Iterator mi = missingFiles.iterator(); mi.hasNext(); )
                    msg.append("\n\t"+(String)mi.next());
                throw new PackageValidationException(msg.toString());
            }

            // Extra files are allowed in IMSCP package:
            if (!packageFiles.isEmpty())
            {
                StringBuffer msg = new StringBuffer("Package is malformed, but still acceptable:  it contains extra files NOT in manifest:");
                for (java.util.Iterator mi = packageFiles.iterator(); mi.hasNext(); )
                    msg.append("\n\t"+(String)mi.next());
                log.warn(msg);
            }

            /* 3. crosswalk the metadata
             */
            crosswalk(context, item, manifest, validate);

            // Sanity-check the resulting metadata on the Item:
            PackageUtils.checkMetadata(item);

            /* 4. Set primary bitstream; same Bundle
             */
            String pbsPath = manifest.getPrimaryBitstreamPath();
            if (pbsPath == null)
                log.warn("Failed to find primary bitstream path in Manifest.");
            else
            {
                Bitstream pbs = contentBundle.getBitstreamByName(pbsPath);
                if (pbs == null)
                    log.error("Got Primary Bitstream file path="+pbsPath+
                             ", but found no corresponding bitstream.");
                else
                    contentBundle.setPrimaryBitstreamID(pbs.getID());
            }

            // have subclass manage license since it may be extra package file.
            addLicense(context, collection, item, manifest, license );

            // subclass hook for final checks and rearrangements
            finishItem(context, item, collection);

            // commit all changes (bundles must update explicitly)
            contentBundle.update();
            mdBundle.update();
            wi.update();
            success = true;
            log.info(LogManager.getHeader(context, "ingest",
                "Created new Item, db ID="+String.valueOf(item.getID())+
                ", WorkspaceItem ID="+String.valueOf(wi.getID())));
            return wi;
        }
        catch (java.sql.SQLException se)
        {
            // disable attempt to delete the workspace object, since
            // database may have suffered a fatal error and the
            // transaction rollback will get rid of it anyway.
            wi = null;

            // Pass this exception on to the next handler.
            throw se;
        }
        finally
        {
            // kill item (which also deletes bundles, bitstreams) if ingest fails
            if (!success && wi != null)
                wi.deleteAll();
        }
    }

    /**
     * XXX FIXME Replace is not implemented yet.
     */
    public Item replace(Context ctx, Item item, java.io.InputStream pckage, PackageParameters params)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               java.sql.SQLException, java.io.IOException
    {
        throw new UnsupportedOperationException("The replace operation is not implemented.");
    }

    /**  Get flavor of manifest used by this ingester. */
    abstract public Class getManifestClass();

    /**
     * Hook for subclass to modify the test of the package's
     * integrity, and add other tests. E.g. evaluate a PGP signature of
     * the manifest in a separate file.
     * <p>
     * The <code>packageFiles</code> contains "extra" files that were in
     * the package but were not referenced by the IMSCP manifest.
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
     * @param packageFiles files in package but not referenced by manifest.
     * @param missingFiles files referenced by manifest but not in package
     *
     */
    abstract public void checkPackageFiles(java.util.Set packageFiles, java.util.Set missingFiles,
                                           IMSCPManifest manifest)
        throws PackageValidationException, CrosswalkException;

    /**
     * Crosswalk item's descriptive (and other Item-level) metadata
     * from the manifest.
     */
    abstract public void crosswalk(Context context, Item item,
                                   IMSCPManifest manifest, boolean validate)
        throws PackageException, CrosswalkException,
               AuthorizeException, java.sql.SQLException, java.io.IOException;

    /**
     * Add license(s) to Item based on contents of manifest and other policies.
     * The implementation of this method controls exactly what licenses
     * are added to the new item, including the DSpace deposit license.
     * It is given the collection (which is the source of a default deposit
     * license), an optional user-supplied deposit license (in the form of
     * a String).
     * It can also apply a Creative Commons license.
     * <p>
     * This framework does not add any licenses by default.
     *
     * @param context the DSpace context
     * @param collection DSpace Collection to which the item is being submitted.
     * @param license optional user-supplied Deposit License text (may be null)
     */
    abstract public void addLicense(Context context, Collection collection,
                                    Item item, IMSCPManifest manifest,
                                    String license)
        throws PackageException, CrosswalkException,
               AuthorizeException, java.sql.SQLException, java.io.IOException;

    /**
     * Hook for final "finishing" operations on the new Item.
     * This method is called when the new Item is otherwise complete and
     * ready to be returned.  The implementation should use this
     * opportunity to make whatever final checks and modifications are
     * necessary.
     *
     * @param context the DSpace context
     * @param item the item
     * @param collection the parent collection to-be
     */
    abstract public void finishItem(Context context, Item item, Collection collection)
        throws PackageException, CrosswalkException,
         AuthorizeException, java.sql.SQLException, java.io.IOException;
}
