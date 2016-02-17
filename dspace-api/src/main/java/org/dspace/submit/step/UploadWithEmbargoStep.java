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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

/**
 * Upload step with the advanced embargo system for DSpace. Processes the actual 
 * upload of files for an item being submitted into DSpace.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized
 * by both the JSP-UI and the Manakin XML-UI
 *
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.step.UploadStep
 * @see org.dspace.submit.AbstractProcessingStep
 *
 * @author Tim Donohue
 * @author Keiji Suzuki
 * @version $Revision$
 */
public class UploadWithEmbargoStep extends UploadStep
{
    public static final int STATUS_EDIT_POLICIES = 30;

    public static final int STATUS_EDIT_POLICIES_ERROR_SELECT_GROUP = 31;
    public static final int STATUS_EDIT_POLICIES_DUPLICATED_POLICY = 32;

    public static final int STATUS_EDIT_POLICY_ERROR_SELECT_GROUP = 33;
    public static final int STATUS_EDIT_POLICY_DUPLICATED_POLICY = 34;


    /** log4j logger */
    private static Logger log = Logger.getLogger(UploadWithEmbargoStep.class);

    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();

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

        // POLICIES FORM MANAGEMENT
        int result = editBitstreamPolicies(request, context, subInfo, buttonPressed);
        if(result != -1) return result;

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
                Bitstream b = bitstreamService.find(context, Util.getUUIDParameter(request
                        , "bitstream_id"));

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
        Map<String, String> descriptions = new HashMap<String, String>();
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


        // execute only if comes from EditBitstreamStep
        if(buttonPressed.equals("submit_save")){
            processAccessFields(context, request, subInfo, subInfo.getBitstream());
        }


        // ---------------------------------------------------
        // Step #5: Check if primary bitstream has changed
        // -------------------------------------------------
        if (request.getParameter("primary_bitstream_id") != null)
        {
            List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
            if (bundles.size() > 0)
            {
                bundles.get(0).setPrimaryBitstreamID(bitstreamService.find(context, Util.getUUIDParameter(request
                        , "primary_bitstream_id")));
                bundleService.update(context, bundles.get(0));
            }
        }

        // ---------------------------------------------------
        // Step #6: Determine if there is an error because no
        // files have been uploaded.
        // ---------------------------------------------------
        //check if a file is required to be uploaded
        if (fileRequired && !itemService.hasUploadedFiles(item))
        {
            return STATUS_NO_FILES_ERROR;
        }
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
    @Override
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


                processAccessFields(context, request, subInfo, b);




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
                    curator.addTask("vscan").curate(item);
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

                // Comment - not sure if this is the right place for a commit here
                // but I'm not brave enough to remove it - Robin.
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

    private void processAccessFields(Context context, HttpServletRequest request, SubmissionInfo subInfo, Bitstream b) throws SQLException, AuthorizeException {
        // ResourcePolicy Management
        boolean isAdvancedFormEnabled= configurationService.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);
        // if it is a simple form we should create the policy for Anonymous
        // if Anonymous does not have right on this collection, create policies for any other groups with
        // DEFAULT_ITEM_READ specified.
        if(!isAdvancedFormEnabled){
            Date startDate = null;
            try {
                startDate = DateUtils.parseDate(request.getParameter("embargo_until_date"), new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
            } catch (Exception e) {
                //Ignore start date already null
            }
            String reason = request.getParameter("reason");
            authorizeService.generateAutomaticPolicies(context, startDate, reason, b, (Collection) handleService.resolveToObject(context, subInfo.getCollectionHandle()));
        }
    }


    private int editBitstreamPolicies(HttpServletRequest request, Context context, SubmissionInfo subInfo, String buttonPressed)
            throws SQLException, AuthorizeException {

        // FORM: EditBitstreamPolicies SELECTED OPERATION: Return
        if (buttonPressed.equals("bitstream_list_submit_return")){
            return STATUS_COMPLETE;
        }
        // FORM: UploadStep SELECTED OPERATION: go to EditBitstreamPolicies
        else if (buttonPressed.startsWith("submit_editPolicy_")){
            UUID bitstreamID = UUID.fromString(buttonPressed.substring("submit_editPolicy_".length()));
            Bitstream b = bitstreamService.find(context, bitstreamID);
            subInfo.setBitstream(b);
            return STATUS_EDIT_POLICIES;
        }
        // FORM: EditBitstreamPolicies SELECTED OPERATION: Add New Policy.
        else if (buttonPressed.startsWith(AccessStep.FORM_ACCESS_BUTTON_ADD)){
            Bitstream b = bitstreamService.find(context, Util.getUUIDParameter(request, "bitstream_id"));
            subInfo.setBitstream(b);

            int result=-1;
            if( (result = AccessStep.checkForm(request))!=0){
                return result;
            }
            Date dateStartDate = AccessStep.getEmbargoUntil(request);
            String reason = request.getParameter("reason");
            String name = request.getParameter("name");

            Group group = null;
            if(request.getParameter("group_id")!=null){
                try{
                    group=groupService.find(context, Util.getUUIDParameter(request, "group_id"));
                }catch (NumberFormatException nfe){
                    return STATUS_EDIT_POLICIES_ERROR_SELECT_GROUP;
                }
            }
            ResourcePolicy rp;
            if( (rp= authorizeService.createOrModifyPolicy(null, context, name, group, null, dateStartDate, org.dspace.core.Constants.READ, reason, b))==null){
                return STATUS_EDIT_POLICIES_DUPLICATED_POLICY;
            }
            resourcePolicyService.update(context, rp);
            context.dispatchEvents();
            return STATUS_EDIT_POLICIES;
        }
        // FORM: EditBitstreamPolicies SELECTED OPERATION: go to EditPolicyForm
        else if(org.dspace.submit.step.AccessStep.wasEditPolicyPressed(context, buttonPressed, subInfo)){
            Bitstream b = bitstreamService.find(context, Util.getUUIDParameter(request, "bitstream_id"));
            subInfo.setBitstream(b);
            return org.dspace.submit.step.AccessStep.STATUS_EDIT_POLICY;
        }
        // FORM: EditPolicy SELECTED OPERATION: Save or Cancel.
        else if(org.dspace.submit.step.AccessStep.comeFromEditPolicy(request)) {
            Bitstream b = bitstreamService.find(context, Util.getUUIDParameter(request, "bitstream_id"));
            subInfo.setBitstream(b);
            String reason = request.getParameter("reason");
            String name = request.getParameter("name");

            Group group = groupService.findByName(context, Group.ANONYMOUS);
            if(request.getParameter("group_id")!=null){
                try{
                    group=groupService.find(context, UUID.fromString(request.getParameter("group_id")));
                }catch (NumberFormatException nfe){
                    return STATUS_EDIT_POLICIES_ERROR_SELECT_GROUP;
                }
            }
            if(org.dspace.submit.step.AccessStep.saveOrCancelEditPolicy(context, request,
                    subInfo, buttonPressed, b, name, group, reason)==org.dspace.submit.step.AccessStep.EDIT_POLICY_STATUS_DUPLICATED_POLICY)
                return STATUS_EDIT_POLICY_DUPLICATED_POLICY;

            return STATUS_EDIT_POLICIES;
        }
        // FORM: EditBitstreamPolicies SELECTED OPERATION: Remove Policies
        if(org.dspace.submit.step.AccessStep.wasRemovePolicyPressed(buttonPressed)){
            Bitstream b = bitstreamService.find(context, Util.getUUIDParameter(request, "bitstream_id"));
            subInfo.setBitstream(b);
            org.dspace.submit.step.AccessStep.removePolicy(context, buttonPressed);
            context.dispatchEvents();
            return STATUS_EDIT_POLICIES;
        }
        return -1;
    }
}
