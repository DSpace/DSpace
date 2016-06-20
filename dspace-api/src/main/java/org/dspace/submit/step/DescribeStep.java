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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
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

    protected final ChoiceAuthorityService choiceAuthorityService;
    protected final MetadataAuthorityService metadataAuthorityService;


    /** Constructor */
    public DescribeStep() throws ServletException
    {
        //load the DCInputsReader
        getInputsReader();
        metadataAuthorityService = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
        choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
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
    @Override
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
        if( (itemService.getMetadataByMetadataString(item, "dc.type") != null) && (itemService.getMetadataByMetadataString(item, "dc.type").size() >0) )
        {
            documentType = itemService.getMetadataByMetadataString(item, "dc.type").get(0).getValue();
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
                    itemService.clearMetadata(context, item, inputs[i].getSchema(), inputs[i].getElement(), qualifier, Item.ANY);
		        }
	        }
	        else
	        {
		        String qualifier = inputs[i].getQualifier();
                itemService.clearMetadata(context, item, inputs[i].getSchema(), inputs[i].getElement(), qualifier, Item.ANY);
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

            String fieldKey = metadataAuthorityService.makeFieldKey(schema, element, qualifier);
            String inputType = inputs[j].getInputType();
            if (inputType.equals("name"))
            {
                readNames(context, request, item, schema, element, qualifier, inputs[j]
                        .getRepeatable());
            }
            else if (inputType.equals("date"))
            {
                readDate(context, request, item, schema, element, qualifier);
            }
            // choice-controlled input with "select" presentation type is
            // always rendered as a dropdown menu
            else if (inputType.equals("dropdown") || inputType.equals("list") ||
                     (choiceAuthorityService.isChoicesConfigured(fieldKey) &&
                      "select".equals(choiceAuthorityService.getPresentation(fieldKey))))
            {
                String[] vals = request.getParameterValues(fieldName);
                if (vals != null)
                {
                    for (int z = 0; z < vals.length; z++)
                    {
                        if (!vals[z].equals(""))
                        {
                            itemService.addMetadata(context, item, schema, element, qualifier, LANGUAGE_QUALIFIER,
                                    vals[z]);
                        }
                    }
                }
            }
            else if (inputType.equals("series"))
            {
                readSeriesNumbers(context, request, item, schema, element, qualifier,
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
                        itemService.addMetadata(context, item, schema, element, thisQual, null,
                                thisVal);
                    }
                }
            }
            else if ((inputType.equals("onebox"))
                    || (inputType.equals("twobox"))
                    || (inputType.equals("textarea")))
            {
                readText(context, request, item, schema, element, qualifier, inputs[j]
                        .getRepeatable(), LANGUAGE_QUALIFIER, inputs[j].getLanguage());
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
                List<MetadataValue> values = itemService.getMetadata(item, inputs[i].getSchema(),
                        inputs[i].getElement(), qualifier, Item.ANY);

                if ((inputs[i].isRequired() && values.size() == 0) &&
                     inputs[i].isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE))
                {
                    // since this field is missing add to list of error fields
                    addErrorField(request, getFieldName(inputs[i]));
                }
            }
        }

        // Step 4:
        // Save changes to database
        ContentServiceFactory.getInstance().getInProgressSubmissionService(subInfo.getSubmissionItem()).update(context, subInfo.getSubmissionItem());

        // commit changes
        context.dispatchEvents();

        // check for request for more input fields, first
        if (moreInput)
        {
            return STATUS_MORE_INPUT_REQUESTED;
        }
        // if one or more fields errored out, return
        else if (getErrorFields(request) != null && getErrorFields(request).size() > 0)
        {
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
    @Override
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
       language = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("default.language");
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
    protected void readNames(Context context, HttpServletRequest request, Item item,
            String schema, String element, String qualifier, boolean repeated) throws SQLException {
        String metadataField = metadataFieldService.findByElement(context, schema, element, qualifier).toString();

        String fieldKey = metadataAuthorityService.makeFieldKey(schema, element, qualifier);
        boolean isAuthorityControlled = metadataAuthorityService.isAuthorityControlled(fieldKey);

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
                    if (metadataAuthorityService.isAuthorityRequired(fieldKey) &&
                        (authKey == null || authKey.length() == 0))
                    {
                        log.warn("Skipping value of "+metadataField+" because the required Authority key is missing or empty.");
                        addErrorField(request, metadataField);
                    }
                    else
                    {
                        itemService.addMetadata(context, item, schema, element, qualifier, null,
                                new DCPersonName(l, f).toString(), authKey,
                                (sconf != null && sconf.length() > 0) ?
                                        Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED);
                    }
                }
                else
                {
                    itemService.addMetadata(context, item, schema, element, qualifier, null,
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
     * @param hasLanguageTag
     *            to check if the field has a language tag
     */
    protected void readText(Context context, HttpServletRequest request, Item item, String schema,
            String element, String qualifier, boolean repeated, String lang, boolean hasLanguageTag)
            throws SQLException
    {
        // FIXME: Of course, language should be part of form, or determined
        // some other way
        String metadataField = metadataFieldService.findByElement(context, schema, element, qualifier).toString();

        String fieldKey = metadataAuthorityService.makeFieldKey(schema, element, qualifier);
        boolean isAuthorityControlled = metadataAuthorityService.isAuthorityControlled(fieldKey);

        // Values to add
        TreeMap<Integer, String> vals = null;
        List<String> auths = null;
        List<String> confs = null;

        if (repeated)
        {
            vals = getRepeatedParameterWithTheirIndices(request, metadataField, metadataField);
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
                                
                //find the key from the given position 
                //for more information take a look at < Edit-metadata.jsp>
                int key = vals.keySet().toArray(new Integer[vals.size()])[valToRemove];
                vals.remove(key);
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
            vals = new TreeMap<Integer, String>();
            String value = request.getParameter(metadataField);
            if (value != null)
            {
                vals.put(0, value.trim());
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

        int i=0;

        // Put the names in the correct form
        for(Map.Entry<Integer, String> entry: vals.entrySet())
        {
            // Add to the database if non-empty
            
            String s = entry.getValue();
            int key = entry.getKey();
            if ((s != null) && !s.equals(""))
            {
                if (hasLanguageTag && !repeated && key == 0) 
                {
                    // the field is like dc_title[lang] for none repeatable element,
                    // dc_title_alternative_2[lang] otherwise
                    lang = request.getParameter(metadataField + "[lang]");

                } 
                else if (hasLanguageTag && repeated) 
                {
                    lang = request.getParameter(metadataField + "_" + key + "[lang]");
                }
                
                if (isAuthorityControlled)
                {
                    String authKey = auths.size() > i ? auths.get(i) : null;
                    String sconf = (authKey != null && confs.size() > i) ? confs.get(i) : null;
                    if (metadataAuthorityService.isAuthorityRequired(fieldKey) &&
                            (authKey == null || authKey.length() == 0))
                    {
                        log.warn("Skipping value of "+metadataField+" because the required Authority key is missing or empty.");
                        addErrorField(request, metadataField);
                    }
                    else
                    {
                        itemService.addMetadata(context, item, schema, element, qualifier, lang, s,
                                authKey, (sconf != null && sconf.length() > 0) ?
                                        Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED);
                    }
                }
                else
                {
                    itemService.addMetadata(context, item, schema, element, qualifier, lang, s);
                }
            }
            i++;
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
     * @throws SQLException if database error
     */
    protected void readDate(Context context, HttpServletRequest request, Item item, String schema,
            String element, String qualifier) throws SQLException
    {
        String metadataField = metadataFieldService.findByElement(context, schema, element, qualifier).toString();

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
            itemService.addMetadata(context, item, schema, element, qualifier, null, d.toString());
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
    protected void readSeriesNumbers(Context context, HttpServletRequest request, Item item,
            String schema, String element, String qualifier, boolean repeated) throws SQLException {
        String metadataField = metadataFieldService.findByElement(context, schema, element, qualifier).toString();

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
                itemService.addMetadata(context, item, schema, element, qualifier, null,
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
     * This Methode has the same function as the getRepeatedParameter.
     * For repeated values with language tag their indices are needed to
     * properly identify their language tag
     * 
     * @param request    
     *               the HTTP request containing the form information
     * @param metadataField    
     *               the metadata field which can store repeated values
     * @param param  
     *               the repeated parameter on the page (used to fill out the
     *               metadataField)  
     * @return     a TreeMap of Integer and Strings
    */ 
    protected TreeMap<Integer, String> getRepeatedParameterWithTheirIndices(HttpServletRequest request,
            String metadataField, String param)
    {
        LinkedHashMap<Integer, String> vals = new LinkedHashMap<Integer, String>();

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
                    vals.put(i, s.trim());
                }
            }

            i++;
        }

        log.debug("getRepeatedParameterWithTheirIndices: metadataField=" + metadataField
                + " param=" + metadataField + ", return count = "+vals.size());

        return new TreeMap(vals);
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
