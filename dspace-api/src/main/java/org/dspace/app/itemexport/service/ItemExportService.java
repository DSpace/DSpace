/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport.service;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import javax.mail.MessagingException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
public interface ItemExportService {

    /**
     * used for export download
     */
    public static final String COMPRESSED_EXPORT_MIME_TYPE = "application/zip";

    public void exportItem(Context c, Iterator<Item> i,
            String destDirName, int seqStart, boolean migrate,
            boolean excludeBitstreams) throws Exception;

    /**
     * Method to perform an export and save it as a zip file.
     *
     * @param context The DSpace Context
     * @param items The items to export
     * @param destDirName The directory to save the export in
     * @param zipFileName The name to save the zip file as
     * @param seqStart The first number in the sequence
     * @param migrate Whether to use the migrate option or not
     * @param excludeBitstreams Whether to exclude bitstreams or not
     * @throws Exception if error
     */
    public void exportAsZip(Context context, Iterator<Item> items,
                                   String destDirName, String zipFileName,
                                   int seqStart, boolean migrate,
                                   boolean excludeBitstreams) throws Exception;

    /**
     * Convenience methot to create export a single Community, Collection, or
     * Item
     *
     * @param dso
     *            - the dspace object to export
     * @param context
     *            - the dspace context
     * @param migrate Whether to use the migrate option or not
     * @throws Exception if error
     */
    public void createDownloadableExport(DSpaceObject dso,
            Context context, boolean migrate) throws Exception;

    /**
     * Convenience method to export a List of dspace objects (Community,
     * Collection or Item)
     *
     * @param dsObjects
     *            - List containing dspace objects
     * @param context
     *            - the dspace context
     * @param migrate Whether to use the migrate option or not
     * @throws Exception if error
     */
    public void createDownloadableExport(List<DSpaceObject> dsObjects,
            Context context, boolean migrate) throws Exception;

    /**
     * Convenience methot to create export a single Community, Collection, or
     * Item
     *
     * @param dso
     *            - the dspace object to export
     * @param context
     *            - the dspace context
     * @param additionalEmail
     *            - cc email to use
     * @param migrate Whether to use the migrate option or not
     * @throws Exception if error
     */
    public void createDownloadableExport(DSpaceObject dso,
            Context context, String additionalEmail, boolean migrate) throws Exception;

    /**
     * Convenience method to export a List of dspace objects (Community,
     * Collection or Item)
     *
     * @param dsObjects
     *            - List containing dspace objects
     * @param context
     *            - the dspace context
     * @param additionalEmail
     *            - cc email to use
     * @param migrate Whether to use the migrate option or not
     * @throws Exception if error
     */
    public void createDownloadableExport(List<DSpaceObject> dsObjects,
            Context context, String additionalEmail, boolean migrate) throws Exception;


    /**
     * Create a file name based on the date and eperson
     *
     * @param type Type of object (as string)
     * @param eperson
     *            - eperson who requested export and will be able to download it
     * @param date
     *            - the date the export process was created
     * @return String representing the file name in the form of
     *         'export_yyy_MMM_dd_count_epersonID'
     * @throws Exception if error
     */
    public String assembleFileName(String type, EPerson eperson,
            Date date) throws Exception;


    /**
     * Use config file entry for org.dspace.app.itemexport.download.dir and id
     * of the eperson to create a download directory name
     *
     * @param ePerson
     *            - the eperson who requested export archive
     * @return String representing a directory in the form of
     *         org.dspace.app.itemexport.download.dir/epersonID
     * @throws Exception if error
     */
    public String getExportDownloadDirectory(EPerson ePerson)
            throws Exception;


    /**
     * Returns config file entry for org.dspace.app.itemexport.work.dir
     *
     * @return String representing config file entry for
     *         org.dspace.app.itemexport.work.dir
     * @throws Exception if error
     */
    public String getExportWorkDirectory() throws Exception;

    /**
     * Used to read the export archived. Inteded for download.
     *
     * @param fileName
     *            the name of the file to download
     * @param eperson
     *            the eperson requesting the download
     * @return an input stream of the file to be downloaded
     * @throws Exception if error
     */
    public InputStream getExportDownloadInputStream(String fileName,
            EPerson eperson) throws Exception;

    /**
     * Get the file size of the export archive represented by the file name.
     *
     * @param context DSpace context
     * @param fileName
     *            name of the file to get the size.
     * @throws Exception if error
     * @return size as long
     */
    public long getExportFileSize(Context context, String fileName) throws Exception;

    /**
     * Get the last modified date of the export archive represented by the file name.
     *
     * @param context DSpace context
     * @param fileName
     *            name of the file to get the size.
     * @return date as long
     * @see java.io.File#lastModified() 
     * @throws Exception if error
     */
    public long getExportFileLastModified(Context context, String fileName)
            throws Exception;

    /**
     * The file name of the export archive contains the eperson id of the person
     * who created it When requested for download this method can check if the
     * person requesting it is the same one that created it
     *
     * @param context
     *            dspace context
     * @param fileName
     *            the file name to check auths for
     * @return true if it is the same person false otherwise
     */
    public boolean canDownload(Context context, String fileName);

    /**
     * Reads the download directory for the eperson to see if any export
     * archives are available
     *
     * @param eperson EPerson object
     * @return a list of file names representing export archives that have been
     *         processed
     * @throws Exception if error
     */
    public List<String> getExportsAvailable(EPerson eperson)
            throws Exception;

    /**
     * A clean up method that is ran before a new export archive is created. It
     * uses the config file entry 'org.dspace.app.itemexport.life.span.hours' to
     * determine if the current exports are too old and need pruging
     *
     * @param eperson
     *            - the eperson to clean up
     * @throws Exception if error
     */
    public void deleteOldExportArchives(EPerson eperson) throws Exception;

    /**
     * A clean up method that is ran before a new export archive is created. It
     * uses the config file entry 'org.dspace.app.itemexport.life.span.hours' to
     * determine if the current exports are too old and need purgeing
     * Removes all old exports, not just those for the person doing the export.
     *
     * @throws Exception if error
     */
    public void deleteOldExportArchives() throws Exception;

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send a success email once the export
     * archive is complete and ready for download
     *
     * @param context
     *            - the current Context
     * @param eperson
     *            - eperson to send the email to
     * @param fileName
     *            - the file name to be downloaded. It is added to the url in
     *            the email
     * @throws MessagingException if error
     */
    public void emailSuccessMessage(Context context, EPerson eperson,
            String fileName) throws MessagingException;

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send an error email if the export
     * archive fails
     *
     * @param eperson
     *            - EPerson to send the error message to
     * @param error
     *            - the error message
     * @throws MessagingException if error
     */
    public void emailErrorMessage(EPerson eperson, String error)
            throws MessagingException;

    /**
     * Zip source to target
     * @param strSource source file
     * @param target target file
     * @throws Exception if error
     */
    public void zip(String strSource, String target) throws Exception;

}
