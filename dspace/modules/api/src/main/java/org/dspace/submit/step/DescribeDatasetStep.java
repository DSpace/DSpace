package org.dspace.submit.step;

import org.dspace.app.util.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.Context;
import org.dspace.usagelogging.EventLogger;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.String;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 27-jan-2010
 * Time: 16:39:38
 *
 * The processing of the describe page for a data file
 */
public class DescribeDatasetStep extends DescribeStep {
    private final static String PROVENANCE = "dc.description.provenance";
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        // check what submit button was pressed in User Interface
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        EventLogger.log(context, "submission-describe-dataset", "button_pressed=" + buttonPressed);

        // CANCEL: remove dataFile and redirect to the Overview Step...
        if(buttonPressed.equals("submit_cancel")){
            EventLogger.log(context, "submission-describe-dataset", "status=cancelled");
            if (subInfo != null) {
                ((WorkspaceItem) subInfo.getSubmissionItem()).deleteAll();
                Item publication = DryadWorkflowUtils.getDataPackage(context, subInfo.getSubmissionItem().getItem());
                WorkflowItem wfi = null;
                if (publication != null) {
                    wfi = WorkflowItem.findByItemId(context, publication.getID());
                }
                if (wfi != null) {
                    // this item is either in review or it's in curation.
                    if (DryadWorkflowUtils.isItemInReview(context, wfi)) {
                        // redirect to the review workflow
                        response.sendRedirect(request.getContextPath() + "/handle/" + publication.getOwningCollection().getHandle() + "/workflow?workflowID=" + wfi.getID() + "&stepID=reviewStep&actionID=reviewAction");
                    } else {
                        // redirect to curation workflow
                        response.sendRedirect(request.getContextPath() + "/handle/" + publication.getOwningCollection().getHandle() + "/workflow?workflowID=" + wfi.getID() + "&stepID=dryadAcceptEditReject&actionID=dryadAcceptEditRejectAction");
                    }
                } else {
                    WorkspaceItem wi = WorkspaceItem.findByItem(context, publication);
                    response.sendRedirect(request.getContextPath() + "/submit-overview?workspaceID=" + wi.getID());
                }
            }
            return STATUS_COMPLETE;
        }

        // get the item and current page
        Item item = subInfo.getSubmissionItem().getItem();
        int currentPage = getCurrentPage(request);


        Collection c = subInfo.getSubmissionItem().getCollection();
        DCInput[] inputs;
        try {
            inputs = inputsReader.getInputs(c.getHandle()).getPageRows(
                    currentPage - 1,
                    subInfo.getSubmissionItem().hasMultipleTitles(),
                    subInfo.getSubmissionItem().isPublishedBefore());
        }
        catch (DCInputsReaderException e) {
            throw new ServletException(e);
        }

        /************************/
        /* Process the metadata */
        /************************/
        // Step 1:
        // clear out all item metadata defined on this page
        for (DCInput input : inputs) {
            if (!input
                    .isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE
                            : DCInput.SUBMISSION_SCOPE)) {
                continue;
            }
            String qualifier = input.getQualifier();
            if (qualifier == null
                    && input.getInputType().equals("qualdrop_value")) {
                qualifier = Item.ANY;
            }
            item.clearMetadata(input.getSchema(), input.getElement(),
                    qualifier, Item.ANY);
        }

        // Clear required-field errors first since missing authority
        // values can add them too.
        clearErrorFields(request);

        // Step 2:
        // now update the item metadata.
        String fieldName;
        boolean moreInput = false;
        for (DCInput input : inputs) {
            if (!input
                    .isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE
                            : DCInput.SUBMISSION_SCOPE)) {
                continue;
            }
            String element = input.getElement();
            String qualifier = input.getQualifier();
            String schema = input.getSchema();
            if (qualifier != null && !qualifier.equals(Item.ANY)) {
                fieldName = schema + "_" + element + '_' + qualifier;
            } else {
                fieldName = schema + "_" + element;
            }

            String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
            ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
            String inputType = input.getInputType();
            if (inputType.equals("name")) {
                readNames(request, item, schema, element, qualifier, input
                        .getRepeatable());
            } else if (inputType.equals("date")) {
                readDate(request, item, schema, element, qualifier);
            }
            // choice-controlled input with "select" presentation type is
            // always rendered as a dropdown menu
            else if (inputType.equals("embargo") || inputType.equals("dropdown") || inputType.equals("list") ||
                    (cmgr.isChoicesConfigured(fieldKey) &&
                            "select".equals(cmgr.getPresentation(fieldKey)))) {
                String[] vals = request.getParameterValues(fieldName);
                if (vals != null) {
                    for (String val : vals) {
                        if (!val.equals("")) {
                            item.addMetadata(schema, element, qualifier, LANGUAGE_QUALIFIER,
                                    val);
                        }
                    }
                }
            } else if (inputType.equals("series")) {
                readSeriesNumbers(request, item, schema, element, qualifier,
                        input.getRepeatable());
            } else if (inputType.equals("seriesyear")) {
                readSeriesNumbersWithYear(request, item, schema, element, qualifier,
                        input.getRepeatable());
            } else if (inputType.equals("qualdrop_value")) {
                List<String> quals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_qualifier");
                List<String> vals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_value");
                for (int z = 0; z < vals.size(); z++) {
                    String thisQual = (String) quals.get(z);
                    if ("".equals(thisQual)) {
                        thisQual = null;
                    }
                    String thisVal = (String) vals.get(z);
                    if (!buttonPressed.equals("submit_" + schema + "_"
                            + element + "_remove_" + z)
                            && !thisVal.equals("")) {
                        item.addMetadata(schema, element, thisQual, null,
                                thisVal);
                    }
                }
            } else if ((inputType.equals("onebox"))
                    || (inputType.equals("twobox"))
                    || (inputType.equals("textarea"))) {
                readText(request, item, schema, element, qualifier, input
                        .getRepeatable(), LANGUAGE_QUALIFIER, input.getSplitter());
            } else if (inputType.equals("file")) {
                //Check if we already have file
                String fileDescr = getFieldName(input);
                boolean hasFile = false;
                Bundle[] origBundle = item.getBundles("ORIGINAL");
                if (origBundle != null && 0 < origBundle.length) {
                    Bitstream[] bits = origBundle[0].getBitstreams();
                    for (Bitstream bit : bits) {
                        if (fileDescr.equals(bit.getDescription()))
                            hasFile = true;
                    }
                }

                if(buttonPressed.startsWith("submit_" + fileDescr + "_remove_")){
                    //We have a file present
                    int bitId = -1;
                    //Find it
                    try{
                        bitId = Integer.parseInt(buttonPressed.split("_")[buttonPressed.split("_").length - 1]);
                    } catch (Exception e){
                        log.error("Error while removing bitstream: " + buttonPressed, e);
                    }
                    removeBitstream(context, item, bitId);
                    hasFile = false;
                }

                if (!hasFile) {
                    int uploadResult = processUploadFile(context, request, subInfo, fileDescr, input.getElement().equals("readme"));
                    if (uploadResult == FILE_UPLOAD_ERROR || (uploadResult == FILE_NO_UPLOAD && input.isRequired()))
                        addErrorField(request, fileDescr);
                }
            } else {
                throw new ServletException("Field " + fieldName
                        + " has an unknown input type: " + inputType);
            }

            // determine if more input fields were requested
            if (!moreInput
                    && buttonPressed.equals("submit_" + fieldName + "_add")) {
                subInfo.setMoreBoxesFor(fieldName);
                subInfo.setJumpToField(fieldName);
                moreInput = true;
            }
            // was XMLUI's "remove" button pushed?
            else if (buttonPressed.equals("submit_" + fieldName + "_delete")) {
                subInfo.setJumpToField(fieldName);
            }
        }

        // Step 3:
        // Check to see if any fields are missing
        // Only check for required fields if user clicked the "next", the "previous" or the "progress bar" button
        if (buttonPressed.equals(NEXT_BUTTON)
                || buttonPressed.startsWith(PROGRESS_BAR_PREFIX)
                || buttonPressed.equals(PREVIOUS_BUTTON)
                || buttonPressed.equals(CANCEL_BUTTON)
                || buttonPressed.equals("submit_remove_dataset")
                )
        {
            for (DCInput input : inputs) {
                DCValue[] values = item.getMetadata(input.getSchema(),
                        input.getElement(), input.getQualifier(), Item.ANY);

                if (input.isRequired() && values.length == 0 && !input.getInputType().equals("file")) {
                    // since this field is missing add to list of error fields
                    addErrorField(request, getFieldName(input));
                }
            }
        }

        // Process our dataset file
        int datasetFileSuccess;
        if(buttonPressed.equals("submit_remove_dataset")){
            item .clearMetadata(MetadataSchema.DC_SCHEMA, "rights", "uri", Item.ANY);
            //Remove the dataset file
            int bitId = -1;
            try{
                bitId = Integer.parseInt(request.getParameter("remove_dataset_id"));
            }catch(Exception e){
                log.error("Error while removing dataset: " + buttonPressed, e);
            }
            removeBitstream(context, item, bitId);

            datasetFileSuccess = FILE_NO_UPLOAD;
        }else
        if(buttonPressed.equals("submit_remove_external")){
            //Remove the external source
            subInfo.getSubmissionItem().getItem().clearMetadata("dryad", "externalIdentifier", null, Item.ANY);
            subInfo.getSubmissionItem().getItem().update();
            datasetFileSuccess = FILE_NO_UPLOAD;
        }else {
            if(request.getParameter("dataset_id_present") == null){
                if("file".equals(request.getParameter("datafile_type"))){
                	item.clearMetadata(MetadataSchema.DC_SCHEMA, "rights", "uri", Item.ANY); // remove any existing
                    item.addMetadata(MetadataSchema.DC_SCHEMA, "rights", "uri", null, "http://creativecommons.org/publicdomain/zero/1.0/");
                    datasetFileSuccess = processUploadFile(context, request, subInfo, "dataset-file", false);
                    if (datasetFileSuccess == FILE_UPLOAD_OK) {
                        item.addMetadata(PROVENANCE, "en", "File was uploaded at " + new Date().toString(), null, -1);
                    }
                }
                else
                if("identifier".equals(request.getParameter("datafile_type"))) {
                    datasetFileSuccess = processExternalUrl(request, subInfo);
                    if(datasetFileSuccess == FILE_UPLOAD_OK) {
                        EventLogger.log(context, "submission-describe-dataset", "subaction=external_file");
                    } else {
                        EventLogger.log(context, "submission-describe-dataset", "subaction=external_file,error=external_failed");
                    }
                } else
                    //We are not uploading anything
                    datasetFileSuccess = FILE_UPLOAD_OK;
            }else
                datasetFileSuccess = FILE_UPLOAD_OK;

        }

        if (datasetFileSuccess != FILE_UPLOAD_OK) {
            //Add an error that the dataset file was not uploaded successfully
            if("file".equals(request.getParameter("datafile_type")))
                addErrorField(request, "dataset-file");
            else
                addErrorField(request, "dataset-file-url");
        }
        

        // Step 4:
        // Save changes to database
        subInfo.getSubmissionItem().update();

        // commit changes
        context.commit();

        if (moreInput)
        {
            EventLogger.log(context, "submission-describe-dataset", "error=more_input_requested");
            return STATUS_MORE_INPUT_REQUESTED;
        }
        // if one or more fields errored out, return
        else if (getErrorFields(request) != null && getErrorFields(request).size() > 0)
        {
            EventLogger.log(context, "submission-describe-dataset", "error=missing_requred_fields");
            return STATUS_MISSING_REQUIRED_FIELDS;
        }
        else if(Util.getBoolParameter(request, "processonly")){
            EventLogger.log(context, "submission-describe-dataset", "status=uploading_file");
            return STATUS_FILE_UPLOAD_ONLY;
        }
        
        // completed without errors
        EventLogger.log(context, "submission-describe-dataset", "status=complete");
        return STATUS_COMPLETE;
    }

    // ****************************************************************
    // ****************************************************************
    // METHODS FOR FILLING DC FIELDS FROM METADATA FORMS
    // ****************************************************************
    // ****************************************************************


    /**
     * Fill out an item's metadata values from a plain standard text field. If
     * the field isn't repeatable, the input field name is called:
     *
     * element_qualifier
     *
     * or for an unqualified element:
     *
     * element
     *
     * Repeated elements are appended with an underscore then an integer. e.g.:
     *
     * dc_title_alternative dc_title_alternative_1
     *
     * The values will be put in separate DCValues, ordered as they appear in
     * the list. These will replace any existing values.
     *
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema
     *            the short schema name
     * @param element
     *            the metadata element
     * @param qualifier
     *            the metadata qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
     * @param lang
     *            language to set (ISO code)
     */
    protected void readText(HttpServletRequest request, Item item, String schema,
            String element, String qualifier, boolean repeated, String lang, String splitChar)
    {
        // FIXME: Of course, language should be part of form, or determined
        // some other way
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);

        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        boolean isAuthorityControlled = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);

        // Values to add
        List<String> vals = null;
        List<String> auths = null;
        List<String> confs = null;

        if (repeated)
        {
            vals = getRepeatedParameter(request, metadataField, metadataField, splitChar);
            if (isAuthorityControlled)
            {
                auths = getRepeatedParameter(request, metadataField, metadataField+"_authority");
                confs = getRepeatedParameter(request, metadataField, metadataField+"_confidence");
            }

            // Find out if the relevant "remove" button was pressed
            // TODO: These separate remove buttons are only relevant
            // for DSpace JSP UI, and the code below can be removed
            // once the DSpace JSP UI is obsolete!
            String buttonPressed = Util.getSubmitButton(request, "");
            String removeButton = "submit_" + metadataField + "_remove_";

            if (buttonPressed.startsWith(removeButton))
            {
                int valToRemove = Integer.parseInt(buttonPressed
                        .substring(removeButton.length()));

                vals.remove(valToRemove);
            }
        }
        else
        {
            // Just a single name
            vals = new LinkedList();
            String value = request.getParameter(metadataField);
            if (value != null)
                vals.add(value.trim());
            if (isAuthorityControlled)
            {
                auths = new LinkedList();
                confs = new LinkedList();
                String av = request.getParameter(metadataField+"_authority");
                String cv = request.getParameter(metadataField+"_confidence");
                auths.add(av == null ? "":av.trim());
                confs.add(cv == null ? "":cv.trim());
        }
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < vals.size(); i++)
        {
            // Add to the database if non-empty
            String s = (String) vals.get(i);
            if ((s != null) && !s.equals(""))
            {
                if (isAuthorityControlled)
                {
                    String authKey = auths.size() > i ? (String)auths.get(i) : null;
                    String sconf = (authKey != null && confs.size() > i) ? (String)confs.get(i) : null;
                    if (MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey) &&
                        (authKey == null || authKey.length() == 0))
                    {
                        log.warn("Skipping value of "+metadataField+" because the required Authority key is missing or empty.");
                        addErrorField(request, metadataField);
                    }
                    else
                        item.addMetadata(schema, element, qualifier, lang, s,
                                authKey, (sconf != null && sconf.length() > 0) ?
                                           Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED);
                }
                else
                    item.addMetadata(schema, element, qualifier, lang, s);
            }
        }
    }

    /**
     * Get repeated values from a form. If "foo" is passed in as the parameter,
     * values in the form of parameters "foo", "foo_1", "foo_2", etc. are
     * returned.
     * <P>
     * This method can also handle "composite fields" (metadata fields which may
     * require multiple params, etc. a first name and last name).
     *
     * @param request
     *            the HTTP request containing the form information
     * @param metadataField
     *            the metadata field which can store repeated values
     * @param param
     *            the repeated parameter on the page (used to fill out the
     *            metadataField)
     * @param splitChar
     *            Character that indicates that each value may contain more values
     *            by using this char each value found will be split into multiple values using the given char
     *
     * @return a List of Strings
     */
    protected List<String> getRepeatedParameter(HttpServletRequest request,
            String metadataField, String param, String splitChar)
    {
        List<String> vals = new LinkedList<String>();

        int i = 1;    //start index at the first of the previously entered values
        boolean foundLast = false;

        // Iterate through the values in the form.
        while (!foundLast)
        {
            String s;

            //First, add the previously entered values.
            // This ensures we preserve the order that these values were entered
            s = request.getParameter(param + "_" + i);

            // If there are no more previously entered values,
            // see if there's a new value entered in textbox
            if (s==null)
            {
                s = request.getParameter(param);
                //this will be the last value added
                foundLast = true;
            }

            // We're only going to add non-null values
            if (s != null)
            {
                boolean addValue = true;

                // Check to make sure that this value was not selected to be
                // removed.
                // (This is for the "remove multiple" option available in
                // Manakin)
                String[] selected = request.getParameterValues(metadataField
                        + "_selected");

                if (selected != null)
                {
                    for (String aSelected : selected) {
                        if (aSelected.equals(metadataField + "_" + i)) {
                            addValue = false;
                        }
                    }
                }

                if (addValue){
                    if(splitChar == null)
                        vals.add(s.trim());
                    else{
                        //Split up our value
                        String[] values = s.split(splitChar);
                        for (String value : values) {
                            vals.add(value.trim());
                        }
                    }
                }
            }

            i++;
        }

        log.debug("getRepeatedParameter: metadataField=" + metadataField
                + " param=" + metadataField + ", return count = "+vals.size());

        return vals;
    }

}
