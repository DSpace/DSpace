/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.jdom.Element;

/**
 * Base class for package ingester of METS (Metadata Encoding and Transmission
 * Standard) Packages.<br>
 * See <a href="http://www.loc.gov/standards/mets/">
 * http://www.loc.gov/standards/mets/</a>.
 * <p>
 * This is a generic packager framework intended to be subclassed to create
 * ingesters for more specific METS "profiles". METS is an abstract and flexible
 * framework that can encompass many different kinds of metadata and inner
 * package structures.
 * 
 * <p>
 * <b>Package Parameters:</b>
 * <ul>
 *   <li><code>validate</code> -- true/false attempt to schema-validate the METS
 * manifest.</li>
 *   <li><code>manifestOnly</code> -- package consists only of a manifest
 * document.</li>
 *   <li><code>ignoreHandle</code> -- true/false, ignore AIP's idea of handle
 * when ingesting.</li>
 *   <li><code>ignoreParent</code> -- true/false, ignore AIP's idea of parent
 * when ingesting.</li>
 * </ul>
 * <p>
 * <b>Configuration Properties:</b>
 * <ul>
 *   <li><code>mets.CONFIGNAME.ingest.preserveManifest</code> - if <em>true</em>,
 * the METS manifest itself is preserved in a bitstream named
 * <code>mets.xml</code> in the <code>METADATA</code> bundle. If it is
 * <em>false</em> (the default), the manifest is discarded after ingestion.</li>
 * 
 *   <li><code>mets.CONFIGNAME.ingest.manifestBitstreamFormat</code> - short name
 * of the bitstream format to apply to the manifest; MUST be specified when
 * preserveManifest is true.</li>
 * 
 *   <li><code>mets.default.ingest.crosswalk.MD_SEC_NAME</code> = PLUGIN_NAME
 * Establishes a default crosswalk plugin for the given type of metadata in a
 * METS mdSec (e.g. "DC", "MODS"). The plugin may be either a stream or
 * XML-oriented ingestion crosswalk. Subclasses can override the default mapping
 * with their own, substituting their configurationName for "default" in the
 * configuration property key above.</li>
 * 
 *   <li><code>mets.CONFIGNAME.ingest.useCollectionTemplate</code> - if
 * <em>true</em>, when an item is created, use the collection template. If it is
 * <em>false</em> (the default), any existing collection template is ignored.</li>
 * </ul>
 * 
 * @author Larry Stone
 * @author Tim Donohue
 * @version $Revision$
 * @see org.dspace.content.packager.METSManifest
 * @see AbstractPackageIngester
 * @see PackageIngester
 */
public abstract class AbstractMETSIngester extends AbstractPackageIngester
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AbstractMETSIngester.class);

    protected final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected final BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    protected final BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected final WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    /**
     * An instance of ZipMdrefManager holds the state needed to retrieve the
     * contents of an external metadata stream referenced by an
     * <code>mdRef</code> element in a Zipped up METS manifest.
     * <p>
     * Initialize it with the Content (ORIGINAL) Bundle containing all of the
     * metadata bitstreams. Match an mdRef by finding the bitstream with the
     * same name.
     */
    protected static final class MdrefManager implements METSManifest.Mdref
    {
        private File packageFile = null;

        private PackageParameters params;

        // constructor initializes from package file
        private MdrefManager(File packageFile, PackageParameters params)
        {
            super();
            this.packageFile = packageFile;
            this.params = params;
        }

        /**
         * Make the contents of an external resource mentioned in an
         * <code>mdRef</code> element available as an <code>InputStream</code>.
         * See the <code>METSManifest.MdRef</code> interface for details.
         * 
         * @param mdref
         *            the METS mdRef element to locate the input for.
         * @return the input stream of its content.
         * @throws MetadataValidationException if validation error
         * @throws IOException if IO error
         * @see METSManifest
         */
        @Override
        public InputStream getInputStream(Element mdref)
                throws MetadataValidationException, IOException
        {
            String path = METSManifest.getFileName(mdref);
            if (packageFile == null)
            {
                throw new MetadataValidationException(
                        "Failed referencing mdRef element, because there is no package specified.");
            }

            // Use the 'getFileInputStream()' method from the
            // AbstractMETSIngester to retrieve the inputstream for the
            // referenced external metadata file.
            return AbstractMETSIngester.getFileInputStream(packageFile, params,
                    path);
        }
    }// end MdrefManager class

    /**
     * Create a new DSpace object out of a METS content package. All contents
     * are dictated by the METS manifest. Package is a ZIP archive (or
     * optionally bare manifest XML document). In a Zip, all files relative to
     * top level and the manifest (as per spec) in mets.xml.
     * 
     * @param context
     *            DSpace context.
     * @param parent
     *            parent under which to create new object (may be null -- in
     *            which case ingester must determine parent from package or
     *            throw an error).
     * @param pkgFile
     *            The package file to ingest
     * @param params
     *            Properties-style list of options (interpreted by each
     *            packager).
     * @param license
     *            may be null, which takes default license.
     * @return DSpaceObject created by ingest.
     * 
     * @throws PackageValidationException if package validation error
     *             if package is unacceptable or there is a fatal error turning
     *             it into a DSpaceObject.
     * @throws CrosswalkException if crosswalk error
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws WorkflowException if workflow error
     */
    @Override
    public DSpaceObject ingest(Context context, DSpaceObject parent,
            File pkgFile, PackageParameters params, String license)
            throws PackageValidationException, CrosswalkException,
            AuthorizeException, SQLException, IOException, WorkflowException {
        // parsed out METS Manifest from the file.
        METSManifest manifest = null;

        // new DSpace object created
        DSpaceObject dso = null;

        try
        {
            log.info(LogManager.getHeader(context, "package_parse",
                    "Parsing package for ingest, file=" + pkgFile.getName()));

            // Parse our ingest package, extracting out the METS manifest in the
            // package
            manifest = parsePackage(context, pkgFile, params);

            // must have a METS Manifest to ingest anything
            if (manifest == null)
            {
                throw new PackageValidationException(
                        "No METS Manifest found (filename="
                                + METSManifest.MANIFEST_FILE
                                + ").  Package is unacceptable!");
            }

            // validate our manifest
            checkManifest(manifest);

            // if we are not restoring an object (i.e. we are submitting a new
            // object) then, default the 'ignoreHandle' option to true (as a new
            // object should get a new handle by default)
            if (!params.restoreModeEnabled()
                    && !params.containsKey("ignoreHandle"))
            { // ignore the handle in the manifest, and instead create a new
              // handle
                params.addProperty("ignoreHandle", "true");
            }

            // if we have a Parent Object, default 'ignoreParent' option to True
            // (this will ignore the Parent specified in manifest)
            if (parent != null && !params.containsKey("ignoreParent"))
            { // ignore the parent in the manifest, and instead use the
              // specified parent object
                params.addProperty("ignoreParent", "true");
            }

            // Actually ingest the object described by the METS Manifest
            dso = ingestObject(context, parent, manifest, pkgFile, params,
                    license);

            //if ingestion was successful
            if(dso!=null)
            {
                // Log whether we finished an ingest (create new obj) or a restore
                // (restore previously existing obj)
                String action = "package_ingest";
                if (params.restoreModeEnabled())
                {
                    action = "package_restore";
                }
                log.info(LogManager.getHeader(context, action,
                        "Created new Object, type="
                                + Constants.typeText[dso.getType()] + ", handle="
                                + dso.getHandle() + ", dbID="
                                + String.valueOf(dso.getID())));

                // Check if the Packager is currently running recursively.
                // If so, this means the Packager will attempt to recursively
                // ingest all referenced child packages.
                if (params.recursiveModeEnabled())
                {
                    // Retrieve list of all Child object METS file paths from the
                    // current METS manifest.
                    // This is our list of known child packages
                    String[] childFilePaths = manifest.getChildMetsFilePaths();

                    // Save this list to our AbstractPackageIngester (and note which
                    // DSpaceObject the pkgs relate to).
                    // NOTE: The AbstractPackageIngester itself will perform the
                    // recursive ingest call, based on these child pkg references
                    for (int i = 0; i < childFilePaths.length; i++)
                    {
                        addPackageReference(dso, childFilePaths[i]);
                    }
                }
            }//end if dso not null
            
            return dso;
        }
        catch (SQLException se)
        {
            // no need to really clean anything up,
            // transaction rollback will get rid of it anyway.
            dso = null;

            // Pass this exception on to the next handler.
            throw se;
        }
    }

    /**
     * Parse a given input package, ultimately returning the METS manifest out
     * of the package. METS manifest is assumed to be a file named 'mets.xml'
     * 
     * @param context
     *            DSpace Context
     * @param pkgFile
     *            package to parse
     * @param params
     *            Ingestion parameters
     * @return parsed out METSManifest
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws MetadataValidationException if metadata validation error
     */
    protected METSManifest parsePackage(Context context, File pkgFile,
            PackageParameters params) throws IOException, SQLException,
            AuthorizeException, MetadataValidationException
    {
        // whether or not to validate the METSManifest before processing
        // (default=false)
        // (Even though it's preferable to validate -- it's costly and takes a
        // lot of time, unless you cache schemas locally)
        boolean validate = params.getBooleanProperty("validate", false);

        // parsed out METS Manifest from the file.
        METSManifest manifest = null;

        // try to locate the METS Manifest in package
        // 1. read "package" stream: it will be either bare Manifest
        // or Package contents into bitstreams, depending on params:
        if (params.getBooleanProperty("manifestOnly", false))
        {
            // parse the bare METS manifest and sanity-check it.
            manifest = METSManifest.create(new FileInputStream(pkgFile),
                    validate, getConfigurationName());
        }
        else
        {
            try(ZipFile zip = new ZipFile(pkgFile))
            {
                // Retrieve the manifest file entry (named mets.xml)
                ZipEntry manifestEntry = zip.getEntry(METSManifest.MANIFEST_FILE);

                if(manifestEntry!=null)
                {
                    // parse the manifest and sanity-check it.
                    manifest = METSManifest.create(zip.getInputStream(manifestEntry),
                        validate, getConfigurationName());
                }
            }
        }

        // return our parsed out METS manifest
        return manifest;
    }

    /**
     * Ingest/import a single DSpace Object, based on the associated METS
     * Manifest and the parameters passed to the METSIngester
     * 
     * @param context
     *            DSpace Context
     * @param parent
     *            Parent DSpace Object
     * @param manifest
     *            the parsed METS Manifest
     * @param pkgFile
     *            the full package file (which may include content files if a
     *            zip)
     * @param params
     *            Parameters passed to METSIngester
     * @param license
     *            DSpace license agreement
     * @return completed result as a DSpace object
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws CrosswalkException if crosswalk error
     * @throws MetadataValidationException if metadata validation error
     * @throws WorkflowException if workflow error
     * @throws PackageValidationException if package validation error
     */
    protected DSpaceObject ingestObject(Context context, DSpaceObject parent,
            METSManifest manifest, File pkgFile, PackageParameters params,
            String license) throws IOException, SQLException,
            AuthorizeException, CrosswalkException,
            PackageValidationException, WorkflowException {
        // type of DSpace Object (one of the type constants)
        int type;

        // -- Step 1 --
        // Extract basic information (type, parent, handle) about DSpace object
        // represented by manifest
        type = getObjectType(manifest);

        // if no parent passed in (or ignoreParent is false),
        // attempt to determine parent DSpace object from manifest
        if (type != Constants.SITE
                && (parent == null || !params.getBooleanProperty(
                        "ignoreParent", false)))
        {
            try
            {
                // get parent object from manifest
                parent = getParentObject(context, manifest);
            }
            catch(UnsupportedOperationException e)
            {
                //If user specified to skip item ingest if any "missing parent" error message occur
                if(params.getBooleanProperty("skipIfParentMissing", false))
                {
                    //log a warning instead of throwing an error
                    log.warn(LogManager.getHeader(context, "package_ingest",
                            "SKIPPING ingest of object '" + manifest.getObjID()
                            + "' as parent DSpace Object could not be found. "
                            + "If you are running a recursive ingest, it is likely this object will be created as soon as its parent is created."));
                    //return a null object (nothing ingested as parent was missing)
                    return null;
                }
                else //else, throw exception upward to display to user
                    throw e;
            }

        }

        String handle = null;
        // if we are *not* ignoring the handle in manifest (i.e. ignoreHandle is
        // false)
        if (!params.getBooleanProperty("ignoreHandle", false))
        {
            // get handle from manifest
            handle = getObjectHandle(manifest);
        }

        // -- Step 2 --
        // Create our DSpace Object based on info parsed from manifest, and
        // packager params
        DSpaceObject dso;
        try
        {
            dso = PackageUtils.createDSpaceObject(context, parent,
                    type, handle, params);
        }
        catch (SQLException sqle)
        {
            throw new PackageValidationException("Exception while ingesting "
                    + pkgFile.getPath(), sqle);
        }

        // if we are uninitialized, throw an error -- something's wrong!
        if (dso == null)
        {
            throw new PackageValidationException(
                    "Unable to initialize object specified by package (type='"
                            + type + "', handle='" + handle + "' and parent='"
                            + parent.getHandle() + "').");
        }

        // -- Step 3 --
        // Run our Administrative metadata crosswalks!

        // initialize callback object which will retrieve external inputstreams
        // for any <mdRef>'s found in METS
        MdrefManager callback = new MdrefManager(pkgFile, params);

        // Crosswalk the sourceMD first, so that we make sure to fill in
        // submitter info (and any other initial applicable info)
        manifest.crosswalkObjectSourceMD(context, params, dso, callback);

        // Next, crosswalk techMD, digiprovMD, rightsMD
        manifest.crosswalkObjectOtherAdminMD(context, params, dso, callback);

        // -- Step 4 --
        // Run our Descriptive metadata (dublin core, etc) crosswalks!
        crosswalkObjectDmd(context, dso, manifest, callback, manifest
                .getItemDmds(), params);

        // For Items, also sanity-check the metadata for minimum requirements.
        if (type == Constants.ITEM)
        {
            PackageUtils.checkItemMetadata((Item) dso);
        }

        // -- Step 5 --
        // Add all content files as bitstreams on new DSpace Object
        if (type == Constants.ITEM)
        {
            Item item = (Item) dso;

            //Check if this item is still in a user's workspace.
            //It should be, as we haven't completed its install yet.
            WorkspaceItem wsi = workspaceItemService.findByItem(context, item);

            // Get collection this item is being submitted to
            Collection collection = item.getOwningCollection();
            if (collection == null)
            {
                // Get the collection this workspace item belongs to
                if (wsi != null)
                {
                    collection = wsi.getCollection();
                }
            }

            // save manifest as a bitstream in Item if desired
            if (preserveManifest())
            {
                addManifestBitstream(context, item, manifest);
            }

            // save all other bitstreams in Item
            addBitstreams(context, item, manifest, pkgFile, params, callback);

            // have subclass manage license since it may be extra package file.
            addLicense(context, item, license, collection, params);

            // Subclass hook for final checks and rearrangements
            // (this allows subclasses to do some final validation / changes as
            // necessary)
            finishObject(context, dso, params);
            
            // Finally, if item is still in the workspace, then we actually need
            // to install it into the archive & assign its handle.
            if(wsi!=null)
            {
                // Finish creating the item. This actually assigns the handle,
                // and will either install item immediately or start a workflow, based on params
                PackageUtils.finishCreateItem(context, wsi, handle, params);
            }

        } // end if ITEM
        else if (type == Constants.COLLECTION || type == Constants.COMMUNITY)
        {
            // Add logo if one is referenced from manifest
            addContainerLogo(context, dso, manifest, pkgFile, params);

            if(type==Constants.COLLECTION)
            {
                //Add template item if one is referenced from manifest (only for Collections)
                addTemplateItem(context, dso, manifest, pkgFile, params, callback);
            }
            
            // Subclass hook for final checks and rearrangements
            // (this allows subclasses to do some final validation / changes as
            // necessary)
            finishObject(context, dso, params);
        }// end if Community/Collection
        else if (type == Constants.SITE)
        {
            // Do nothing by default -- Crosswalks will handle anything necessary to ingest at Site-level
            
            // Subclass hook for final checks and rearrangements
            // (this allows subclasses to do some final validation / changes as
            // necessary)
            finishObject(context, dso, params);
        }
        else
        {
            throw new PackageValidationException(
                    "Unknown DSpace Object type in package, type="
                            + String.valueOf(type));
        }

        // -- Step 6 --
        // Finish things up!

        // Update the object to make sure all changes are committed
        PackageUtils.updateDSpaceObject(context, dso);

        return dso;
    }

    /**
     * Replace the contents of a single DSpace Object, based on the associated
     * METS Manifest and the parameters passed to the METSIngester.
     * 
     * @param context
     *            DSpace Context
     * @param dso
     *            DSpace Object to replace
     * @param manifest
     *            the parsed METS Manifest
     * @param pkgFile
     *            the full package file (which may include content files if a
     *            zip)
     * @param params
     *            Parameters passed to METSIngester
     * @param license
     *            DSpace license agreement
     * @return completed result as a DSpace object
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws CrosswalkException if crosswalk error
     * @throws MetadataValidationException if metadata validation error
     * @throws PackageValidationException if package validation error
     */
    protected DSpaceObject replaceObject(Context context, DSpaceObject dso,
            METSManifest manifest, File pkgFile, PackageParameters params,
            String license) throws IOException, SQLException,
            AuthorizeException, CrosswalkException,
            MetadataValidationException, PackageValidationException
    {
        // -- Step 1 --
        // Before going forward with the replace, let's verify these objects are
        // of the same TYPE! (We don't want to go around trying to replace a
        // COMMUNITY with an ITEM -- that's dangerous.)
        int manifestType = getObjectType(manifest);
        if (manifestType != dso.getType())
        {
            throw new PackageValidationException(
                    "The object type of the METS manifest ("
                            + Constants.typeText[manifestType]
                            + ") does not match up with the object type ("
                            + Constants.typeText[dso.getType()]
                            + ") of the DSpaceObject to be replaced!");
        }

        if (log.isDebugEnabled())
        {
            log.debug("Object to be replaced (handle=" + dso.getHandle()
                    + ") is " + Constants.typeText[dso.getType()] + " id="
                    + dso.getID());
        }

        // -- Step 2 --
        // Clear out current object (as we are replacing all its contents &
        // metadata)

        // remove all files attached to this object
        // (For communities/collections this just removes the logo bitstream)
        PackageUtils.removeAllBitstreams(context, dso);

        // clear out all metadata values associated with this object
        PackageUtils.clearAllMetadata(context, dso);

        // TODO -- We are currently NOT clearing out the following during a
        // replace.  So, even after a replace, the following information may be
        // retained in the system:
        // o  Rights/Permissions in system or on objects
        // o  Collection item templates or Content Source info (e.g. OAI
        //    Harvesting collections)
        // o  Item status (embargo, withdrawn) or mappings to other collections

        // -- Step 3 --
        // Run our Administrative metadata crosswalks!

        // initialize callback object which will retrieve external inputstreams
        // for any <mdRef>s found in METS
        MdrefManager callback = new MdrefManager(pkgFile, params);

        // Crosswalk the sourceMD first, so that we make sure to fill in
        // submitter info (and any other initial applicable info)
        manifest.crosswalkObjectSourceMD(context, params, dso, callback);

        // Next, crosswalk techMD, digiprovMD, rightsMD
        manifest.crosswalkObjectOtherAdminMD(context, params, dso, callback);

        // -- Step 4 --
        // Add all content files as bitstreams on new DSpace Object
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item) dso;

            // save manifest as a bitstream in Item if desired
            if (preserveManifest())
            {
                addManifestBitstream(context, item, manifest);
            }

            // save all other bitstreams in Item
            addBitstreams(context, item, manifest, pkgFile, params, callback);

            // have subclass manage license since it may be extra package file.
            Collection owningCollection = (Collection) ContentServiceFactory.getInstance().getDSpaceObjectService(dso).getParentObject(context, dso);
            if(owningCollection == null)
            {
                //We are probably dealing with an item that isn't archived yet
                InProgressSubmission inProgressSubmission = workspaceItemService.findByItem(context, item);
                if(inProgressSubmission == null)
                {
                    inProgressSubmission = WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(context, item);
                }
                owningCollection = inProgressSubmission.getCollection();
            }

            addLicense(context, item, license, owningCollection
                    , params);

            // FIXME ?
            // should set lastModifiedTime e.g. when ingesting AIP.
            // maybe only do it in the finishObject() callback for AIP.

        } // end if ITEM
        else if (dso.getType() == Constants.COLLECTION
                || dso.getType() == Constants.COMMUNITY)
        {
            // Add logo if one is referenced from manifest
            addContainerLogo(context, dso, manifest, pkgFile, params);
        } // end if Community/Collection
        else if (dso.getType() == Constants.SITE)
        {
            // Do nothing -- Crosswalks will handle anything necessary to replace at Site-level
        }

        // -- Step 5 --
        // Run our Descriptive metadata (dublin core, etc) crosswalks!
        crosswalkObjectDmd(context, dso, manifest, callback, manifest
                .getItemDmds(), params);

        // For Items, also sanity-check the metadata for minimum requirements.
        if (dso.getType() == Constants.ITEM)
        {
            PackageUtils.checkItemMetadata((Item) dso);
        }

        // -- Step 6 --
        // Finish things up!

        // Subclass hook for final checks and rearrangements
        // (this allows subclasses to do some final validation / changes as
        // necessary)
        finishObject(context, dso, params);

        // Update the object to make sure all changes are committed
        PackageUtils.updateDSpaceObject(context, dso);

        return dso;
    }

    /**
     * Add Bitstreams to an Item, based on the files listed in the METS Manifest
     * 
     * @param context
     *            DSpace Context
     * @param item
     *            DSpace Item
     * @param manifest
     *            METS Manifest
     * @param pkgFile
     *            the full package file (which may include content files if a
     *            zip)
     * @param params
     *            Ingestion Parameters
     * @param mdRefCallback
     *            MdrefManager storing info about mdRefs in manifest
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     * @throws MetadataValidationException if metadata validation error
     * @throws CrosswalkException if crosswalk error
     * @throws PackageValidationException if package validation error
     */
    protected void addBitstreams(Context context, Item item,
            METSManifest manifest, File pkgFile, PackageParameters params,
            MdrefManager mdRefCallback) throws SQLException, IOException,
            AuthorizeException, MetadataValidationException,
            CrosswalkException, PackageValidationException
    {
        // Step 1 -- find the ID of the primary or Logo bitstream in manifest
        String primaryID = null;
        Element primaryFile = manifest.getPrimaryOrLogoBitstream();
        if (primaryFile != null)
        {
            primaryID = primaryFile.getAttributeValue("ID");
            if (log.isDebugEnabled())
            {
                log.debug("Got primary bitstream file ID=\"" + primaryID + "\"");
            }
        }

        // Step 2 -- find list of all content files from manifest
        // Loop through these files, and add them one by one to Item
        List<Element> manifestContentFiles = manifest
                .getContentFiles();
        List<Element> manifestBundleFiles = manifest
                .getBundleFiles();

        boolean setPrimaryBitstream = false;
        BitstreamFormat unknownFormat = bitstreamFormatService.findUnknown(context);

        for (Iterator<Element> mi = manifestContentFiles.iterator(); mi
                .hasNext();)
        {
            Element mfile = mi.next();

            // basic validation -- check that it has an ID attribute
            String mfileID = mfile.getAttributeValue("ID");
            if (mfileID == null)
            {
                throw new PackageValidationException(
                        "Invalid METS Manifest: file element without ID attribute.");
            }

            // retrieve path/name of file in manifest
            String path = METSManifest.getFileName(mfile);

            // extract the file input stream from package (or retrieve
            // externally, if it is an externally referenced file)
            InputStream fileStream = getFileInputStream(pkgFile, params, path);

            // retrieve bundle name from manifest
            String bundleName = METSManifest.getBundleName(mfile);

            // Find or create the bundle where bitstream should be attached
            Bundle bundle;
            List<Bundle> bns = itemService.getBundles(item, bundleName);
            if (CollectionUtils.isNotEmpty(bns))
            {
                bundle = bns.get(0);
            }
            else
            {
                bundle = bundleService.create(context, item, bundleName);
            }

            // Create the bitstream in the bundle & initialize its name
            Bitstream bitstream = bitstreamService.create(context, bundle, fileStream);
            bitstream.setName(context, path);

             // Set bitstream sequence id, if known
            String seqID = mfile.getAttributeValue("SEQ");
            if(seqID!=null && !seqID.isEmpty())
                bitstream.setSequenceID(Integer.parseInt(seqID));
            
            // crosswalk this bitstream's administrative metadata located in
            // METS manifest (or referenced externally)
            manifest.crosswalkBitstream(context, params, bitstream, mfileID,
                    mdRefCallback);

            // is this the primary bitstream?
            if (primaryID != null && mfileID.equals(primaryID))
            {
                bundle.setPrimaryBitstreamID(bitstream);
                bundleService.update(context, bundle);
                setPrimaryBitstream = true;
            }

            // Run any finishing activities -- this allows subclasses to
            // change default bitstream information
            finishBitstream(context, bitstream, mfile, manifest, params);

            // Last-ditch attempt to divine the format, if crosswalk failed to
            // set it:
            // 1. attempt to guess from MIME type
            // 2. if that fails, guess from "name" extension.
            if (bitstream.getFormat(context).equals(unknownFormat))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Guessing format of Bitstream left un-set: "
                            + bitstream.toString());
                }
                String mimeType = mfile.getAttributeValue("MIMETYPE");
                BitstreamFormat bf = (mimeType == null) ? null
                        : bitstreamFormatService.findByMIMEType(context, mimeType);
                if (bf == null)
                {
                    bf = bitstreamFormatService.guessFormat(context, bitstream);
                }
                bitstreamService.setFormat(context, bitstream, bf);
            }
            bitstreamService.update(context, bitstream);
        }// end for each manifest file

        for (Iterator<Element> mi = manifestBundleFiles.iterator(); mi
                .hasNext();)
        {
            Element mfile = mi.next();

            String bundleName = METSManifest.getBundleName(mfile, false);

            Bundle bundle;
            List<Bundle> bns = itemService.getBundles(item, bundleName);
            if (CollectionUtils.isNotEmpty(bns))
            {
                bundle = bns.get(0);
            }
            else
            {
                bundle = bundleService.create(context, item, bundleName);
            }

	        String mfileGrp = mfile.getAttributeValue("ADMID");
	        if (mfileGrp != null)
	        {
		        manifest.crosswalkBundle(context, params, bundle, mfileGrp,mdRefCallback);
	        }
	        else
	        {
		        if (log.isDebugEnabled())
		        {
		            log.debug("Ingesting bundle with no ADMID, not crosswalking bundle metadata");
		        }
	        }

            bundleService.update(context, bundle);
        }// end for each manifest file

        // Step 3 -- Sanity checks
        // sanity check for primary bitstream
        if (primaryID != null && !setPrimaryBitstream)
        {
            log.warn("Could not find primary bitstream file ID=\"" + primaryID
                    + "\" in manifest file \"" + pkgFile.getAbsolutePath()
                    + "\"");
        }
    }

    /**
     * Save/Preserve the METS Manifest as a Bitstream attached to the given
     * DSpace item.
     * 
     * @param context
     *            DSpace Context
     * @param item
     *            DSpace Item
     * @param manifest
     *            The METS Manifest
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws PackageValidationException if package validation error
     */
    protected void addManifestBitstream(Context context, Item item,
            METSManifest manifest) throws IOException, SQLException,
            AuthorizeException, PackageValidationException
    {
        // We'll save the METS Manifest as part of the METADATA bundle.
        Bundle mdBundle = bundleService.create(context, item, Constants.METADATA_BUNDLE_NAME);

        // Create a Bitstream from the METS Manifest's content
        Bitstream manifestBitstream = bitstreamService.create(context, mdBundle, manifest
                .getMetsAsStream());
        manifestBitstream.setName(context, METSManifest.MANIFEST_FILE);
        manifestBitstream.setSource(context, METSManifest.MANIFEST_FILE);
        bitstreamService.update(context, manifestBitstream);

        // Get magic bitstream format to identify manifest.
        String fmtName = getManifestBitstreamFormat();
        if (fmtName == null)
        {
            throw new PackageValidationException(
                    "Configuration Error: No Manifest BitstreamFormat configured for METS ingester type="
                            + getConfigurationName());
        }
        BitstreamFormat manifestFormat = PackageUtils
                .findOrCreateBitstreamFormat(context, fmtName,
                        "application/xml", fmtName + " package manifest");
        manifestBitstream.setFormat(context, manifestFormat);
        bitstreamService.update(context, manifestBitstream);
    }

    /**
     * Add a Logo to a Community or Collection container object based on a METS
     * Manifest.
     * 
     * @param context
     *            DSpace Context
     * @param dso
     *            DSpace Container Object
     * @param manifest
     *            METS Manifest
     * @param pkgFile
     *            the full package file (which may include content files if a
     *            zip)
     * @param params
     *            Ingestion Parameters
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     * @throws MetadataValidationException if metadata validation error
     * @throws PackageValidationException if package validation error
     */
    protected void addContainerLogo(Context context, DSpaceObject dso,
            METSManifest manifest, File pkgFile, PackageParameters params)
            throws SQLException, IOException, AuthorizeException,
            MetadataValidationException, PackageValidationException
    {

        Element logoRef = manifest.getPrimaryOrLogoBitstream();

        // only continue if a logo specified in manifest
        if (logoRef != null)
        {
            // Find ID of logo file
            String logoID = logoRef.getAttributeValue("ID");

            // Loop through manifest content files to find actual logo file
            for (Iterator<Element> mi = manifest
                    .getContentFiles().iterator(); mi.hasNext();)
            {
                Element mfile = mi.next();
                if (logoID.equals(mfile.getAttributeValue("ID")))
                {
                    String path = METSManifest.getFileName(mfile);

                    // extract the file input stream from package (or retrieve
                    // externally, if it is an externally referenced file)
                    InputStream fileStream = getFileInputStream(pkgFile,
                            params, path);

                    // Add this logo to the Community/Collection
                    if (dso.getType() == Constants.COLLECTION)
                    {
                        collectionService.setLogo(context, ((Collection) dso), fileStream);
                    }
                    else
                    {
                        communityService.setLogo(context, ((Community) dso), fileStream);
                    }

                    break;
                }
            }// end for each file in manifest
        }// end if logo reference found
    }

    /**
     * Add a Template Item to a Collection container object based on a METS
     * Manifest.
     *
     * @param context
     *            DSpace Context
     * @param dso
     *            DSpace Container Object
     * @param manifest
     *            METS Manifest
     * @param pkgFile
     *            the full package file (which may include content files if a
     *            zip)
     * @param params
     *            Ingestion Parameters
     * @param callback
     *            the MdrefManager (manages all external metadata files
     *            referenced by METS <code>mdref</code> elements)
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     * @throws MetadataValidationException if metadata validation error
     * @throws PackageValidationException if package validation error
     */
    protected void addTemplateItem(Context context, DSpaceObject dso,
            METSManifest manifest, File pkgFile, PackageParameters params,
            MdrefManager callback)
            throws SQLException, IOException, AuthorizeException,
            CrosswalkException, PackageValidationException
    {
        //Template items only valid for collections
        if(dso.getType()!=Constants.COLLECTION)
            return;
        
        Collection collection = (Collection) dso;

        //retrieve list of all <div>s representing child objects from manifest
        List childObjList = manifest.getChildObjDivs();

        if(childObjList!=null && !childObjList.isEmpty())
        {
            Element templateItemDiv = null;

            Iterator childIterator = childObjList.iterator();
            //Search for the child with a type of "DSpace ITEM Template"
            while(childIterator.hasNext())
            {
                Element childDiv = (Element) childIterator.next();
                String childType = childDiv.getAttributeValue("TYPE");
                //should be the only child of type "ITEM" with "Template" for a suffix
                if(childType.contains(Constants.typeText[Constants.ITEM]) &&
                   childType.endsWith(AbstractMETSDisseminator.TEMPLATE_TYPE_SUFFIX))
                {
                    templateItemDiv = childDiv;
                    break;
                }
            }

            //If an Template Item was found, create it with the specified metadata
            if(templateItemDiv!=null)
            {
                //make sure this templateItemDiv is associated with one or more dmdSecs
                String templateDmdIds = templateItemDiv.getAttributeValue("DMDID");
                if(templateDmdIds!=null)
                {
                    //create our template item & get a reference to it
                    itemService.createTemplateItem(context, collection);
                    Item templateItem = collection.getTemplateItem();

                    //get a reference to the dmdSecs which describe the metadata for this template item
                    Element[] templateDmds = manifest.getDmdElements(templateDmdIds);

                    // Run our Descriptive metadata (dublin core, etc) crosswalks to add metadata to template item
                    crosswalkObjectDmd(context, templateItem, manifest, callback, templateDmds, params);

                    // update the template item to save metadata changes
                    PackageUtils.updateDSpaceObject(context, templateItem);
                }
            }
        }
    }

    /**
     * Replace an existing DSpace object with the contents of a METS-based
     * package. All contents are dictated by the METS manifest. Package is a ZIP
     * archive (or optionally bare manifest XML document). In a Zip, all files
     * relative to top level and the manifest (as per spec) in mets.xml.
     * <P>
     * This method is similar to ingest(), except that if the object already
     * exists in DSpace, it is emptied of files and metadata. The METS-based
     * package is then used to ingest new values for these.
     * 
     * @param context
     *            DSpace Context
     * @param dsoToReplace
     *            DSpace Object to be replaced (may be null if it will be
     *            specified in the METS manifest itself)
     * @param pkgFile
     *            The package file to ingest
     * @param params
     *            Parameters passed from the packager script
     * @return DSpaceObject created by ingest.
     * @throws PackageValidationException if package validation error
     *             if package is unacceptable or there is a fatal error turning
     *             it into a DSpace Object.
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws CrosswalkException if crosswalk error
     * @throws WorkflowException if workflow error
     */
    @Override
    public DSpaceObject replace(Context context, DSpaceObject dsoToReplace,
            File pkgFile, PackageParameters params)
            throws PackageValidationException, CrosswalkException,
            AuthorizeException, SQLException, IOException, WorkflowException {
        // parsed out METS Manifest from the file.
        METSManifest manifest = null;

        // resulting DSpace Object
        DSpaceObject dso = null;

        try
        {
            log.info(LogManager.getHeader(context, "package_parse",
                    "Parsing package for replace, file=" + pkgFile.getName()));

            // Parse our ingest package, extracting out the METS manifest in the
            // package
            manifest = parsePackage(context, pkgFile, params);

            // must have a METS Manifest to replace anything
            if (manifest == null)
            {
                throw new PackageValidationException(
                        "No METS Manifest found (filename="
                                + METSManifest.MANIFEST_FILE
                                + ").  Package is unacceptable!");
            }

            // It's possible that the object to replace will be passed in as
            // null.  Let's determine the handle of the object to replace.
            if (dsoToReplace == null)
            {
                // since we don't know what we are replacing, we'll have to
                // try to determine it from the parsed manifest

                // Handle of object described by METS should be in OBJID
                String handleURI = manifest.getObjID();
                String handle = decodeHandleURN(handleURI);
                try
                {
                    // Attempt to resolve this handle to an existing object
                    dsoToReplace = handleService.resolveToObject(context,
                            handle);
                }
                catch (IllegalStateException ie)
                {
                    // We don't care if this errors out -- we can continue
                    // whether or not an object exists with this handle.
                }
            }
            // NOTE: At this point, it's still possible we don't have an object
            // to replace. This could happen when there is actually no existing
            // object in DSpace using that handle. (In which case, we're
            // actually just doing a "restore" -- so we aren't going to throw an
            // error or complain.)

            // If we were unable to find the object to replace, then assume we
            // are restoring it
            if (dsoToReplace == null)
            {
                // As this object doesn't already exist, we will perform an
                // ingest of a new object in order to restore it
                // NOTE: passing 'null' as parent object in order to force
                // ingestObject() method to determine parent using manifest.
                dso = ingestObject(context, null, manifest, pkgFile, params,
                        null);

                //if ingestion was successful
                if(dso!=null)
                {
                    // Log that we created an object
                    log.info(LogManager.getHeader(context, "package_replace",
                            "Created new Object, type="
                                    + Constants.typeText[dso.getType()]
                                    + ", handle=" + dso.getHandle() + ", dbID="
                                    + String.valueOf(dso.getID())));
                }
            }
            else
            // otherwise, we found the DSpaceObject to replace -- so, replace
            // it!
            {
                // Actually replace the object described by the METS Manifest.
                // NOTE: This will perform an in-place replace of all metadata
                // and files currently associated with the object.
                dso = replaceObject(context, dsoToReplace, manifest, pkgFile,
                        params, null);

                // Log that we replaced an object
                log.info(LogManager.getHeader(context, "package_replace",
                        "Replaced Object, type="
                                + Constants.typeText[dso.getType()]
                                + ", handle=" + dso.getHandle() + ", dbID="
                                + String.valueOf(dso.getID())));
            }

            //if ingest/restore/replace successful
            if(dso!=null)
            {
                // Check if the Packager is currently running recursively.
                // If so, this means the Packager will attempt to recursively
                // replace all referenced child packages.
                if (params.recursiveModeEnabled())
                {
                    // Retrieve list of all Child object METS file paths from the
                    // current METS manifest.
                    // This is our list of known child packages.
                    String[] childFilePaths = manifest.getChildMetsFilePaths();

                    // Save this list to our AbstractPackageIngester (and note which
                    // DSpaceObject the pkgs relate to)
                    // NOTE: The AbstractPackageIngester itself will perform the
                    // recursive ingest call, based on these child pkg references.
                    for (int i = 0; i < childFilePaths.length; i++)
                    {
                        addPackageReference(dso, childFilePaths[i]);
                    }
                }
            }

            return dso;
        }
        catch (SQLException se)
        {
            // no need to really clean anything up,
            // transaction rollback will get rid of it anyway, and will also
            // restore everything to previous state.
            dso = null;

            // Pass this exception on to the next handler.
            throw se;
        }
    }

    // whether or not to save manifest as a bitstream in METADATA bundle.
    protected boolean preserveManifest()
    {
        return ConfigurationManager.getBooleanProperty("mets."
                + getConfigurationName() + ".ingest.preserveManifest", false);
    }

    // return short name of manifest bitstream format
    protected String getManifestBitstreamFormat()
    {
        return ConfigurationManager.getProperty("mets."
                + getConfigurationName() + ".ingest.manifestBitstreamFormat");
    }

    // whether or not to use Collection Templates when creating a new item
    protected boolean useCollectionTemplate()
    {
        return ConfigurationManager.getBooleanProperty("mets."
                + getConfigurationName() + ".ingest.useCollectionTemplate",
                false);
    }

    /**
     * Parse the hdl: URI/URN format into a raw Handle.
     * 
     * @param value
     *            handle URI string
     * @return raw handle (with 'hdl:' prefix removed)
     */
    protected String decodeHandleURN(String value)
    {
        if (value != null && value.startsWith("hdl:"))
        {
            return value.substring(4);
        }
        else
        {
            return null;
        }
    }

    /**
     * Remove an existing DSpace Object (called during a replace)
     * 
     * @param context context
     * @param dso
     *            DSpace Object
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    protected void removeObject(Context context, DSpaceObject dso)
            throws AuthorizeException, SQLException, IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Removing object " + Constants.typeText[dso.getType()]
                    + " id=" + dso.getID());
        }

        switch (dso.getType())
        {
        case Constants.ITEM:
            Item item = (Item) dso;

            itemService.delete(context, item);
            break;

        case Constants.COLLECTION:
            Collection collection = (Collection) dso;
            collectionService.delete(context, collection);
            break;

        case Constants.COMMUNITY:
            // Just remove the Community entirely
            communityService.delete(context, (Community) dso);
            break;
        }

    }

    /**
     * Determines what parent DSpace object is referenced in this METS doc.
     * <p>
     * This is a default implementation which assumes the parent will be
     * specified in a &lt;structMap LABEL="Parent"&gt;. You should override this
     * method if your METS manifest specifies the parent object in another
     * location.
     * 
     * @param context
     *            DSpace Context
     * @param manifest
     *            METS manifest
     * @return a DSpace Object which is the parent (or null, if not found)
     * @throws PackageValidationException if package validation error
     *             if parent reference cannot be found in manifest
     * @throws MetadataValidationException if metadata validation error
     * @throws SQLException if database error
     */
    public DSpaceObject getParentObject(Context context, METSManifest manifest)
            throws PackageValidationException, MetadataValidationException,
            SQLException
    {
        DSpaceObject parent = null;
        // look for a Parent Object link in manifest <structmap>
        String parentLink = manifest.getParentOwnerLink();

        // verify we have a valid Parent Object
        if (parentLink != null && parentLink.length() > 0)
        {
            parent = handleService.resolveToObject(context, parentLink);
            if (parent == null)
            {
                throw new UnsupportedOperationException(
                        "Could not find a parent DSpaceObject referenced as '"
                                + parentLink
                                + "' in the METS Manifest for object "
                                + manifest.getObjID()
                                + ". A parent DSpaceObject must be specified from either the 'packager' command or noted in the METS Manifest itself.");
            }
        }
        else
        {
            throw new UnsupportedOperationException(
                    "Could not find a parent DSpaceObject where we can ingest the packaged object "
                    + manifest.getObjID()
                    + ".  A parent DSpaceObject must be specified from either the 'packager' command or noted in the METS Manifest itself.");
        }

        return parent;
    }

    /**
     * Determines the handle of the DSpace object represented in this METS doc.
     * <p>
     * This is a default implementation which assumes the handle of the DSpace
     * Object can be found in the &lt;mets&gt; @OBJID attribute. You should
     * override this method if your METS manifest specifies the handle in
     * another location.
     * 
     * If no handle was found then null is returned.
     * 
     * @param manifest
     *            METS manifest
     * @return handle as a string (or null, if not found)
     * @throws PackageValidationException if package validation error
     *             if handle cannot be found in manifest
     * @throws MetadataValidationException if validation error
     * @throws SQLException if database error 
     */
    public String getObjectHandle(METSManifest manifest)
            throws PackageValidationException, MetadataValidationException,
            SQLException
    {
        // retrieve handle URI from manifest
        String handleURI = manifest.getObjID();

        // decode this URI (by removing the 'hdl:' prefix)
        String handle = decodeHandleURN(handleURI);


        return handle;
    }

    /**
     * Retrieve the inputStream for a File referenced from a specific path
     * within a METS package.
     * <p>
     * If the packager is set to 'manifest-only' (i.e. pkgFile is just a
     * manifest), we assume the file is available for download via a URL.
     * <p>
     * Otherwise, the pkgFile is a Zip, so the file should be retrieved from
     * within that Zip package.
     * 
     * @param pkgFile
     *            the full package file (which may include content files if a
     *            zip)
     * @param params
     *            Parameters passed to METSIngester
     * @param path
     *            the File path (either path in Zip package or a URL)
     * @return the InputStream for the file
     * @throws MetadataValidationException if validation error
     * @throws IOException if IO error
     */
    protected static InputStream getFileInputStream(File pkgFile,
            PackageParameters params, String path)
            throws MetadataValidationException, IOException
    {
        // If this is a manifest only package (i.e. not a zip file)
        if (params.getBooleanProperty("manifestOnly", false))
        {
            // NOTE: since we are only dealing with a METS manifest,
            // we will assume all external files are available via URLs.
            try
            {
                // attempt to open a connection to given URL
                URL fileURL = new URL(path);
                URLConnection connection = fileURL.openConnection();

                // open stream to access file contents
                return connection.getInputStream();
            }
            catch (IOException io)
            {
                log
                        .error("Unable to retrieve external file from URL '"
                                + path
                                + "' for manifest-only METS package.  All externally referenced files must be retrievable via URLs.");
                // pass exception upwards
                throw io;
            }
        }
        else
        {
            // open the Zip package
            ZipFile zipPackage = new ZipFile(pkgFile);

            // Retrieve the manifest file entry by name
            ZipEntry manifestEntry = zipPackage.getEntry(path);

            // Get inputStream associated with this file
            if (manifestEntry != null)
                return zipPackage.getInputStream(manifestEntry);
            else
            {
                throw new MetadataValidationException("Manifest file references file '"
                                        + path + "' not included in the zip.");
            }
        }
    }


    /**
     * Returns a user help string which should describe the
     * additional valid command-line options that this packager
     * implementation will accept when using the <code>-o</code> or
     * <code>--option</code> flags with the Packager script.
     *
     * @return a string describing additional command-line options available
     * with this packager
     */
    @Override
    public String getParameterHelp()
    {
        return "* ignoreHandle=[boolean]      " +
                   "If true, the ingester will ignore any Handle specified in the METS manifest itself, and instead create a new Handle during the ingest process (this is the default when running in Submit mode, using the -s flag). " +
                   "If false, the ingester attempts to restore the Handles specified in the METS manifest (this is the default when running in Restore/replace mode, using the -r flag). " +
               "\n\n" +
               "* ignoreParent=[boolean]      " +
                   "If true, the ingester will ignore any Parent object specified in the METS manifest itself, and instead ingest under a new Parent object (this is the default when running in Submit mode, using the -s flag). The new Parent object must be specified via the -p flag. " +
                   "If false, the ingester attempts to restore the object directly under its old Parent (this is the default when running in Restore/replace mode, using the -r flag). " +
               "\n\n" +
               "* manifestOnly=[boolean]      " +
                   "Specify true if the ingest package consists of just a METS manifest (mets.xml), without any content files (defaults to false)." +
               "\n\n" +
               "* validate=[boolean]      " +
                   "If true, enable XML validation of METS file using schemas in document (default is true).";
    }

    /**
     * Profile-specific tests to validate manifest. The implementation can
     * access the METS document through the <code>manifest</code> variable, an
     * instance of <code>METSManifest</code>.
     * 
     * @throws MetadataValidationException if metadata validation error
     *             if there is a fatal problem with the METS document's
     *             conformance to the expected profile.
     */
    abstract void checkManifest(METSManifest manifest)
            throws MetadataValidationException;

    /**
     * Select the <code>dmdSec</code> element(s) to apply to the Item. The
     * implementation is responsible for choosing which (if any) of the metadata
     * sections to crosswalk to get the descriptive metadata for the item being
     * ingested. It is responsible for calling the crosswalk, using the
     * manifest's helper i.e.
     * <code>manifest.crosswalkItemDmd(context,item,dmdElement,callback);</code>
     * (The <code>callback</code> argument is a reference to itself since the
     * class also implements the <code>METSManifest.MdRef</code> interface to
     * fetch package files referenced by mdRef elements.)
     * <p>
     * Note that <code>item</code> and <code>manifest</code> are available as
     * protected fields from the superclass.
     * 
     * @param context the DSpace context
     * @param dso DSpace Object
     * @param manifest the METSManifest
     * @param callback
*            the MdrefManager (manages all external metadata files
*            referenced by METS <code>mdref</code> elements)
     * @param dmds
*            array of Elements, each a METS <code>dmdSec</code> that
*            applies to the Item as a whole.
     * @param params
     * @throws CrosswalkException if crosswalk error
     * @throws PackageValidationException if package validation error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public abstract void crosswalkObjectDmd(Context context, DSpaceObject dso,
            METSManifest manifest, MdrefManager callback, Element dmds[],
            PackageParameters params) throws CrosswalkException,
            PackageValidationException, AuthorizeException, SQLException,
            IOException;

    /**
     * Add license(s) to Item based on contents of METS and other policies. The
     * implementation of this method controls exactly what licenses are added to
     * the new item, including the DSpace deposit license. It is given the
     * collection (which is the source of a default deposit license), an
     * optional user-supplied deposit license (in the form of a String), and the
     * METS manifest. It should invoke <code>manifest.getItemRightsMD()</code>
     * to get an array of <code>rightsMd</code> elements which might contain
     * other license information of interest, e.g. a Creative Commons license.
     * <p>
     * This framework does not add any licenses by default.
     * <p>
     * Note that crosswalking rightsMD sections can also add a deposit or CC
     * license to the object.
     * 
     * @param context
     *            the DSpace context
     * @param item Item
     * @param collection
     *            DSpace Collection to which the item is being submitted.
     * @param license
     *            optional user-supplied Deposit License text (may be null)
     * @param params
     * @throws PackageValidationException if package validation error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public abstract void addLicense(Context context, Item item, String license,
            Collection collection, PackageParameters params)
            throws PackageValidationException, AuthorizeException,
            SQLException, IOException;

    /**
     * Hook for final "finishing" operations on the new Object. This method is
     * called when the new Object is otherwise complete and ready to be
     * returned. The implementation should use this opportunity to make whatever
     * final checks and modifications are necessary.
     * 
     * @param context
     *            the DSpace context
     * @param dso
     *            the DSpace Object
     * @param params
     *            the Packager Parameters
     * @throws CrosswalkException if crosswalk error
     * @throws PackageValidationException if package validation error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public abstract void finishObject(Context context, DSpaceObject dso,
            PackageParameters params) throws PackageValidationException,
            CrosswalkException, AuthorizeException, SQLException, IOException;

    /**
     * Determines what type of DSpace object is represented in this METS doc.
     * 
     * @param manifest METS manifest
     * @return one of the object types in Constants.
     * @throws PackageValidationException if package validation error
     */
    public abstract int getObjectType(METSManifest manifest)
            throws PackageValidationException;

    /**
     * Subclass-dependent final processing on a Bitstream; could include fixing
     * up the name, bundle, other attributes.
     * @param context context
     * @param manifest METS manifest
     * @param bs bitstream
     * @param mfile element
     * @param params package params
     * @throws MetadataValidationException if validation error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public abstract void finishBitstream(Context context, Bitstream bs,
            Element mfile, METSManifest manifest, PackageParameters params)
            throws MetadataValidationException, SQLException,
            AuthorizeException, IOException;

    /**
     * Returns keyword that makes the configuration keys of this subclass
     * unique, e.g. if it returns NAME, the key would be:
     * "mets.NAME.ingest.preserveManifest = true"
     * @return name
     */
    public abstract String getConfigurationName();

}
