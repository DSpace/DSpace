/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.JSONUploadResponse;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.submit.step.AccessStep;
import org.dspace.submit.step.UploadStep;
import org.dspace.submit.step.UploadWithEmbargoStep;

import com.google.gson.Gson;

/**
 * Upload step with the advanced embargo fucntion for DSpace JSP-UI. Handles the pages 
 * that revolve around uploading files (and verifying a successful upload) for an item 
 * being submitted into DSpace.
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
 * doProcessing() method is called and the step completes immediately</li>
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
 * @see org.dspace.app.webui.submit.step.JSPUploadStep
 * @see org.dspace.submit.step.UploadStep
 *
 * @author Tim Donohue
 * @author Keiji Suzuki
 * @version $Revision$
 */
public class JSPUploadWithEmbargoStep extends JSPUploadStep
{
    /** JSP to set access policies of uploaded files * */
    private static final String ACCESS_POLICIES_JSP = "/submit/set-policies.jsp";

    /** JSP to edit access policy of selected policy * */
    private static final String EDIT_POLICY_JSP = "/submit/edit-policy.jsp";

    /** JSP to edit access policy of selected policy * */
    private static final String EDIT_BITSTREAM_ACCESS_JSP = "/submit/edit-bitstream-access.jsp";

    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPUploadWithEmbargoStep.class);
    
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

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
        
        if (UIUtil.getBoolParameter(request, "ajaxUpload"))
        {
            Gson gson = new Gson();
            // old browser need to see this response as html to work            
            response.setContentType("text/html");
            JSONUploadResponse jsonResponse = new JSONUploadResponse();
            String bitstreamName = null;
            UUID bitstreamID = null;
            long size = 0;
            String url = null;
            if (subInfo.getBitstream() != null)
            {
                Bitstream bitstream = subInfo.getBitstream();
                bitstreamName = bitstream.getName();
                bitstreamID = bitstream.getID();
                size = bitstream.getSizeBytes();
                url = request.getContextPath() + "/retrieve/" + bitstreamID
                        + "/" + UIUtil.encodeBitstreamName(bitstreamName);
                jsonResponse.addUploadFileStatus(bitstreamName, bitstreamID, size,
                        url, status);
                response.getWriter().print(gson.toJson(jsonResponse));
                response.flushBuffer();
            }
            return;
        }
        
        if (buttonPressed.equalsIgnoreCase(UploadStep.SUBMIT_SKIP_BUTTON) ||
            (buttonPressed.equalsIgnoreCase(UploadStep.SUBMIT_UPLOAD_BUTTON) && !fileRequired))
        {
            List<Bundle> bundles = itemService.getBundles(subInfo.getSubmissionItem().getItem(), "ORIGINAL");

            boolean fileAlreadyUploaded = false;
            
            for (Bundle bnd : bundles)
            {
            	fileAlreadyUploaded = bnd.getBitstreams().size() > 0;
            	if (fileAlreadyUploaded)
            	{
            		break;
            	}
            }
            
            // if user already has uploaded at least one file
            if (fileAlreadyUploaded)
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
        // 
        else if (status == UploadWithEmbargoStep.STATUS_EDIT_POLICIES)
        {
            showEditBitstreamAccess(context, request, response, subInfo);
        }
        // [Cancel] button is pressed at edit-policy page
        else if (status == UploadWithEmbargoStep.STATUS_EDIT_COMPLETE)
        {
            showUploadPage(context, request, response, subInfo, false);
        }
        // 
        else if (status == AccessStep.STATUS_EDIT_POLICY)
        {
            showEditPolicy(context, request, response, subInfo);
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
                        try
                        {
                            Collection c = subInfo.getSubmissionItem().getCollection();
                            DCInputsReader inputsReader = new DCInputsReader();
                            request.setAttribute("submission.inputs", inputsReader
                                    .getInputs(c.getHandle()));
                        }
                        catch (DCInputsReaderException e)
                        {
                            throw new ServletException(e);
                        }

                    }
                    JSPStepManager.showJSP(request, response, subInfo, UPLOAD_ERROR_JSP);
                }
            }
            else if (status == UploadStep.STATUS_VIRUS_CHECKER_UNAVAILABLE)
            {
                // We need to show the file upload error page
                if (subInfo != null)
                {
                    try
                    {
                        Collection c = subInfo.getSubmissionItem().getCollection();
                        DCInputsReader inputsReader = new DCInputsReader();
                        request.setAttribute("submission.inputs", inputsReader
                                .getInputs(c.getHandle()));
                    }
                    catch (DCInputsReaderException e)
                    {
                        throw new ServletException(e);
                    }

                }
                JSPStepManager.showJSP(request, response, subInfo, VIRUS_CHECKER_ERROR_JSP);
            }
            else if (status == UploadStep.STATUS_CONTAINS_VIRUS)
            {
                // We need to show the file upload error page
                if (subInfo != null)
                {
                    try
                    {
                        Collection c = subInfo.getSubmissionItem().getCollection();
                        DCInputsReader inputsReader = new DCInputsReader();
                        request.setAttribute("submission.inputs", inputsReader
                                .getInputs(c.getHandle()));
                    }
                    catch (DCInputsReaderException e)
                    {
                        throw new ServletException(e);
                    }

                }
                JSPStepManager.showJSP(request, response, subInfo, VIRUS_ERROR_JSP);
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
                UUID id = UUID.fromString(buttonPressed.substring(16));
                bitstream = bitstreamService.find(context, id);
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
                UUID id = UUID.fromString(buttonPressed.substring(14));
                bitstream = bitstreamService.find(context, id);
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
            {
                fileUpload = true;
            }

            // show the appropriate upload page
            // (based on if a file has just been uploaded or not)
            showUploadPage(context, request, response, subInfo, fileUpload);
        }
    }

    /**
     * Show the page which allows the user to edit bitstream access settings
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
    private void showEditBitstreamAccess(Context context, HttpServletRequest request,
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

        // display choose file format JSP next
        JSPStepManager.showJSP(request, response, subInfo, EDIT_BITSTREAM_ACCESS_JSP);
    }

    /**
     * Show the page which allows the user to edit the specific resource policy
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
    private void showEditPolicy(Context context, HttpServletRequest request,
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

        // display choose file format JSP next
        JSPStepManager.showJSP(request, response, subInfo, EDIT_POLICY_JSP);
    }

    @Override
    public String getReviewJSP(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
    {
        request.setAttribute("submission.step.uploadwithembargo", true);
        return super.getReviewJSP(context, request, response, subInfo);
    }
}
