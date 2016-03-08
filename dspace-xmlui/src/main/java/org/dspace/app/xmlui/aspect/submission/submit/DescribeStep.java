/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInput.ComplexDefinition;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Field;
import org.dspace.app.xmlui.wing.element.Hidden;
import org.dspace.app.xmlui.wing.element.Instance;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Params;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCPersonName;
import org.dspace.content.DCSeriesNumber;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.dspace.app.util.ACL;

/**
 * This is a step of the item submission processes. The describe step queries
 * the user for various metadata items about the item. For the most part all the
 * questions queried are completely configurable via the input-sets.xml file.
 * This system allows for multiple pages to be defined so this step is different
 * from all other stages in that it may represent multiple stages within the
 * submission processes.
 *
 * based on class by Scott Phillips and Tim Donohue (updated for Configurable Submission)
 * modified for LINDAT/CLARIN
 */
public class DescribeStep extends AbstractSubmissionStep
{
    public static final Message T_regexp_error = message("xmlui.Submission.submit.DescribeStep.regexp_error");
    private static Logger log = Logger.getLogger(DescribeStep.class);
        /** Language Strings **/
    protected static final Message T_head =
        message("xmlui.Submission.submit.DescribeStep.head");
    protected static final Message T_head2 =
        message("xmlui.Submission.submit.DescribeStep.head2");
    protected static final Message T_unknown_field=
        message("xmlui.Submission.submit.DescribeStep.unknown_field");
    protected static final Message T_required_field=
        message("xmlui.Submission.submit.DescribeStep.required_field");
    protected static final Message T_last_name_help=
        message("xmlui.Submission.submit.DescribeStep.last_name_help");
    protected static final Message T_first_name_help=
        message("xmlui.Submission.submit.DescribeStep.first_name_help");
    protected static final Message T_year=
        message("xmlui.Submission.submit.DescribeStep.year");
    protected static final Message T_month=
        message("xmlui.Submission.submit.DescribeStep.month");
    protected static final Message T_day=
        message("xmlui.Submission.submit.DescribeStep.day");
    protected static final Message T_series_name=
        message("xmlui.Submission.submit.DescribeStep.series_name");
    protected static final Message T_report_no=
        message("xmlui.Submission.submit.DescribeStep.report_no");
    protected static final Message T_missing_field =
            message("xmlui.Submission.submit.DescribeStep.missing_field");
        
        /**
     * A shared resource of the inputs reader. The 'inputs' are the
     * questions we ask the user to describe an item during the
     * submission process. The reader is a utility class to read
     * that configuration file.
     */
    protected static DCInputsReader INPUTS_READER = null;
    private static final Message T_vocabulary_link = message("xmlui.Submission.submit.DescribeStep.controlledvocabulary.link");
    private java.util.Map<String,String> regexError = null;

    /**
     * Ensure that the inputs reader has been initialized, this method may be
     * called multiple times with no ill-effect.
     */
    private static void initializeInputsReader( DCInputsReader inputs_reader ) throws DCInputsReaderException
    {
        if (INPUTS_READER == null)
        {
            if ( inputs_reader != null )
                INPUTS_READER = inputs_reader;
            else
            INPUTS_READER = new DCInputsReader();
        }
    }
    
    /**
     * Return the inputs reader. Note, the reader must have been
     * initialized before the reader can be accessed.
     *
     * @return The input reader.
     */
    protected static DCInputsReader getInputsReader()
    {
        return INPUTS_READER;
    }
    

        /**
         * Establish our required parameters, abstractStep will enforce these.
         */
        public DescribeStep() throws ServletException
        {
                this.requireSubmission = true;
                this.requireStep = true;
                
                // Ensure that the InputsReader is initialized.
                try
                {
                    initializeInputsReader( null );
                }
                catch (DCInputsReaderException e)
                {
                    throw new ServletException(e);
                }
        }
        public DescribeStep( DCInputsReader input ) throws ServletException
        {
                this.requireSubmission = true;
                this.requireStep = true;

                //Ensure that the InputsReader is initialized.
                try
                {
                    initializeInputsReader( input );
                }
                catch (DCInputsReaderException e)
                {
                    throw new ServletException(e);
                }
        }
        
        public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters)
        		throws ProcessingException, SAXException, IOException
        {
        	super.setup(resolver, objectModel, src, parameters);
			this.regexError = getRegexError(parameters);
        	
        }
        
        public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
        {
            super.addPageMeta(pageMeta);
            int collectionID = submission.getCollection().getID();
            pageMeta.addMetadata("choice", "collection").addContent(String.valueOf(collectionID));
            pageMeta.addMetadata("stylesheet", "screen", "datatables", true).addContent("../../static/Datatables/DataTables-1.8.0/media/css/datatables.css");
            pageMeta.addMetadata("javascript", "static", "datatables", true).addContent("static/Datatables/DataTables-1.8.0/media/js/jquery.dataTables.min.js");
            pageMeta.addMetadata("stylesheet", "screen", "person-lookup", true).addContent("../../static/css/authority/person-lookup.css");
            pageMeta.addMetadata("javascript", null, "person-lookup", true).addContent("../../static/js/person-lookup.js");
            pageMeta.addMetadata("include-library", "datepicker");
            pageMeta.addMetadata("include-library", "jquery-ui");
            pageMeta.addMetadata("include-library", "authority-control");


            String jumpTo = submissionInfo.getJumpToField();
            if (jumpTo != null)
            {
                pageMeta.addMetadata("page", "jumpTo").addContent(jumpTo);
            }
        }

        public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
        {
                // Obtain the inputs (i.e. metadata fields we are going to display)
                Item item = submission.getItem();
                Collection collection = submission.getCollection();
                String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

                DCInputSet inputSet;
                DCInput[] inputs;

                //set publishedbefore here after hiding initial question page
                submission.setPublishedBefore(true);
                submission.update();
                context.commit();

                try
                {
                        inputSet = getInputsReader().getInputs(submission.getCollection().getHandle());
                        inputs = inputSet.getPageRows(getPage()-1, submission.hasMultipleTitles(), submission.isPublishedBefore());
                }
                catch (DCInputsReaderException se)
                {
                        throw new UIException(se);
                }

                Division div = body.addInteractiveDivision("submit-describe",actionURL,Division.METHOD_POST,"primary submission");
                div.setHead(T_submission_head);
                addSubmissionProgressList(div);

                List form = div.addList("submit-describe",List.TYPE_FORM);
                if ( this.getPage() == 1 ) {
                form.setHead(T_head);
                }else {
                	form.setHead(message("xmlui.Submission.submit.DescribeStep.head"+this.getPage()));
                }

                // Fetch the document type (dc.type)
                String documentType = "";
                if( (item.getMetadataByMetadataString("dc.type") != null) && (item.getMetadataByMetadataString("dc.type").length >0) )
                {
                    documentType = item.getMetadataByMetadataString("dc.type")[0].value;
                }
                
                // Iterate over all inputs and add it to the form.
                for(DCInput dcInput : inputs)
                {
                    String scope = submissionInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
                    boolean readonly = dcInput.isReadOnly(scope);
                    
                	// Omit fields not allowed for this document type
                    if(!dcInput.isAllowedFor(documentType))
                    {
                    	continue;
                    }
                    
                    // If the input is invisible in this scope, then skip it.
                        if (!dcInput.isVisible(scope) && !readonly)
                        {
                            continue;
                        }
                        
                        //skip if acl says so
                        if(!isInputAuthorized(context,dcInput)){
                        	continue;
                        }
                        
                        //add some eye candy
                        if(dcInput.hasACL()) 
                        {
                            if(AuthorizeManager.isAdmin(context)) 
                            {
                                dcInput.addRend("admin-field");
                            }
                            else 
                            {
                                dcInput.addRend("specialuser-field");
                            }
                        }
                        
                        String schema = dcInput.getSchema();
                        String element = dcInput.getElement();
                        String qualifier = dcInput.getQualifier();

                        Metadatum[] dcValues = item.getMetadata(schema, element, qualifier, Item.ANY);

                        String fieldName = FlowUtils.getFieldName(dcInput);
                        String inputType = dcInput.getInputType();

                        // if this field is configured as choice control and its
                        // presentation format is SELECT, render it as select field:
                        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
                        ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
                        if (cmgr.isChoicesConfigured(fieldKey) &&
                            Params.PRESENTATION_SELECT.equals(cmgr.getPresentation(fieldKey)))
                        {
                                renderChoiceSelectField(form, fieldName, collection, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("name"))
                        {
                                renderNameField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("date"))
                        {
                                renderDateField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("FormattedDate"))
                        {
                                renderFormattedDateField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("series"))
                        {
                                renderSeriesField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("twobox"))
                        {
                                // We don't have a twobox field, instead it's just a
                                // one box field that the theme can render in two columns.
                                renderOneboxField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("qualdrop_value"))
                        {
                                // Determine the real field's values. Since the qualifier is
                                // selected we need to search through all the metadata and see
                                // if any match for another field, if not we assume that this field
                                // should handle it.
                                Metadatum[] unfiltered = item.getMetadata(dcInput.getSchema(), dcInput.getElement(), Item.ANY, Item.ANY);
                                ArrayList<Metadatum> filtered = new ArrayList<Metadatum>();
                                for (Metadatum dcValue : unfiltered)
                                {
                                        String unfilteredFieldName = dcValue.element + "." + dcValue.qualifier;
                                        if ( ! inputSet.isFieldPresent(unfilteredFieldName) )
                                        {
                                                filtered.add( dcValue );
                                        }
                                }
                                
                                renderQualdropField(form, fieldName, dcInput, filtered.toArray(new Metadatum[filtered.size()]), readonly);
                        }
                        else if (inputType.equals("textarea"))
                        {
                                renderTextArea(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("dropdown"))
                        {
                                renderDropdownField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("list"))
                        {
                                renderSelectFromListField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("onebox"))
                        {
                                renderOneboxField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else if (inputType.equals("complex")){
                        		renderComplexField(form, fieldName, dcInput, dcValues, readonly);
                        }
                        else
                        {
                                form.addItem(T_unknown_field);
                        }
                }

                // add standard control/paging buttons
        addControlButtons(form);
        }

    private void renderFormattedDateField(List form, String fieldName,
				DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException {
    		//onebox field expecting date in preset format eg. yyyy-mm-dd
    		renderOneboxField(form, fieldName, dcInput, dcValues, readonly);
		}

	/**
     * Each submission step must define its own information to be reviewed
     * during the final Review/Verify Step in the submission process.
     * <P>
     * The information to review should be tacked onto the passed in
     * List object.
     * <P>
     * NOTE: To remain consistent across all Steps, you should first
     * add a sub-List object (with this step's name as the heading),
     * by using a call to reviewList.addList().   This sublist is
     * the list you return from this method!
     *
     * @param reviewList
     *      The List to which all reviewable information should be added
     * @return
     *      The new sub-List object created by this step, which contains
     *      all the reviewable information.  If this step has nothing to
     *      review, then return null!
     */
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        //Create a new list section for this step (and set its heading)
        List describeSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
        String suffix = this.getPage() == 1 ? "" : Integer.toString(this.getPage());
        describeSection.setHead(message("xmlui.Submission.submit.DescribeStep.head" + suffix));
        
        //Review the values assigned to all inputs
        //on this page of the Describe step.
        DCInputSet inputSet = null;
        try
        {
            inputSet = getInputsReader().getInputs(submission.getCollection().getHandle());
        }
        catch (DCInputsReaderException se)
        {
            throw new UIException(se);
        }
        
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        DCInput[] inputs = inputSet.getPageRows(getPage()-1, submission.hasMultipleTitles(), submission.isPublishedBefore());

        // Fetch the document type (dc.type)
        Item item = submission.getItem();
        String documentType = "";
        if( (item.getMetadataByMetadataString("dc.type") != null) && (item.getMetadataByMetadataString("dc.type").length >0) )
        {
            documentType = item.getMetadataByMetadataString("dc.type")[0].value;
        }
        

        for (DCInput input : inputs)
        {
            // If the input is invisible in this scope, then skip it.
            String scope = submissionInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
            if (!input.isVisible(scope) && !input.isReadOnly(scope))
            {
                continue;
            }
	        if(!input.isAllowedFor(documentType))
			{
            	continue;
            }
	

            String inputType = input.getInputType();
            String pairsName = input.getPairsType();
            Metadatum[] values;

            if (inputType.equals("qualdrop_value"))
            {
                values = submission.getItem().getMetadata(input.getSchema(), input.getElement(), Item.ANY, Item.ANY);
            }
            else
            {
                values = submission.getItem().getMetadata(input.getSchema(), input.getElement(), input.getQualifier(), Item.ANY);
            }

            if (values != null && values.length > 0)
            {
                for (Metadatum value : values)
                {
                    String displayValue = null;
                    if (inputType.equals("date"))
                    {
                        DCDate date = new DCDate(value.value);
                        displayValue = date.toString();
                    }
                    else if (inputType.equals("dropdown"))
                    {
                        displayValue = input.getDisplayString(pairsName,value.value);
                    }
                    else if (inputType.equals("qualdrop_value"))
                    {
                        String qualifier = value.qualifier;
                        String displayQual = input.getDisplayString(pairsName,qualifier);
                        if (displayQual!=null && displayQual.length()>0)
                        {
                            displayValue = displayQual + ":" + value.value;
                        }
                    }
                    else
                    {
                        displayValue = value.value;
                    }

                    // Only display this field if we have a value to display
                    if (displayValue!=null && displayValue.length()>0)
                    {

                        describeSection.addLabel(message(input.getLabel()));
                        if (mam.isAuthorityControlled(value.schema, value.element, value.qualifier))
                        {
                            String confidence = (value.authority != null && value.authority.length() > 0) ?
                                Choices.getConfidenceText(value.confidence).toLowerCase() :
                                "blank";
                            org.dspace.app.xmlui.wing.element.Item authItem =
                                describeSection.addItem("submit-review-field-with-authority", "ds-authority-confidence cf-"+confidence);
                            authItem.addContent(message(displayValue));
                        }
                        else
                        {
                        	if(inputType.equals("complex")){
                        		displayValue = StringUtils.join(displayValue.split(DCInput.ComplexDefinition.SEPARATOR),";");
                        		/*describeSection.addItem();
                        		for(String fVal : fieldValues){
                        			describeSection.addItem().addHighlight("label label-default").addContent(fVal);
                        		}*/
                        	} //else{                        	
                            describeSection.addItem(message(displayValue));
                        	//}
                        }
                    }
                } // For each Metadatum
            } // If values exist
        } // For each input
        
        // return this new "describe" section
        return describeSection;
    }
    
        
        /**
         * Render a Name field to the DRI document. The name field consists of two
         * text fields, one for the last name and the other for a first name (plus
         * all other names).
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderNameField(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                // The name field is a composite field containing two text fields, one
                // for first name the other for last name.
        		
        		String rend = dcInput.getRendsAsString();
        		
        		org.dspace.app.xmlui.wing.element.Item item = form.addItem(null, rend);
        		
        		rend = "submit-name";
        		
        		Composite fullName = item.addComposite(fieldName, rend);        		
        	
	        	if(isAutocompletable(dcInput)) {
	            	rend += " autocomplete";
	            	addAutocompleteComponents(fieldName+"_last", dcInput, item);
	            }        
                               
                Text lastName = fullName.addText(fieldName+"_last", rend);
                Text firstName = fullName.addText(fieldName+"_first");

                String fieldKey = MetadataAuthorityManager.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
                boolean isAuthorityControlled = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);

                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.length > 1)
                {
                        for (Metadatum dcValue : dcValues)
                        {
                                DCPersonName dpn = new DCPersonName(dcValue.value);
                
                                lastName.addInstance().setValue(dpn.getLastName());
                                firstName.addInstance().setValue(dpn.getFirstNames());
                                Instance fi = fullName.addInstance();
                                fi.setValue(dcValue.value);
                                if (isAuthorityControlled)
                                {
                                    if (dcValue.authority == null || dcValue.authority.equals(""))
                                    {
                                        fi.setAuthorityValue("", "blank");
                                    }
                                    else
                                    {
                                        fi.setAuthorityValue(dcValue.authority, Choices.getConfidenceText(dcValue.confidence));
                                    }
                        }
                }
                }
                else if (dcValues.length == 1)
                {
                        DCPersonName dpn = new DCPersonName(dcValues[0].value);
                
                        lastName.setValue(dpn.getLastName());
                        firstName.setValue(dpn.getFirstNames());
                        if (isAuthorityControlled)
                        {
                            if (dcValues[0].authority == null || dcValues[0].authority.equals(""))
                            {
                                lastName.setAuthorityValue("", "blank");
                            }
                            else
                            {
                                lastName.setAuthorityValue(dcValues[0].authority, Choices.getConfidenceText(dcValues[0].confidence));
                            }
                }
        }
                // Setup the full name
                fullName.setLabel(message(dcInput.getLabel()));
                fullName.setHelp(message(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    fullName.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, fullName);
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    fullName.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.length > 1)  && !readonly)
                {
                    fullName.enableDeleteOperation();
                }
                if (isAuthorityControlled)
                {
                    fullName.setAuthorityControlled();
                    fullName.setAuthorityRequired(MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey));
                }
                if (ChoiceAuthorityManager.getManager().isChoicesConfigured(fieldKey))
                {
                    fullName.setChoices(fieldKey);
                    fullName.setChoicesPresentation(ChoiceAuthorityManager.getManager().getPresentation(fieldKey));
                    fullName.setChoicesClosed(ChoiceAuthorityManager.getManager().isClosed(fieldKey));
                }

                // Setup the first and last name
                lastName.setLabel(T_last_name_help);
                firstName.setLabel(T_first_name_help);
                
                if (readonly)
                {
                    lastName.setDisabled();
                    firstName.setDisabled();
                    fullName.setDisabled();
                }
                
        }
        
        /**
         * Render a date field to the DRI document. The date field consists of
         * three component fields, a 4 character text field for the year, a select
         * box for the month, and a 2 character text field for the day.
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderDateField(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                // The date field consists of three primitive fields: a text field
                // for the year, followed by a select box of the months, follewed
                // by a text box for the day.
            
                String rend = dcInput.getRendsAsString();
                rend += " submit-date";
            
                Composite fullDate = form.addItem().addComposite(fieldName, rend);
                Text year = fullDate.addText(fieldName+"_year");
                Select month = fullDate.addSelect(fieldName+"_month");
                Text day = fullDate.addText(fieldName+"_day");

                // Set up the full field
                fullDate.setLabel(message(dcInput.getLabel()));
                fullDate.setHelp(message(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    fullDate.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, fullDate);
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    fullDate.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
                {
                    fullDate.enableDeleteOperation();
                }

                if (readonly)
                {
                    year.setDisabled();
                    month.setDisabled();
                    day.setDisabled();
                }
                
                // Setup the year field
                year.setLabel(T_year);
                year.setSize(4,4);

                // Setup the month field
                month.setLabel(T_month);
                month.addOption(0,"");
                for (int i = 1; i < 13; i++)
                {
                        month.addOption(i,org.dspace.content.DCDate.getMonthName(i,Locale.getDefault()));
                }

                // Setup the day field
                day.setLabel(T_day);
                day.setSize(2,2);
                
                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.length > 1)
                {
                        for (Metadatum dcValue : dcValues)
                        {
                                DCDate dcDate = new DCDate(dcValue.value);

                                year.addInstance().setValue(String.valueOf(dcDate.getYear()));
                                month.addInstance().setOptionSelected(dcDate.getMonth());
                                day.addInstance().setValue(String.valueOf(dcDate.getDay()));
                                fullDate.addInstance().setValue(dcDate.toString());
                        }
                }
                else if (dcValues.length == 1)
                {
                        DCDate dcDate = new DCDate(dcValues[0].value);

                        year.setValue(String.valueOf(dcDate.getYear()));
                        month.setOptionSelected(dcDate.getMonth());
                        
                        // Check if the day field is not specified, if so then just
                        // put a blank value in instead of the weird looking -1.
                        if (dcDate.getDay() == -1)
                        {
                            day.setValue("");
                        }
                        else
                        {
                            day.setValue(String.valueOf(dcDate.getDay()));
                        }
                }
        }
        
        /**
         * Render a series field to the DRI document. The series field conists of
         * two component text fields. When interpreted each of these fields are
         * combined back together to be a single value joined together by a
         * semicolon. The primary use case is for the journal or report number
         * the left hand side is the journal and the right hand side in a
         * unique number within the journal.
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderSeriesField(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                String rend = dcInput.getRendsAsString();
                rend += " submit-"+dcInput.getInputType();
            
                // The series field consists of two parts, a series name (text field)
                // and report or paper number (also a text field).
                Composite fullSeries = form.addItem().addComposite(fieldName, rend);
                Text series = fullSeries.addText(fieldName+"_series");
                Text number = fullSeries.addText(fieldName+"_number");

                // Setup the full field.
                fullSeries.setLabel(message(dcInput.getLabel()));
                fullSeries.setHelp(message(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    fullSeries.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, fullSeries);
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    fullSeries.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
                {
                    fullSeries.enableDeleteOperation();
                }

                series.setLabel(T_series_name);
                number.setLabel(T_report_no);

                if (readonly)
                {
                    fullSeries.setDisabled();
                    series.setDisabled();
                    number.setDisabled();
                }
                
                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.length > 1)
                {
                        for (Metadatum dcValue : dcValues)
                        {
                                DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(dcValue.value);

                                series.addInstance().setValue(dcSeriesNumber.getSeries());
                                number.addInstance().setValue(dcSeriesNumber.getNumber());
                                fullSeries.addInstance().setValue(dcSeriesNumber.toString());
                        }
                        
                }
                else if (dcValues.length == 1)
                {
                        DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(dcValues[0].value);

                        series.setValue(dcSeriesNumber.getSeries());
                        number.setValue(dcSeriesNumber.getNumber());
                }
        }
        
        /**
         * Render a qualdrop field to the DRI document. Qualdrop fields are complicated,
         * widget wise they are composed of two fields, a select and text box field.
         * The select field selects the metadata's qualifier and the text box is the
         * value. This means that that there is not just one metadata element that is
         * represented so the confusing part is that the name can change.
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderQualdropField(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                String rend = dcInput.getRendsAsString();
                rend += " submit-qualdrop";

                Composite qualdrop = form.addItem().addComposite(fieldName, rend);
                Select qual = qualdrop.addSelect(fieldName+"_qualifier");
                Text value = qualdrop.addText(fieldName+"_value");

                // Setup the full field.
                qualdrop.setLabel(message(dcInput.getLabel()));
                qualdrop.setHelp(message(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    qualdrop.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, qualdrop);
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    qualdrop.enableAddOperation();
                }
                // Update delete based upon the filtered values.
                if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
                {
                    qualdrop.enableDeleteOperation();
                }
                
                if (readonly)
                {
                    qualdrop.setDisabled();
                    qual.setDisabled();
                    value.setDisabled();
                }

                // Setup the possible options
                @SuppressWarnings("unchecked") // This cast is correct
                java.util.List<String> pairs = dcInput.getPairs();
                for (int i = 0; i < pairs.size(); i += 2)
                {
                        Message display = message(pairs.get(i));
                        String returnValue = pairs.get(i+1);
                        qual.addOption(returnValue,display);
                }

                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.length > 1)
                {
                        for (Metadatum dcValue : dcValues)
                        {
                                qual.addInstance().setOptionSelected(dcValue.qualifier);
                                value.addInstance().setValue(dcValue.value);
                                qualdrop.addInstance().setValue(dcValue.qualifier + ":" + dcValue.value);
                        }
                }
                else if (dcValues.length == 1)
                {
                        qual.setOptionSelected(dcValues[0].qualifier);
                        value.setValue(dcValues[0].value);
                }
        }
        
        /**
         * Render a Text Area field to the DRI document. The text area is a simple
         * multi row and column text field.
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderTextArea(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                String rend = dcInput.getRendsAsString();
                rend += " submit-textarea submission-textarea";
            
                // Plain old Textarea
                TextArea textArea = form.addItem().addTextArea(fieldName, rend);

                // Setup the text area
                textArea.setLabel(message(dcInput.getLabel()));
                textArea.setHelp(message(dcInput.getHints()));
                String fieldKey = MetadataAuthorityManager.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
                boolean isAuth = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);
                if (isAuth)
                {
                    textArea.setAuthorityControlled();
                    textArea.setAuthorityRequired(MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey));
                }
                if (ChoiceAuthorityManager.getManager().isChoicesConfigured(fieldKey))
                {
                    textArea.setChoices(fieldKey);
                    textArea.setChoicesPresentation(ChoiceAuthorityManager.getManager().getPresentation(fieldKey));
                    textArea.setChoicesClosed(ChoiceAuthorityManager.getManager().isClosed(fieldKey));
                }
                if (dcInput.isRequired())
                {
                    textArea.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, textArea);
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    textArea.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
                {
                    textArea.enableDeleteOperation();
                }

                if (readonly)
                {
                    textArea.setDisabled();
                }
                
                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.length > 1)
                {
                        for (Metadatum dcValue : dcValues)
                        {
                                Instance ti = textArea.addInstance();
                                ti.setValue(dcValue.value);
                                if (isAuth)
                                {
                                    if (dcValue.authority == null || dcValue.authority.equals(""))
                                    {
                                        ti.setAuthorityValue("", "blank");
                                    }
                                    else
                                    {
                                        ti.setAuthorityValue(dcValue.authority, Choices.getConfidenceText(dcValue.confidence));
                                    }
                        }
                }
                }
                else if (dcValues.length == 1)
                {
                        textArea.setValue(dcValues[0].value);
                        if (isAuth)
                        {
                            if (dcValues[0].authority == null || dcValues[0].authority.equals(""))
                            {
                                textArea.setAuthorityValue("", "blank");
                            }
                            else
                            {
                                textArea.setAuthorityValue(dcValues[0].authority, Choices.getConfidenceText(dcValues[0].confidence));
                            }
                }
        }
        }
        
        /**
         * Render a dropdown field for a choice-controlled input of the
         * 'select' presentation to the DRI document. The dropdown field
         * consists of an HTML select box.
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderChoiceSelectField(List form, String fieldName, Collection coll, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                String fieldKey = MetadataAuthorityManager.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
                if (MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey))
                {
                    throw new WingException("Field " + fieldKey + " has choice presentation of type \"" + Params.PRESENTATION_SELECT + "\", it may NOT be authority-controlled.");
                }

                String rend = dcInput.getRendsAsString();
                rend += " submit-select";

                // Plain old select list.
                Select select = form.addItem().addSelect(fieldName, rend);

                //Setup the select field
                select.setLabel(message(dcInput.getLabel()));
                select.setHelp(message(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    select.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, select);
                }
                if (dcInput.isRepeatable() || dcValues.length > 1)
                {
                        // Use the multiple functionality from the HTML
                        // widget instead of DRI's version.
                        select.setMultiple();
                        select.setSize(6);
                }
                else
                {
                    select.setSize(1);
                }

                if (readonly)
                {
                    select.setDisabled();
                }

                Choices cs = ChoiceAuthorityManager.getManager().getMatches(fieldKey, "", coll.getID(), 0, 0, null);
                if (dcValues.length == 0)
                {
                    select.addOption(true, "", "");
                }
                for (Choice c : cs.values)
                {
                    select.addOption(c.value, c.label);
                }

                // Setup the field's pre-selected values
                for (Metadatum dcValue : dcValues)
                {
                        select.setOptionSelected(dcValue.value);
                }
        }

        /**
         * Render a dropdown field to the DRI document. The dropdown field consists
         * of an HTML select box.
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderDropdownField(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                String rend = dcInput.getRendsAsString();
                rend += " submit-select";
                
                // Plain old select list.
                Select select = form.addItem().addSelect(fieldName, rend);

                //Setup the select field
                select.setLabel(message(dcInput.getLabel()));
                select.setHelp(message(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    select.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, select);
                }
                if (dcInput.isRepeatable() || dcValues.length > 1)
                {
                        // Use the multiple functionality from the HTML
                        // widget instead of DRI's version.
                        select.setMultiple();
                        select.setSize(6);
                }
                
                if (readonly)
                {
                    select.setDisabled();
                }

                // Setup the possible options
                @SuppressWarnings("unchecked") // This cast is correct
                java.util.List<String> pairs = dcInput.getPairs();
                for (int i = 0; i < pairs.size(); i += 2)
                {
                        Message display = message(pairs.get(i));
                        String value   = pairs.get(i+1);
                        select.addOption(value,display);
                }
                
                // Setup the field's pre-selected values
                for (Metadatum dcValue : dcValues)
                {
                    log.info( "dc values are present for "+fieldName+" with "+dcValue.value );	
                    // jmisutka/UFAL - special hack for iso languages
                    // add the option there artificially
                    if ( fieldName.equals("dc_language_iso" ) ) {
                      select.addOption( dcValue.value, dcValue.value );
                    }
                        select.setOptionSelected(dcValue.value);
                }
        }
        
        /**
         * Render a select-from-list field to the DRI document.
         * This field consists of either a series of checkboxes
         * (if repeatable) or a series of radio buttons (if not repeatable).
         * <P>
         * Note: This is NOT the same as a List element
         * (org.dspace.app.xmlui.wing.element.List).  It's just unfortunately
         * similarly named.
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderSelectFromListField(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                Field listField = null;
                
                String rend = dcInput.getRendsAsString();
                
                //if repeatable, this list of fields should be checkboxes
                if (dcInput.isRepeatable())
                {
                        listField = form.addItem(null, rend).addCheckBox(fieldName);
                }
                else //otherwise this is a list of radio buttons
                {
                        listField = form.addItem(null, rend).addRadio(fieldName);
                }
                
                if (readonly)
                {
                    listField.setDisabled();
                }
                
                //      Setup the field
                listField.setLabel(message(dcInput.getLabel()));
                listField.setHelp(message(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    listField.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, listField);
                }

        
                //Setup each of the possible options
                java.util.List<String> pairs = dcInput.getPairs();
                for (int i = 0; i < pairs.size(); i += 2)
                {
                        Message display = message(pairs.get(i));
                        String value   = pairs.get(i+1);
                        
                        if(listField instanceof CheckBox)
                        {
                                ((CheckBox)listField).addOption(value, display);
                        }
                        else if(listField instanceof Radio)
                        {
                                ((Radio)listField).addOption(value, display);
                        }
                }
                
                // Setup the field's pre-selected values
                for (Metadatum dcValue : dcValues)
                {
                        if(listField instanceof CheckBox)
                        {
                                ((CheckBox)listField).setOptionSelected(dcValue.value);
                        }
                        else if(listField instanceof Radio)
                        {
                                ((Radio)listField).setOptionSelected(dcValue.value);
                        }
                }
        }
        
        protected void renderComplexField(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {

            String rend = dcInput.getRendsAsString();

            org.dspace.app.xmlui.wing.element.Item item = form.addItem(null, rend + " list-complex-field");

            rend += "submit-complex";

            Composite composite = item.addComposite(fieldName, rend);

            DCInput.ComplexDefinition definition = dcInput.getComplexDefinition();
            Map<String, Field> fields = new HashMap<>();
            for (String name : definition.getInputNames()) {
                String fullInputName = fieldName + "_" + name;
                Field field = null;
                Map<String, String> input_map = definition.getInput(name);
                String type = input_map.get("type");
                String rend_field = "";
                if (null != input_map.get("class")) {
                    rend_field += " " + input_map.get("class");
                }

                if ("text".equals(type)) {
                        String autocomplete = input_map.get("autocomplete");
                        if(isAutocompletable(autocomplete)) {
                            field = composite.addText(fullInputName, rend_field + " autocomplete");
                            addAutocompleteComponents(fullInputName, autocomplete, item);
                        } else {
                            field = composite.addText(fullInputName, rend_field);
                        }
                } else if ("dropdown".equals(type)){
                        Select select = composite.addSelect(fullInputName, rend_field);
                        // Setup the possible options
                        java.util.List<String> pairs = definition.getValuePairsForInput(name);
                        for (int i = 0; i < pairs.size(); i += 2)
                        {
                            Message display = message(pairs.get(i));
                            String value   = pairs.get(i+1);
                            select.addOption(value,display);
                        }
                        field = select;
                } else {
                        // nothing yet;
                }

                if (null == field) {
                    continue;
                }
                String label = input_map.get("label");
                if(label != null){
                    field.setLabel(message(label));
                }
                String help = input_map.get("help");
                if(help != null){
                    field.setHelp(message(help));
                }
                String placeholder = input_map.get("placeholder");
                if(placeholder != null){
                    //This is translated in themes/UFAL/lib/xsl/core/forms.xsl:~1089
                    field.setPlaceholder(placeholder);
                }
                String field_readonly = input_map.get("readonly");
                if(field_readonly != null && field_readonly.equals("true")){
                    field.setDisabled();
                }
                String value = input_map.get("value");
                if(value != null){
                    field.setValue(value);
                }
                fields.put(name, field);
            }

            // Setup the field's values
            // The values must be added to the right fields, watch out for the fields order!
            for (Metadatum dcValue : dcValues) {
                    java.util.List<String> values = split(dcValue.value);
                    fillExistingValues(definition, fields, values);
                    composite.addInstance().setValue(StringUtils.join(values.iterator(), ";"));
            }

             if(regexError.containsKey(fieldName)) {
                 // partially fill the form
                 java.util.List<String> values = split(regexError.get(fieldName));
                 fillPartialError(definition, fields, values);
            }

            // Setup the full name
            composite.setLabel(message(dcInput.getLabel()));
            composite.setHelp(message(dcInput.getHints()));
            if (dcInput.isRequired()) {
                    composite.setRequired();
            }
            if (isFieldInError(fieldName) || regexError.containsKey(fieldName)) {
                setFieldWarning(dcInput, composite, true, T_missing_field);
            }
            if (dcInput.isRepeatable() && !readonly) {
                    composite.enableAddOperation();
            }
            if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly) {
                    composite.enableDeleteOperation();
            }

            if (readonly) {
                    composite.setDisabled();
                    for (Field field : fields.values()) {
                            field.setDisabled();
                    }
            }

        }

        private void fillExistingValues(ComplexDefinition definition, Map<String, Field> fields, java.util.List<String> values) throws WingException {
            //fill the form
            int i = 0;
            for (String name : definition.getSortedInputNames()) {
                Field field = fields.get(name);
                try {
                    String value = values.get(i);
                    Instance instance = field.addInstance();
                    //XXX: Branching on type
                    if (field instanceof Select) {
                        instance.setOptionSelected(value);
                    } else {
                        instance.setValue(value);
                    }
                    ++i;
                } catch (IndexOutOfBoundsException e){
                    //someone possibly added a new field to this complex input
                    //we might be starting a new version
                    break;
                }
            }
        }

        private void fillPartialError(ComplexDefinition definition,
                                      Map<String, Field> fields,
                                      java.util.List<String> values) throws WingException {
            int i = 0;
            for (String input_name : definition.getSortedInputNames()) {
                try{
                    // input value
                    String value = values.get(i);
                    // field specified by input name (could be displayed differently than stored)
                    Field field = fields.get(input_name);
                    field.setValue(value);
                    // any regexp error?
                    String regex = definition.getInput(input_name).get("regexp");
                    if(!DCInput.isAllowedValue(value, regex)){
                        //attach the regex error to the right field
                        field.addError(T_regexp_error.parameterize(regex));
                    }
                } catch (IndexOutOfBoundsException e){
                    break;
                }
                ++i;
            }
        }

		private java.util.List<String> split(String value) {
        	//
        	return Arrays.asList(value.split(DCInput.ComplexDefinition.SEPARATOR, -1));
		}

        protected boolean isAutocompletable(DCInput dcInput){
        	 // autocomplete
            String autocomplete = dcInput.getAutocomplete();
            return isAutocompletable(autocomplete);
        }
		protected boolean isAutocompletable(String autocomplete) {
            if ( null != autocomplete ) 
            {
                //ConfigurationService cs = new DSpace().getConfigurationService();
                //String is_on = cs.getProperty("lr.autocomplete.on");
            	
            	String is_on = ConfigurationManager.getProperty("lr", "lr.autocomplete.on");
            	
                // UI will have an indication that autocomplete is turned on by
                // a) using the class autocomplete in the main text box
                // b) the url will be in a hidden attribute which will be marked by the fieldName
                // c) the type will be also set in a hidden element
                if ( null != is_on && is_on.trim().equals("true") ) 
                {                    
                	return true;
                }
            }
            return false;
        }
        
		protected void addAutocompleteComponents(String fieldName, DCInput dcInput, org.dspace.app.xmlui.wing.element.Item item) throws WingException {
        	String autocomplete = dcInput.getAutocomplete();
        	addAutocompleteComponents(fieldName, autocomplete, item);
		}
		
        protected void addAutocompleteComponents(String fieldName, String autocomplete, org.dspace.app.xmlui.wing.element.Item item) throws WingException {
        	String[] parts = autocomplete.split("-");
            String url_property = String.format("lr.autocomplete.%s.url", parts[0]);
            //String auto_url = cs.getProperty(url_property);
            String auto_url = ConfigurationManager.getProperty("lr", url_property);
            if ( auto_url != null ) {                                       
	        	item.addHidden(fieldName+"-url").setValue(auto_url);
	            item.addHidden(fieldName+"-type").setValue(autocomplete);
            }
            else {
            	log.warn(String.format(
                        "autocomplete has been specified but cannot find the actual url in configuration [%s]",url_property));
            }
        }
        
        /**
         * Render a simple text field to the DRI document
         *
         * @param form
         *                      The form list to add the field to
         * @param fieldName
         *                      The field's name.
         * @param dcInput
         *                      The field's input definition
         * @param dcValues
         *                      The field's pre-existing values.
         */
        protected void renderOneboxField(List form, String fieldName, DCInput dcInput, Metadatum[] dcValues, boolean readonly) throws WingException
        {
                // Both onebox and twobox consist a free form text field
                // that the user may enter any value. The difference between
                // the two is that a onebox should be rendered in one column
                // as twobox should be listed in a two column format. Since this
                // decision is not something the Aspect can effect we merely place
                // as a render hint.
            
            String rend = dcInput.getRendsAsString();
            
            org.dspace.app.xmlui.wing.element.Item item = form.addItem(null, rend);
            rend = "submit-text";

            if(isAutocompletable(dcInput)) {
            	rend += " autocomplete";
            	addAutocompleteComponents(fieldName, dcInput, item);
            }

            if (null != dcInput.getExtraClass()) {
                rend += " " + dcInput.getExtraClass();
            }
            
            Text text = item.addText(fieldName, rend);                                    

            if(dcInput.getVocabulary() != null){
                String vocabularyUrl = new DSpace().getConfigurationService().getProperty("dspace.url");
                vocabularyUrl += "/JSON/controlled-vocabulary?vocabularyIdentifier=" + dcInput.getVocabulary();
                //Also hand down the field name so our summoning script knows the field the selected value is to end up in
                vocabularyUrl += "&metadataFieldName=" + fieldName;
                item.addXref("vocabulary:" + vocabularyUrl).addContent(T_vocabulary_link);
            }
            
                // Setup the select field
                text.setLabel(message(dcInput.getLabel()));
                text.setHelp(message(dcInput.getHints()));
                String fieldKey = MetadataAuthorityManager.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
                boolean isAuth = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);
                if (isAuth)
                {
                    text.setAuthorityControlled();
                    text.setAuthorityRequired(MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey));
                }
                if (ChoiceAuthorityManager.getManager().isChoicesConfigured(fieldKey))
                {
                    text.setChoices(fieldKey);
                    text.setChoicesPresentation(ChoiceAuthorityManager.getManager().getPresentation(fieldKey));
                    text.setChoicesClosed(ChoiceAuthorityManager.getManager().isClosed(fieldKey));
                }

                if (dcInput.isRequired())
                {
                    text.setRequired();
                }

                boolean regexp_warning_issued = false;
                // regexp checking - should rather be in dspace-api
                for(Metadatum dcv : dcValues)
                {
                    if( !dcInput.isAllowedValue(dcv.value) ) {
                        regexp_warning_issued = true;
                        if (dcInput.getRegexpWarning() != null && dcInput.getRegexpWarning().length() > 0) {
                            text.addError(message(dcInput.getRegexpWarning()));
                        }else {
                            text.addError(T_regexp_error.parameterize(dcInput.getRegexp()) );
                        }
                    }
                }

                if (isFieldInError(fieldName))
                {
                    setFieldWarning(dcInput, text, !regexp_warning_issued || dcInput.isRequired(), T_required_field);
                }


                if (dcInput.isRepeatable() && !readonly)
                {
                    text.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
                {
                    text.enableDeleteOperation();
                }

                if (readonly)
                {
                    text.setDisabled();
                }else {
                    if (null != dcInput.getPlaceholder())
                    {
                        text.setPlaceholder(dcInput.getPlaceholder());
                    }
                }
                
                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.length > 1)
                {
                        for (Metadatum dcValue : dcValues)
                        {
                                Instance ti = text.addInstance();
                                ti.setValue(dcValue.value);
                                if (isAuth)
                                {
                                    if (dcValue.authority == null || dcValue.authority.equals(""))
                                    {
                                        ti.setAuthorityValue("", "blank");
                                    }
                                    else
                                    {
                                        ti.setAuthorityValue(dcValue.authority, Choices.getConfidenceText(dcValue.confidence));
                                    }
                        }
                    }
                }
                else if (dcValues.length == 1)
                {
                        text.setValue(dcValues[0].value);
                        if (isAuth)
                        {
                            if (dcValues[0].authority == null || dcValues[0].authority.equals(""))
                            {
                                text.setAuthorityValue("", "blank");
                            }
                            else
                            {
                                text.setAuthorityValue(dcValues[0].authority, Choices.getConfidenceText(dcValues[0].confidence));
                            }
                }
            }
        }



        /**
         * Check if the given fieldname is listed as being in error.
         *
         * @param fieldName
         * @return
         */
        private boolean isFieldInError(String fieldName)
        {
            return (this.errorFields.contains(fieldName));
        }
        
        /**
         * Adds hidden field with the name of the element to jump to using js after page reload
         * 
         * @param div
         * @throws WingException
         */
        protected void addJumpToInput(Division div) throws WingException
        {
            String name = submissionInfo.getJumpToField();        
            Hidden jumpTo = div.addHidden("jump_to");
            jumpTo.setValue(name);        
        }
        
        /**
         * should we render the metadata field based on field description?
         */
        protected boolean isInputDisplayable(Context c, DCInput dcInput, String scope, String documentType)
        {
            // Omit fields not allowed for this document type
            if(!dcInput.isAllowedFor(documentType)) {
                return false;
            }
             
            // If the input is invisible in this scope, then skip it.
            if (!dcInput.isVisible(scope) && !dcInput.isReadOnly(scope)) {
                return false;
            }
                    
            return true;
        }
        
        /**
         * should we render the metadata field based on authorization?
         */
        protected boolean isInputAuthorized(Context c, DCInput dcInput)
        {       
            
            // If the input is not allowed according to ACL, skip it.
            if(!dcInput.isAllowedAction(context, ACL.ACTION_READ) && !dcInput.isAllowedAction(context, ACL.ACTION_WRITE)) {
                return false;
            }
            
            return true;
        }
        
    	public Map<String, String> getRegexError(Parameters parameters)
    	{
    		java.util.Map<String,String> fields = new HashMap<String,String>();
    		
    		String errors = parameters.getParameter("regex_error","");
    		
    		if (errors!=null && errors.length() > 0)
    		{	
    			String[] fvs = errors.split(",");
    			for(int i=0; i<fvs.length; i+=2){
    				String field = fvs[i];
    				//XXX this is obscure, see org.dspace.submit.step.DescribeStep::addRegexError
    				String value = new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(fvs[i+1]));
    				fields.put(field, value);
    			}
    		}
    		
    		return fields;
    	}

        private void setFieldWarning(DCInput dcInput, Field field) throws WingException {
            setFieldWarning(dcInput, field, true, T_required_field);
        }

        private void setFieldWarning(DCInput dcInput, Field field, boolean useDefault, Message defaultMessage) throws WingException {
            if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
            {
                field.addError(message(dcInput.getWarning()));
            }
            else if (useDefault)
            {
                field.addError(defaultMessage);
            }
        }
}
