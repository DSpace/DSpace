/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.submit.AbstractProcessingStep;

/**
 * Upload step for DSpace. Processes the actual upload of files
 * for an item being submitted into DSpace.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Tim Donohue
 */
public class UploadStep extends AbstractProcessingStep
{
    /** Button to upload a file * */
    public static final String SUBMIT_UPLOAD_BUTTON = "submit_upload";

    /** Button to skip uploading a file * */
    public static final String SUBMIT_SKIP_BUTTON = "submit_skip";

    /** Button to submit more files * */
    public static final String SUBMIT_MORE_BUTTON = "submit_more";

    /** Button to cancel editing of file info * */
    public static final String CANCEL_EDIT_BUTTON = "submit_edit_cancel";

    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    // integrity error occurred
    public static final int STATUS_INTEGRITY_ERROR = 1;

    // error in uploading file
    public static final int STATUS_UPLOAD_ERROR = 2;

    // error - no files uploaded!
    public static final int STATUS_NO_FILES_ERROR = 5;

    // format of uploaded file is unknown
    public static final int STATUS_UNKNOWN_FORMAT = 10;

    // virus checker unavailable ?
    public static final int STATUS_VIRUS_CHECKER_UNAVAILABLE = 14;

    // file failed virus check
    public static final int STATUS_CONTAINS_VIRUS = 16;

    // edit file information
    public static final int STATUS_EDIT_BITSTREAM = 20;

    // return from editing file information
    public static final int STATUS_EDIT_COMPLETE = 25;

    /** log4j logger */
    private static final Logger log = Logger.getLogger(UploadStep.class);

    /** is the upload required? */
    protected boolean fileRequired = configurationService.getBooleanProperty("webui.submit.upload.required", true);

    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <P>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the appropriate user interface (JSP-UI or XML-UI)
     * <P>
     * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
     * it should perform *all* of its processing in this method!
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // get button user pressed
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        // get reference to item
        Item item = subInfo.getSubmissionItem().getItem();

        // -----------------------------------
        // Step #0: Upload new files (if any)
        // -----------------------------------
        String contentType = request.getContentType();

        // if multipart form, then we are uploading a file
        if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1))
        {
            // This is a multipart request, so it's a file upload
            // (return any status messages or errors reported)
            int status = processUploadFile(context, request, response, subInfo);

            // if error occurred, return immediately
            if (status != STATUS_COMPLETE)
            {
                return status;
            }
        }
        
        // if user pressed jump-to button in process bar,
        // return success (so that jump will occur)
        if (buttonPressed.startsWith(PROGRESS_BAR_PREFIX) || 
        		buttonPressed.startsWith(PREVIOUS_BUTTON))
        {
            // check if a file is required to be uploaded
            if (fileRequired && !itemService.hasUploadedFiles(item))
            {
                return STATUS_NO_FILES_ERROR;
            }
            else
            {
                return STATUS_COMPLETE;
            }
        }

        // ---------------------------------------------
        // Step #1: Check if this was just a request to
        // edit file information.
        // (or canceled editing information)
        // ---------------------------------------------
        // check if we're already editing a specific bitstream
        if (request.getParameter("bitstream_id") != null)
        {
            if (buttonPressed.equals(CANCEL_EDIT_BUTTON))
            {
                // canceled an edit bitstream request
                subInfo.setBitstream(null);

                // this flag will just return us to the normal upload screen
                return STATUS_EDIT_COMPLETE;
            }
            else
            {
                // load info for bitstream we are editing
                Bitstream b = bitstreamService.find(context, Util.getUUIDParameter(request,
                        "bitstream_id"));

                // save bitstream to submission info
                subInfo.setBitstream(b);
            }
        }
        else if (buttonPressed.startsWith("submit_edit_"))
        {
            // get ID of bitstream that was requested for editing
            String bitstreamID = buttonPressed.substring("submit_edit_"
                    .length());

            Bitstream b = bitstreamService
                    .find(context, UUID.fromString(bitstreamID));

            // save bitstream to submission info
            subInfo.setBitstream(b);

            // return appropriate status flag to say we are now editing the
            // bitstream
            return STATUS_EDIT_BITSTREAM;
        }

        // ---------------------------------------------
        // Step #2: Process any remove file request(s)
        // ---------------------------------------------
        // Remove-selected requests come from Manakin
        if (buttonPressed.equalsIgnoreCase("submit_remove_selected"))
        {
            // this is a remove multiple request!

            if (request.getParameter("remove") != null)
            {
                // get all files to be removed
                String[] removeIDs = request.getParameterValues("remove");

                // remove each file in the list
                for (int i = 0; i < removeIDs.length; i++)
                {
                    UUID id = UUID.fromString(removeIDs[i]);

                    int status = processRemoveFile(context, item, id);

                    // if error occurred, return immediately
                    if (status != STATUS_COMPLETE)
                    {
                        return status;
                    }
                }

                // remove current bitstream from Submission Info
                subInfo.setBitstream(null);
            }
        }
        else if (buttonPressed.startsWith("submit_remove_"))
        {
            // A single file "remove" button must have been pressed

            UUID id = UUID.fromString(buttonPressed.substring(14));
            int status = processRemoveFile(context, item, id);

            // if error occurred, return immediately
            if (status != STATUS_COMPLETE)
            {
                return status;
            }

            // remove current bitstream from Submission Info
            subInfo.setBitstream(null);
        }

        // -------------------------------------------------
        // Step #3: Check for a change in file description
        // -------------------------------------------------
        // We have to check for descriptions from users using the resumable upload
        // and from users using the simple upload.
        // Beginning with the resumable ones.
        Enumeration<String> parameterNames = request.getParameterNames();
        Map<String, String> descriptions = new HashMap<>();
        while (parameterNames.hasMoreElements())
        {
            String name = parameterNames.nextElement();
            if (StringUtils.startsWithIgnoreCase(name, "description["))
            {
                descriptions.put(
                        name.substring("description[".length(), name.length()-1),
                        request.getParameter(name));
            }
        }
        if (!descriptions.isEmpty())
        {
            // we got descriptions from the resumable upload
            if (item != null)
            {
                List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
                for (Bundle bundle : bundles)
                {
                    List<Bitstream> bitstreams = bundle.getBitstreams();
                    for (Bitstream bitstream : bitstreams)
                    {
                        if (descriptions.containsKey(bitstream.getName()))
                        {
                            bitstream.setDescription(context, descriptions.get(bitstream.getName()));
                            bitstreamService.update(context, bitstream);
                        }
                    }
                }
            }
            return STATUS_COMPLETE;
        }
        
        // Going on with descriptions from the simple upload
        String fileDescription = request.getParameter("description");

        if (fileDescription != null && fileDescription.length() > 0)
        {
            // save this file description
            int status = processSaveFileDescription(context, request, response,
                    subInfo);

            // if error occurred, return immediately
            if (status != STATUS_COMPLETE)
            {
                return status;
            }
        }

        // ------------------------------------------
        // Step #4: Check for a file format change
        // (if user had to manually specify format)
        // ------------------------------------------
        int formatTypeID = Util.getIntParameter(request, "format");
        String formatDesc = request.getParameter("format_description");

        // if a format id or description was found, then save this format!
        if (formatTypeID >= 0
                || (formatDesc != null && formatDesc.length() > 0))
        {
            // save this specified format
            int status = processSaveFileFormat(context, request, response,
                    subInfo);

            // if error occurred, return immediately
            if (status != STATUS_COMPLETE)
            {
                return status;
            }
        }

        // ---------------------------------------------------
        // Step #5: Check if primary bitstream has changed
        // -------------------------------------------------
        if (request.getParameter("primary_bitstream_id") != null)
        {
            List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
            if (bundles.size() > 0)
            {
            	bundles.get(0).setPrimaryBitstreamID(bitstreamService.find(context, Util.getUUIDParameter(request,
                        "primary_bitstream_id")));
            	bundleService.update(context, bundles.get(0));
            }
        }

        // ---------------------------------------------------
        // Step #6: Determine if there is an error because no
        // files have been uploaded.
        // ---------------------------------------------------
        //check if a file is required to be uploaded
        if (fileRequired && !itemService.hasUploadedFiles(item)
                && !buttonPressed.equals(SUBMIT_MORE_BUTTON))
        {
            return STATUS_NO_FILES_ERROR;
        }

        context.dispatchEvents();

        return STATUS_COMPLETE;
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used to build the progress bar.
     * <P>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using input-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <P>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     * 
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     * 
     * @return the number of pages in this step
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        // Despite using many JSPs, this step only appears
        // ONCE in the Progress Bar, so it's only ONE page
        return 1;
    }

    // ****************************************************************
    // ****************************************************************
    // METHODS FOR UPLOADING FILES (and associated information)
    // ****************************************************************
    // ****************************************************************

    /**
     * Remove a file from an item
     * 
     * @param context
     *            current DSpace context
     * @param item
     *            Item where file should be removed from
     * @param bitstreamID
     *            The id of bitstream representing the file to remove
     * @return Status or error flag which will be processed by
     *         UI-related code! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    protected int processRemoveFile(Context context, Item item, UUID bitstreamID)
            throws IOException, SQLException, AuthorizeException
    {
        Bitstream bitstream;

        // Try to find bitstream
        try
        {
            bitstream = bitstreamService.find(context, bitstreamID);
        }
        catch (NumberFormatException nfe)
        {
            bitstream = null;
        }

        if (bitstream == null)
        {
            // Invalid or mangled bitstream ID
            // throw an error and return immediately
            return STATUS_INTEGRITY_ERROR;
        }

        // remove bitstream from bundle..
        // delete bundle if it's now empty
        List<Bundle> bundles = bitstream.getBundles();

        Bundle bundle = bundles.get(0);
        bundleService.removeBitstream(context, bundle, bitstream);

        List<Bitstream> bitstreams = bundle.getBitstreams();

        // remove bundle if it's now empty
        if (bitstreams.size() < 1)
        {
            itemService.removeBundle(context, item, bundle);
            itemService.update(context, item);
        }

        // no errors occurred
        return STATUS_COMPLETE;
    }

    /**
     * Process the upload of a new file!
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * 
     * @return Status or error flag which will be processed by
     *         UI-related code! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    public int processUploadFile(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        boolean formatKnown = true;
        boolean fileOK = false;
        BitstreamFormat bf = null;
        Bitstream b = null;
 
        //NOTE: File should already be uploaded. 
        //Manakin does this automatically via Cocoon.
        //For JSP-UI, the SubmissionController.uploadFiles() does the actual upload

        Enumeration attNames = request.getAttributeNames();
        
        //loop through our request attributes
        while(attNames.hasMoreElements())
        {
            String attr = (String) attNames.nextElement();
            
            //if this ends with "-path", this attribute
            //represents a newly uploaded file
            if(attr.endsWith("-path"))
            {
                //strip off the -path to get the actual parameter 
                //that the file was uploaded as
                String param = attr.replace("-path", "");
                
                // Load the file's path and input stream and description
                String filePath = (String) request.getAttribute(param + "-path");
                InputStream fileInputStream = (InputStream) request.getAttribute(param + "-inputstream");
                
                //attempt to get description from attribute first, then direct from a parameter
                String fileDescription =  (String) request.getAttribute(param + "-description");
                if(fileDescription==null ||fileDescription.length()==0)
                {
                    fileDescription = request.getParameter("description");
                }
                
                // if information wasn't passed by User Interface, we had a problem
                // with the upload
                if (filePath == null || fileInputStream == null)
                {
                    return STATUS_UPLOAD_ERROR;
                }

                if (subInfo == null)
                {
                    // In any event, if we don't have the submission info, the request
                    // was malformed
                    return STATUS_INTEGRITY_ERROR;
                }


                // Create the bitstream
                Item item = subInfo.getSubmissionItem().getItem();

                // do we already have a bundle?
                List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");

                if (bundles.size() < 1)
                {
                    // set bundle's name to ORIGINAL
                    b = itemService.createSingleBitstream(context, fileInputStream, item, "ORIGINAL");
                }
                else
                {
                    // we have a bundle already, just add bitstream
                    b = bitstreamService.create(context, bundles.get(0), fileInputStream);
                }

                // Strip all but the last filename. It would be nice
                // to know which OS the file came from.
                String noPath = filePath;

                while (noPath.indexOf('/') > -1)
                {
                    noPath = noPath.substring(noPath.indexOf('/') + 1);
                }

                while (noPath.indexOf('\\') > -1)
                {
                    noPath = noPath.substring(noPath.indexOf('\\') + 1);
                }

                b.setName(context, noPath);
                b.setSource(context, filePath);
                b.setDescription(context, fileDescription);

                // Identify the format
                bf = bitstreamFormatService.guessFormat(context, b);
                b.setFormat(context, bf);

                // Update to DB
                bitstreamService.update(context, b);
                itemService.update(context, item);

                if ((bf != null) && (bf.isInternal()))
                {
                    log.warn("Attempt to upload file format marked as internal system use only");
                    backoutBitstream(context, subInfo, b, item);
                    return STATUS_UPLOAD_ERROR;
                }

                // Check for virus
                if (configurationService.getBooleanProperty("submission-curation.virus-scan"))
                {
                    Curator curator = new Curator();
                    curator.addTask("vscan").curate(context, item);
                    int status = curator.getStatus("vscan");
                    if (status == Curator.CURATE_ERROR)
                    {
                        backoutBitstream(context, subInfo, b, item);
                        return STATUS_VIRUS_CHECKER_UNAVAILABLE;
                    }
                    else if (status == Curator.CURATE_FAIL)
                    {
                        backoutBitstream(context, subInfo, b, item);
                        return STATUS_CONTAINS_VIRUS;
                    }
                }

                // If we got this far then everything is more or less ok.

                context.dispatchEvents();

                // save this bitstream to the submission info, as the
                // bitstream we're currently working with
                subInfo.setBitstream(b);

                //if format was not identified
                if (bf == null)
                {
                    return STATUS_UNKNOWN_FORMAT;
                }

            }//end if attribute ends with "-path"
        }//end while
        

        return STATUS_COMPLETE;

              
    }

    /*
     * If we created a new Bitstream but now realise there is a problem then remove it.
     */
    protected void backoutBitstream(Context context, SubmissionInfo subInfo, Bitstream b, Item item)
            throws SQLException, AuthorizeException, IOException
    {
        // remove bitstream from bundle..
        List<Bundle> bundles = b.getBundles();
        if (bundles.isEmpty())
            throw new SQLException("Bitstream is not in any Bundles.");

        Bundle firstBundle = bundles.get(0);

        bundleService.removeBitstream(context, firstBundle, b);

        List<Bitstream> bitstreams = firstBundle.getBitstreams();

        // remove bundle if it's now empty
        if (bitstreams.isEmpty())
        {
            itemService.removeBundle(context, item, firstBundle);
            itemService.update(context, item);
        }
        else
            bundleService.update(context, firstBundle);

        subInfo.setBitstream(null);
    }

    /**
     * Process input from get file type page
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * 
     * @return Status or error flag which will be processed by
     *         UI-related code! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    protected int processSaveFileFormat(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        if (subInfo.getBitstream() != null)
        {
            // Did the user select a format?
            int typeID = Util.getIntParameter(request, "format");

            BitstreamFormat format = bitstreamFormatService.find(context, typeID);

            if (format != null)
            {
                subInfo.getBitstream().setFormat(context, format);
            }
            else
            {
                String userDesc = request.getParameter("format_description");

                subInfo.getBitstream().setUserFormatDescription(context, userDesc);
            }

            // update database
            bitstreamService.update(context, subInfo.getBitstream());
        }
        else
        {
            return STATUS_INTEGRITY_ERROR;
        }

        return STATUS_COMPLETE;
    }

    /**
     * Process input from the "change file description" page
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * 
     * @return Status or error flag which will be processed by
     *         UI-related code! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    protected int processSaveFileDescription(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        if (subInfo.getBitstream() != null)
        {
            subInfo.getBitstream().setDescription(context,
                    request.getParameter("description"));
            bitstreamService.update(context, subInfo.getBitstream());

            context.dispatchEvents();
        }
        else
        {
            return STATUS_INTEGRITY_ERROR;
        }

        return STATUS_COMPLETE;
    }

}
