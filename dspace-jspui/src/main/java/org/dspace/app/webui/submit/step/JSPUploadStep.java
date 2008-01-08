/*
 * JSPUploadStep.java
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
package org.dspace.app.webui.submit.step;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.Util;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.submit.step.UploadStep;

/**
 * Upload step for DSpace JSP-UI. Handles the pages that revolve around uploading files
 * (and verifying a successful upload) for an item being submitted into DSpace.
 * <P>
 * This JSPStep class works with the SubmissionController servlet
 * for the JSP-UI
 * <P>
 * The following methods are called in this order:
 * <ul>
 * <li>Call doPreProcessing() method</li>
 * <li>If showJSP() was specified from doPreProcessing(), then the JSP
 * specified will be displayed</li>
 * <li>If showJSP() was not specified from doPreProcessing(), then the
 * doProcessing() method is called an the step completes immediately</li>
 * <li>Call doProcessing() method on appropriate AbstractProcessingStep after the user returns from the JSP, in order
 * to process the user input</li>
 * <li>Call doPostProcessing() method to determine if more user interaction is
 * required, and if further JSPs need to be called.</li>
 * <li>If there are more "pages" in this step then, the process begins again
 * (for the new page).</li>
 * <li>Once all pages are complete, control is forwarded back to the
 * SubmissionController, and the next step is called.</li>
 * </ul>
 * 
 * @see org.dspace.app.webui.servlet.SubmissionController
 * @see org.dspace.app.webui.submit.JSPStep
 * @see org.dspace.submit.step.UploadStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class JSPUploadStep extends JSPStep
{
    /** JSP to choose files to upload * */
    private static final String CHOOSE_FILE_JSP = "/submit/choose-file.jsp";

    /** JSP to show files that were uploaded * */
    private static final String UPLOAD_LIST_JSP = "/submit/upload-file-list.jsp";

    /** JSP to single file that was upload * */
    private static final String UPLOAD_FILE_JSP = "/submit/show-uploaded-file.jsp";

    /** JSP to edit file description * */
    private static final String FILE_DESCRIPTION_JSP = "/submit/change-file-description.jsp";

    /** JSP to edit file format * */
    private static final String FILE_FORMAT_JSP = "/submit/get-file-format.jsp";

    /** JSP to show any upload errors * */
    private static final String UPLOAD_ERROR_JSP = "/submit/upload-error.jsp";
    
    /** JSP to review uploaded files * */
    private static final String REVIEW_JSP = "/submit/review-upload.jsp";

    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPUploadStep.class);

    /**
     * Do any pre-processing to determine which JSP (if any) is used to generate
     * the UI for this step. This method should include the gathering and
     * validating of all data required by the JSP. In addition, if the JSP
     * requires any variable to passed to it on the Request, this method should
     * set those variables.
     * <P>
     * If this step requires user interaction, then this method must call the
     * JSP to display, using the "showJSP()" method of the JSPStepManager class.
     * <P>
     * If this step doesn't require user interaction OR you are solely using
     * Manakin for your user interface, then this method may be left EMPTY,
     * since all step processing should occur in the doProcessing() method.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     */
    public void doPreProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // pass on the fileupload setting
        if (subInfo != null)
        {
            Collection c = subInfo.getSubmissionItem().getCollection();
            DCInputsReader inputsReader = new DCInputsReader();
            request.setAttribute("submission.inputs", inputsReader.getInputs(c
                    .getHandle()));
        }

        // show whichever upload page is appropriate
        // (based on if this item already has files or not)
        showUploadPage(context, request, response, subInfo, false);
    }
    
    /**
     * Do any post-processing after the step's backend processing occurred (in
     * the doProcessing() method).
     * <P>
     * It is this method's job to determine whether processing completed
     * successfully, or display another JSP informing the users of any potential
     * problems/errors.
     * <P>
     * If this step doesn't require user interaction OR you are solely using
     * Manakin for your user interface, then this method may be left EMPTY,
     * since all step processing should occur in the doProcessing() method.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @param status
     *            any status/errors reported by doProcessing() method
     */
    public void doPostProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, int status)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, UploadStep.NEXT_BUTTON);

        // Do we need to skip the upload entirely?
        boolean fileRequired = ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);
        if (buttonPressed.equalsIgnoreCase(UploadStep.SUBMIT_SKIP_BUTTON) ||
            (buttonPressed.equalsIgnoreCase(UploadStep.SUBMIT_UPLOAD_BUTTON) && !fileRequired))
        {
            Bundle[] bundles = subInfo.getSubmissionItem().getItem()
                    .getBundles("ORIGINAL");

            // if user already has uploaded at least one file
            if (bundles.length > 0)
            {
                // return to list of uploaded files
                showUploadFileList(context, request, response, subInfo, true,
                        false);
            }
            
            return; // return immediately, since we are skipping upload
        }
        
        //If upload failed in JSPUI (just came from upload-error.jsp), user can retry the upload
        if(buttonPressed.equalsIgnoreCase("submit_retry"))
        {
            showUploadPage(context, request, response, subInfo, false);
        }
        
        // ------------------------------
        // Check for Errors!
        // ------------------------------
        // if an error or message was passed back, determine what to do!
        if (status != UploadStep.STATUS_COMPLETE)
        {
            if (status == UploadStep.STATUS_INTEGRITY_ERROR)
            {
                // Some type of integrity error occurred
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);
            }
            else if (status == UploadStep.STATUS_UPLOAD_ERROR || status == UploadStep.STATUS_NO_FILES_ERROR)
            {
                // There was a problem uploading the file!

                //First, check if we just removed our uploaded file
                if(buttonPressed.startsWith("submit_remove_"))
                {
                    //if file was just removed, go back to upload page
                    showUploadPage(context, request, response, subInfo, false);
                }
                else
                {
                    // We need to show the file upload error page
                    if (subInfo != null)
                    {
                        Collection c = subInfo.getSubmissionItem().getCollection();
                        DCInputsReader inputsReader = new DCInputsReader();
                        request.setAttribute("submission.inputs", inputsReader
                                .getInputs(c.getHandle()));
                    }
                    JSPStepManager.showJSP(request, response, subInfo, UPLOAD_ERROR_JSP);
                }
            }
            else if (status == UploadStep.STATUS_UNKNOWN_FORMAT)
            {
                // user uploaded a file where the format is unknown to DSpace

                // forward user to page to request the file format
                showGetFileFormat(context, request, response, subInfo);
            }
        }
        
        // As long as there are no errors, clicking Next
        // should immediately send them to the next step
        if (status == UploadStep.STATUS_COMPLETE && buttonPressed.equals(UploadStep.NEXT_BUTTON))
        {
            // just return, so user will continue on to next step!
            return;
        }

        // ------------------------------
        // Check for specific buttons
        // ------------------------------
        if (buttonPressed.equals(UploadStep.SUBMIT_MORE_BUTTON))
        {
            // Upload another file (i.e. show the Choose File jsp again)
            showChooseFile(context, request, response, subInfo);
        }
        else if (buttonPressed.equals("submit_show_checksums"))
        {
            // Show the checksums
            showUploadFileList(context, request, response, subInfo, false, true);
        }
        else if (buttonPressed.startsWith("submit_describe_"))
        {
            // Change the description of a bitstream
            Bitstream bitstream;

            // Which bitstream does the user want to describe?
            try
            {
                int id = Integer.parseInt(buttonPressed.substring(16));
                bitstream = Bitstream.find(context, id);
            }
            catch (NumberFormatException nfe)
            {
                bitstream = null;
            }

            if (bitstream == null)
            {
                // Invalid or mangled bitstream ID
                // throw an error and return immediately
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);
            }

            // save the bitstream
            subInfo.setBitstream(bitstream);

            // load the change file description page
            showFileDescription(context, request, response, subInfo);
        }
        else if (buttonPressed.startsWith("submit_format_"))
        {
            // A "format is wrong" button must have been pressed
            Bitstream bitstream;

            // Which bitstream does the user want to describe?
            try
            {
                int id = Integer.parseInt(buttonPressed.substring(14));
                bitstream = Bitstream.find(context, id);
            }
            catch (NumberFormatException nfe)
            {
                bitstream = null;
            }

            if (bitstream == null)
            {
                // Invalid or mangled bitstream ID
                // throw an error and return immediately
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);
            }

            subInfo.setBitstream(bitstream);
            showGetFileFormat(context, request, response, subInfo);
        }
        else
        {
            // BY DEFAULT: just display either the first upload page or the
            // upload file listing
            String contentType = request.getContentType();
            boolean fileUpload = false;

            // if multipart form, then we just finished a file upload
            if ((contentType != null)
                    && (contentType.indexOf("multipart/form-data") != -1))
                fileUpload = true;

            // show the appropriate upload page
            // (based on if a file has just been uploaded or not)
            showUploadPage(context, request, response, subInfo, fileUpload);
        }
    }

    /**
     * Display the appropriate upload page in the file upload sequence. Which
     * page this is depends on whether the user has uploaded any files in this
     * item already.
     * 
     * @param context
     *            the DSpace context object
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     * @param justUploaded
     *            true, if the user just finished uploading a file
     */
    private void showUploadPage(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo,
            boolean justUploaded) throws SQLException, ServletException,
            IOException
    {
        Bundle[] bundles = subInfo.getSubmissionItem().getItem().getBundles(
                "ORIGINAL");

        if (justUploaded || bundles.length > 0)
        {
            // The item already has files associated with it.
            showUploadFileList(context, request, response, subInfo,
                    justUploaded, false);
        }
        else
        {
            // show the page to choose a file to upload
            showChooseFile(context, request, response, subInfo);
        }
    }

    /**
     * Show the page which allows the user to choose another file to upload
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     */
    private void showChooseFile(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws SQLException, ServletException, IOException
    {

        // load JSP which allows the user to select a file to upload
        JSPStepManager.showJSP(request, response, subInfo, CHOOSE_FILE_JSP);
    }

    /**
     * Show the page which lists all the currently uploaded files
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     * @param justUploaded
     *            pass in true if the user just successfully uploaded a file
     * @param showChecksums
     *            pass in true if checksums should be displayed
     */
    private void showUploadFileList(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo, boolean justUploaded, boolean showChecksums)
            throws SQLException, ServletException, IOException
    {
        // Set required attributes
        request.setAttribute("just.uploaded", new Boolean(justUploaded));
        request.setAttribute("show.checksums", new Boolean(showChecksums));

        // Always go to advanced view in workflow mode
        if (subInfo.isInWorkflow()
                || subInfo.getSubmissionItem().hasMultipleFiles())
        {
            // next, load JSP listing multiple files
            JSPStepManager.showJSP(request, response, subInfo, UPLOAD_LIST_JSP);
        }
        else
        {
            // next, load JSP listing a single file
            JSPStepManager.showJSP(request, response, subInfo, UPLOAD_FILE_JSP);
        }
    }

    /**
     * Show the page which allows the user to change the format of the file that
     * was just uploaded
     * 
     * @param context
     *            context object
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     */
    private void showGetFileFormat(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws SQLException, ServletException, IOException
    {
        if (subInfo == null || subInfo.getBitstream() == null)
        {
            // We have an integrity error, since we seem to have lost
            // which bitstream was just uploaded
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }

        BitstreamFormat[] formats = BitstreamFormat.findNonInternal(context);

        request.setAttribute("bitstream.formats", formats);

        // What does the system think it is?
        BitstreamFormat guess = FormatIdentifier.guessFormat(context, subInfo
                .getBitstream());

        request.setAttribute("guessed.format", guess);

        // display choose file format JSP next
        JSPStepManager.showJSP(request, response, subInfo, FILE_FORMAT_JSP);
    }

    /**
     * Show the page which allows the user to edit the description of already
     * uploaded files
     * 
     * @param context
     *            context object
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     */
    private void showFileDescription(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws SQLException, ServletException,
            IOException
    {
        // load JSP which allows the user to select a file to upload
        JSPStepManager.showJSP(request, response, subInfo, FILE_DESCRIPTION_JSP);
    }
    
    /**
     * Return the URL path (e.g. /submit/review-metadata.jsp) of the JSP
     * which will review the information that was gathered in this Step.
     * <P>
     * This Review JSP is loaded by the 'Verify' Step, in order to dynamically
     * generate a submission verification page consisting of the information
     * gathered in all the enabled submission steps.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     */
    public String getReviewJSP(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
    {
        return REVIEW_JSP;
    } 
}
