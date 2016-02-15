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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;
import org.dspace.license.CreativeCommons;
import org.dspace.workflow.WorkflowManager;
import org.dspace.xmlworkflow.XmlWorkflowManager;

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
    private static final String ccMetadataMap[] =
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
    private static final Map<String,String> ccMetadataToDC = new HashMap<String,String>();
    private static final Map<String,String> ccDCToMetadata = new HashMap<String,String>();
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
     */
    public static void checkItemMetadata(Item item)
        throws PackageValidationException
    {
        Metadatum t[] = item.getDC( "title", null, Item.ANY);
        if (t == null || t.length == 0)
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
     */
    public static void addDepositLicense(Context context, String license,
                                       Item item, Collection collection)
        throws SQLException, IOException, AuthorizeException
    {
        if (license == null)
        {
            license = collection.getLicense();
        }
        InputStream lis = new ByteArrayInputStream(license.getBytes());

        Bundle lb;
        //If LICENSE bundle is missing, create it
        Bundle[] bundles = item.getBundles(Constants.LICENSE_BUNDLE_NAME);
        if(bundles==null || bundles.length==0)
        {
            lb = item.createBundle(Constants.LICENSE_BUNDLE_NAME);
        }
        else
        {
            lb = bundles[0];
        }

        //Create the License bitstream
        Bitstream lbs = lb.createBitstream(lis);
        lis.close();
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(context, "License");
        if (bf == null)
        {
            bf = FormatIdentifier.guessFormat(context, lbs);
        }
        lbs.setFormat(bf);
        lbs.setName(Constants.LICENSE_BITSTREAM_NAME);
        lbs.setSource(Constants.LICENSE_BITSTREAM_NAME);
        lbs.update();
    }

    /**
     * Find bitstream by its Name, looking in all bundles.
     *
     * @param item Item whose bitstreams to search.
     * @param name Bitstream's name to match.
     * @return first bitstream found or null.
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
     */
    public static Bitstream getBitstreamByName(Item item, String bsName, String bnName)
        throws SQLException
    {
        Bundle[] bundles;
        if (bnName == null)
        {
            bundles = item.getBundles();
        }
        else
        {
            bundles = item.getBundles(bnName);
        }
        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int k = 0; k < bitstreams.length; k++)
            {
                if (bsName.equals(bitstreams[k].getName()))
                {
                    return bitstreams[k];
                }
            }
        }
        return null;
    }

    /**
     * Find bitstream by its format, looking in a specific bundle.
     * Used to look for particularly-typed Package Manifest bitstreams.
     *
     * @param item - dspace item whose bundles to search.
     * @param bsf - BitstreamFormat object to match.
     * @param bnName - bundle name to match, or null for all.
     * @return the format found or null if none found.
     */
    public static Bitstream getBitstreamByFormat(Item item,
            BitstreamFormat bsf, String bnName)
        throws SQLException
    {
        int fid = bsf.getID();
        Bundle[] bundles;
        if (bnName == null)
        {
            bundles = item.getBundles();
        }
        else
        {
            bundles = item.getBundles(bnName);
        }
        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int k = 0; k < bitstreams.length; k++)
            {
                if (bitstreams[k].getFormat().getID() == fid)
                {
                    return bitstreams[k];
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
                bn.getName().equals(CreativeCommons.CC_BUNDLE_NAME) ||
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
     * @param internal value for the 'internal' flag of a new format if created.
     * @return BitstreamFormat object that was found or created.  Never null.
     */
     public static BitstreamFormat findOrCreateBitstreamFormat(Context context,
            String shortDesc, String MIMEType, String desc, int supportLevel, boolean internal)
        throws SQLException, AuthorizeException
     {
        BitstreamFormat bsf = BitstreamFormat.findByShortDescription(context,
                                shortDesc);
        // not found, try to create one
        if (bsf == null)
        {
            bsf = BitstreamFormat.create(context);
            bsf.setShortDescription(shortDesc);
            bsf.setMIMEType(MIMEType);
            bsf.setDescription(desc);
            bsf.setSupportLevel(supportLevel);
            bsf.setInternal(internal);
            bsf.update();
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
     *
     * @throws IOException
     *             if the license bitstream can't be read
     */
    public static Bitstream findDepositLicense(Context context, Item item)
            throws SQLException, IOException, AuthorizeException
    {
        // get license format ID
        int licenseFormatId = -1;
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(context,
                "License");
        if (bf != null)
        {
            licenseFormatId = bf.getID();
        }

        Bundle[] bundles = item.getBundles(Constants.LICENSE_BUNDLE_NAME);
        for (int i = 0; i < bundles.length; i++)
        {
            // Assume license will be in its own bundle
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for(int j=0; j < bitstreams.length; j++)
            {
                // The License should have a file format of "License"
                if (bitstreams[j].getFormat().getID() == licenseFormatId)
                {
                    //found a bitstream with format "License" -- return it
                    return bitstreams[j];
                }
            }

            // If we couldn't find a bitstream with format = "License",
            // we will just assume the first bitstream is the deposit license
            // (usually a safe assumption as it is in the LICENSE bundle)
            if(bitstreams.length>0)
            {
                return bitstreams[0];
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
     * @throws AuthorizeException
     * @throws SQLException
     * @throws IOException
     */
    public static DSpaceObject createDSpaceObject(Context context, DSpaceObject parent, int type, String handle, PackageParameters params)
        throws AuthorizeException, SQLException, IOException
    {
        DSpaceObject dso = null;

        switch (type)
        {
            case Constants.COLLECTION:
                dso = ((Community)parent).createCollection(handle);
                return dso;

            case Constants.COMMUNITY:
                // top-level community?
                if (parent == null || parent.getType() == Constants.SITE)
                {
                    dso = Community.create(null, context, handle);
                }
                else
                {
                    dso = ((Community) parent).createSubcommunity(handle);
                }
                return dso;

            case Constants.ITEM:
                //Initialize a WorkspaceItem
                //(Note: Handle is not set until item is finished)
                WorkspaceItem wsi = WorkspaceItem.create(context, (Collection)parent, params.useCollectionTemplate());

                // Please note that we are returning an Item which is *NOT* yet in the Archive,
                // and doesn't yet have a handle assigned.
                // This Item will remain "incomplete" until 'PackageUtils.finishCreateItem()' is called
                return wsi.getItem();
                
            case Constants.SITE:
                return Site.find(context, Site.SITE_ID);
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
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    public static Item finishCreateItem(Context context, WorkspaceItem wsi, String handle, PackageParameters params)
            throws IOException, SQLException, AuthorizeException {
        // if we are restoring/replacing existing object using the package
        if (params.restoreModeEnabled())
        {
            // Restore & install item immediately
            //(i.e. skip over any Collection workflows, as we are essentially restoring item from backup)
            InstallItem.restoreItem(context, wsi, handle);

            //return newly restored item
            return wsi.getItem();
        }
        // if we are treating package as a SIP, and we are told to respect workflows
        else if (params.workflowEnabled())
        {
            // Start an item workflow
            // (NOTICE: The specified handle is ignored, as Workflows *always* end in a new handle being assigned)
            if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow")) {
                try {
                    return XmlWorkflowManager.startWithoutNotify(context, wsi).getItem();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            } else {
                return WorkflowManager.startWithoutNotify(context, wsi).getItem();
            }
        }

        // default: skip workflow, but otherwise normal submission (i.e. package treated like a SIP)
        else
        {
            // Install item immediately with the specified handle
            InstallItem.installItem(context, wsi, handle);

            // return newly installed item
            return wsi.getItem();
        }
    }//end finishCreateItem


    /**
     * Commit all recent changes to DSpaceObject.
     * <p>
     * This method is necessary as there is no generic 'update()' on a DSpaceObject
     *
     * @param dso DSpaceObject to update
     */
    public static void updateDSpaceObject(DSpaceObject dso)
            throws AuthorizeException, SQLException, IOException
    {
        if (dso != null)
        {
            switch (dso.getType())
            {
                case Constants.BITSTREAM:
                    ((Bitstream)dso).update();
                    break;
                case Constants.ITEM:
                    ((Item)dso).update();
                    break;
                case Constants.COLLECTION:
                    ((Collection)dso).update();
                    break;
                case Constants.COMMUNITY:
                    ((Community)dso).update();
                    break;
            }
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
     * @param file
     * @return boolean true if succeeded, false otherwise
     * @throws IOException
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
     * If this object is an Item, it removes all bundles & bitstreams.  If this
     * object is a Community or Collection, it removes all logo bitstreams.
     * <P>
     * This method is useful for replace functionality.
     *
     * @param dso The object to remove all bitstreams from
     */
    public static void removeAllBitstreams(DSpaceObject dso)
            throws SQLException, IOException, AuthorizeException
    {
        //If we are dealing with an Item
        if(dso.getType()==Constants.ITEM)
        {
            Item item = (Item) dso;
            // Get a reference to all Bundles in Item (which contain the bitstreams)
            Bundle[] bunds = item.getBundles();

            // Remove each bundle -- this will in turn remove all bitstreams associated with this Item.
            for (int i = 0; i < bunds.length; i++)
            {
                item.removeBundle(bunds[i]);
            }
        }
        else if (dso.getType()==Constants.COLLECTION)
        {
            Collection collection = (Collection) dso;
            //clear out the logo for this collection
            collection.setLogo(null);
        }
        else if (dso.getType()==Constants.COMMUNITY)
        {
            Community community = (Community) dso;
            //clear out the logo for this community
            community.setLogo(null);
        }
    }


    /**
     * Removes all metadata associated with a DSpace object.
     * <P>
     * This method is useful for replace functionality.
     *
     * @param dso The object to remove all metadata from
     */
    public static void clearAllMetadata(DSpaceObject dso)
            throws SQLException, IOException, AuthorizeException
    {
        //If we are dealing with an Item
        if(dso.getType()==Constants.ITEM)
        {
            Item item = (Item) dso;
            //clear all metadata entries
            item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
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
                    collection.setMetadata(dbField, null);
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
                    community.setMetadata(dbField, null);
                }
                catch(IllegalArgumentException ie)
                {
                    // ignore the error -- just means the field doesn't exist in DB
                    // Communities & Collections don't include the exact same metadata fields
                }
            }
        }
    }

    /** Recognize and pick apart likely "magic" group names */
    private static final Pattern groupAnalyzer
        = Pattern.compile("^(COMMUNITY|COLLECTION)_([0-9]+)_(.+)");

    /** Lookaside list for translations we've already done, so we don't generate
     * multiple names for the same group
     */
    private static final Map<String, String> orphanGroups = new HashMap<String, String>();

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
     * <li> COMMUNITY_[HANDLE]_ADMIN (e.g. COMMUNITY_hdl:123456789/10_ADMIN), etc.
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
     */
    public static String translateGroupNameForExport(Context context, String groupName)
            throws PackageException
    {
        // See if this resembles a default Group name
        Matcher matched = groupAnalyzer.matcher(groupName);
        if (!matched.matches())
            return groupName;

        // It does!  Pick out the components
        String objType = matched.group(1);
        String objID = matched.group(2);
        String groupType = matched.group(3);

        try
        {
            //We'll translate this internal ID into a Handle

            //First, get the object via the Internal ID
            DSpaceObject dso = DSpaceObject.find(context, Constants
                    .getTypeID(objType), Integer.parseInt(objID));

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
            return objType + "_" + "hdl:" + dso.getHandle() + "_" + groupType;
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
                DSpaceObject dso = HandleManager.resolveToObject(context, objID.substring(4));

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
