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
import java.util.Locale;
import java.util.UUID;

import javax.servlet.ServletException;

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Field;
import org.dspace.app.xmlui.wing.element.Instance;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Params;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;

import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * This is a step of the item submission processes. The describe step queries
 * the user for various metadata items about the item. For the most part all the
 * questions queried are completely configurable via the input-sets.xml file.
 * This system allows for multiple pages to be defined so this step is different
 * from all other stages in that it may represent multiple stages within the
 * submission processes.
 *
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class DescribeStep extends AbstractSubmissionStep
{
        /** Language Strings **/
    protected static final Message T_head =
        message("xmlui.Submission.submit.DescribeStep.head");
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
        
    /**
     * A shared resource of the inputs reader. The 'inputs' are the
     * questions we ask the user to describe an item during the
     * submission process. The reader is a utility class to read
     * that configuration file.
     */
    private static DCInputsReader INPUTS_READER = null;

    private static final Message T_vocabulary_link = message("xmlui.Submission.submit.DescribeStep.controlledvocabulary.link");

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
    protected MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();

    /**
     * Ensure that the inputs reader has been initialized, this method may be
     * called multiple times with no ill-effect.
     */
    private static void initializeInputsReader() throws DCInputsReaderException
    {
        if (INPUTS_READER == null)
        {
            INPUTS_READER = new DCInputsReader();
        }
    }
    
    /**
     * Return the inputs reader. Note, the reader must have been
     * initialized before the reader can be accessed.
     *
     * @return The input reader.
     */
    private static DCInputsReader getInputsReader()
    {
        return INPUTS_READER;
    }

    /**
     * Establish our required parameters.  AbstractStep will enforce these.
     * @throws javax.servlet.ServletException if the reader could not initialize.
     */
        public DescribeStep() throws ServletException
        {
                this.requireSubmission = true;
                this.requireStep = true;
                
                // Ensure that the InputsReader is initialized.
                try
                {
                    initializeInputsReader();
                }
                catch (DCInputsReaderException e)
                {
                    throw new ServletException(e);
                }
        }
        
    @Override
        public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
        {
            super.addPageMeta(pageMeta);
            UUID collectionID = submission.getCollection().getID();
            pageMeta.addMetadata("choice", "collection").addContent(String.valueOf(collectionID));
            pageMeta.addMetadata("stylesheet", "screen", "datatables", true).addContent("../../static/Datatables/DataTables-1.8.0/media/css/datatables.css");
            pageMeta.addMetadata("javascript", "static", "datatables", true).addContent("static/Datatables/DataTables-1.8.0/media/js/jquery.dataTables.min.js");
            pageMeta.addMetadata("stylesheet", "screen", "person-lookup", true).addContent("../../static/css/authority/person-lookup.css");
            pageMeta.addMetadata("javascript", null, "person-lookup", true).addContent("../../static/js/person-lookup.js");


            String jumpTo = submissionInfo.getJumpToField();
            if (jumpTo != null)
            {
                pageMeta.addMetadata("page", "jumpTo").addContent(jumpTo);
            }
        }

    @Override
        public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
        {
                // Obtain the inputs (i.e. metadata fields we are going to display)
                Item item = submission.getItem();
                Collection collection = submission.getCollection();
                String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

                DCInputSet inputSet;
                DCInput[] inputs;
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
                form.setHead(T_head);

                // Fetch the document type (dc.type)
                String documentType = "";
                if( (itemService.getMetadataByMetadataString(item, "dc.type") != null) && (itemService.getMetadataByMetadataString(item, "dc.type").size() >0) )
                {
                    documentType = itemService.getMetadataByMetadataString(item, "dc.type").get(0).getValue();
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
                        
                        String schema = dcInput.getSchema();
                        String element = dcInput.getElement();
                        String qualifier = dcInput.getQualifier();

                        java.util.List<MetadataValue> dcValues = itemService.getMetadata(item, schema, element, qualifier, Item.ANY);

                        String fieldName = FlowUtils.getFieldName(dcInput);
                        String inputType = dcInput.getInputType();

                        // if this field is configured as choice control and its
                        // presentation format is SELECT, render it as select field:
                        String fieldKey = metadataAuthorityService.makeFieldKey(schema, element, qualifier);
                        if (choiceAuthorityService.isChoicesConfigured(fieldKey) &&
                            Params.PRESENTATION_SELECT.equals(choiceAuthorityService.getPresentation(fieldKey)))
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
                                java.util.List<MetadataValue> unfiltered = itemService.getMetadata(item, dcInput.getSchema(), dcInput.getElement(), Item.ANY, Item.ANY);
                                ArrayList<MetadataValue> filtered = new ArrayList<MetadataValue>();
                                for (MetadataValue dcValue : unfiltered)
                                {
                                        String unfilteredFieldName = dcValue.getMetadataField().getElement() + "." + dcValue.getMetadataField().getQualifier();
                                        if ( ! inputSet.isFieldPresent(unfilteredFieldName, documentType) )
                                        {
                                                filtered.add( dcValue );
                                        }
                                }
                                
                                renderQualdropField(form, fieldName, dcInput, filtered, readonly);
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
                        else
                        {
                                form.addItem(T_unknown_field);
                        }
                }

                // add standard control/paging buttons
        addControlButtons(form);
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
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException on reader error.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        //Create a new list section for this step (and set its heading)
        List describeSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
        describeSection.setHead(T_head);
        
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
        
        DCInput[] inputs = inputSet.getPageRows(getPage()-1, submission.hasMultipleTitles(), submission.isPublishedBefore());

        for (DCInput input : inputs)
        {
            // If the input is invisible in this scope, then skip it.
            String scope = submissionInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
            if (!input.isVisible(scope) && !input.isReadOnly(scope))
            {
                continue;
            }

            String inputType = input.getInputType();
            String pairsName = input.getPairsType();
            java.util.List<MetadataValue> values;

            if (inputType.equals("qualdrop_value"))
            {
                values = itemService.getMetadata(submission.getItem(), input.getSchema(), input.getElement(), Item.ANY, Item.ANY);
            }
            else
            {
                values = itemService.getMetadata(submission.getItem(), input.getSchema(), input.getElement(), input.getQualifier(), Item.ANY);
            }

            if (values != null && values.size() > 0)
            {
                for (MetadataValue value : values)
                {
                    String displayValue = null;
                    if (inputType.equals("date"))
                    {
                        DCDate date = new DCDate(value.getValue());
                        displayValue = date.toString();
                    }
                    else if (inputType.equals("dropdown"))
                    {
                        displayValue = input.getDisplayString(pairsName,value.getValue());
                    }
                    else if (inputType.equals("qualdrop_value"))
                    {
                        String qualifier = value.getMetadataField().getQualifier();
                        String displayQual = input.getDisplayString(pairsName,qualifier);
                        if (displayQual!=null && displayQual.length()>0)
                        {
                            displayValue = displayQual + ":" + value.getValue();
                        }
                    }
                    else
                    {
                        displayValue = value.getValue();
                    }

                    // Only display this field if we have a value to display
                    if (displayValue!=null && displayValue.length()>0)
                    {

                        describeSection.addLabel(input.getLabel());
                        if (metadataAuthorityService.isAuthorityControlled(value.getMetadataField()))
                        {
                            String confidence = (value.getAuthority() != null && value.getAuthority().length() > 0) ?
                                Choices.getConfidenceText(value.getConfidence()).toLowerCase() :
                                "blank";
                            org.dspace.app.xmlui.wing.element.Item authItem =
                                describeSection.addItem("submit-review-field-with-authority", "ds-authority-confidence cf-"+confidence);
                            authItem.addContent(displayValue);
                        }
                        else
                        {
                            describeSection.addItem(displayValue);
                        }
                    }
                } // For each MetadataValue
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
        private void renderNameField(List form, String fieldName, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly)
                throws WingException
        {
                // The name field is a composite field containing two text fields, one
                // for first name the other for last name.
                Composite fullName = form.addItem().addComposite(fieldName, "submit-name");
                Text lastName = fullName.addText(fieldName+"_last");
                Text firstName = fullName.addText(fieldName+"_first");

                // Setup the full name
                fullName.setLabel(dcInput.getLabel());
                fullName.setHelp(cleanHints(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    fullName.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        fullName.addError(dcInput.getWarning());
                    }
                    else
                    {
                        fullName.addError(T_required_field);
                    }
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    fullName.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.size() > 1)  && !readonly)
                {
                    fullName.enableDeleteOperation();
                }
                String fieldKey = metadataAuthorityService.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
                boolean isAuthorityControlled = metadataAuthorityService.isAuthorityControlled(fieldKey);
                if (isAuthorityControlled)
                {
                    fullName.setAuthorityControlled();
                    fullName.setAuthorityRequired(metadataAuthorityService.isAuthorityRequired(fieldKey));
                }
                if (choiceAuthorityService.isChoicesConfigured(fieldKey))
                {
                    fullName.setChoices(fieldKey);
                    fullName.setChoicesPresentation(choiceAuthorityService.getPresentation(fieldKey));
                    fullName.setChoicesClosed(choiceAuthorityService.isClosed(fieldKey));
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
                
                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.size() > 1)
                {
                        for (MetadataValue dcValue : dcValues)
                        {
                                DCPersonName dpn = new DCPersonName(dcValue.getValue());
                
                                lastName.addInstance().setValue(dpn.getLastName());
                                firstName.addInstance().setValue(dpn.getFirstNames());
                                Instance fi = fullName.addInstance();
                                fi.setValue(dcValue.getValue());
                                if (isAuthorityControlled)
                                {
                                    if (dcValue.getAuthority() == null || dcValue.getAuthority().equals(""))
                                    {
                                        fi.setAuthorityValue("", "blank");
                                    }
                                    else
                                    {
                                        fi.setAuthorityValue(dcValue.getAuthority(), Choices.getConfidenceText(dcValue.getConfidence()));
                                    }
                        }
                }
                }
                else if (dcValues.size() == 1)
                {
                        DCPersonName dpn = new DCPersonName(dcValues.get(0).getValue());
                
                        lastName.setValue(dpn.getLastName());
                        firstName.setValue(dpn.getFirstNames());
                        if (isAuthorityControlled)
                        {
                            if (dcValues.get(0).getAuthority() == null || dcValues.get(0).getAuthority().equals(""))
                            {
                                lastName.setAuthorityValue("", "blank");
                            }
                            else
                            {
                                lastName.setAuthorityValue(dcValues.get(0).getAuthority(), Choices.getConfidenceText(dcValues.get(0).getConfidence()));
                            }
                }
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
        private void renderDateField(List form, String fieldName, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly) throws WingException
        {
                // The date field consists of three primitive fields: a text field
                // for the year, followed by a select box of the months, follewed
                // by a text box for the day.
                Composite fullDate = form.addItem().addComposite(fieldName, "submit-date");
                Text year = fullDate.addText(fieldName+"_year");
                Select month = fullDate.addSelect(fieldName+"_month");
                Text day = fullDate.addText(fieldName+"_day");

                // Set up the full field
                fullDate.setLabel(dcInput.getLabel());
                fullDate.setHelp(cleanHints(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    fullDate.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        fullDate.addError(dcInput.getWarning());
                    }
                    else
                    {
                        fullDate.addError(T_required_field);
                    }
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    fullDate.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.size() > 1) && !readonly)
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
                if (dcInput.isRepeatable() || dcValues.size() > 1)
                {
                        for (MetadataValue dcValue : dcValues)
                        {
                                DCDate dcDate = new DCDate(dcValue.getValue());

                                year.addInstance().setValue(String.valueOf(dcDate.getYear()));
                                month.addInstance().setOptionSelected(dcDate.getMonth());
                                day.addInstance().setValue(String.valueOf(dcDate.getDay()));
                                fullDate.addInstance().setValue(dcDate.toString());
                        }
                }
                else if (dcValues.size() == 1)
                {
                        DCDate dcDate = new DCDate(dcValues.get(0).getValue());

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
        private void renderSeriesField(List form, String fieldName, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly) throws WingException
        {
                // The series field consists of two parts, a series name (text field)
                // and report or paper number (also a text field).
                Composite fullSeries = form.addItem().addComposite(fieldName,"submit-"+dcInput.getInputType());
                Text series = fullSeries.addText(fieldName+"_series");
                Text number = fullSeries.addText(fieldName+"_number");

                // Setup the full field.
                fullSeries.setLabel(dcInput.getLabel());
                fullSeries.setHelp(cleanHints(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    fullSeries.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        fullSeries.addError(dcInput.getWarning());
                    }
                    else
                    {
                        fullSeries.addError(T_required_field);
                    }
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    fullSeries.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.size() > 1) && !readonly)
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
                if (dcInput.isRepeatable() || dcValues.size() > 1)
                {
                        for (MetadataValue dcValue : dcValues)
                        {
                                DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(dcValue.getValue());

                                series.addInstance().setValue(dcSeriesNumber.getSeries());
                                number.addInstance().setValue(dcSeriesNumber.getNumber());
                                fullSeries.addInstance().setValue(dcSeriesNumber.toString());
                        }
                        
                }
                else if (dcValues.size() == 1)
                {
                        DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(dcValues.get(0).getValue());

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
        private void renderQualdropField(List form, String fieldName, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly) throws WingException
        {
                Composite qualdrop = form.addItem().addComposite(fieldName,"submit-qualdrop");
                Select qual = qualdrop.addSelect(fieldName+"_qualifier");
                Text value = qualdrop.addText(fieldName+"_value");

                // Setup the full field.
                qualdrop.setLabel(dcInput.getLabel());
                qualdrop.setHelp(cleanHints(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    qualdrop.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        qualdrop.addError(dcInput.getWarning());
                    }
                    else
                    {
                        qualdrop.addError(T_required_field);
                    }
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    qualdrop.enableAddOperation();
                }
                // Update delete based upon the filtered values.
                if ((dcInput.isRepeatable() || dcValues.size() > 1) && !readonly)
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
                        String display = pairs.get(i);
                        String returnValue = pairs.get(i+1);
                        qual.addOption(returnValue,display);
                }

                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.size() > 1)
                {
                        for (MetadataValue dcValue : dcValues)
                        {
                                qual.addInstance().setOptionSelected(dcValue.getMetadataField().getQualifier());
                                value.addInstance().setValue(dcValue.getValue());
                                qualdrop.addInstance().setValue(dcValue.getMetadataField().getQualifier() + ":" + dcValue.getValue());
                        }
                }
                else if (dcValues.size() == 1)
                {
                        qual.setOptionSelected(dcValues.get(0).getMetadataField().getQualifier());
                        value.setValue(dcValues.get(0).getValue());
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
        private void renderTextArea(List form, String fieldName, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly) throws WingException
        {
                // Plain old Textarea
                TextArea textArea = form.addItem().addTextArea(fieldName,"submit-textarea");

                // Setup the text area
                textArea.setLabel(dcInput.getLabel());
                textArea.setHelp(cleanHints(dcInput.getHints()));
                String fieldKey = metadataAuthorityService.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
                boolean isAuth = metadataAuthorityService.isAuthorityControlled(fieldKey);
                if (isAuth)
                {
                    textArea.setAuthorityControlled();
                    textArea.setAuthorityRequired(metadataAuthorityService.isAuthorityRequired(fieldKey));
                }
                if (choiceAuthorityService.isChoicesConfigured(fieldKey))
                {
                    textArea.setChoices(fieldKey);
                    textArea.setChoicesPresentation(choiceAuthorityService.getPresentation(fieldKey));
                    textArea.setChoicesClosed(choiceAuthorityService.isClosed(fieldKey));
                }
                if (dcInput.isRequired())
                {
                    textArea.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        textArea.addError(dcInput.getWarning());
                    }
                    else
                    {
                        textArea.addError(T_required_field);
                    }
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    textArea.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.size() > 1) && !readonly)
                {
                    textArea.enableDeleteOperation();
                }

                if (readonly)
                {
                    textArea.setDisabled();
                }
                
                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.size() > 1)
                {
                        for (MetadataValue dcValue : dcValues)
                        {
                                Instance ti = textArea.addInstance();
                                ti.setValue(dcValue.getValue());
                                if (isAuth)
                                {
                                    if (dcValue.getAuthority() == null || dcValue.getAuthority().equals(""))
                                    {
                                        ti.setAuthorityValue("", "blank");
                                    }
                                    else
                                    {
                                        ti.setAuthorityValue(dcValue.getAuthority(), Choices.getConfidenceText(dcValue.getConfidence()));
                                    }
                        }
                }
                }
                else if (dcValues.size() == 1)
                {
                        textArea.setValue(dcValues.get(0).getValue());
                        if (isAuth)
                        {
                            if (dcValues.get(0).getAuthority() == null || dcValues.get(0).getAuthority().equals(""))
                            {
                                textArea.setAuthorityValue("", "blank");
                            }
                            else
                            {
                                textArea.setAuthorityValue(dcValues.get(0).getAuthority(), Choices.getConfidenceText(dcValues.get(0).getConfidence()));
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
        private void renderChoiceSelectField(List form, String fieldName, Collection coll, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly) throws WingException
        {
                String fieldKey = metadataAuthorityService.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
                if (metadataAuthorityService.isAuthorityControlled(fieldKey))
                {
                    throw new WingException("Field " + fieldKey + " has choice presentation of type \"" + Params.PRESENTATION_SELECT + "\", it may NOT be authority-controlled.");
                }

                // Plain old select list.
                Select select = form.addItem().addSelect(fieldName,"submit-select");

                //Setup the select field
                select.setLabel(dcInput.getLabel());
                select.setHelp(cleanHints(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    select.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        select.addError(dcInput.getWarning());
                    }
                    else
                    {
                        select.addError(T_required_field);
                    }
                }
                if (dcInput.isRepeatable() || dcValues.size() > 1)
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

                Choices cs = choiceAuthorityService.getMatches(fieldKey, "", coll, 0, 0, null);
                if (dcValues.size() == 0)
                {
                    select.addOption(true, "", "");
                }
                for (Choice c : cs.values)
                {
                    select.addOption(c.value, c.label);
                }

                // Setup the field's pre-selected values
                for (MetadataValue dcValue : dcValues)
                {
                        select.setOptionSelected(dcValue.getValue());
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
        private void renderDropdownField(List form, String fieldName, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly) throws WingException
        {
                // Plain old select list.
                Select select = form.addItem().addSelect(fieldName,"submit-select");

                //Setup the select field
                select.setLabel(dcInput.getLabel());
                select.setHelp(cleanHints(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    select.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        select.addError(dcInput.getWarning());
                    }
                    else
                    {
                        select.addError(T_required_field);
                    }
                }
                if (dcInput.isRepeatable() || dcValues.size() > 1)
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
                        String display = pairs.get(i);
                        String value   = pairs.get(i+1);
                        select.addOption(value,display);
                }
                
                // Setup the field's pre-selected values
                for (MetadataValue dcValue : dcValues)
                {
                        select.setOptionSelected(dcValue.getValue());
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
        private void renderSelectFromListField(List form, String fieldName, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly) throws WingException
        {
                Field listField = null;
                
                //if repeatable, this list of fields should be checkboxes
                if (dcInput.isRepeatable())
                {
                        listField = form.addItem().addCheckBox(fieldName);
                }
                else //otherwise this is a list of radio buttons
                {
                        listField = form.addItem().addRadio(fieldName);
                }
                
                if (readonly)
                {
                    listField.setDisabled();
                }
                
                //      Setup the field
                listField.setLabel(dcInput.getLabel());
                listField.setHelp(cleanHints(dcInput.getHints()));
                if (dcInput.isRequired())
                {
                    listField.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        listField.addError(dcInput.getWarning());
                    }
                    else
                    {
                        listField.addError(T_required_field);
                    }
                }

        
                //Setup each of the possible options
                java.util.List<String> pairs = dcInput.getPairs();
                for (int i = 0; i < pairs.size(); i += 2)
                {
                        String display = pairs.get(i);
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
                for (MetadataValue dcValue : dcValues)
                {
                        if(listField instanceof CheckBox)
                        {
                                ((CheckBox)listField).setOptionSelected(dcValue.getValue());
                        }
                        else if(listField instanceof Radio)
                        {
                                ((Radio)listField).setOptionSelected(dcValue.getValue());
                        }
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
        private void renderOneboxField(List form, String fieldName, DCInput dcInput, java.util.List<MetadataValue> dcValues, boolean readonly) throws WingException
        {
                // Both onebox and twobox consist a free form text field
                // that the user may enter any value. The difference between
                // the two is that a onebox should be rendered in one column
                // as twobox should be listed in a two column format. Since this
                // decision is not something the Aspect can effect we merely place
                // as a render hint.
            org.dspace.app.xmlui.wing.element.Item item = form.addItem();
            Text text = item.addText(fieldName, "submit-text");

            if(dcInput.getVocabulary() != null){
                String vocabularyUrl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.url");
                vocabularyUrl += "/JSON/controlled-vocabulary?vocabularyIdentifier=" + dcInput.getVocabulary();
                //Also hand down the field name so our summoning script knows the field the selected value is to end up in
                vocabularyUrl += "&metadataFieldName=" + fieldName;
                item.addXref("vocabulary:" + vocabularyUrl).addContent(T_vocabulary_link);
            }
            
                // Setup the select field
                text.setLabel(dcInput.getLabel());
                text.setHelp(cleanHints(dcInput.getHints()));
                String fieldKey = metadataAuthorityService.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
                boolean isAuth = metadataAuthorityService.isAuthorityControlled(fieldKey);
                if (isAuth)
                {
                    text.setAuthorityControlled();
                    text.setAuthorityRequired(metadataAuthorityService.isAuthorityRequired(fieldKey));
                }
                if (choiceAuthorityService.isChoicesConfigured(fieldKey))
                {
                    text.setChoices(fieldKey);
                    text.setChoicesPresentation(choiceAuthorityService.getPresentation(fieldKey));
                    text.setChoicesClosed(choiceAuthorityService.isClosed(fieldKey));
                }

                if (dcInput.isRequired())
                {
                    text.setRequired();
                }
                if (isFieldInError(fieldName))
                {
                    if (dcInput.getWarning() != null && dcInput.getWarning().length() > 0)
                    {
                        text.addError(dcInput.getWarning());
                    }
                    else
                    {
                        text.addError(T_required_field);
                    }
                }
                if (dcInput.isRepeatable() && !readonly)
                {
                    text.enableAddOperation();
                }
                if ((dcInput.isRepeatable() || dcValues.size() > 1) && !readonly)
                {
                    text.enableDeleteOperation();
                }

                if (readonly)
                {
                    text.setDisabled();
                }
                
                // Setup the field's values
                if (dcInput.isRepeatable() || dcValues.size() > 1)
                {
                        for (MetadataValue dcValue : dcValues)
                        {
                                Instance ti = text.addInstance();
                                ti.setValue(dcValue.getValue());
                                if (isAuth)
                                {
                                    if (dcValue.getAuthority() == null || dcValue.getAuthority().equals(""))
                                    {
                                        ti.setAuthorityValue("", "blank");
                                    }
                                    else
                                    {
                                        ti.setAuthorityValue(dcValue.getAuthority(), Choices.getConfidenceText(dcValue.getConfidence()));
                                    }
                        }
                    }
                }
                else if (dcValues.size() == 1)
                {
                        text.setValue(dcValues.get(0).getValue());
                        if (isAuth)
                        {
                            if (dcValues.get(0).getAuthority() == null || dcValues.get(0).getAuthority().equals(""))
                            {
                                text.setAuthorityValue("", "blank");
                            }
                            else
                            {
                                text.setAuthorityValue(dcValues.get(0).getAuthority(), Choices.getConfidenceText(dcValues.get(0).getConfidence()));
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
         * There is a problem with the way hints are handled. The class that we use to
         * read the input-forms.xml configuration will append and prepend HTML to hints.
         * This causes all sorts of confusion when inserting into the DRI page, so this
         * method will strip that extra HTML and just leave the cleaned comments.
         *
         *
         * However this method will not remove naughty or sexual innuendoes from the
         * field's hints.
         *
         *
         * @param dirtyHints HTML-ized hints
         * @return Hints without HTML.
         */
        private static final String HINT_HTML_PREFIX = "<tr><td colspan=\"4\" class=\"submitFormHelp\">";
        private static final String HINT_HTML_POSTFIX = "</td></tr>";
        private String cleanHints(String dirtyHints)
        {
                String clean = (dirtyHints!=null ? dirtyHints : "");

                if (clean.startsWith(HINT_HTML_PREFIX))
                {
                        clean = clean.substring(HINT_HTML_PREFIX.length());
                }

                if (clean.endsWith(HINT_HTML_POSTFIX))
                {
                        clean = clean.substring(0,clean.length() - HINT_HTML_POSTFIX.length());
                }

                return clean;
        }
}
