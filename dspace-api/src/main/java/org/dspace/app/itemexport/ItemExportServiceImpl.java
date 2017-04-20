/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.MessagingException;
import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Item exporter to create simple AIPs for DSpace content. Currently exports
 * individual items, or entire collections. For instructions on use, see
 * printUsage() method.
 * <P>
 * ItemExport creates the simple AIP package that the importer also uses. It
 * consists of:
 * <P>
 * /exportdir/42/ (one directory per item) / dublin_core.xml - qualified dublin
 * core in RDF schema / contents - text file, listing one file per line / file1
 * - files contained in the item / file2 / ...
 * <P>
 * issues -doesn't handle special characters in metadata (needs to turn {@code &'s} into
 * {@code &amp;}, etc.)
 * <P>
 * Modified by David Little, UCSD Libraries 12/21/04 to allow the registration
 * of files (bitstreams) into DSpace.
 *
 * @author David Little
 * @author Jay Paz
 */
public class ItemExportServiceImpl implements ItemExportService
{
    protected final int SUBDIR_LIMIT = 0;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected EPersonService ePersonService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected HandleService handleService;


    /** log4j logger */
    private Logger log = Logger.getLogger(ItemExportServiceImpl.class);

    protected ItemExportServiceImpl()
    {

    }


    @Override
    public void exportItem(Context c, Iterator<Item> i,
            String destDirName, int seqStart, boolean migrate,
            boolean excludeBitstreams) throws Exception
    {
        int mySequenceNumber = seqStart;
        int counter = SUBDIR_LIMIT - 1;
        int subDirSuffix = 0;
        String fullPath = destDirName;
        String subdir = "";
        File dir;

        if (SUBDIR_LIMIT > 0)
        {
            dir = new File(destDirName);
            if (!dir.isDirectory())
            {
                throw new IOException(destDirName + " is not a directory.");
            }
        }

        System.out.println("Beginning export");

        while (i.hasNext())
        {
            if (SUBDIR_LIMIT > 0 && ++counter == SUBDIR_LIMIT)
            {
                subdir = Integer.valueOf(subDirSuffix++).toString();
                fullPath = destDirName + File.separatorChar + subdir;
                counter = 0;

                if (!new File(fullPath).mkdirs())
                {
                    throw new IOException("Error, can't make dir " + fullPath);
                }
            }

            System.out.println("Exporting item to " + mySequenceNumber);
            Item item = i.next();
            exportItem(c, item, fullPath, mySequenceNumber, migrate, excludeBitstreams);
            c.uncacheEntity(item);
            mySequenceNumber++;
        }
    }

    protected void exportItem(Context c, Item myItem, String destDirName,
            int seqStart, boolean migrate, boolean excludeBitstreams) throws Exception
    {
        File destDir = new File(destDirName);

        if (destDir.exists())
        {
            // now create a subdirectory
            File itemDir = new File(destDir + "/" + seqStart);
            
            System.out.println("Exporting Item " + myItem.getID() +
            		(myItem.getHandle() != null ? ", handle " + myItem.getHandle() : "") +
            		" to " + itemDir);

            if (itemDir.exists())
            {
                throw new Exception("Directory " + destDir + "/" + seqStart
                        + " already exists!");
            }

            if (itemDir.mkdir())
            {
                // make it this far, now start exporting
                writeMetadata(c, myItem, itemDir, migrate);
                writeBitstreams(c, myItem, itemDir, excludeBitstreams);
                if (!migrate)
                {
                    writeHandle(c, myItem, itemDir);
                }
            }
            else
            {
                throw new Exception("Error, can't make dir " + itemDir);
            }
        }
        else
        {
            throw new Exception("Error, directory " + destDirName
                    + " doesn't exist!");
        }
    }

    /**
     * Discover the different schemas in use and output a separate metadata XML
     * file for each schema.
     *
     * @param c DSpace context
     * @param i DSpace Item
     * @param destDir destination directory
     * @param migrate Whether to use the migrate option or not
     * @throws Exception if error
     */
    protected void writeMetadata(Context c, Item i, File destDir, boolean migrate)
            throws Exception
    {
        Set<String> schemas = new HashSet<String>();
        List<MetadataValue> dcValues = itemService.getMetadata(i, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (MetadataValue metadataValue : dcValues)
        {
            schemas.add(metadataValue.getMetadataField().getMetadataSchema().getName());
        }

        // Save each of the schemas into it's own metadata file
        for (String schema : schemas)
        {
            writeMetadata(c, schema, i, destDir, migrate);
        }
    }

    /**
     * output the item's dublin core into the item directory
     * @param c DSpace context
     * @param schema schema
     * @param i DSpace Item
     * @param destDir destination directory
     * @param migrate Whether to use the migrate option or not
     * @throws Exception if error
     */
    protected void writeMetadata(Context c, String schema, Item i,
            File destDir, boolean migrate) throws Exception
    {
        String filename;
        if (schema.equals(MetadataSchema.DC_SCHEMA))
        {
            filename = "dublin_core.xml";
        }
        else
        {
            filename = "metadata_" + schema + ".xml";
        }

        File outFile = new File(destDir, filename);

        System.out.println("Attempting to create file " + outFile);

        if (outFile.createNewFile())
        {
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(outFile));

            List<MetadataValue> dcorevalues = itemService.getMetadata(i, schema, Item.ANY, Item.ANY,
                    Item.ANY);

            // XML preamble
            byte[] utf8 = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n"
                    .getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            String dcTag = "<dublin_core schema=\"" + schema + "\">\n";
            utf8 = dcTag.getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            String dateIssued = null;
            String dateAccessioned = null;

            for (MetadataValue dcv : dcorevalues)
            {
                MetadataField metadataField = dcv.getMetadataField();
                String qualifier = metadataField.getQualifier();

                if (qualifier == null)
                {
                    qualifier = "none";
                }

                String language = dcv.getLanguage();

                if (language != null)
                {
                    language = " language=\"" + language + "\"";
                }
                else
                {
                    language = "";
                }

                utf8 = ("  <dcvalue element=\"" + metadataField.getElement() + "\" "
                        + "qualifier=\"" + qualifier + "\""
                        + language + ">"
                        + Utils.addEntities(dcv.getValue()) + "</dcvalue>\n")
                        .getBytes("UTF-8");

                if ((!migrate) ||
                    (migrate && !(
                     ("date".equals(metadataField.getElement()) && "issued".equals(qualifier)) ||
                     ("date".equals(metadataField.getElement()) && "accessioned".equals(qualifier)) ||
                     ("date".equals(metadataField.getElement()) && "available".equals(qualifier)) ||
                     ("identifier".equals(metadataField.getElement()) && "uri".equals(qualifier) &&
                      (dcv.getValue() != null && dcv.getValue().startsWith("http://hdl.handle.net/" +
                          handleService.getPrefix() + "/"))) ||
                     ("description".equals(metadataField.getElement()) && "provenance".equals(qualifier)) ||
                     ("format".equals(metadataField.getElement()) && "extent".equals(qualifier)) ||
                     ("format".equals(metadataField.getElement()) && "mimetype".equals(qualifier)))))
                {
                    out.write(utf8, 0, utf8.length);
                }

                // Store the date issued and accession to see if they are different
                // because we need to keep date.issued if they are, when migrating
                if (("date".equals(metadataField.getElement()) && "issued".equals(qualifier)))
                {
                    dateIssued = dcv.getValue();
                }
                if (("date".equals(metadataField.getElement()) && "accessioned".equals(qualifier)))
                {
                    dateAccessioned = dcv.getValue();
                }
            }

            // When migrating, only keep date.issued if it is different to date.accessioned
            if ((migrate) &&
                (dateIssued != null) &&
                (dateAccessioned != null) &&
                (!dateIssued.equals(dateAccessioned)))
            {
                utf8 = ("  <dcvalue element=\"date\" "
                        + "qualifier=\"issued\">"
                        + Utils.addEntities(dateIssued) + "</dcvalue>\n")
                        .getBytes("UTF-8");
                out.write(utf8, 0, utf8.length);
            }

            utf8 = "</dublin_core>\n".getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            out.close();
        }
        else
        {
            throw new Exception("Cannot create dublin_core.xml in " + destDir);
        }
    }

    /**
     * create the file 'handle' which contains the handle assigned to the item
     * @param c DSpace Context
     * @param i DSpace Item
     * @param destDir destination directory
     * @throws Exception if error
     */
    protected void writeHandle(Context c, Item i, File destDir)
            throws Exception
    {
        if (i.getHandle() == null)
        {
            return;
        }
        String filename = "handle";

        File outFile = new File(destDir, filename);

        if (outFile.createNewFile())
        {
            PrintWriter out = new PrintWriter(new FileWriter(outFile));

            out.println(i.getHandle());

            // close the contents file
            out.close();
        }
        else
        {
            throw new Exception("Cannot create file " + filename + " in "
                    + destDir);
        }
    }

    /**
     * Create both the bitstreams and the contents file. Any bitstreams that
     * were originally registered will be marked in the contents file as such.
     * However, the export directory will contain actual copies of the content
     * files being exported.
     *
     * @param c
     *            the DSpace context
     * @param i
     *            the item being exported
     * @param destDir
     *            the item's export directory
     * @param excludeBitstreams
     *             whether to exclude bitstreams
     * @throws Exception if error
     *             if there is any problem writing to the export directory
     */
    protected void writeBitstreams(Context c, Item i, File destDir,
    		boolean excludeBitstreams) throws Exception
    {
        File outFile = new File(destDir, "contents");

        if (outFile.createNewFile())
        {
            PrintWriter out = new PrintWriter(new FileWriter(outFile));

            List<Bundle> bundles = i.getBundles();

            for (Bundle bundle : bundles) {
                // bundles can have multiple bitstreams now...
                List<Bitstream> bitstreams = bundle.getBitstreams();

                String bundleName = bundle.getName();

                for (Bitstream bitstream : bitstreams) {
                    String myName = bitstream.getName();
                    String oldName = myName;

                    String description = bitstream.getDescription();
                    if (!StringUtils.isEmpty(description)) {
                        description = "\tdescription:" + description;
                    } else {
                        description = "";
                    }

                    String primary = "";
                    if (bitstream.equals(bundle.getPrimaryBitstream())) {
                        primary = "\tprimary:true ";
                    }

                    int myPrefix = 1; // only used with name conflict

                    boolean isDone = false; // done when bitstream is finally
                    // written

                    while (!excludeBitstreams && !isDone) {
                        if (myName.contains(File.separator)) {
                            String dirs = myName.substring(0, myName
                                    .lastIndexOf(File.separator));
                            File fdirs = new File(destDir + File.separator
                                    + dirs);
                            if (!fdirs.exists() && !fdirs.mkdirs()) {
                                log.error("Unable to create destination directory");
                            }
                        }

                        File fout = new File(destDir, myName);

                        if (fout.createNewFile()) {
                        	InputStream is = bitstreamService.retrieve(c, bitstream);
                            FileOutputStream fos = new FileOutputStream(fout);
                            Utils.bufferedCopy(is, fos);
                            // close streams
                            is.close();
                            fos.close();

                            isDone = true;
                        } else {
                            myName = myPrefix + "_" + oldName; // keep
                            // appending
                            // numbers to the
                            // filename until
                            // unique
                            myPrefix++;
                        }
                    }
                    
                    // write the manifest file entry
                    if (bitstreamService.isRegisteredBitstream(bitstream)) {
                        out.println("-r -s " + bitstream.getStoreNumber()
                                + " -f " + myName +
                                "\tbundle:" + bundleName +
                                primary + description);
                    } else {
                        out.println(myName + "\tbundle:" + bundleName +
                                primary + description);
                    }
                    
                }
            }

            // close the contents file
            out.close();
        }
        else
        {
            throw new Exception("Cannot create contents in " + destDir);
        }
    }

    @Override
    public void exportAsZip(Context context, Iterator<Item> items,
                                   String destDirName, String zipFileName,
                                   int seqStart, boolean migrate,
                                   boolean excludeBitstreams) throws Exception
                                   
    {
        String workDir = getExportWorkDirectory() +
                         System.getProperty("file.separator") +
                         zipFileName;

        File wkDir = new File(workDir);
        if (!wkDir.exists() && !wkDir.mkdirs())
        {
            log.error("Unable to create working direcory");
        }

        File dnDir = new File(destDirName);
        if (!dnDir.exists() && !dnDir.mkdirs())
        {
            log.error("Unable to create destination directory");
        }

        // export the items using normal export method
        exportItem(context, items, workDir, seqStart, migrate, excludeBitstreams);

        // now zip up the export directory created above
        zip(workDir, destDirName + System.getProperty("file.separator") + zipFileName);
    }

    @Override
    public void createDownloadableExport(DSpaceObject dso,
            Context context, boolean migrate) throws Exception
    {
        EPerson eperson = context.getCurrentUser();
        ArrayList<DSpaceObject> list = new ArrayList<DSpaceObject>(1);
        list.add(dso);
        processDownloadableExport(list, context, eperson == null ? null
                : eperson.getEmail(), migrate);
    }

    @Override
    public void createDownloadableExport(List<DSpaceObject> dsObjects,
            Context context, boolean migrate) throws Exception
    {
        EPerson eperson = context.getCurrentUser();
        processDownloadableExport(dsObjects, context, eperson == null ? null
                : eperson.getEmail(), migrate);
    }

    @Override
    public void createDownloadableExport(DSpaceObject dso,
            Context context, String additionalEmail, boolean migrate) throws Exception
    {
        ArrayList<DSpaceObject> list = new ArrayList<DSpaceObject>(1);
        list.add(dso);
        processDownloadableExport(list, context, additionalEmail, migrate);
    }

    @Override
    public void createDownloadableExport(List<DSpaceObject> dsObjects,
            Context context, String additionalEmail, boolean migrate) throws Exception
    {
        processDownloadableExport(dsObjects, context, additionalEmail, migrate);
    }

    /**
     * Does the work creating a List with all the Items in the Community or
     * Collection It then kicks off a new Thread to export the items, zip the
     * export directory and send confirmation email
     *
     * @param dsObjects
     *            - List of dspace objects to process
     * @param context
     *            - the dspace context
     * @param additionalEmail
     *            - email address to cc in addition the the current user email
     * @param toMigrate Whether to use the migrate option or not
     * @throws Exception if error
     */
    protected void processDownloadableExport(List<DSpaceObject> dsObjects,
            Context context, final String additionalEmail, boolean toMigrate) throws Exception
    {
        final EPerson eperson = context.getCurrentUser();
        final boolean migrate = toMigrate;

        // before we create a new export archive lets delete the 'expired'
        // archives
        //deleteOldExportArchives(eperson.getID());
        deleteOldExportArchives();

        // keep track of the commulative size of all bitstreams in each of the
        // items
        // it will be checked against the config file entry
        double size = 0;
        final HashMap<String, List<UUID>> itemsMap = new HashMap<>();
        for (DSpaceObject dso : dsObjects)
        {
            if (dso.getType() == Constants.COMMUNITY)
            {
                Community community = (Community) dso;
                // get all the collections in the community
                List<Collection> collections = communityService.getAllCollections(context, community);
                for (Collection collection : collections)
                {
                    ArrayList<UUID> items = new ArrayList<>();
                    // get all the items in each collection
                    Iterator<Item> iitems = itemService.findByCollection(context, collection);
                    try
                    {
                        while (iitems.hasNext())
                        {
                            Item item = iitems.next();
                            // get all the bundles in the item
                            List<Bundle> bundles = item.getBundles();
                            for (Bundle bundle : bundles)
                            {
                                // get all the bitstreams in each bundle
                                List<Bitstream> bitstreams = bundle.getBitstreams();
                                for (Bitstream bitstream : bitstreams)
                                {
                                    // add up the size
                                    size += bitstream.getSize();
                                }
                            }
                            items.add(item.getID());
                        }
                    }
                    finally
                    {
                        if (items.size() > 0)
                        {
                            itemsMap.put("collection_"+collection.getID(), items);
                        }
                    }
                }
            }
            else if (dso.getType() == Constants.COLLECTION)
            {
                Collection collection = (Collection) dso;
                ArrayList<UUID> items = new ArrayList<>();

                // get all the items in the collection
                Iterator<Item> iitems = itemService.findByCollection(context, collection);
                try
                {
                    while (iitems.hasNext())
                    {
                        Item item = iitems.next();
                        // get all thebundles in the item
                        List<Bundle> bundles = item.getBundles();
                        for (Bundle bundle : bundles)
                        {
                            // get all the bitstreams in the bundle
                            List<Bitstream> bitstreams = bundle.getBitstreams();
                            for (Bitstream bitstream : bitstreams)
                            {
                                // add up the size
                                size += bitstream.getSize();
                            }
                        }
                        items.add(item.getID());
                    }
                }
                finally
                {
                    if (items.size() > 0)
                    {
                        itemsMap.put("collection_"+collection.getID(), items);
                    }
                }
            }
            else if (dso.getType() == Constants.ITEM)
            {
                Item item = (Item) dso;
                // get all the bundles in the item
                List<Bundle> bundles = item.getBundles();
                for (Bundle bundle : bundles)
                {
                    // get all the bitstreams in the bundle
                    List<Bitstream> bitstreams = bundle.getBitstreams();
                    for (Bitstream bitstream : bitstreams)
                    {
                        // add up the size
                        size += bitstream.getSize();
                    }
                }
                ArrayList<UUID> items = new ArrayList<>();
                items.add(item.getID());
                itemsMap.put("item_"+item.getID(), items);
            }
            else
            {
                // nothing to do just ignore this type of DSpaceObject
            }
        }

        // check the size of all the bitstreams against the configuration file
        // entry if it exists
        String megaBytes = ConfigurationManager
                .getProperty("org.dspace.app.itemexport.max.size");
        if (megaBytes != null)
        {
            float maxSize = 0;
            try
            {
                maxSize = Float.parseFloat(megaBytes);
            }
            catch (Exception e)
            {
                // ignore...configuration entry may not be present
            }

            if (maxSize > 0 && maxSize < (size / 1048576.00))
            { // a megabyte
                throw new ItemExportException(ItemExportException.EXPORT_TOO_LARGE,
                                              "The overall size of this export is too large.  Please contact your administrator for more information.");
            }
        }

        // if we have any items to process then kick off anonymous thread
        if (itemsMap.size() > 0)
        {
            Thread go = new Thread()
            {
                @Override
                public void run()
                {
                    Context context = null;
                    Iterator<Item> iitems = null;
                    try
                    {
                        // create a new dspace context
                        context = new Context();
                        // ignore auths
                        context.turnOffAuthorisationSystem();

                        String fileName = assembleFileName("item", eperson,
                                new Date());
                        String workParentDir = getExportWorkDirectory()
                                + System.getProperty("file.separator")
                                + fileName;
                        String downloadDir = getExportDownloadDirectory(eperson);
                        File dnDir = new File(downloadDir);
                        if (!dnDir.exists() && !dnDir.mkdirs())
                        {
                            log.error("Unable to create download directory");
                        }

                        Iterator<String> iter = itemsMap.keySet().iterator();
                        while(iter.hasNext())
                        {
                            String keyName = iter.next();
                            List<UUID> uuids = itemsMap.get(keyName);
                            List<Item> items = new ArrayList<Item>();
                            for (UUID uuid : uuids) {
                                items.add(itemService.find(context, uuid));
                            }
                            iitems = items.iterator();

                            String workDir = workParentDir 
                                    + System.getProperty("file.separator")
                                    + keyName;

                            File wkDir = new File(workDir);
                            if (!wkDir.exists() && !wkDir.mkdirs())
                            {
                                log.error("Unable to create working directory");
                            }


                            // export the items using normal export method
                            exportItem(context, iitems, workDir, 1, migrate, false);
                        }

                        // now zip up the export directory created above
                        zip(workParentDir, downloadDir
                                + System.getProperty("file.separator")
                                + fileName + ".zip");
                        // email message letting user know the file is ready for
                        // download
                        emailSuccessMessage(context, eperson, fileName + ".zip");
                        // return to enforcing auths
                        context.restoreAuthSystemState();
                    }
                    catch (Exception e1)
                    {
                        try
                        {
                            emailErrorMessage(eperson, e1.getMessage());
                        }
                        catch (Exception e)
                        {
                            // wont throw here
                        }
                        throw new IllegalStateException(e1);
                    }
                    finally
                    {
                        // Make sure the database connection gets closed in all conditions.
                    	try {
							context.complete();
						} catch (SQLException sqle) {
							context.abort();
						}
                    }
                }

            };

            go.isDaemon();
            go.start();
        }
        else
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            emailErrorMessage(eperson, I18nUtil.getMessage("org.dspace.app.itemexport.no-result", supportedLocale));
        }
    }

    @Override
    public String assembleFileName(String type, EPerson eperson,
            Date date) throws Exception
    {
        // to format the date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MMM_dd");
        String downloadDir = getExportDownloadDirectory(eperson);
        // used to avoid name collision
        int count = 1;
        boolean exists = true;
        String fileName = null;
        while (exists)
        {
            fileName = type + "_export_" + sdf.format(date) + "_" + count + "_"
                    + eperson.getID();
            exists = new File(downloadDir
                    + System.getProperty("file.separator") + fileName + ".zip")
                    .exists();
            count++;
        }
        return fileName;
    }

    @Override
    public String getExportDownloadDirectory(EPerson ePerson)
            throws Exception
    {
        String downloadDir = ConfigurationManager
                .getProperty("org.dspace.app.itemexport.download.dir");
        if (downloadDir == null)
        {
            throw new Exception(
                    "A dspace.cfg entry for 'org.dspace.app.itemexport.download.dir' does not exist.");
        }
        File result = new File(downloadDir + System.getProperty("file.separator") + ePerson.getID());
        if(!result.exists() && ePerson.getLegacyId()!=null)
        {
            //Check for the old identifier
            result = new File(downloadDir + System.getProperty("file.separator") + ePerson.getLegacyId());
        }

        return result.getAbsolutePath();
    }

    @Override
    public String getExportWorkDirectory() throws Exception
    {
        String exportDir = ConfigurationManager
                .getProperty("org.dspace.app.itemexport.work.dir");
        if (exportDir == null)
        {
            throw new Exception(
                    "A dspace.cfg entry for 'org.dspace.app.itemexport.work.dir' does not exist.");
        }
        return exportDir;
    }

    @Override
    public InputStream getExportDownloadInputStream(String fileName,
            EPerson eperson) throws Exception
    {
        File file = new File(getExportDownloadDirectory(eperson)
                + System.getProperty("file.separator") + fileName);
        if (file.exists())
        {
            return new FileInputStream(file);
        }
        else
        {
            return null;
        }
    }

    @Override
    public long getExportFileSize(Context context, String fileName) throws Exception
    {
        String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
                fileName.lastIndexOf('.'));
        EPerson ePerson = getEPersonFromString(context, strID);


        File file = new File(
                getExportDownloadDirectory(ePerson)
                        + System.getProperty("file.separator") + fileName);
        if (!file.exists() || !file.isFile())
        {
            throw new FileNotFoundException("The file "
                    + getExportDownloadDirectory(ePerson)
                    + System.getProperty("file.separator") + fileName
                    + " does not exist.");
        }

        return file.length();
    }

    /**
     * Attempt to find an EPerson based on string ID
     * @param context DSpace context
     * @param strID string identifier
     * @return EPerson object (if found)
     * @throws SQLException if database error
     */
    protected EPerson getEPersonFromString(Context context, String strID) throws SQLException {
        EPerson eperson;
        try{
            UUID ePersonId = UUID.fromString(strID);
            eperson = ePersonService.find(context, ePersonId);
        }catch (Exception e){
            eperson = ePersonService.findByLegacyId(context, Integer.parseInt(strID));
        }
        return eperson;
    }

    @Override
    public long getExportFileLastModified(Context context, String fileName)
            throws Exception
    {
        String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
                fileName.lastIndexOf('.'));
        EPerson ePerson = getEPersonFromString(context, strID);

        File file = new File(
                getExportDownloadDirectory(ePerson)
                        + System.getProperty("file.separator") + fileName);
        if (!file.exists() || !file.isFile())
        {
            throw new FileNotFoundException("The file "
                    + getExportDownloadDirectory(ePerson)
                    + System.getProperty("file.separator") + fileName
                    + " does not exist.");
        }

        return file.lastModified();
    }

    @Override
    public boolean canDownload(Context context, String fileName)
    {
        EPerson eperson = context.getCurrentUser();
        if (eperson == null)
        {
            return false;
        }
        String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
                fileName.lastIndexOf('.'));
        try
        {
            if (strID.equals(eperson.getID().toString()))
            {
                return true;
            }
        }
        catch (Exception e)
        {
            return false;
        }
        return false;
    }

    @Override
    public List<String> getExportsAvailable(EPerson eperson)
            throws Exception
    {
        File downloadDir = new File(getExportDownloadDirectory(eperson));
        if (!downloadDir.exists() || !downloadDir.isDirectory())
        {
            return null;
        }

        List<String> fileNames = new ArrayList<String>();

        for (String fileName : downloadDir.list())
        {
            if (fileName.contains("export") && fileName.endsWith(".zip"))
            {
                fileNames.add(fileName);
            }
        }

        if (fileNames.size() > 0)
        {
            return fileNames;
        }

        return null;
    }

    @Override
    public void deleteOldExportArchives(EPerson eperson) throws Exception
    {
        int hours = ConfigurationManager
                .getIntProperty("org.dspace.app.itemexport.life.span.hours");
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.add(Calendar.HOUR, (-hours));
        File downloadDir = new File(getExportDownloadDirectory(eperson));
        if (downloadDir.exists())
        {
            File[] files = downloadDir.listFiles();
            for (File file : files)
            {
                if (file.lastModified() < now.getTimeInMillis())
                {
                    if (!file.delete())
                    {
                        log.error("Unable to delete export file");
                    }
                }
            }
        }

    }

    @Override
    public void deleteOldExportArchives() throws Exception
    {
        int hours = ConfigurationManager.getIntProperty("org.dspace.app.itemexport.life.span.hours");
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.add(Calendar.HOUR, (-hours));
        File downloadDir = new File(ConfigurationManager.getProperty("org.dspace.app.itemexport.download.dir"));
        if (downloadDir.exists())
        {
            // Get a list of all the sub-directories, potentially one for each ePerson.
            File[] dirs = downloadDir.listFiles();
            for (File dir : dirs)
            {
                // For each sub-directory delete any old files.
                File[] files = dir.listFiles();
                for (File file : files)
                {
                    if (file.lastModified() < now.getTimeInMillis())
                    {
                        if (!file.delete())
                        {
                            log.error("Unable to delete old files");
                        }
                    }
                }

                // If the directory is now empty then we delete it too.
                if (dir.listFiles().length == 0)
                {
                    if (!dir.delete())
                    {
                        log.error("Unable to delete directory");
                    }
                }
            }
        }

    }


    @Override
    public void emailSuccessMessage(Context context, EPerson eperson,
            String fileName) throws MessagingException
    {
        try
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "export_success"));
            email.addRecipient(eperson.getEmail());
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/exportdownload/" + fileName);
            email.addArgument(ConfigurationManager.getProperty("org.dspace.app.itemexport.life.span.hours"));

            email.send();
        }
        catch (Exception e)
        {
            log.warn(LogManager.getHeader(context, "emailSuccessMessage", "cannot notify user of export"), e);
        }
    }

    @Override
    public void emailErrorMessage(EPerson eperson, String error)
            throws MessagingException
    {
        log.warn("An error occurred during item export, the user will be notified. " + error);
        try
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "export_error"));
            email.addRecipient(eperson.getEmail());
            email.addArgument(error);
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/feedback");

            email.send();
        }
        catch (Exception e)
        {
            log.warn("error during item export error notification", e);
        }
    }

    @Override
    public void zip(String strSource, String target) throws Exception
    {
        ZipOutputStream cpZipOutputStream = null;
        String tempFileName = target + "_tmp";
        try
        {
            File cpFile = new File(strSource);
            if (!cpFile.isFile() && !cpFile.isDirectory())
            {
                return;
            }
            File targetFile = new File(tempFileName);
            if (!targetFile.createNewFile())
            {
                log.warn("Target file already exists: " + targetFile.getName());
            }

            FileOutputStream fos = new FileOutputStream(tempFileName);
            cpZipOutputStream = new ZipOutputStream(fos);
            cpZipOutputStream.setLevel(9);
            zipFiles(cpFile, strSource, tempFileName, cpZipOutputStream);
            cpZipOutputStream.finish();
            cpZipOutputStream.close();
            cpZipOutputStream = null;

            // Fix issue on Windows with stale file handles open before trying to delete them
            System.gc();

            deleteDirectory(cpFile);
            if (!targetFile.renameTo(new File(target)))
            {
                log.error("Unable to rename file");
            }
        }
        finally
        {
            if (cpZipOutputStream != null)
            {
                cpZipOutputStream.close();
            }
        }
    }

    /**
     * 
     * @param cpFile file
     * @param strSource source location
     * @param strTarget target location
     * @param cpZipOutputStream current zip outputstream
     * @throws Exception if error
     */
    protected void zipFiles(File cpFile, String strSource,
            String strTarget, ZipOutputStream cpZipOutputStream)
            throws Exception
    {
        int byteCount;
        final int DATA_BLOCK_SIZE = 2048;
        FileInputStream cpFileInputStream = null;
        if (cpFile.isDirectory())
        {
            File[] fList = cpFile.listFiles();
            for (File aFList : fList) {
                zipFiles(aFList, strSource, strTarget, cpZipOutputStream);
            }
        }
        else
        {
            try
            {
                if (cpFile.getAbsolutePath().equalsIgnoreCase(strTarget))
                {
                    return;
                }
                String strAbsPath = cpFile.getPath();
                String strZipEntryName = strAbsPath.substring(strSource
                        .length() + 1, strAbsPath.length());

                // byte[] b = new byte[ (int)(cpFile.length()) ];

                cpFileInputStream = new FileInputStream(cpFile);

                ZipEntry cpZipEntry = new ZipEntry(strZipEntryName);
                cpZipOutputStream.putNextEntry(cpZipEntry);

                byte[] b = new byte[DATA_BLOCK_SIZE];
                while ((byteCount = cpFileInputStream.read(b, 0,
                        DATA_BLOCK_SIZE)) != -1)
                {
                    cpZipOutputStream.write(b, 0, byteCount);
                }

                // cpZipOutputStream.write(b, 0, (int)cpFile.length());
            }
            finally
            {
                if (cpFileInputStream != null)
                {
                    cpFileInputStream.close();
                }
                cpZipOutputStream.closeEntry();
            }
        }
    }

    /**
     * Delete a directory
     * @param path directory path
     * @return true if successful, false otherwise
     */
    protected boolean deleteDirectory(File path)
    {
        if (path.exists())
        {
            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        log.error("Unable to delete file: " + file.getName());
                    }
                }
            }
        }

        return (path.delete());
    }

}
