/*
 * AbstractMETSIngester
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.dspace.app.mediafilter.MediaFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.jdom.Element;

/**
 * Base class for package ingester of
 * METS (Metadata Encoding & Transmission Standard) Package.<br>
 *   See <a href="http://www.loc.gov/standards/mets/">http://www.loc.gov/standards/mets/</a>
 * <p>
 * This is a generic packager framework intended to be subclassed to create
 * ingesters for more specific METS "profiles".   METS is an
 * abstract and flexible framework that can encompass many
 * different kinds of metadata and inner package structures.
 * <p>
 * <b>Configuration:</b>
 * If the property <code>mets.submission.preserveManifest</code> is <em>true</em>,
 * the METS manifest itself is preserved in a bitstream named
 * <code>mets.xml</code> in the <code>METADATA</code> bundle.  If it is
 * <em>false</em> (the default), the manifest is discarded after ingestion.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.content.packager.METSManifest
 */
public abstract class AbstractMETSIngester
       implements PackageIngester
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AbstractMETSIngester.class);

    /** Filename of manifest, relative to package toplevel. */
    public static final String MANIFEST_FILE = "mets.xml";

    // bitstream format name of magic METS SIP format..
    private static final String MANIFEST_BITSTREAM_FORMAT =
            "DSpace METS SIP";

    // value of mets.submission.preserveManifest config key
    private static final boolean preserveManifest =
        ConfigurationManager.getBooleanProperty("mets.submission.preserveManifest", false);

    /**
     * An instance of MdrefManager holds the state needed to
     * retrieve the contents (or bitstream corresponding to) an
     * external metadata stream referenced by an <code>mdRef</code>
     * element in the METS manifest.
     * <p>
     * Initialize it with the DSpace Bundle containing all of the
     * metadata bitstreams.  Match an mdRef by finding the bitstream
     * with the same name.
     */
    protected class MdrefManager
        implements METSManifest.Mdref
    {
    private Bundle mdBundle = null;

        // constructor initializes metadata bundle.
        private MdrefManager(Bundle mdBundle)
        {
            super();
            this.mdBundle = mdBundle;
        }

        /**
         * Find the local Bitstream referenced in
         * an <code>mdRef</code> element.
         * @param mdref the METS mdRef element to locate the bitstream for.
         * @return bitstream or null if none found.
         */
        public Bitstream getBitstreamForMdRef(Element mdref)
            throws MetadataValidationException, IOException, SQLException, AuthorizeException
        {
            String path = METSManifest.getFileName(mdref);
            if (mdBundle == null)
                throw new MetadataValidationException("Failed referencing mdRef element, because there were no metadata files.");
            return mdBundle.getBitstreamByName(path);
        }
         
        /**
         * Make the contents of an external resource mentioned in
         * an <code>mdRef</code> element available as an <code>InputStream</code>.
         * See the <code>METSManifest.MdRef</code> interface for details.
         * @param mdref the METS mdRef element to locate the input for.
         * @return the input stream of its content.
         */
        public InputStream getInputStream(Element mdref)
            throws MetadataValidationException, IOException, SQLException, AuthorizeException
        {
            Bitstream mdbs = getBitstreamForMdRef(mdref);
            if (mdbs == null)
                throw new MetadataValidationException("Failed dereferencing bitstream for mdRef element="+mdref.toString());
            return mdbs.retrieve();
        }
    }

    /**
     * Create a new DSpace item out of a METS content package.
     * All contents are dictated by the METS manifest.
     * Package is a ZIP archive, all files relative to top level
     * and the manifest (as per spec) in mets.xml.
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
                                InputStream pkg, PackageParameters params,
                                String license)
        throws PackageValidationException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        ZipInputStream zip = new ZipInputStream(pkg);
        HashMap fileIdToBitstream = new HashMap();
        WorkspaceItem wi = null;
        boolean success = false;
        HashSet packageFiles = new HashSet();

        boolean validate = params.getBooleanProperty("validate", true);

        try
        {
            /* 1. Read all the files in the Zip into bitstreams first,
             *  because we only get to take one pass through a Zip input
             *  stream.  Give them temporary bitstream names corresponding
             *  to the same names they had in the Zip, since those MUST
             *  match the URL references in <Flocat> and <mdRef> elements.
             */
            METSManifest manifest = null;
            wi = WorkspaceItem.create(context, collection, false);
            Item item = wi.getItem();
            Bundle contentBundle = item.createBundle(Constants.CONTENT_BUNDLE_NAME);
            Bundle mdBundle = null;
            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null)
            {
                if (ze.isDirectory())
                    continue;
                Bitstream bs = null;
                String fname = ze.getName();
                if (fname.equals(MANIFEST_FILE))
                {
                    if (preserveManifest)
                    {
                        mdBundle = item.createBundle(Constants.METADATA_BUNDLE_NAME);
                        bs = mdBundle.createBitstream(new PackageUtils.UnclosableInputStream(zip));
                        bs.setName(fname);
                        bs.setSource(fname);

                        // Get magic bitstream format to identify manifest.
                        BitstreamFormat manifestFormat = null;
                        manifestFormat = PackageUtils.findOrCreateBitstreamFormat(context,
                             MANIFEST_BITSTREAM_FORMAT, "application/xml",
                             MANIFEST_BITSTREAM_FORMAT+" package manifest");
                        bs.setFormat(manifestFormat);

                        manifest = METSManifest.create(bs.retrieve(), validate);
                    }
                    else
                    {
                        manifest = METSManifest.create(new PackageUtils.UnclosableInputStream(zip), validate);
                        continue;
                    }
                }
                else
                {
                    bs = contentBundle.createBitstream(new PackageUtils.UnclosableInputStream(zip));
                    bs.setSource(fname);
                    bs.setName(fname);
                }
                packageFiles.add(fname);
                bs.setSource(fname);
                bs.update();
            }
            zip.close();

            if (manifest == null)
                throw new PackageValidationException("No METS Manifest found (filename="+MANIFEST_FILE+").  Package is unacceptable.");

            // initial sanity checks on manifest (in subclass)
            checkManifest(manifest);

            /* 2. Grovel a file list out of METS Manifest and compare
             *  it to the files in package, as an integrity test.
             */
            List manifestContentFiles = manifest.getContentFiles();

            // Compare manifest files with the ones found in package:
            //  a. Start with content files (mentioned in <fileGrp>s)
            HashSet missingFiles = new HashSet();
            for (Iterator mi = manifestContentFiles.iterator(); mi.hasNext(); )
            {
                // First locate corresponding Bitstream and make
                // map of Bitstream to <file> ID.
                Element mfile = (Element)mi.next();
                String mfileId = mfile.getAttributeValue("ID");
                if (mfileId == null)
                    throw new PackageValidationException("Invalid METS Manifest: file element without ID attribute.");
                String path = METSManifest.getFileName(mfile);
                Bitstream bs = contentBundle.getBitstreamByName(path);
                if (bs == null)
                {
                    log.warn("Cannot find bitstream for filename=\""+path+
                             "\", skipping it..may cause problems later.");
                    missingFiles.add(path);
                }
                else
                {
                    fileIdToBitstream.put(mfileId, bs);

                    // Now that we're done using Name to match to <file>,
                    // set default bitstream Name to last path element;
                    // Zip entries all have '/' pathname separators
                    // NOTE: set default here, hopefully crosswalk of
                    // a bitstream techMD section will override it.
                    String fname = bs.getName();
                    int lastSlash = fname.lastIndexOf('/');
                    if (lastSlash >= 0  && lastSlash+1 < fname.length())
                        bs.setName(fname.substring(lastSlash+1));

                    // Set Default bitstream format:
                    //  1. attempt to guess from MIME type
                    //  2. if that fails, guess from "name" extension.
                    String mimeType = mfile.getAttributeValue("MIMETYPE");
                    BitstreamFormat bf = (mimeType == null) ? null :
                            BitstreamFormat.findByMIMEType(context, mimeType);
                    if (bf == null)
                        bf = FormatIdentifier.guessFormat(context, bs);
                    bs.setFormat(bf);

                    // if this bitstream belongs in another Bundle, move it:
                    String bundleName = manifest.getBundleName(mfile);
                    if (!bundleName.equals(Constants.CONTENT_BUNDLE_NAME))
                    {
                        Bundle bn;
                        Bundle bns[] = item.getBundles(bundleName);
                        if (bns != null && bns.length > 0)
                            bn = bns[0];
                        else
                            bn = item.createBundle(bundleName);
                        bn.addBitstream(bs);
                        contentBundle.removeBitstream(bs);
                    }

                    // finally, build compare lists by deleting matches.
                    if (packageFiles.contains(path))
                        packageFiles.remove(path);
                    else
                        missingFiles.add(path);
                }
            }

            //  b. Process files mentioned in <mdRef>s - check and move
            //     to METADATA bundle.
            for (Iterator mi = manifest.getMdFiles().iterator(); mi.hasNext(); )
            {
                Element mdref = (Element)mi.next();
                String path = METSManifest.getFileName(mdref);

                // finally, build compare lists by deleting matches.
                if (packageFiles.contains(path))
                    packageFiles.remove(path);
                else
                    missingFiles.add(path);

                // if there is a bitstream with that name in Content, move
                // it to the Metadata bundle:
                Bitstream mdbs = contentBundle.getBitstreamByName(path);
                if (mdbs != null)
                {
                    if (mdBundle == null)
                        mdBundle = item.createBundle(Constants.METADATA_BUNDLE_NAME);
                    mdBundle.addBitstream(mdbs);
                    contentBundle.removeBitstream(mdbs);
                }
            }

            // KLUDGE: make sure Manifest file doesn't get flagged as missing
            // or extra, since it won't be mentioned in the manifest.
            if (packageFiles.contains(MANIFEST_FILE))
                packageFiles.remove(MANIFEST_FILE);

            // Give subclass a chance to refine the lists of in-package
            // and missing files, delete extraneous files, etc.
            checkPackageFiles(packageFiles, missingFiles, manifest);

            // Any discrepency in file lists is a fatal error:
            if (!(packageFiles.isEmpty() && missingFiles.isEmpty()))
            {
                StringBuffer msg = new StringBuffer("Package is unacceptable: contents do not match manifest.");
                if (!missingFiles.isEmpty())
                {
                    msg.append("\nPackage is missing these files listed in Manifest:");
                    for (Iterator mi = missingFiles.iterator(); mi.hasNext(); )
                        msg.append("\n\t"+(String)mi.next());
                }
                if (!packageFiles.isEmpty())
                {
                    msg.append("\nPackage contains extra files NOT in manifest:");
                    for (Iterator mi = packageFiles.iterator(); mi.hasNext(); )
                        msg.append("\n\t"+(String)mi.next());
                }
                throw new PackageValidationException(msg.toString());
            }

            /* 3. crosswalk the metadata
             */
            // get mdref'd streams from "callback" object.
            MdrefManager callback = new MdrefManager(mdBundle);

            chooseItemDmd(context, item, manifest, callback, manifest.getItemDmds());

            // crosswalk content bitstreams too.
            for (Iterator ei = fileIdToBitstream.entrySet().iterator();
                 ei.hasNext();)
            {
                Map.Entry ee = (Map.Entry)ei.next();
                manifest.crosswalkBitstream(context, (Bitstream)ee.getValue(),
                                        (String)ee.getKey(), callback);
            }

            // Take a second pass over files to correct names of derived files
            // (e.g. thumbnails, extracted text) to what DSpace expects:
            for (Iterator mi = manifestContentFiles.iterator(); mi.hasNext(); )
            {
                Element mfile = (Element)mi.next();
                String bundleName = manifest.getBundleName(mfile);
                if (!bundleName.equals(Constants.CONTENT_BUNDLE_NAME))
                {
                    Element origFile = manifest.getOriginalFile(mfile);
                    if (origFile != null)
                    {
                        String ofileId = origFile.getAttributeValue("ID");
                        Bitstream obs = (Bitstream)fileIdToBitstream.get(ofileId);
                        String newName = makeDerivedFilename(bundleName, obs.getName());
                        if (newName != null)
                        {
                            String mfileId = mfile.getAttributeValue("ID");
                            Bitstream bs = (Bitstream)fileIdToBitstream.get(mfileId);
                            bs.setName(newName);
                            bs.update();
                        }
                    }
                }
            }

            // Sanity-check the resulting metadata on the Item:
            PackageUtils.checkMetadata(item);

            /* 4. Set primary bitstream; same Bundle
             */
            Element pbsFile = manifest.getPrimaryBitstream();
            if (pbsFile != null)
            {
                Bitstream pbs = (Bitstream)fileIdToBitstream.get(pbsFile.getAttributeValue("ID"));
                if (pbs == null)
                    log.error("Got Primary Bitstream file ID="+pbsFile.getAttributeValue("ID")+
                             ", but found no corresponding bitstream.");
                else
                {
                    Bundle bn[] = pbs.getBundles();
                    if (bn.length > 0)
                        bn[0].setPrimaryBitstreamID(pbs.getID());
                    else
                        log.error("Sanity check, got primary bitstream without any parent bundle.");
                }
            }

            // have subclass manage license since it may be extra package file.
            addLicense(context, collection, item, manifest, callback, license );

            // subclass hook for final checks and rearrangements
            finishItem(context, item);

            // commit any changes to bundles
            Bundle allBn[] = item.getBundles();
            for (int i = 0; i < allBn.length; ++i)
            {
                allBn[i].update();
            }

            wi.update();
            success = true;
            log.info(LogManager.getHeader(context, "ingest",
                "Created new Item, db ID="+String.valueOf(item.getID())+
                ", WorkspaceItem ID="+String.valueOf(wi.getID())));
            return wi;
        }
        catch (SQLException se)
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
    public Item replace(Context ctx, Item item, InputStream pckage, PackageParameters params)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               SQLException, IOException
    {
        throw new UnsupportedOperationException("The replace operation is not implemented.");
    }

    // return name of derived file as if MediaFilter created it, or null
    private String makeDerivedFilename(String bundleName, String origName)
    {
        // get the MediaFilter that would create this bundle:
        String mfNames[] = PluginManager.getAllPluginNames(MediaFilter.class);

        for (int i = 0; i < mfNames.length; ++i)
        {
            MediaFilter mf = (MediaFilter)PluginManager.getNamedPlugin(MediaFilter.class, mfNames[i]);
            if (bundleName.equals(mf.getBundleName()))
                return mf.getFilteredName(origName);
        }
        return null;
    }

    /**
     * Profile-specific tests to validate manifest.  The implementation
     * can access the METS document through the <code>manifest</code>
     * variable, an instance of <code>METSManifest</code>.
     * @throws MetadataValidationException if there is a fatal problem with the METS document's conformance to the expected profile.
     */
    abstract void checkManifest(METSManifest manifest)
        throws MetadataValidationException;

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
    abstract public void checkPackageFiles(Set packageFiles, Set missingFiles,
                                           METSManifest manifest)
        throws PackageValidationException, CrosswalkException;

    /**
     * Select the <code>dmdSec</code> element(s) to apply to the
     * Item.  The implementation is responsible for choosing which
     * (if any) of the metadata sections to crosswalk to get the
     * descriptive metadata for the item being ingested.  It is
     * responsible for calling the crosswalk, using the manifest's helper
     * i.e. <code>manifest.crosswalkItem(context,item,dmdElement,callback);</code>
     * (The final argument is a reference to itself since the
     * class also implements the <code>METSManifest.MdRef</code> interface
     * to fetch package files referenced by mdRef elements.)
     * <p>
     * Note that <code>item</code> and <code>manifest</code> are available
     * as protected fields from the superclass.
     *
     * @param context the DSpace context
     * @param dmds array of Elements, each a METS dmdSec that applies to the Item as a whole.
     *
     */
    abstract public void chooseItemDmd(Context context, Item item,
                                       METSManifest manifest, MdrefManager cb,
                                       Element dmds[])
        throws CrosswalkException,
               AuthorizeException, SQLException, IOException;

    /**
     * Add license(s) to Item based on contents of METS and other policies.
     * The implementation of this method controls exactly what licenses
     * are added to the new item, including the DSpace deposit license.
     * It is given the collection (which is the source of a default deposit
     * license), an optional user-supplied deposit license (in the form of
     * a String), and the METS manifest.  It should invoke
     * <code>manifest.getItemRightsMD()</code> to get an array of
     * <code>rightsMd</code> elements which might contain other license
     * information of interest, e.g. a Creative Commons license.
     * <p>
     * This framework does not add any licenses by default.
     *
     * @param context the DSpace context
     * @param collection DSpace Collection to which the item is being submitted.
     * @param license optional user-supplied Deposit License text (may be null)
     */
    abstract public void addLicense(Context context, Collection collection,
                                    Item item, METSManifest manifest,
                                    MdrefManager callback, String license)
        throws PackageValidationException, CrosswalkException,
               AuthorizeException, SQLException, IOException;

    /**
     * Hook for final "finishing" operations on the new Item.
     * This method is called when the new Item is otherwise complete and
     * ready to be returned.  The implementation should use this
     * opportunity to make whatever final checks and modifications are
     * necessary.
     *
     * @param context the DSpace context
     */
    abstract public void finishItem(Context context, Item item)
        throws PackageValidationException, CrosswalkException,
         AuthorizeException, SQLException, IOException;

}
