/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport.service;

import org.dspace.app.itemimport.BatchUpload;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Import items into DSpace. The conventional use is upload files by copying
 * them. DSpace writes the item's bitstreams into its assetstore. Metadata is
 * also loaded to the DSpace database.
 * <P>
 * A second use assumes the bitstream files already exist in a storage
 * resource accessible to DSpace. In this case the bitstreams are 'registered'.
 * That is, the metadata is loaded to the DSpace database and DSpace is given
 * the location of the file which is subsumed into DSpace.
 * <P>
 * The distinction is controlled by the format of lines in the 'contents' file.
 * See comments in processContentsFile() below.
 * <P>
 * Modified by David Little, UCSD Libraries 12/21/04 to
 * allow the registration of files (bitstreams) into DSpace.
 */
public interface ItemImportService {


    public void addItemsAtomic(Context c, List<Collection> mycollections, String sourceDir, String mapFile, boolean template) throws Exception;

    public void addItems(Context c, List<Collection> mycollections,
            String sourceDir, String mapFile, boolean template) throws Exception;

    public String unzip(File zipfile) throws IOException;

    public String unzip(File zipfile, String destDir) throws IOException;

    public String unzip(String sourcedir, String zipfilename) throws IOException;

    /**
     *
     * Given a public URL to a zip file that has the Simple Archive Format, this method imports the contents to DSpace
     * @param url The public URL of the zip file
     * @param owningCollection The owning collection the items will belong to
     * @param collections The collections the created items will be inserted to, apart from the owning one
     * @param resumeDir In case of a resume request, the directory that containsthe old mapfile and data
     * @param context The context
     * @throws Exception
     */
    public void processUIImport(String url, Collection owningCollection, String[] collections, String resumeDir, String inputType, Context context, boolean template) throws Exception;

    /**
     * Since the BTE batch import is done in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplish this
     * communication with email instead. Send a success email once the batch
     * import is complete
     *
     * @param context
     *            - the current Context
     * @param eperson
     *            - eperson to send the email to
     * @param fileName
     *            - the filepath to the mapfile created by the batch import
     * @throws javax.mail.MessagingException
     */
    public void emailSuccessMessage(Context context, EPerson eperson,
            String fileName) throws MessagingException;

    /**
     * Since the BTE batch import is done in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send an error email if the batch
     * import fails
     *
     * @param eperson
     *            - EPerson to send the error message to
     * @param error
     *            - the error message
     * @throws MessagingException
     */
    public void emailErrorMessage(EPerson eperson, String error)
            throws MessagingException;


    public List<BatchUpload> getImportsAvailable(EPerson eperson)
            throws Exception;

    public String getImportUploadableDirectory(EPerson ePerson)
            throws Exception;

    public void deleteBatchUpload(Context c, String uploadId) throws Exception;

    public void replaceItems(Context c, List<Collection> mycollections, String sourcedir, String mapfile, boolean template) throws Exception;

    public void deleteItems(Context c, String mapfile) throws Exception;

    public void addBTEItems(Context c, List<Collection> mycollections, String sourcedir, String mapfile, boolean template, String bteInputType, String workingDir) throws Exception;

    public String getTempWorkDir();

    public File getTempWorkDirFile();

    public void cleanupZipTemp();

    public void setTest(boolean isTest);

    public void setResume(boolean isResume);

    public void setUseWorkflow(boolean useWorkflow);

    public void setUseWorkflowSendEmail(boolean useWorkflow);

    public void setQuiet(boolean isQuiet);
}
