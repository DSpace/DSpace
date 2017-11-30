/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCPersonName;
import org.dspace.content.DCSeriesNumber;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Metadatum;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Describe step for DSpace submission process. Handles the gathering of
 * descriptive information (i.e. metadata) for an item being submitted into
 * DSpace.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 *
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 *
 * @author Tim Donohue
 * @version $Revision$
 */
public class DescribeStep extends AbstractProcessingStep
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(DescribeStep.class);

    /** hash of all submission forms details */
    private static DCInputsReader inputsReader = null;

    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     *
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    // user requested an extra input field to be displayed
    public static final int STATUS_MORE_INPUT_REQUESTED = 1;

    // there were required fields that were not filled out
    public static final int STATUS_MISSING_REQUIRED_FIELDS = 2;
    
    // the metadata language qualifier
    public static final String LANGUAGE_QUALIFIER = getDefaultLanguageQualifier();

    // there were validation errors found
    public static final int STATUS_ERROR_VALIDATION_FIELDS = 3;
    
    private HashMap<String,DCInput> fieldName2input =new HashMap<String,DCInput>();

    /** Constructor */
    public DescribeStep() throws ServletException
    {
        //load the DCInputsReader
        getInputsReader();
    }

    public String getHeading(HttpServletRequest request, SubmissionInfo subInfo, int pageNumber, String heading) {
        try
        {
            // get the item and current page
            Item item = subInfo.getSubmissionItem().getItem();

            // lookup applicable inputs
            Collection c = subInfo.getSubmissionItem().getCollection();
            
            String customHeading = inputsReader.getInputs(c.getHandle()).getHeading(pageNumber);
            if (StringUtils.isNotBlank(customHeading)) {
                return customHeading;
            }
        }
        catch (DCInputsReaderException | NullPointerException e)
        {
            return heading;
        }
        return heading;
    }
    
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
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        if(!request.getParameterNames().hasMoreElements()){
            //In case of an empty request do NOT just remove all metadata, just return to the submission page
            return STATUS_MORE_INPUT_REQUESTED;
        }
        // check what submit button was pressed in User Interface
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        // get the item and current page
        Item item = subInfo.getSubmissionItem().getItem();
        int currentPage = getCurrentPage(request);

        // lookup applicable inputs
        Collection c = subInfo.getSubmissionItem().getCollection();
        DCInput[] inputs = null;
        try
        {
            inputs = inputsReader.getInputs(c.getHandle()).getPageRows(
                    currentPage - 1,
                    subInfo.getSubmissionItem().hasMultipleTitles(),
                    subInfo.getSubmissionItem().isPublishedBefore());
            
        }
        catch (DCInputsReaderException e)
        {
            throw new ServletException(e);
        }

        for(DCInput input: inputs){
            String field = MetadataField
                    .formKey(input.getSchema(), input.getElement(), input.getQualifier());
            fieldName2input.put(field, input);
            
            
        }
        // Fetch the document type (dc.type)
        String documentType = "";
        if( (item.getMetadataByMetadataString("dc.type") != null) && (item.getMetadataByMetadataString("dc.type").length >0) )
        {
            documentType = item.getMetadataByMetadataString("dc.type")[0].value;
        }
        
        String scope ="";
        if(subInfo.isInWorkflow()){
            WorkflowItem wfi = (WorkflowItem)subInfo.getSubmissionItem();
            int wfState = wfi.getState();
            switch (wfState){
                case WorkflowManager.WFSTATE_STEP1:
                    scope = DCInput.WORKFLOW_STEP1_SCOPE;
                    break;
                case WorkflowManager.WFSTATE_STEP2:
                    scope = DCInput.WORKFLOW_STEP2_SCOPE;
                    break;
                case WorkflowManager.WFSTATE_STEP3:
                    scope = DCInput.WORKFLOW_STEP3_SCOPE;
                    break;
                default:
                    scope = DCInput.WORKFLOW_SCOPE;
            }
            
        }else{
            scope = DCInput.SUBMISSION_SCOPE;
        }
        // Step 1:
        // clear out all item metadata defined on this page
        for (int i = 0; i < inputs.length; i++)
        {

            // Allow the clearing out of the metadata defined for other document types, provided it can change anytime
            if (!subInfo.isEditing() && !inputs[i]
                    .isVisible(scope))
            {
                continue;
            }
            
            if (inputs[i].getInputType().equals("qualdrop_value"))
            {
                @SuppressWarnings("unchecked") // This cast is correct
                List<String> pairs = inputs[i].getPairs();
                for (int j = 0; j < pairs.size(); j += 2)
                {
                    String qualifier = pairs.get(j+1);
                    item.clearMetadata(inputs[i].getSchema(), inputs[i].getElement(), qualifier, Item.ANY);
                }
            }
            else
            {
                String qualifier = inputs[i].getQualifier();
                item.clearMetadata(inputs[i].getSchema(), inputs[i].getElement(), qualifier, Item.ANY);
            }
        }

        // Clear required-field errors first since missing authority
        // values can add them too.
        clearErrorFields(request);
        clearValidationErrorFields(request);

        // Step 2:
        // now update the item metadata.
        String fieldName;
        boolean moreInput = false;
        for (int j = 0; j < inputs.length; j++)
        {
            // Omit fields not allowed for this document type
            if(!inputs[j].isAllowedFor(documentType))
            {
                continue;
            }

            if (!subInfo.isEditing() && !inputs[j]
                        .isVisible(scope))
            {
                continue;
            }
            String element = inputs[j].getElement();
            String qualifier = inputs[j].getQualifier();
            String schema = inputs[j].getSchema();
            if (qualifier != null && !qualifier.equals(Item.ANY))
            {
                fieldName = schema + "_" + element + '_' + qualifier;
            }
            else
            {
                fieldName = schema + "_" + element;
            }

            String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
            ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
            String inputType = inputs[j].getInputType();
            if (inputType.equals("name"))
            {
                readNames(request, item, schema, element, qualifier, inputs[j]);
            }
            else if (inputType.equals("date"))
            {
                readDate(request, item, schema, element, qualifier, inputs[j]);
            }
            // choice-controlled input with "select" presentation type is
            // always rendered as a dropdown menu
            else if (inputType.equals("dropdown") || inputType.equals("list") 
                    || inputType.equals("year")
                    || inputType.equals("year_noinprint") 
                    || (cmgr.isChoicesConfigured(fieldKey) &&
                      "select".equals(cmgr.getPresentation(fieldKey))))
            {
                String[] vals = request.getParameterValues(fieldName);
                if (vals != null)
                {
                    for (int z = 0; z < vals.length; z++)
                    {
                        if (!vals[z].equals(""))
                        {
                            item.addMetadata(schema, element, qualifier, LANGUAGE_QUALIFIER,
                                    vals[z]);
                            validateField(request, inputs[j], fieldKey, vals[z]);
                        }
                    }
                }
            }
            else if (inputType.equals("series"))
            {
                readSeriesNumbers(request, item, schema, element, qualifier,
                        inputs[j]);
            }
            else if (inputType.equals("qualdrop_value"))
            {
                List<String> quals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_qualifier");
                List<String> vals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_value");
                for (int z = 0; z < vals.size(); z++)
                {
                    String thisQual = quals.get(z);
                    if ("".equals(thisQual))
                    {
                        thisQual = null;
                    }
                    String thisVal = vals.get(z);
                    if (!buttonPressed.equals("submit_" + schema + "_"
                            + element + "_remove_" + z)
                            && !thisVal.equals(""))
                    {
                        item.addMetadata(schema, element, thisQual, null,
                                thisVal);
                        validateField(request, inputs[j], fieldKey, thisVal);
                    }
                }
            }
            else if (inputType.equals("number") || (inputType.equals("onebox"))
                    || (inputType.equals("twobox"))
                    || (inputType.equals("textarea")))
            {
                readText(request, item, schema, element, qualifier, LANGUAGE_QUALIFIER, inputs[j]);
            }
            else
            {
                throw new ServletException("Field " + fieldName
                        + " has an unknown input type: " + inputType);
            }

            // determine if more input fields were requested
            if (!moreInput
                    && buttonPressed.equals("submit_" + fieldName + "_add"))
            {
                subInfo.setMoreBoxesFor(fieldName);
                subInfo.setJumpToField(fieldName);
                moreInput = true;
            }
            // was XMLUI's "remove" button pushed?
            else if (buttonPressed.equals("submit_" + fieldName + "_delete"))
            {
                subInfo.setJumpToField(fieldName);
            }
        }

        // Step 3:
        // Check to see if any fields are missing
        // Only check for required fields if user clicked the "next", the "previous" or the "progress bar" button
        if (buttonPressed.equals(NEXT_BUTTON)
                || buttonPressed.startsWith(PROGRESS_BAR_PREFIX)
                || buttonPressed.equals(PREVIOUS_BUTTON)
                || buttonPressed.equals(CANCEL_BUTTON))
        {
            for (int i = 0; i < inputs.length; i++)
            {
                // Do not check the required attribute if it is not visible or not allowed for the document type

				if (!(subInfo.isEditing() && inputs[i].isAllowedFor(documentType))
						&& !(inputs[i].isVisible(scope) && inputs[i].isAllowedFor(documentType)))
                {
                    continue;
                }

                String qualifier = inputs[i].getQualifier();
                if (qualifier == null
                        && inputs[i].getInputType().equals("qualdrop_value"))
                {
                    qualifier = Item.ANY;
                }
                Metadatum[] values = item.getMetadata(inputs[i].getSchema(),
                        inputs[i].getElement(), qualifier, Item.ANY);

                if ((inputs[i].isRequired() && values.length == 0) &&
                     inputs[i].isVisible(scope))
                {
                    // since this field is missing add to list of error fields
                    addErrorField(request, getFieldName(inputs[i]));
                }
            }
        }

        if (getValidationErrorFields(request) == null) 
        {
            // Step 4:
            // Save changes to database
            subInfo.getSubmissionItem().update();
    
            // commit changes
            context.commit();
        }
        else {
            return STATUS_ERROR_VALIDATION_FIELDS;
        }

        // check for request for more input fields, first
        if (moreInput)
        {
            return STATUS_MORE_INPUT_REQUESTED;
        }
        // if one or more fields errored out, return
        else if (getErrorFields(request) != null && getErrorFields(request).size() > 0)
        {
            if (!subInfo.isEditing() && !subInfo.isInWorkflow()
                    && subInfo.getSubmissionItem() != null)
            {
                WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();
                wi.setStageReached(AbstractProcessingStep.getCurrentStepConfig(
                        request, subInfo).getStepNumber());
                wi.setPageReached(currentPage);
                wi.update();
                context.commit();
            }
            return STATUS_MISSING_REQUIRED_FIELDS;
        }

        // completed without errors
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
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        // by default, use the "default" collection handle
        String collectionHandle = DCInputsReader.DEFAULT_COLLECTION;

        if (subInfo.getSubmissionItem() != null)
        {
            collectionHandle = subInfo.getSubmissionItem().getCollection()
                    .getHandle();
        }

        // get number of input pages (i.e. "Describe" pages)
        try
        {
            return getInputsReader().getNumberInputPages(collectionHandle);
        }
        catch (DCInputsReaderException e)
        {
            throw new ServletException(e);
        }
    }

    /**
     *
     * @return the current DCInputsReader
     */
    public static DCInputsReader getInputsReader() throws ServletException
    {
        // load inputsReader only the first time
        if (inputsReader == null)
        {
            // read configurable submissions forms data
            try
            {
                inputsReader = new DCInputsReader();
            }
            catch (DCInputsReaderException e)
            {
                throw new ServletException(e);
            }
        }
        
        return inputsReader;
    }
    
    /**
     * @param filename
     *        file to get the input reader for
     * @return the current DCInputsReader
     */
    public static DCInputsReader getInputsReader(String filename) throws ServletException
    {
        try
        {
            inputsReader = new DCInputsReader(filename);
        }
        catch (DCInputsReaderException e)
        {
            throw new ServletException(e);
        }
        return inputsReader;
    }
    
    /**
     * @return the default language qualifier for metadata
     */
    
    public static String getDefaultLanguageQualifier()
    {
       String language = "";
       language = ConfigurationManager.getProperty("default.language");
       if (StringUtils.isEmpty(language))
       {
           language = "en";
       }
       return language;
    }
    
    // ****************************************************************
    // ****************************************************************
    // METHODS FOR FILLING DC FIELDS FROM METADATA FORMS
    // ****************************************************************
    // ****************************************************************

    /**
     * Set relevant metadata fields in an item from name values in the form.
     * Some fields are repeatable in the form. If this is the case, and the
     * field is "dc.contributor.author", the names in the request will be from
     * the fields as follows:
     *
     * dc_contributor_author_last -> last name of first author
     * dc_contributor_author_first -> first name(s) of first author
     * dc_contributor_author_last_1 -> last name of second author
     * dc_contributor_author_first_1 -> first name(s) of second author
     *
     * and so on. If the field is unqualified:
     *
     * dc_contributor_last -> last name of first contributor
     * dc_contributor_first -> first name(s) of first contributor
     *
     * If the parameter "submit_dc_contributor_author_remove_n" is set, that
     * value is removed.
     *
     * Otherwise the parameters are of the form:
     *
     * dc_contributor_author_last dc_contributor_author_first
     *
     * The values will be put in separate Metadatums, in the form "last name,
     * first name(s)", ordered as they appear in the list. These will replace
     * any existing values.
     *
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema
     *            the metadata schema
     * @param element
     *            the metadata element
     * @param qualifier
     *            the metadata qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
     */
    protected void readNames(HttpServletRequest request, Item item,
            String schema, String element, String qualifier, DCInput dcInput)
    {
        boolean repeated = dcInput.getRepeatable();
        
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);

        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        boolean isAuthorityControlled = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);

        // Names to add
        List<String> firsts = new LinkedList<String>();
        List<String> lasts = new LinkedList<String>();
        List<String> auths = new LinkedList<String>();
        List<String> confs = new LinkedList<String>();

        if (repeated)
        {
            firsts = getRepeatedParameter(request, metadataField, metadataField
                    + "_first");
            lasts = getRepeatedParameter(request, metadataField, metadataField
                    + "_last");

            if(isAuthorityControlled)
            {
               auths = getRepeatedParameter(request, metadataField, metadataField
                    + "_authority");
               confs = getRepeatedParameter(request, metadataField, metadataField
                    + "_confidence");
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

                firsts.remove(valToRemove);
                lasts.remove(valToRemove);
                if(isAuthorityControlled)
                {
                    if(valToRemove < auths.size())
                    {
                        auths.remove(valToRemove);
                        confs.remove(valToRemove);
                    }
                }
            }
        }
        else
        {
            // Just a single name
            String lastName = request.getParameter(metadataField + "_last");
            String firstNames = request.getParameter(metadataField + "_first");
            String authority = request.getParameter(metadataField + "_authority");
            String confidence = request.getParameter(metadataField + "_confidence");

            if (lastName != null)
            {
                lasts.add(lastName);
            }
            if (firstNames != null)
            {
                firsts.add(firstNames);
            }
            auths.add(authority == null ? "" : authority);
            confs.add(confidence == null ? "" : confidence);
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < lasts.size(); i++)
        {
            String f = firsts.get(i);
            String l = lasts.get(i);

            // only add if lastname is non-empty
            if ((l != null) && !((l.trim()).equals("")))
            {
                // Ensure first name non-null
                if (f == null)
                {
                    f = "";
                }

                // If there is a comma in the last name, we take everything
                // after that comma, and add it to the right of the
                // first name
                int comma = l.indexOf(',');

                if (comma >= 0)
                {
                    f = f + l.substring(comma + 1);
                    l = l.substring(0, comma);

                    // Remove leading whitespace from first name
                    while (f.startsWith(" "))
                    {
                        f = f.substring(1);
                    }
                }

                // Add to the database -- unless required authority is missing
                DCPersonName dcPersonName = new DCPersonName(l, f);
                if (isAuthorityControlled)
                {
                    String authKey = auths.size() > i ? auths.get(i) : null;
                    String sconf = (authKey != null && confs.size() > i) ? confs.get(i) : null;
                    if (MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey) &&
                        (authKey == null || authKey.length() == 0))
                    {
                        log.warn("Skipping value of "+metadataField+" because the required Authority key is missing or empty.");
                        addErrorField(request, metadataField);
                    }
                    else
                    {
                        item.addMetadata(schema, element, qualifier, null,
                                dcPersonName.toString(), authKey,
                                (sconf != null && sconf.length() > 0) ?
                                        Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED);
                        validateField(request, dcInput, metadataField, dcPersonName.toString());
                    }
                }
                else
                {
                    item.addMetadata(schema, element, qualifier, null,
                            dcPersonName.toString());
                    validateField(request, dcInput, metadataField, dcPersonName.toString());
                }
            }
        }
    }

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
     * The values will be put in separate Metadatums, ordered as they appear in
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
            String element, String qualifier, String lang, DCInput dcInput)
    {
        boolean repeated = dcInput.getRepeatable();
        // FIXME: Of course, language should be part of form, or determined
        // some other way
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);

        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        boolean isAuthorityControlled = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);
        
        String parentMetadataField = "";
        String parentMetadataFieldParam = "";
        boolean parentRepeatable = false;
        if(dcInput.hasParent()){
            parentMetadataField=dcInput.getParent();
            parentRepeatable = fieldName2input.get(parentMetadataField).isRepeatable();
        }

        // Values to add
        List<String> vals = null;
        List<String> auths = null;
        List<String> confs = null;

        if (parentRepeatable)
        {
            String parentType= fieldName2input.get(parentMetadataField).getInputType();
            if (StringUtils.equals(parentType,"name"))
            {
                parentMetadataFieldParam = parentMetadataField+"_last";
            }
            else if (StringUtils.equals(parentType,"date"))
            {
                parentMetadataFieldParam = parentMetadataField+"_year";
            }
            else if (StringUtils.equals(parentType,"series"))
            {
                parentMetadataFieldParam = parentMetadataField+"_series";
            }
            else if (StringUtils.equals(parentType,"qualdrop_value"))
            {
                parentMetadataFieldParam = parentMetadataField+"_value";
            }else{
                parentMetadataFieldParam = parentMetadataField;
            }
                
            
            vals = getRepeatedParameterParent(request, metadataField, metadataField,parentMetadataField,parentMetadataFieldParam);
            if (isAuthorityControlled)
            {
                auths = getRepeatedParameterParent(request, metadataField, metadataField+"_authority",parentMetadataField,parentMetadataFieldParam);
                confs = getRepeatedParameterParent(request, metadataField, metadataField+"_confidence",parentMetadataField,parentMetadataFieldParam);
            }

            // Find out if the relevant "remove" button was pressed
            // TODO: These separate remove buttons are only relevant
            // for DSpace JSP UI, and the code below can be removed
            // once the DSpace JSP UI is obsolete!
            String buttonPressed = Util.getSubmitButton(request, "");
            String removeButton = "submit_" + parentMetadataField + "_remove_";

            if (buttonPressed.startsWith(removeButton))
            {
                int valToRemove = Integer.parseInt(buttonPressed
                        .substring(removeButton.length()));

                vals.remove(valToRemove);
                if(isAuthorityControlled)
                {
                   auths.remove(valToRemove);
                   confs.remove(valToRemove);
                }
            }
        }
        else if(!dcInput.hasParent() && repeated ){
            vals = getRepeatedParameter(request, metadataField, metadataField);
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
                if(isAuthorityControlled)
                {
                   auths.remove(valToRemove);
                   confs.remove(valToRemove);
                }
            }

        }else if(StringUtils.isNotBlank(parentMetadataField)){
            vals = new LinkedList<String>();
            String value = request.getParameter(metadataField);
            String parent = "";
            String parentType= fieldName2input.get(parentMetadataField).getInputType();
            
            if (StringUtils.equals(parentType,"name"))
            {
                parent = request.getParameter(parentMetadataField+"_last");
            }
            else if (StringUtils.equals(parentType,"date"))
            {
                parent = request.getParameter(parentMetadataField+"_year");
            }
            else if (StringUtils.equals(parentType,"series"))
            {
                parent = request.getParameter(parentMetadataField+"_series");
            }
            else if (StringUtils.equals(parentType,"qualdrop_value"))
            {
                parent = request.getParameter(parentMetadataField+"_value");
            }else{
                parent = request.getParameter(parentMetadataField);
            }

            if (StringUtils.isNotBlank(parent))
            {
                if(StringUtils.isNotBlank(value)){
                    vals.add(value.trim());
                }else{
                    vals.add(MetadataValue.PARENT_PLACEHOLDER_VALUE);
                }
                
                if (isAuthorityControlled)
                {
                    auths = new LinkedList<String>();
                    confs = new LinkedList<String>();
                    String av = request.getParameter(metadataField+"_authority");
                    String cv = request.getParameter(metadataField+"_confidence");
                    auths.add(av == null ? "":av.trim());
                    confs.add(cv == null ? "":cv.trim());
                }
            }
        }
        else
        {
            // Just a single name
            vals = new LinkedList<String>();
            String value = request.getParameter(metadataField);
            
            if (value != null)
            {
                vals.add(value.trim());
            }
            if (isAuthorityControlled)
            {
                auths = new LinkedList<String>();
                confs = new LinkedList<String>();
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
            String s = vals.get(i);
            if ((s != null) && !s.equals(""))
            {
                if (isAuthorityControlled)
                {
                    String authKey = auths.size() > i ? auths.get(i) : null;
                    String sconf = (authKey != null && confs.size() > i) ? confs.get(i) : null;
                    if (MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey) &&
                        (authKey == null || authKey.length() == 0))
                    {
                        log.warn("Skipping value of "+metadataField+" because the required Authority key is missing or empty.");
                        addErrorField(request, metadataField);
                    }
                    else
                    {
                        item.addMetadata(schema, element, qualifier, lang, s,
                                authKey, (sconf != null && sconf.length() > 0) ?
                                        Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED);
                        validateField(request, dcInput, metadataField, s);
                    }
                }
                else
                {
                    item.addMetadata(schema, element, qualifier, lang, s);
                    validateField(request, dcInput, metadataField, s);
                }
            }
        }
    }

    private void validateField(HttpServletRequest request, DCInput dcInput,
            String metadataField, String s)
    {
        if(!(dcInput.validate(s))) {
            addValidationErrorField(request, metadataField); 
        }
    }

    /**
     * Fill out a metadata date field with the value from a form. The date is
     * taken from the three parameters:
     *
     * element_qualifier_year element_qualifier_month element_qualifier_day
     *
     * The granularity is determined by the values that are actually set. If the
     * year isn't set (or is invalid)
     *
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema
     *            the metadata schema
     * @param element
     *            the metadata element
     * @param qualifier
     *            the metadata qualifier, or null if unqualified
     * @throws SQLException
     */
    protected void readDate(HttpServletRequest request, Item item, String schema,
            String element, String qualifier, DCInput dcInput) throws SQLException
    {
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);
        String parentMetadataField ="";
        String parentMetadataFieldParam ="";
        List<String> vals =null;
        
        boolean parentRepeatable= false;
        if(dcInput.hasParent() ){
            parentMetadataField = dcInput.getParent();
            parentRepeatable = fieldName2input.get(parentMetadataField).isRepeatable();
        }
        
        if(parentRepeatable){
            String parentType= fieldName2input.get(parentMetadataField).getInputType();
            if (StringUtils.equals(parentType,"name"))
            {
                parentMetadataFieldParam = parentMetadataField+"_last";
            }
            else if (StringUtils.equals(parentType,"date"))
            {
                parentMetadataFieldParam = parentMetadataField+"_year";
            }
            else if (StringUtils.equals(parentType,"series"))
            {
                parentMetadataFieldParam = parentMetadataField+"_series";
            }
            else if (StringUtils.equals(parentType,"qualdrop_value"))
            {
                parentMetadataFieldParam = parentMetadataField+"_value";
            }else{
                parentMetadataFieldParam = parentMetadataField;
            }
                
            
            vals = getDateRepeatedParameterParent(request, metadataField, metadataField,parentMetadataField,parentMetadataFieldParam);

            // Find out if the relevant "remove" button was pressed
            // TODO: These separate remove buttons are only relevant
            // for DSpace JSP UI, and the code below can be removed
            // once the DSpace JSP UI is obsolete!
            String buttonPressed = Util.getSubmitButton(request, "");
            String removeButton = "submit_" + parentMetadataField + "_remove_";

            if (buttonPressed.startsWith(removeButton))
            {
                int valToRemove = Integer.parseInt(buttonPressed
                        .substring(removeButton.length()));

                vals.remove(valToRemove);

            }
        }
        int year = Util.getIntParameter(request, metadataField + "_year");
        int month = Util.getIntParameter(request, metadataField + "_month");
        int day = Util.getIntParameter(request, metadataField + "_day");


        
        // FIXME: Probably should be some more validation
        // Make a standard format date
        DCDate d = new DCDate(year, month, day, -1, -1, -1);

        // already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        if (year > 0)
        {
            // Only put in date if there is one!
            item.addMetadata(schema, element, qualifier, null, d.toString());
            validateField(request, dcInput, metadataField, d.toString());
        }
    }

    /**
     * Set relevant metadata fields in an item from series/number values in the
     * form. Some fields are repeatable in the form. If this is the case, and
     * the field is "relation.ispartof", the names in the request will be from
     * the fields as follows:
     *
     * dc_relation_ispartof_series dc_relation_ispartof_number
     * dc_relation_ispartof_series_1 dc_relation_ispartof_number_1
     *
     * and so on. If the field is unqualified:
     *
     * dc_relation_series dc_relation_number
     *
     * Otherwise the parameters are of the form:
     *
     * dc_relation_ispartof_series dc_relation_ispartof_number
     *
     * The values will be put in separate Metadatums, in the form "last name,
     * first name(s)", ordered as they appear in the list. These will replace
     * any existing values.
     *
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema
     *            the metadata schema
     * @param element
     *            the metadata element
     * @param qualifier
     *            the metadata qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
     */
    protected void readSeriesNumbers(HttpServletRequest request, Item item,
            String schema, String element, String qualifier, DCInput dcInput)
    {
        boolean repeated = dcInput.getRepeatable();
        
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);

        // Names to add
        List<String> series = new LinkedList<String>();
        List<String> numbers = new LinkedList<String>();

        if (repeated)
        {
            series = getRepeatedParameter(request, metadataField, metadataField
                    + "_series");
            numbers = getRepeatedParameter(request, metadataField,
                    metadataField + "_number");

            // Find out if the relevant "remove" button was pressed
            String buttonPressed = Util.getSubmitButton(request, "");
            String removeButton = "submit_" + metadataField + "_remove_";

            if (buttonPressed.startsWith(removeButton))
            {
                int valToRemove = Integer.parseInt(buttonPressed
                        .substring(removeButton.length()));

                series.remove(valToRemove);
                numbers.remove(valToRemove);
            }
        }
        else
        {
            // Just a single name
            String s = request.getParameter(metadataField + "_series");
            String n = request.getParameter(metadataField + "_number");

            // Only put it in if there was a name present
            if ((s != null) && !s.equals(""))
            {
                // if number is null, just set to a nullstring
                if (n == null)
                {
                    n = "";
                }

                series.add(s);
                numbers.add(n);
            }
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < series.size(); i++)
        {
            String s = (series.get(i)).trim();
            String n = (numbers.get(i)).trim();

            // Only add non-empty
            if (!s.equals("") || !n.equals(""))
            {
                DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(s, n);
                item.addMetadata(schema, element, qualifier, null,
                        dcSeriesNumber.toString());
                validateField(request, dcInput, metadataField, dcSeriesNumber.toString());
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
     *
     * @return a List of Strings
     */
    protected List<String> getRepeatedParameter(HttpServletRequest request,
            String metadataField, String param)
    {
        List<String> vals = new LinkedList<String>();

        int i = 1;    //start index at the first of the previously entered values
        boolean foundLast = false;

        // Iterate through the values in the form.
        while (!foundLast)
        {
            String s = null;

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
                    for (int j = 0; j < selected.length; j++)
                    {
                        if (selected[j].equals(metadataField + "_" + i))
                        {
                            addValue = false;
                        }
                    }
                }

                if (addValue)
                {
                    vals.add(s.trim());
                }
            }

            i++;
        }

        log.debug("getRepeatedParameter: metadataField=" + metadataField
                + " param=" + metadataField + ", return count = "+vals.size());

        return vals;
    }

   
    protected List<String> getDateRepeatedParameterParent(HttpServletRequest request,
            String metadataField, String param,String parentMetadataField, String parentParam)
    {
        List<String> vals = new LinkedList<String>();

        int i = 1;    //start index at the first of the previously entered values
        boolean foundLast = false;

        // Iterate through the values in the form.
        while (!foundLast)
        {
            String s = null;
            String parent = null;
            //First, add the previously entered values.
            // This ensures we preserve the order that these values were entered
            s = request.getParameter(param + "_" + i);
            parent = request.getParameter(parentParam + "_" + i);

            // If there are no more previously entered values,
            // see if there's a new value entered in textbox
            if (!StringUtils.isNotBlank(parent))
            {
                s = request.getParameter(param);
                parent= request.getParameter(parentParam);
                //this will be the last value added
                foundLast = true;
            }

            // We're only going to add non-null values
            if (StringUtils.isNotBlank(parent))
            {
                boolean addValue = true;

                // Check to make sure that this value was not selected to be
                // removed.
                // (This is for the "remove multiple" option available in
                // Manakin)
                String[] selected = request.getParameterValues(parentMetadataField
                        + "_selected");

                if (selected != null)
                {
                    for (int j = 0; j < selected.length; j++)
                    {
                        if (selected[j].equals(parentMetadataField + "_" + i))
                        {
                            addValue = false;
                        }
                    }
                }

                if (addValue)
                {
                    if(!StringUtils.isNotBlank(s) && StringUtils.equals(param, metadataField)){
                        //vals.add(MetadataValue.PARENT_PLACEHOLDER_VALUE);
                    }else if(StringUtils.isNotBlank(s)){
                        //vals.add();
                    }else{
                        //vals.add("");
                    }
                        

                }
            }

            i++;
        }

        log.debug("getRepeatedParameter: metadataField=" + metadataField
                + " param=" + metadataField + ", return count = "+vals.size());

        return vals;
    }    
    
    protected List<String> getRepeatedParameterParent(HttpServletRequest request,
            String metadataField, String param,String parentMetadataField, String parentParam)
    {
        List<String> vals = new LinkedList<String>();

        int i = 1;    //start index at the first of the previously entered values
        boolean foundLast = false;

        // Iterate through the values in the form.
        while (!foundLast)
        {
            String s = null;
            String parent = null;
            //First, add the previously entered values.
            // This ensures we preserve the order that these values were entered
            s = request.getParameter(param + "_" + i);
            parent = request.getParameter(parentParam + "_" + i);

            // If there are no more previously entered values,
            // see if there's a new value entered in textbox
            if (!StringUtils.isNotBlank(parent))
            {
                s = request.getParameter(param);
                parent= request.getParameter(parentParam);
                //this will be the last value added
                foundLast = true;
            }

            // We're only going to add non-null values
            if (StringUtils.isNotBlank(parent))
            {
                boolean addValue = true;

                // Check to make sure that this value was not selected to be
                // removed.
                // (This is for the "remove multiple" option available in
                // Manakin)
                String[] selected = request.getParameterValues(parentMetadataField
                        + "_selected");

                if (selected != null)
                {
                    for (int j = 0; j < selected.length; j++)
                    {
                        if (selected[j].equals(parentMetadataField + "_" + i))
                        {
                            addValue = false;
                        }
                    }
                }

                if (addValue)
                {
                    if(!StringUtils.isNotBlank(s) && StringUtils.equals(param, metadataField)){
                        vals.add(MetadataValue.PARENT_PLACEHOLDER_VALUE);
                    }else if(StringUtils.isNotBlank(s)){
                        vals.add(StringUtils.trim(s));
                    }else{
                        vals.add("");
                    }
                        

                }
            }

            i++;
        }

        log.debug("getRepeatedParameter: metadataField=" + metadataField
                + " param=" + metadataField + ", return count = "+vals.size());

        return vals;
    }
    
    
    
    /**
     * Return the HTML / DRI field name for the given input.
     *
     * @param input
     */
    public static String getFieldName(DCInput input)
    {
        String dcSchema = input.getSchema();
        String dcElement = input.getElement();
        String dcQualifier = input.getQualifier();
        if (dcQualifier != null && !dcQualifier.equals(Item.ANY))
        {
            return dcSchema + "_" + dcElement + '_' + dcQualifier;
        }
        else
        {
            return dcSchema + "_" + dcElement;
        }

    }
}
