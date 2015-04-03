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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

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
 * based on class by Tim Donohue
 * modified for LINDAT/CLARIN
 * @version $Revision$
 */
public class DescribeStep extends AbstractProcessingStep
{
    
    public static final String REPEATABLE_SPLIT_REGEX = ",|;";
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(DescribeStep.class);

    /** hash of all submission forms details */
    protected static DCInputsReader inputsReader = null;

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
    
    public static final int STATUS_REGEX_ERROR = 3;

    private static final String REGEX_ERROR_ATTRIBUTE = "dspace.submit.regex_error";
    
    // the metadata language qualifier
    public static final String LANGUAGE_QUALIFIER = getDefaultLanguageQualifier();
    
    // UFAL: the following metadata can have non-unique values (input-type: "onebox")
    private static final String nonUniqueMd[] = new String[] {
    	"metashare.ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo.projectName", 
    	"metashare.ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo.fundingType"
    }; 
    private static final Set<String> nonUniqueMdSet = new HashSet<String>(Arrays.asList(nonUniqueMd));
    
    
    /** Constructor */
    public DescribeStep() throws ServletException
    {
        //load the DCInputsReader
        getInputsReader();
    }

    public DescribeStep(DCInputsReader inputs_reader) throws ServletException
    {
        //load the DCInputsReader
        inputsReader = inputs_reader;
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

        // Fetch the document type (dc.type)
        String documentType = "";
        if( (item.getMetadataByMetadataString("dc.type") != null) && (item.getMetadataByMetadataString("dc.type").length >0) )
        {
            documentType = item.getMetadataByMetadataString("dc.type")[0].value;
        }
        
        // Step 1:
        // clear out all item metadata defined on this page
        for (int i = 0; i < inputs.length; i++)
        {

        	// Allow the clearing out of the metadata defined for other document types, provided it can change anytime
        	
            if (!inputs[i]
                    .isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE
                            : DCInput.SUBMISSION_SCOPE))
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

            if (!inputs[j]
                        .isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE
                                : DCInput.SUBMISSION_SCOPE))
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
                readNames(request, item, schema, element, qualifier, inputs[j]
                        .getRepeatable());
            }
            else if (inputType.equals("date"))
            {
                readDate(request, item, schema, element, qualifier);
            }
            // choice-controlled input with "select" presentation type is
            // always rendered as a dropdown menu
            else if (inputType.equals("dropdown") || inputType.equals("list") ||
                     (cmgr.isChoicesConfigured(fieldKey) &&
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
                        }
                    }
                }
            }
            else if (inputType.equals("series"))
            {
                readSeriesNumbers(request, item, schema, element, qualifier,
                        inputs[j].getRepeatable());
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
                    }
                }
            }
            else if ((inputType.equals("onebox"))
                    || (inputType.equals("twobox"))
                    || (inputType.equals("textarea")))
            {
                readText(request, item, schema, element, qualifier, inputs[j]
                        .getRepeatable(), LANGUAGE_QUALIFIER, inputs[j].getRepeatableParse());
            } else if(inputType.equals("complex")){
                readComplex(request, item, schema, element, qualifier, inputs[j]);
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
            	String scope = subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
                if ( !( inputs[i].isVisible(scope) && inputs[i].isAllowedFor(documentType) ) )
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
                     inputs[i].isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE))
                {
                    // since this field is missing add to list of error fields
                    addErrorField(request, getFieldName(inputs[i]));
                }
                
                // Check whether the value matches given regexp
                //TODO: use regexp handling that was created for complex fields see addRegexError
                for(Metadatum dcval:values){
                    if(!inputs[i].isAllowedValue(dcval.value)){
                        addErrorField(request, getFieldName(inputs[i]));
                    }
                }         
            }
        }

        // Step 4:
        // Save changes to database
        subInfo.getSubmissionItem().update();

        // commit changes
        context.commit();

        // check for request for more input fields, first
        if (moreInput)
        {
            return STATUS_MORE_INPUT_REQUESTED;
        }
        else if(getBrokenValues(request) != null && getBrokenValues(request).size() > 0){
        	return STATUS_REGEX_ERROR;
        }
        // if one or more fields errored out, return
        else if (getErrorFields(request) != null && getErrorFields(request).size() > 0)
        {
            return STATUS_MISSING_REQUIRED_FIELDS;
        }

        // completed without errors
        return STATUS_COMPLETE;
    }

    

    private void readComplex(HttpServletRequest request, Item item,
			String schema, String element, String qualifier, DCInput input) {

        String metadataField = MetadataField
                .formKey(schema, element, qualifier);
        boolean repeated = input.getRepeatable();
        
        DCInput.ComplexDefinition definition = input.getComplexDefinition();
        java.util.Map<String, java.util.List<String>> fieldsValues = new HashMap<String, java.util.List<String>>();
        int valuesPerField = 0;

        if (repeated)
        {
        	for(String name : definition.getInputNames()){
        		List<String> list = getRepeatedParameter(request, metadataField, metadataField + "_" + name);
                fieldsValues.put(name, list);
                //assume the list are all the same size
                valuesPerField = list.size();
        	}

        }
        else
        {
        	for(String name : definition.getInputNames()){
        		List<String> list = new ArrayList<String>();
        		String value = request.getParameter(metadataField + "_" + name);
        		if(value != null){
        			list.add(value);
        			fieldsValues.put(name, list);
        		}
        	}
        	valuesPerField = 1;
        }


        boolean error = false;
        //for all values
        for(int i = 0; i < valuesPerField; i++){
        	StringBuilder complexValue = new StringBuilder();
        	int emptyFields = 0;
        	//separator empty for first iter
        	String separator = "";
        	//in all fields
        	for(String name : definition.getInputNames()){
        		String value = fieldsValues.get(name).get(i);
        		if(value != null){
        			value = value.trim();
        			value = value.replaceAll(DCInput.ComplexDefinition.SEPARATOR, DCInput.ComplexDefinition.SEPARATOR.replaceAll("(.)", "_$1_"));
        		} else{
        			value = "";
        		}
        		if("".equals(value)){
        			++emptyFields;
        		}
                String regex = definition.getInput(name).get("regexp");
                if(!value.isEmpty() && !DCInput.isAllowedValue(value, regex)){
                	error = true;
                }
        		complexValue.append(separator).append(value);
        		//non empty separator for the remaining iterations;
        		separator = DCInput.ComplexDefinition.SEPARATOR;
        	}
        	//required and all empty handled by doProcessing
        	//this checks whether one of the fields was empty
        	String finalValue = complexValue.toString();
        	if( emptyFields > 0 && emptyFields < definition.inputsCount()){
        		error = true;
        	}
        	if(error){
        		//incomplete as regex errors
        		addRegexError(request,metadataField,finalValue);
        	}else if(emptyFields != definition.inputsCount()){
        		//add the final value only if it is not empty
        		item.addMetadata(schema, element, qualifier, null, finalValue);
        	}
        }
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
     * The values will be put in separate DCValues, in the form "last name,
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
            String schema, String element, String qualifier, boolean repeated)
    {
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
                                new DCPersonName(l, f).toString(), authKey,
                                (sconf != null && sconf.length() > 0) ?
                                        Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED);
                    }
                }
                else
                {
                    item.addMetadata(schema, element, qualifier, null,
                            new DCPersonName(l, f).toString());
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
            String element, String qualifier, boolean repeated, String lang, boolean repeatable_parse) {
      readText( request, item, schema, element, qualifier, repeated, lang, null, repeatable_parse);
    }

    protected void readText(HttpServletRequest request, Item item, String schema,
            String element, String qualifier, boolean repeated, String lang, String repeatable_component,
            boolean repeatable_parse)
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
        
        String mdString;
        if ((qualifier == null) || qualifier.equals("")) {
        	mdString = schema + "." + element;
        }
        else {
        	mdString = schema + "." + element + "." + qualifier;
        }

        if (repeated)
        {
            vals = getRepeatedParameter(request, metadataField, metadataField);
            
            // if someone adds strings to repeatable with ;, split them if specified 
            //   in input-forms.xml
            if ( repeatable_parse ) 
            {
                split_strings(vals);
            }

            // only unique values are allowed except the ones 
            // defined in nonUniqMdSet
            if (!nonUniqueMdSet.contains(mdString)) {
                Set<String> uniqueValues = new HashSet<String>();
                List<String> uniqueVals = new LinkedList<String>();
                for ( int i=0; i < vals.size(); i++ ) {
                	if (!uniqueValues.contains(vals.get(i))) {
                    	uniqueValues.add(vals.get(i));
                    	uniqueVals.add(vals.get(i));
                	}
                }
                vals = uniqueVals;            	
            }

                        
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
        int max_num = 0;
        for (int i = 0; i < vals.size(); i++)
        {
            // Add to the database if non-empty
            String s = (String) vals.get(i);

            // UFAL/jmisutka
            if ( null != repeatable_component  &&
                    Util.getSubmitButton(request,NEXT_BUTTON).startsWith("submit_component_"+repeatable_component) ) {
              Pattern p = Pattern.compile( "#(\\d+)-.*" );
              Matcher m = p.matcher(s);
              if ( m.find() ) {
                max_num = Math.max( max_num, Integer.parseInt(m.group(1)) );
              }else {
                ++max_num;
                s = String.format("#%s-%s", String.valueOf(max_num),s );
              }
          }

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
                    }
                }
                else
                {
                    item.addMetadata(schema, element, qualifier, lang, s);
                }
            }
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
            String element, String qualifier) throws SQLException
    {
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);

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
     * The values will be put in separate DCValues, in the form "last name,
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
            String schema, String element, String qualifier, boolean repeated)
    {
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
                item.addMetadata(schema, element, qualifier, null,
                        new DCSeriesNumber(s, n).toString());
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

    
    /**
     * Split strings on separators.
     * 
     * @param vals
     */
    public static void split_strings( List<String> string_list )
    {
        for ( int i = 0; i < string_list.size(); ++i ) 
        {
            String[] splits = string_list.get(i).split(REPEATABLE_SPLIT_REGEX);
            // replace the one value with multiple
            if ( splits.length > 1 ) 
            {
                string_list.remove(i);
                // preserve the order 
                for ( int j = splits.length - 1; j >= 0; --j ) {
                    string_list.add(i, splits[j].trim() );
                }
            }
        }
    }
    
    public static final List<String> getBrokenValues(HttpServletRequest request){
        return (List<String>) request.getAttribute(REGEX_ERROR_ATTRIBUTE);
    }
    
    protected static final void addRegexError(HttpServletRequest request, String fieldName, String value)
    {
        //get current list
        List<String> errorFields = getBrokenValues(request);
        
        if (errorFields == null)
        {
            errorFields = new ArrayList<String>();
        }

        //add this field
        errorFields.add(fieldName);
	//XXX this is obscure, but the list essentially gets converted to a comma separated string in submission.js::saveRegexError. Commas in user input would break that, but they can't be just .replaced as we need the entered value
        errorFields.add(javax.xml.bind.DatatypeConverter.printBase64Binary(value.getBytes()));
        
        //save updated list
        setRegexError(request, errorFields);
    }
    private static final void setRegexError(HttpServletRequest request, List<String> errorFields)
    {
        if(errorFields==null)
        {
            request.removeAttribute(REGEX_ERROR_ATTRIBUTE);
        }
        else
        {
            request.setAttribute(REGEX_ERROR_ATTRIBUTE, errorFields);
        }
    }
}
