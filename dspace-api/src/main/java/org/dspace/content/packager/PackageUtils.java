/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Container class for code that is useful to many packagers.
 *
 * @author Larry Stone
 * @version $Revision$
 */

public class PackageUtils
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(PackageUtils.class);

    // Map of metadata elements for Communities and Collections
    // Format is alternating key/value in a straight array; use this
    // to initialize hash tables that convert to and from.
    protected static final String ccMetadataMap[] =
    {
        // getMetadata()  ->  DC element.term
        "name",                    "dc.title",
        "introductory_text",       "dc.description",
        "short_description",       "dc.description.abstract",
        "side_bar_text",           "dc.description.tableofcontents",
        "copyright_text",          "dc.rights",
        "provenance_description",  "dc.provenance",
        "license",                 "dc.rights.license"
    };

    // HashMaps to convert Community/Collection metadata to/from Dublin Core
    // (useful when crosswalking Communities/Collections)
    protected static final Map<String,String> ccMetadataToDC = new HashMap<String,String>();
    protected static final Map<String,String> ccDCToMetadata = new HashMap<String,String>();

    protected static final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected static final BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    protected static final BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected static final InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected static final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected static final WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected static final SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    static
    {
        for (int i = 0; i < ccMetadataMap.length; i += 2)
        {
            ccMetadataToDC.put(ccMetadataMap[i], ccMetadataMap[i+1]);
            ccDCToMetadata.put(ccMetadataMap[i+1], ccMetadataMap[i]);
        }
    }

    /**
     * Translate a Dublin Core metadata field into a Container's (Community or Collection)
     * database column for that metadata entry.
     * <P>
     * e.g. "dc.title" would translate to the "name" database column
     * <P>
     * This method is of use when crosswalking Community or Collection metadata for ingest, 
     * as most ingest Crosswalks tend to deal with translating to DC-based metadata.
     * 
     * @param dcField The dublin core metadata field
     * @return The Community or Collection DB column where this metadata info is stored.
     */
    public static String dcToContainerMetadata(String dcField)
    {
        return ccDCToMetadata.get(dcField);
    }

    /**
     * Translate a Container's (Community or Collection) database column into
     * a valid Dublin Core metadata field.  This is the opposite of 'dcToContainerMetadata()'.
     * <P>
     * e.g. the "name" database column would translate to "dc.title"
     * <P>
     * This method is of use when crosswalking Community or Collection metadata for dissemination,
     * as most dissemination Crosswalks tend to deal with translating from DC-based metadata.
     *
     *
     * @param databaseField The Community or Collection DB column
     * @return The Dublin Core metadata field that this metadata translates to.
     */
    public static String containerMetadataToDC(String databaseField)
    {
        return ccMetadataToDC.get(databaseField);
    }

    /**
     * Test that item has adequate metadata.
     * Check item for the minimal DC metadata required to ingest a
     * new item, and throw a PackageValidationException if test fails.
     * Used by all SIP processors as a common sanity test.
     *
     * @param item - item to test.
     * @throws PackageValidationException if validation error
     */
    public static void checkItemMetadata(Item item)
        throws PackageValidationException
    {
        List<MetadataValue> t = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        if (t == null || t.size() == 0)
        {
            throw new PackageValidationException("Item cannot be created without the required \"title\" DC metadata.");
        }
    }

    /**
     * Add DSpace Deposit License to an Item.
     * Utility function to add the a user-supplied deposit license or
     * a default one if none was given; creates new bitstream in the
     * "LICENSE" bundle and gives it the special license bitstream format.
     *
     * @param context - dspace context
     * @param license - license string to add, may be null to invoke default.
     * @param item - the item.
     * @param collection - get the default license from here.
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public static void addDepositLicense(Context context, String license,
                                       Item item, Collection collection)
        throws SQLException, IOException, AuthorizeException
    {
        if (license == null)
        {
            license = collection.getLicenseCollection();
        }
        InputStream lis = new ByteArrayInputStream(license.getBytes());

        Bundle lb;
        //If LICENSE bundle is missing, create it
        List<Bundle> bundles = itemService.getBundles(item, Constants.LICENSE_BUNDLE_NAME);
        if(CollectionUtils.isEmpty(bundles))
        {
            lb = bundleService.create(context, item, Constants.LICENSE_BUNDLE_NAME);
        }
        else
        {
            lb = bundles.get(0);
        }

        //Create the License bitstream
        Bitstream lbs = bitstreamService.create(context, lb, lis);
        lis.close();
        BitstreamFormat bf = bitstreamFormatService.findByShortDescription(context, "License");
        if (bf == null)
        {
            bf = bitstreamFormatService.guessFormat(context, lbs);
        }
        lbs.setFormat(context, bf);
        lbs.setName(context, Constants.LICENSE_BITSTREAM_NAME);
        lbs.setSource(context, Constants.LICENSE_BITSTREAM_NAME);
        bitstreamService.update(context, lbs);
    }

    /**
     * Find bitstream by its Name, looking in all bundles.
     *
     * @param item Item whose bitstreams to search.
     * @param name Bitstream's name to match.
     * @return first bitstream found or null.
     * @throws SQLException if database error
     */
    public static Bitstream getBitstreamByName(Item item, String name)
        throws SQLException
    {
        return getBitstreamByName(item, name, null);
    }

    /**
     * Find bitstream by its Name, looking in specific named bundle.
     *
     * @param item - dspace item whose bundles to search.
     * @param bsName - name of bitstream to match.
     * @param bnName - bundle name to match, or null for all.
     * @return the format found or null if none found.
     * @throws SQLException if database error
     */
    public static Bitstream getBitstreamByName(Item item, String bsName, String bnName)
        throws SQLException
    {
        List<Bundle> bundles;
        if (bnName == null)
        {
            bundles = item.getBundles();
        }
        else
        {
            bundles = itemService.getBundles(item, bnName);
        }
        for (Bundle bundle : bundles)
        {
            List<Bitstream> bitstreams = bundle.getBitstreams();

            for (Bitstream bitstream : bitstreams)
            {
                if (bsName.equals(bitstream.getName())) {
                    return bitstream;
                }
            }
        }
        return null;
    }

    /**
     * Find bitstream by its format, looking in a specific bundle.
     * Used to look for particularly-typed Package Manifest bitstreams.
     *
     * @param context context
     * @param item - dspace item whose bundles to search.
     * @param bsf - BitstreamFormat object to match.
     * @param bnName - bundle name to match, or null for all.
     * @return the format found or null if none found.
     * @throws SQLException if database error
     */
    public static Bitstream getBitstreamByFormat(Context context, Item item,
            BitstreamFormat bsf, String bnName)
        throws SQLException
    {
        int fid = bsf.getID();
        List<Bundle> bundles;
        if (bnName == null)
        {
            bundles = item.getBundles();
        }
        else
        {
            bundles = itemService.getBundles(item, bnName);
        }
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();

            for (Bitstream bitstream : bitstreams) {
                if (bitstream.getFormat(context).getID() == fid) {
                    return bitstream;
                }
            }
        }
        return null;
    }

    /**
     * Predicate, does this bundle container meta-information.  I.e.
     * does this bundle contain descriptive metadata or other metadata
     * such as license bitstreams?  If so we probably don't want to put
     * it into the "content" section of a package; hence this predicate.
     *
     * @param bn -- the bundle
     * @return true if this bundle name indicates it is a meta-info bundle.
     */
    public static boolean isMetaInfoBundle(Bundle bn)
    {
        return (bn.getName().equals(Constants.LICENSE_BUNDLE_NAME) ||
                bn.getName().equals(CreativeCommonsService.CC_BUNDLE_NAME) ||
                bn.getName().equals(Constants.METADATA_BUNDLE_NAME));
    }

    /**
     * Stream wrapper that does not allow its wrapped stream to be
     * closed.  This is needed to work around problem when loading
     * bitstreams from ZipInputStream.  The Bitstream constructor
     * invokes close() on the input stream, which would prematurely end
     * the ZipInputStream.
     * Example:
     * <pre>
     *      ZipEntry ze = zip.getNextEntry();
     *      Bitstream bs = bundle.createBitstream(new PackageUtils.UnclosableInputStream(zipInput));
     * </pre>
     */
    public static class UnclosableInputStream extends FilterInputStream
    {
        public UnclosableInputStream(InputStream in)
        {
            super(in);
        }

        /**
         * Do nothing, to prevent wrapped stream from being closed prematurely.
         */
        @Override
        public void close()
        {
        }
    }

    /**
     * Find or create a bitstream format to match the given short
     * description.
     * Used by packager ingesters to obtain a special bitstream
     * format for the manifest (and/or metadata) file.
     * <p>
     * NOTE: When creating a new format, do NOT set any extensions, since
     *  we don't want any file with the same extension, which may be something
     *  generic like ".xml", to accidentally get set to this format.
     * @param context - the context.
     * @param shortDesc - short descriptive name, used to locate existing format.
     * @param MIMEType - MIME content-type
     * @param desc - long description
     * @return BitstreamFormat object that was found or created.  Never null.
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
     public static BitstreamFormat findOrCreateBitstreamFormat(Context context,
            String shortDesc, String MIMEType, String desc)
        throws SQLException, AuthorizeException
     {
        return findOrCreateBitstreamFormat(context, shortDesc, MIMEType, desc, BitstreamFormat.KNOWN, false);
     }

    /**
     * Find or create a bitstream format to match the given short
     * description.
     * Used by packager ingesters to obtain a special bitstream
     * format for the manifest (and/or metadata) file.
     * <p>
     * NOTE: When creating a new format, do NOT set any extensions, since
     *  we don't want any file with the same extension, which may be something
     *  generic like ".xml", to accidentally get set to this format.
     * @param context - the context.
     * @param shortDesc - short descriptive name, used to locate existing format.
     * @param MIMEType - mime content-type
     * @param desc - long description
     * @param supportLevel support level
     * @param internal value for the 'internal' flag of a new format if created.
     * @return BitstreamFormat object that was found or created.  Never null.
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
     public static BitstreamFormat findOrCreateBitstreamFormat(Context context,
            String shortDesc, String MIMEType, String desc, int supportLevel, boolean internal)
        throws SQLException, AuthorizeException
     {
        BitstreamFormat bsf = bitstreamFormatService.findByShortDescription(context,
                                shortDesc);
        // not found, try to create one
        if (bsf == null)
        {
            bsf = bitstreamFormatService.create(context);
            bsf.setShortDescription(context, shortDesc);
            bsf.setMIMEType(MIMEType);
            bsf.setDescription(desc);
            bsf.setSupportLevel(supportLevel);
            bsf.setInternal(internal);
            bitstreamFormatService.update(context, bsf);
        }
        return bsf;
    }

    /**
     * Utility to find the license bitstream from an item
     *
     * @param context
     *            DSpace context
     * @param item
     *            the item
     * @return the license bitstream or null
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if the license bitstream can't be read
     */
    public static Bitstream findDepositLicense(Context context, Item item)
            throws SQLException, IOException, AuthorizeException
    {
        // get license format ID
        int licenseFormatId = -1;
        BitstreamFormat bf = bitstreamFormatService.findByShortDescription(context,
                "License");
        if (bf != null)
        {
            licenseFormatId = bf.getID();
        }

        List<Bundle> bundles = itemService.getBundles(item, Constants.LICENSE_BUNDLE_NAME);
        for (Bundle bundle : bundles)
        {
            // Assume license will be in its own bundle
            List<Bitstream> bitstreams = bundle.getBitstreams();

            for (Bitstream bitstream : bitstreams)
            {
                // The License should have a file format of "License"
                if (bitstream.getFormat(context).getID() == licenseFormatId) {
                    //found a bitstream with format "License" -- return it
                    return bitstream;
                }
            }

            // If we couldn't find a bitstream with format = "License",
            // we will just assume the first bitstream is the deposit license
            // (usually a safe assumption as it is in the LICENSE bundle)
            if (bitstreams.size() > 0) {
                return bitstreams.get(0);
            }
        }

        // Oops! No license!
        return null;
    }


    /*=====================================================
     *  Utility Methods -- may be useful for subclasses
     *====================================================*/


    /**
     * Create the specified DSpace Object, based on the passed
     * in Package Parameters (along with other basic info required
     * to create the object)
     *
     * @param context DSpace Context
     * @param parent Parent Object
     * @param type Type of new Object
     * @param handle Handle of new Object (may be null)
     * @param params Properties-style list of options (interpreted by each packager).
     * @return newly created DSpace Object (or null)
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    public static DSpaceObject createDSpaceObject(Context context, DSpaceObject parent, int type, String handle, PackageParameters params)
        throws AuthorizeException, SQLException, IOException
    {
        DSpaceObject dso = null;

        switch (type)
        {
            case Constants.COLLECTION:
                dso = collectionService.create(context, (Community) parent, handle);
                return dso;

            case Constants.COMMUNITY:
                // top-level community?
                if (parent == null || parent.getType() == Constants.SITE)
                {
                    dso = communityService.create(null, context, handle);
                }
                else
                {
                    dso = communityService.createSubcommunity(context, ((Community) parent), handle);
                }
                return dso;

            case Constants.ITEM:
                //Initialize a WorkspaceItem
                //(Note: Handle is not set until item is finished)
                WorkspaceItem wsi = workspaceItemService.create(context, (Collection)parent, params.useCollectionTemplate());

                // Please note that we are returning an Item which is *NOT* yet in the Archive,
                // and doesn't yet have a handle assigned.
                // This Item will remain "incomplete" until 'PackageUtils.finishCreateItem()' is called
                return wsi.getItem();
                
            case Constants.SITE:
                return siteService.findSite(context);
        }

        return null;
    }

    /**
     * Perform any final tasks on a newly created WorkspaceItem in order to finish
     * ingestion of an Item.
     * <p>
     * This may include starting up a workflow for the new item, restoring it,
     * or archiving it (based on params passed in)
     *
     * @param context DSpace Context
     * @param wsi Workspace Item that requires finishing
     * @param handle Handle to assign to item (may be null)
     * @param params Properties-style list of options (interpreted by each packager).
     * @return finished Item
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws WorkflowException if workflow error
     */
    public static Item finishCreateItem(Context context, WorkspaceItem wsi, String handle, PackageParameters params)
            throws IOException, SQLException, AuthorizeException, WorkflowException {
        // if we are restoring/replacing existing object using the package
        if (params.restoreModeEnabled())
        {
            // Restore & install item immediately
            //(i.e. skip over any Collection workflows, as we are essentially restoring item from backup)
            installItemService.restoreItem(context, wsi, handle);

            //return newly restored item
            return wsi.getItem();
        }
        // if we are treating package as a SIP, and we are told to respect workflows
        else if (params.workflowEnabled())
        {
            WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
            // Start an item workflow
            // (NOTICE: The specified handle is ignored, as Workflows *always* end in a new handle being assigned)
            return workflowService.startWithoutNotify(context, wsi).getItem();
        }

        // default: skip workflow, but otherwise normal submission (i.e. package treated like a SIP)
        else
        {
            // Install item immediately with the specified handle
            installItemService.installItem(context, wsi, handle);

            // return newly installed item
            return wsi.getItem();
        }
    }//end finishCreateItem


    /**
     * Commit all recent changes to DSpaceObject.
     * <p>
     * This method is necessary as there is no generic 'update()' on a DSpaceObject
     *
     * @param context context
     * @param dso DSpaceObject to update
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     */
    public static void updateDSpaceObject(Context context, DSpaceObject dso)
            throws AuthorizeException, SQLException, IOException
    {
        if (dso != null)
        {
            DSpaceObjectService<DSpaceObject> dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
            dsoService.update(context, dso);
        }
    }


    /**
     * Utility method to retrieve the file extension off of a filename.
     *
     * @param filename Full filename
     * @return file extension
     */
    public static String getFileExtension(String filename)
    {
        // Extract the file extension off of a filename
        String extension = filename;
        int lastDot = filename.lastIndexOf('.');

        if (lastDot != -1)
        {
            extension = filename.substring(lastDot + 1);
        }

        return extension;
    }


    /**
     * Returns name of a dissemination information package (DIP), based on the
     * DSpace object and a provided fileExtension
     * <p>
     * Format: [dspace-obj-type]@[handle-with-dashes].[fileExtension]
     * OR      [dspace-obj-type]@internal-id-[dspace-ID].[fileExtension]
     *
     * @param dso  DSpace Object to create file name for
     * @param fileExtension file Extension of output file.
     * @return filename of a DIP representing the DSpace Object
     */
    public static String getPackageName(DSpaceObject dso, String fileExtension)
    {
        String handle = dso.getHandle();
        // if Handle is empty, use internal ID for name
        if(handle==null || handle.isEmpty())
        {
            handle = "internal-id-" + dso.getID();
        }
        else // if Handle exists, replace '/' with '-' to meet normal file naming conventions
        {
            handle = handle.replace("/", "-");
        }

        //Get type name
        int typeID = dso.getType();
        String type = Constants.typeText[typeID];

        //check if passed in file extension already starts with "."
        if(!fileExtension.startsWith("."))
        {
            fileExtension = "." + fileExtension;
        }

        //Here we go, here's our magical file name!
        //Format: typeName@handle.extension
        return type + "@" + handle + fileExtension;
    }


    /**
     * Creates the specified file (along with all parent directories) if it doesn't already
     * exist.  If the file already exists, nothing happens.
     * 
     * @param file file
     * @return boolean true if succeeded, false otherwise
     * @throws IOException if IO error
     */
    public static boolean createFile(File file)
            throws IOException
    {
        boolean success = false;

        //Check if file exists
        if(!file.exists())
        {
            //file doesn't exist yet, does its parent directory exist?
            File parentFile = file.getCanonicalFile().getParentFile();

            //create the parent directory structure
            if ((null != parentFile) && !parentFile.exists() && !parentFile.mkdirs())
            {
                log.error("Unable to create parent directory");
            }
            //create actual file
            success = file.createNewFile();
        }
        return success;
    }

    /**
     * Remove all bitstreams (files) associated with a DSpace object.
     * <P>
     * If this object is an Item, it removes all bundles and bitstreams.  If this
     * object is a Community or Collection, it removes all logo bitstreams.
     * <P>
     * This method is useful for replace functionality.
     *
     * @param context context
     * @param dso The object to remove all bitstreams from
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public static void removeAllBitstreams(Context context, DSpaceObject dso)
            throws SQLException, IOException, AuthorizeException
    {
        //If we are dealing with an Item
        if(dso.getType()==Constants.ITEM)
        {
            Item item = (Item) dso;
            // Get a reference to all Bundles in Item (which contain the bitstreams)
            Iterator<Bundle> bunds = item.getBundles().iterator();

            // Remove each bundle -- this will in turn remove all bitstreams associated with this Item.
            while (bunds.hasNext()) {
                Bundle bundle = bunds.next();
                bunds.remove();
                itemService.removeBundle(context, item, bundle);
            }
        }
        else if (dso.getType()==Constants.COLLECTION)
        {
            Collection collection = (Collection) dso;
            //clear out the logo for this collection
            collectionService.setLogo(context, collection, null);
        }
        else if (dso.getType()==Constants.COMMUNITY)
        {
            Community community = (Community) dso;
            //clear out the logo for this community
            communityService.setLogo(context, community, null);
        }
    }


    /**
     * Removes all metadata associated with a DSpace object.
     * <P>
     * This method is useful for replace functionality.
     *
     * @param context context
     * @param dso The object to remove all metadata from
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public static void clearAllMetadata(Context context, DSpaceObject dso)
            throws SQLException, IOException, AuthorizeException
    {
        //If we are dealing with an Item
        if(dso.getType()==Constants.ITEM)
        {
            Item item = (Item) dso;
            //clear all metadata entries
            itemService.clearMetadata(context, item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        }
        //Else if collection, clear its database table values
        else if (dso.getType()==Constants.COLLECTION)
        {
            Collection collection = (Collection) dso;

            // Use the MetadataToDC map (defined privately in this class)
            // to clear out all the Collection database fields.
            for(String dbField : ccMetadataToDC.keySet())
            {
                try
                {
                    collectionService.setMetadata(context, collection, dbField, null);
                }
                catch(IllegalArgumentException ie)
                {
                    // ignore the error -- just means the field doesn't exist in DB
                    // Communities & Collections don't include the exact same metadata fields
                }
            }
        }
        //Else if community, clear its database table values
        else if (dso.getType()==Constants.COMMUNITY)
        {
            Community community = (Community) dso;

            // Use the MetadataToDC map (defined privately in this class)
            // to clear out all the Community database fields.
            for(String dbField : ccMetadataToDC.keySet())
            {
                try
                {
                    communityService.setMetadata(context, community, dbField, null);
                }
                catch(IllegalArgumentException ie)
                {
                    // ignore the error -- just means the field doesn't exist in DB
                    // Communities & Collections don't include the exact same metadata fields
                }
            }
        }
    }


    /** Lookaside list for translations we've already done, so we don't generate
     * multiple names for the same group
     */
    protected static final Map<String, String> orphanGroups = new HashMap<String, String>();

    /**
     * When DSpace creates Default Group Names they are of a very specific format,
     * for example:
     * <ul>
     * <li> COMMUNITY_[ID]_ADMIN </li>
     * <li> COLLECTION_[ID]_ADMIN </li>
     * <li> COLLECTION_[ID]_SUBMIT </li>
     * <li> COLLECTION_[ID]_WORKFLOW_STEP_# </li>
     * </ul>
     * <p>
     * Although these names work fine within DSpace, the DSpace internal ID
     * (represented by [ID] above) becomes meaningless when content is exported
     * outside of DSpace.  In order to make these Group names meaningful outside
     * of DSpace, they must be translated into a different format:
     * {@code COMMUNITY_[HANDLE]_ADMIN} (e.g. COMMUNITY_hdl:123456789/10_ADMIN), etc.
     * <p>
     * This format replaces the internal ID with an external Handle identifier
     * (which is expected to be more meaningful even when content is exported
     *  from DSpace).
     * <p>
     * This method prepares group names for export by replacing any found
     * internal IDs with the appropriate external Handle identifier.  If
     * the group name doesn't have an embedded internal ID, it is returned
     * as is. If the group name contains an embedded internal ID, but the
     * corresponding Handle cannot be determined, then it will be translated to
     * GROUP_[random]_[objectType]_[groupType] and <em>not</em> re-translated on
     * import.
     * <p>
     * This method may be useful to any Crosswalks/Packagers which deal with
     * import/export of DSpace Groups.
     * <p>
     * Also see the translateGroupNameForImport() method which does the opposite
     * of this method.
     *
     * @param context current DSpace Context
     * @param groupName Group's name
     * @return the group name, with any internal IDs translated to Handles
     * @throws PackageException if package error
     */
    public static String translateGroupNameForExport(Context context, String groupName)
            throws PackageException
    {
        Pattern defaultGroupNamePattern = Pattern.compile("^([^_]+)_([^_]+)_(.+)$");
        // Check if this looks like a default Group name
        Matcher matcher = defaultGroupNamePattern.matcher(groupName);
        if(!matcher.matches())
        {
            //if this is not a valid default group name, just return group name as-is (no crosswalking necessary)
            return groupName;
        }

        String objTypeText = matcher.group(1);
        String objID = matcher.group(2);
        String groupType = matcher.group(3);

        int objType = Constants.getTypeID(objTypeText);
        if (objID == null || objType == -1)
            return groupName;


        try
        {
            //We'll translate this internal ID into a Handle

            //First, get the object via the Internal ID
            DSpaceObject dso = ContentServiceFactory.getInstance().getDSpaceLegacyObjectService(objType).findByIdOrLegacyId(context, objID);
            ;

            if(dso==null)
            {
                // No such object.  Change the name to something harmless, but predictable.
                // NOTE: this name *must* be predictable. If we generate the same AIP
                // twice in a row, we must end up with the same group name each time.
                String newName;
                if (orphanGroups.containsKey(groupName))
                    newName =  orphanGroups.get(groupName);
                else
                {
                    newName= "ORPHANED_" + objType + "_GROUP_"
                            + objID + "_" + groupType;
                    orphanGroups.put(groupName, newName);
                    // A given group should only be translated once, since the
                    // new name contains unique random elements which would be
                    // different every time.
                }

                // Just log a warning -- it's possible this Group was not
                // cleaned up when the associated DSpace Object was removed.
                // So, we don't want to throw an error and stop all other processing.
                log.warn("DSpace Object (ID='" + objID
                        + "', type ='" + objType
                        + "') no longer exists -- translating " + groupName
                        + " to " + newName + ".");

                return newName;
            }

            //Create an updated group name, using the Handle to replace the InternalID
            // Format: <DSpace-Obj-Type>_hdl:<Handle>_<Group-Type>
            return objTypeText + "_" + "hdl:" + dso.getHandle() + "_" + groupType;
        }
        catch (SQLException sqle)
        {
            throw new PackageException("Database error while attempting to translate group name ('" + groupName + "') for export.", sqle);
        }
    }


    /**
     * This method does the exact opposite of the translateGroupNameForExport()
     * method.  It prepares group names for import by replacing any found
     * external Handle identifiers with the appropriate DSpace Internal
     * identifier.  As a basic example, it would change a group named
     * "COLLECTION_hdl:123456789/10_ADMIN" to a name similar to
     * "COLLECTION_11_ADMIN (where '11' is the internal ID of that Collection).
     * <P>
     * If the group name either doesn't have an embedded handle, then it is
     * returned as is.  If it has an embedded handle, but the corresponding
     * internal ID cannot be determined, then an error is thrown.  It is up
     * to the calling method whether that error should be displayed to the user
     * or if the group should just be skipped (since its associated object
     * doesn't currently exist).
     * <p>
     * This method may be useful to any Crosswalks/Packagers which deal with
     * import/export of DSpace Groups.
     * <p>
     * Also see the translateGroupNameForExport() method which does the opposite
     * of this method.
     *
     * @param context current DSpace Context
     * @param groupName Group's name
     * @return the group name, with any Handles translated to internal IDs
     * @throws PackageException if package error
     */
    public static String translateGroupNameForImport(Context context, String groupName)
            throws PackageException
    {
        // Check if this looks like a default Group name -- must have at LEAST two underscores surrounded by other characters
        if(!groupName.matches("^.+_.+_.+$"))
        {
            //if this is not a valid default group name, just return group name as-is (no crosswalking necessary)
            return groupName;
        }

        //Pull apart default group name into its three main parts
        // Format: <DSpace-Obj-Type>_<DSpace-Obj-ID>_<Group-Type>
        // (e.g. COLLECTION_123_ADMIN)
        String objType = groupName.substring(0, groupName.indexOf('_'));
        String tmpEndString = groupName.substring(groupName.indexOf('_')+1);
        String objID = tmpEndString.substring(0, tmpEndString.indexOf('_'));
        String groupType = tmpEndString.substring(tmpEndString.indexOf('_')+1);

        try
        {
            if(objID.startsWith("hdl:"))
            {
                //We'll translate this handle into an internal ID
                //Format for Handle => "hdl:<handle-prefix>/<handle-suffix>"
                // (e.g. "hdl:123456789/10")

                //First, get the object via the Handle
                DSpaceObject dso = handleService.resolveToObject(context, objID.substring(4));

                if(dso==null)
                {
                    //throw an error as we cannot accurately rename/recreate this Group without its related DSpace Object
                    throw new PackageException("Unable to translate Handle to Internal ID in group named '" + groupName + "' as DSpace Object (Handle='" + objID + "') does not exist.");
                }

                //verify our group specified object Type corresponds to this object's type
                if(Constants.getTypeID(objType)!=dso.getType())
                {
                    throw new PackageValidationException("DSpace Object referenced by handle '" + objID + "' does not correspond to the object type specified by Group named '" + groupName + "'.  This Group doesn't seem to correspond to this DSpace Object!");
                }

                //Create an updated group name, using the Internal ID to replace the Handle
                // Format: <DSpace-Obj-Type>_<DSpace-Obj-ID>_<Group-Type>
                return objType + "_" + dso.getID() + "_" + groupType;
            }
            else // default -- return group name as is
            {
                return groupName;
            }
        }
        catch (SQLException sqle)
        {
            throw new PackageException("Database error while attempting to translate group name ('" + groupName + "') for import.", sqle);
        }
    }

}
