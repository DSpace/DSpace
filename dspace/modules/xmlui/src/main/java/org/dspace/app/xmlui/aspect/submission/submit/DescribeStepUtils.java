package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.log4j.Logger;
import org.dspace.app.util.*;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.content.*;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.Choice;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.workflow.DryadWorkflowUtils;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;
import java.io.UnsupportedEncodingException;
import org.dspace.content.Collection;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 29-jan-2010
 * Time: 16:30:19
 * Method with some methods in it that are utelized by the describe dataset & the describe publications
 */
public class DescribeStepUtils extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(DescribeStepUtils.class);

    protected static final Message T_unknown_field = message("xmlui.Submission.submit.DescribeStep.unknown_field");
    protected static final Message T_required_field = message("xmlui.Submission.submit.DescribeStep.required_field");
    protected static final Message T_last_name_help = message("xmlui.Submission.submit.DescribeStep.last_name_help");
    protected static final Message T_first_name_help = message("xmlui.Submission.submit.DescribeStep.first_name_help");
    protected static final Message T_year = message("xmlui.Submission.submit.DescribeStep.year");
    protected static final Message T_month = message("xmlui.Submission.submit.DescribeStep.month");
    protected static final Message T_day = message("xmlui.Submission.submit.DescribeStep.day");
    protected static final Message T_series_name = message("xmlui.Submission.submit.DescribeStep.series_name");
    protected static final Message T_report_no = message("xmlui.Submission.submit.DescribeStep.report_no");
    protected static final Message T_YEAR = message("xmlui.Submission.submit.DescribeStep.year");
    private static java.util.List<String> errorFields;


    /**
     * A shared resource of the inputs reader. The 'inputs' are the
     * questions we ask the user to describe an item during the
     * submission process. The reader is a utility class to read
     * that configuration file.
     */
    private static DCInputsReader INPUTS_READER = null;

    private static final String FULLNAME = "fullname";
    private static final String METADATADIR = "metadataDir";
    private static final String INTEGRATED = "integrated";
    private static final String PUBLICATION_BLACKOUT = "publicationBlackout";
    private static final String NOTIFY_ON_REVIEW = "notifyOnReview";
    private static final String NOTIFY_ON_ARCHIVE = "notifyOnArchive";


    public static final java.util.Map<String, Map<String, String>> journalProperties = new HashMap<String, Map<String, String>>();
    static{
        String journalPropFile = ConfigurationManager.getProperty("submit.journal.config");
        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(new FileInputStream(journalPropFile), "UTF-8"));
            String journalTypes = properties.getProperty("journal.order");

            for (int i = 0; i < journalTypes.split(",").length; i++) {
                String journalType = journalTypes.split(",")[i].trim();

                String str = "journal." + journalType + ".";

                Map<String, String> map = new HashMap<String, String>();
                map.put(FULLNAME, properties.getProperty(str + FULLNAME));
                map.put(METADATADIR, properties.getProperty(str + METADATADIR));
                map.put(INTEGRATED, properties.getProperty(str + INTEGRATED));
                map.put(PUBLICATION_BLACKOUT, properties.getProperty(str + PUBLICATION_BLACKOUT, "false"));
                map.put(NOTIFY_ON_REVIEW, properties.getProperty(str + NOTIFY_ON_REVIEW));
                map.put(NOTIFY_ON_ARCHIVE, properties.getProperty(str + NOTIFY_ON_ARCHIVE));

                String key = properties.getProperty(str + FULLNAME);
                journalProperties.put(key, map);
            }

        }catch (IOException e) {
            log.error("Error while loading journal properties", e);
        }
    }





    /**
     * Return the inputs reader. Note, the reader must have been
     * initialized before the reader can be accessed.
     *
     * @return The input reader.
     */
    private static DCInputsReader getInputsReader() throws DCInputsReaderException {
        if (INPUTS_READER == null)
            INPUTS_READER = new DCInputsReader();
        return INPUTS_READER;
    }




    public static java.util.List<String> renderFormList(Context c, String contextPath, List form, int page, java.util.List<String> errorFieldsList, SubmissionInfo submissionInfo, org.dspace.content.Item item, Collection collection) throws WingException, SQLException {
        // Iterate over all inputs and add it to the form.
        errorFields = errorFieldsList;

        InProgressSubmission submission = submissionInfo.getSubmissionItem();
        DCInputSet inputSet;
        DCInput[] inputs;
        try {
            inputSet = getInputsReader().getInputs(submission.getCollection().getHandle());
            inputs = inputSet.getPageRows(page - 1, submission.hasMultipleTitles(), submission.isPublishedBefore());
        }
        catch (Exception se) {
            throw new UIException(se);
        }

        boolean showEmbargoField = true;
        try{
            org.dspace.content.Item dataPackage = DryadWorkflowUtils.getDataPackage(c, submission.getItem());
            if(dataPackage != null){
                DCValue[] showEmbargoVals = dataPackage.getMetadata("internal", "submit", "showEmbargo", org.dspace.content.Item.ANY);
                if(0 < showEmbargoVals.length)
                    showEmbargoField = Boolean.valueOf(showEmbargoVals[0].value);
            }
        } catch (Exception e){
            log.error("Error while retrieving the showEmbargoField boolean", e);
        }


        for (DCInput dcInput : inputs) {
            String scope = submissionInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
            boolean readonly = dcInput.isReadOnly(scope);

            // If the input is invisible in this scope, then skip it.
            if (!dcInput.isVisible(scope) && !readonly)
                continue;

            String schema = dcInput.getSchema();
            String element = dcInput.getElement();
            String qualifier = dcInput.getQualifier();

            DCValue[] dcValues = item.getMetadata(schema, element, qualifier, org.dspace.content.Item.ANY);

            String fieldName = org.dspace.app.xmlui.aspect.submission.FlowUtils.getFieldName(dcInput);
            String inputType = dcInput.getInputType();

            // if this field is configured as choice control and its
            // presentation format is SELECT, render it as select field:
            String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
            ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
            if (cmgr.isChoicesConfigured(fieldKey) &&
                    Params.PRESENTATION_SELECT.equals(cmgr.getPresentation(fieldKey))) {
                renderChoiceSelectField(form, fieldName, collection, dcInput, dcValues, readonly);
            } else if (inputType.equals("name")) {
                renderNameField(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("date")) {
                renderDateField(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("embargo")) {
                if(showEmbargoField)
                    renderEmbargoField(c, form, fieldName, dcInput, dcValues, readonly, item);
            } else if (inputType.equals("series")) {
                renderSeriesField(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("seriesyear")) {
                renderSeriesFieldWithYear(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("twobox")) {
                // We don't have a twobox field, instead it's just a
                // one box field that the theme can render in two columns.
                renderOneboxField(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("qualdrop_value")) {
                // Determine the real field's values. Since the qualifier is
                // selected we need to search through all the metadata and see
                // if any match for another field, if not we assume that this field
                // should handle it.
                DCValue[] unfiltered = item.getMetadata(dcInput.getSchema(), dcInput.getElement(), org.dspace.content.Item.ANY, org.dspace.content.Item.ANY);
                ArrayList<DCValue> filtered = new ArrayList<DCValue>();
                for (DCValue dcValue : unfiltered) {
                    String unfilteredFieldName = dcValue.element + "." + dcValue.qualifier;
                    if (!inputSet.isFieldPresent(unfilteredFieldName)) {
                        filtered.add(dcValue);
                    }
                }

                renderQualdropField(form, fieldName, dcInput, filtered.toArray(new DCValue[filtered.size()]), readonly);
            } else if (inputType.equals("textarea")) {
                renderTextArea(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("dropdown")) {
                renderDropdownField(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("list")) {
                renderSelectFromListField(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("onebox")) {
                renderOneboxField(form, fieldName, dcInput, dcValues, readonly);
            } else if (inputType.equals("file")) {
                renderFileField(contextPath, form, fieldName, dcInput, item);
            } else if (inputType.equals("metadatadropdown")){
                renderMetadataDropdownField(item, form, fieldName, dcInput, dcValues, readonly);
            } else {
                form.addItem(T_unknown_field);
            }

        }
        return errorFields;
    }


/**
     * Render a Name field to the DRI document. The name field consists of two
     * text fields, one for the last name and the other for a first name (plus
     * all other names).
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     */
    private static void renderNameField(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        // The name field is a composite field containing two text fields, one
        // for first name the other for last name.
        Composite fullName = form.addItem().addComposite(fieldName, "submit-name");
        Text lastName = fullName.addText(fieldName + "_last");
        Text firstName = fullName.addText(fieldName + "_first");

        // Setup the full name
        fullName.setLabel(dcInput.getLabel());
        fullName.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            fullName.setRequired();
        if (isFieldInError(fieldName))
            fullName.addError(T_required_field);
        if (dcInput.isRepeatable() && !readonly)
            fullName.enableAddOperation();
        if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
            fullName.enableDeleteOperation();
        String fieldKey = MetadataAuthorityManager.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
        boolean isAuthorityControlled = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);
        if (isAuthorityControlled) {
            fullName.setAuthorityControlled();
            fullName.setAuthorityRequired(MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey));
        }
        if (ChoiceAuthorityManager.getManager().isChoicesConfigured(fieldKey)) {
            fullName.setChoices(fieldKey);
            fullName.setChoicesPresentation(ChoiceAuthorityManager.getManager().getPresentation(fieldKey));
            fullName.setChoicesClosed(ChoiceAuthorityManager.getManager().isClosed(fieldKey));
        }

        // Setup the first and last name
        lastName.setLabel(T_last_name_help);
        firstName.setLabel(T_first_name_help);

        if (readonly) {
            lastName.setDisabled();
            firstName.setDisabled();
            fullName.setDisabled();
        }

        // Setup the field's values
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            for (DCValue dcValue : dcValues) {
                DCPersonName dpn = new DCPersonName(dcValue.value);

                lastName.addInstance().setValue(dpn.getLastName());
                firstName.addInstance().setValue(dpn.getFirstNames());
                Instance fi = fullName.addInstance();
                fi.setValue(dcValue.value);
                if (isAuthorityControlled) {
                    if (dcValue.authority == null || dcValue.authority.equals(""))
                        fi.setAuthorityValue("", "blank");
                    else
                        fi.setAuthorityValue(dcValue.authority, Choices.getConfidenceText(dcValue.confidence));
                }
            }
        } else if (dcValues.length == 1) {
            DCPersonName dpn = new DCPersonName(dcValues[0].value);

            lastName.setValue(dpn.getLastName());
            firstName.setValue(dpn.getFirstNames());
            if (isAuthorityControlled) {
                if (dcValues[0].authority == null || dcValues[0].authority.equals(""))
                    lastName.setAuthorityValue("", "blank");
                else
                    lastName.setAuthorityValue(dcValues[0].authority, Choices.getConfidenceText(dcValues[0].confidence));
            }
        }
    }

    /**
     * Render a date field to the DRI document. The date field consists of
     * three component fields, a 4 character text field for the year, a select
     * box for the month, and a 2 character text field for the day.
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  Is the field readonly
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderDateField(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        // The date field consists of three primitive fields: a text field
        // for the year, followed by a select box of the months, follewed
        // by a text box for the day.
        Composite fullDate = form.addItem().addComposite(fieldName, "submit-date");
        Text year = fullDate.addText(fieldName + "_year");
        Select month = fullDate.addSelect(fieldName + "_month");
        Text day = fullDate.addText(fieldName + "_day");

        // Set up the full field
        fullDate.setLabel(dcInput.getLabel());
        fullDate.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            fullDate.setRequired();
        if (isFieldInError(fieldName))
            fullDate.addError(T_required_field);
        if (dcInput.isRepeatable() && !readonly)
            fullDate.enableAddOperation();
        if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
            fullDate.enableDeleteOperation();

        if (readonly) {
            year.setDisabled();
            month.setDisabled();
            day.setDisabled();
        }

        // Setup the year field
        year.setLabel(T_year);
        year.setSize(4, 4);

        // Setup the month field
        month.setLabel(T_month);
        month.addOption(0, "");
        for (int i = 1; i < 13; i++) {
            month.addOption(i, org.dspace.content.DCDate.getMonthName(i, Locale.getDefault()));
        }

        // Setup the day field
        day.setLabel(T_day);
        day.setSize(2, 2);

        // Setup the field's values
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            for (DCValue dcValue : dcValues) {
                DCDate dcDate = new DCDate(dcValue.value);

                year.addInstance().setValue(String.valueOf(dcDate.getYear()));
                month.addInstance().setOptionSelected(dcDate.getMonth());
                day.addInstance().setValue(String.valueOf(dcDate.getDay()));
                fullDate.addInstance().setValue(dcDate.toString());
            }
        } else if (dcValues.length == 1) {
            DCDate dcDate = new DCDate(dcValues[0].value);

            year.setValue(String.valueOf(dcDate.getYear()));
            month.setOptionSelected(dcDate.getMonth());

            // Check if the day field is not specified, if so then just
            // put a blank value in instead of the wiered looking -1.
            if (dcDate.getDay() == -1)
                day.setValue("");
            else
                day.setValue(String.valueOf(dcDate.getDay()));
        }
    }

    private static void renderEmbargoField(Context context, List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly,  org.dspace.content.Item item) throws WingException {
        // Plain old select list.
        Select select = form.addItem().addSelect(fieldName, "submit-select");

        //Setup the select field
        select.setLabel(dcInput.getLabel());
        select.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            select.setRequired();
        if (isFieldInError(fieldName))
            select.addError(T_required_field);
        /*
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            // Use the multiple functionality from the HTML
            // widget instead of DRI's version.
            select.setMultiple();
            select.setSize(6);
        }
        */

        if (readonly) {
            select.setDisabled();
        }


        // get Journal
        org.dspace.content.Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
        DCValue[] journalFullNames = dataPackage.getMetadata("prism.publicationName");
        String journalFullName=null;
        if(journalFullNames!=null && journalFullNames.length > 0){
            journalFullName=journalFullNames[0].value;
        }

        // show "Publish immediately" only if publicationBlackout=false or not defined in DryadJournalSubmission.properties.
        Map<String, String> values = journalProperties.get(journalFullName);
        String isBlackedOut=null;
        if(values!=null && values.size() > 0)
            isBlackedOut = values.get(PUBLICATION_BLACKOUT);

        if(isBlackedOut==null || isBlackedOut.equals("false"))
            select.addOption("none", "Publish immediately");

        select.addOption("oneyear", "1 year embargo");
        select.addOption("custom", "Custom length embargo (approved by journal editor)");
        select.addOption("untilArticleAppears", "Embargo until article appears");


        // Setup the field's pre-selected values
        if (dcValues.length > 0) {
            for (DCValue dcValue : dcValues) {
                select.setOptionSelected(dcValue.value);
            }
        } else {
            select.setOptionSelected("untilArticleAppears");
        }
    }

    /**
     * Render a series field to the DRI document. The series field conist of
     * two component text fields. When interpreted each of these fields are
     * combined back together to be a single value joined together by a
     * semicolen. The primary use case is for the journal or report number
     * the left hand side is the journal and the right hand side in a
     * unique number within the journal.
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  determine wheter the field is read only
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderSeriesField(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        // The seiries field consists of two parts, a series name (text field)
        // and report or paper number (also a text field).
        Composite fullSeries = form.addItem().addComposite(fieldName, "submit-" + dcInput.getInputType());
        Text series = fullSeries.addText(fieldName + "_series");
        Text number = fullSeries.addText(fieldName + "_number");

        // Setup the full field.
        fullSeries.setLabel(dcInput.getLabel());
        fullSeries.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            fullSeries.setRequired();
        if (isFieldInError(fieldName))
            fullSeries.addError(T_required_field);
        if (dcInput.isRepeatable() && !readonly)
            fullSeries.enableAddOperation();
        if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
            fullSeries.enableDeleteOperation();

        series.setLabel(T_series_name);
        number.setLabel(T_report_no);

        if (readonly) {
            fullSeries.setDisabled();
            series.setDisabled();
            number.setDisabled();
        }

        // Setup the field's values
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            for (DCValue dcValue : dcValues) {
                DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(dcValue.value);

                series.addInstance().setValue(dcSeriesNumber.getSeries());
                number.addInstance().setValue(dcSeriesNumber.getNumber());
                fullSeries.addInstance().setValue(dcSeriesNumber.toString());
            }

        } else if (dcValues.length == 1) {
            DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(dcValues[0].value);

            series.setValue(dcSeriesNumber.getSeries());
            number.setValue(dcSeriesNumber.getNumber());
        }
    }


    /**
     * Render a series field to the DRI document. The series field conist of
     * two component text fields. When interpreted each of these fields are
     * combined back together to be a single value joined together by a
     * semicolen. The primary use case is for the journal or report number
     * the left hand side is the journal and the right hand side in a
     * unique number within the journal.
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  determine wheter the field is read only
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderSeriesFieldWithYear(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        // The seiries field consists of two parts, a series name (text field)
        // and report or paper number (also a text field).
        Composite fullSeries = form.addItem().addComposite(fieldName, "submit-" + dcInput.getInputType());
        Text series = fullSeries.addText(fieldName + "_series");
        Text number = fullSeries.addText(fieldName + "_number");
        Text year = fullSeries.addText(fieldName + "_year");

        // Setup the full field.
        fullSeries.setLabel(dcInput.getLabel());
        fullSeries.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            fullSeries.setRequired();
        if (isFieldInError(fieldName))
            fullSeries.addError(T_required_field);
        if (dcInput.isRepeatable() && !readonly)
            fullSeries.enableAddOperation();
        if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
            fullSeries.enableDeleteOperation();

        series.setLabel(T_series_name);
        number.setLabel(T_report_no);
        year.setLabel(T_YEAR);

        if (readonly) {
            fullSeries.setDisabled();
            series.setDisabled();
            number.setDisabled();
            year.setDisabled();
        }

        // Setup the field's values
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            for (DCValue dcValue : dcValues) {
                DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(dcValue.value);

                series.addInstance().setValue(dcSeriesNumber.getSeries());
                number.addInstance().setValue(dcSeriesNumber.getNumber());
                year.addInstance().setValue(dcSeriesNumber.getYear());
                fullSeries.addInstance().setValue(dcSeriesNumber.toString());
            }

        } else if (dcValues.length == 1) {
            DCSeriesNumber dcSeriesNumber = new DCSeriesNumber(dcValues[0].value);

            series.setValue(dcSeriesNumber.getSeries());
            number.setValue(dcSeriesNumber.getNumber());
            year.setValue(dcSeriesNumber.getYear());
        }
    }

    /**
     * Render a qualdrop field to the DRI document. Qualdrop fields are complicated,
     * widget wise they are composed of two fields, a select and text box field.
     * The select field selects the metedata's qualifier and the text box is the
     * value. This means that that there is not just one metadata element that is
     * represented so the confusing part is that the name can change.
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  Is the field read only
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderQualdropField(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        Composite qualdrop = form.addItem().addComposite(fieldName, "submit-qualdrop");
        Select qual = qualdrop.addSelect(fieldName + "_qualifier");
        Text value = qualdrop.addText(fieldName + "_value");

        // Setup the full field.
        qualdrop.setLabel(dcInput.getLabel());
        qualdrop.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            qualdrop.setRequired();
        if (isFieldInError(fieldName))
            qualdrop.addError(T_required_field);
        if (dcInput.isRepeatable() && !readonly)
            qualdrop.enableAddOperation();
        // Update delete based upon the filtered values.
        if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
            qualdrop.enableDeleteOperation();

        if (readonly) {
            qualdrop.setDisabled();
            qual.setDisabled();
            value.setDisabled();
        }

        // Setup the possible options
        @SuppressWarnings("unchecked") // This cast is correct
                java.util.List<String> pairs = dcInput.getPairs();
        for (int i = 0; i < pairs.size(); i += 2) {
            String display = pairs.get(i);
            String returnValue = pairs.get(i + 1);
            qual.addOption(returnValue, display);
        }

        // Setup the field's values
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            for (DCValue dcValue : dcValues) {
                qual.addInstance().setOptionSelected(dcValue.qualifier);
                value.addInstance().setValue(dcValue.value);
                qualdrop.addInstance().setValue(dcValue.qualifier + ":" + dcValue.value);
            }
        } else if (dcValues.length == 1) {
            qual.setOptionSelected(dcValues[0].qualifier);
            value.setValue(dcValues[0].value);
        }
    }

    /**
     * Render a Text Area field to the DRI document. The text area is a simple
     * multi row and column text field.
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  Is the field readonly
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderTextArea(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        // Plain old Textarea
        TextArea textArea = form.addItem().addTextArea(fieldName, "submit-textarea");

        // Setup the text area
        textArea.setLabel(dcInput.getLabel());
        textArea.setSize(5, 43);
        textArea.setHelp(cleanHints(dcInput.getHints()));
        String fieldKey = MetadataAuthorityManager.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
        boolean isAuth = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);
        if (isAuth) {
            textArea.setAuthorityControlled();
            textArea.setAuthorityRequired(MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey));
        }
        if (ChoiceAuthorityManager.getManager().isChoicesConfigured(fieldKey)) {
            textArea.setChoices(fieldKey);
            textArea.setChoicesPresentation(ChoiceAuthorityManager.getManager().getPresentation(fieldKey));
            textArea.setChoicesClosed(ChoiceAuthorityManager.getManager().isClosed(fieldKey));
        }
        if (dcInput.isRequired())
            textArea.setRequired();
        if (isFieldInError(fieldName))
            textArea.addError(T_required_field);
        if (dcInput.isRepeatable() && !readonly)
            textArea.enableAddOperation();
        if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
            textArea.enableDeleteOperation();

        if (readonly) {
            textArea.setDisabled();
        }

        // Setup the field's values
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            for (DCValue dcValue : dcValues) {
                Instance ti = textArea.addInstance();
                ti.setValue(dcValue.value);
                if (isAuth) {
                    if (dcValue.authority == null || dcValue.authority.equals(""))
                        ti.setAuthorityValue("", "blank");
                    else
                        ti.setAuthorityValue(dcValue.authority, Choices.getConfidenceText(dcValue.confidence));
                }
            }
        } else if (dcValues.length == 1) {
            textArea.setValue(dcValues[0].value);
            if (isAuth) {
                if (dcValues[0].authority == null || dcValues[0].authority.equals(""))
                    textArea.setAuthorityValue("", "blank");
                else
                    textArea.setAuthorityValue(dcValues[0].authority, Choices.getConfidenceText(dcValues[0].confidence));
            }
        }
    }

    /**
     * Render a dropdown field for a choice-controlled input of the
     * 'select' presentation  to the DRI document. The dropdown field
     * consists of an HTML select box.
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param coll      The collection
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  Is the field readonly
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderChoiceSelectField(List form, String fieldName, Collection coll, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        String fieldKey = MetadataAuthorityManager.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
        if (MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey))
            throw new WingException("Field " + fieldKey + " has choice presentation of type \"" + Params.PRESENTATION_SELECT + "\", it may NOT be authority-controlled.");

        // Plain old select list.
        Select select = form.addItem().addSelect(fieldName, "submit-select");

        //Setup the select field
        select.setLabel(dcInput.getLabel());
        select.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            select.setRequired();
        if (isFieldInError(fieldName))
            select.addError(T_required_field);
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            // Use the multiple functionality from the HTML
            // widget instead of DRI's version.
            select.setMultiple();
            select.setSize(6);
        } else
            select.setSize(1);

        if (readonly) {
            select.setDisabled();
        }

        Choices cs = ChoiceAuthorityManager.getManager().getMatches(fieldKey, "", coll.getID(), 0, 0, null);
        if (dcValues.length == 0)
            select.addOption(true, "", "");
        for (Choice c : cs.values) {
            select.addOption(c.value, c.label);
        }

        // Setup the field's pre-selected values
        for (DCValue dcValue : dcValues) {
            select.setOptionSelected(dcValue.value);
        }
    }

    /**
     * Render a dropdown field to the DRI document. The dropdown field consists
     * of an HTML select box.
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  Is the field readonly
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderDropdownField(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        // Plain old select list.
        Select select = form.addItem().addSelect(fieldName, "submit-select");

        //Setup the select field
        select.setLabel(dcInput.getLabel());
        select.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            select.setRequired();
        if (isFieldInError(fieldName))
            select.addError(T_required_field);
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            // Use the multiple functionality from the HTML
            // widget instead of DRI's version.
            select.setMultiple();
            select.setSize(6);
        }

        if (readonly) {
            select.setDisabled();
        }

        // Setup the possible options
        @SuppressWarnings("unchecked") // This cast is correct
                java.util.List<String> pairs = dcInput.getPairs();
        for (int i = 0; i < pairs.size(); i += 2) {
            String display = pairs.get(i);
            String value = pairs.get(i + 1);
            select.addOption(value, display);
        }

        // Setup the field's pre-selected values
        for (DCValue dcValue : dcValues) {
            select.setOptionSelected(dcValue.value);
        }
    }

    private static void renderMetadataDropdownField(org.dspace.content.Item item, List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        // Start with a Plain old select list.
        Select select = form.addItem().addSelect(fieldName, "submit-select");

        //Setup the select field
        select.setLabel(dcInput.getLabel());
        select.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            select.setRequired();
        if (isFieldInError(fieldName))
            select.addError(T_required_field);
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            // Use the multiple functionality from the HTML
            // widget instead of DRI's version.
            select.setMultiple();
            select.setSize(6);
        }

        if (readonly) {
            select.setDisabled();
        }

        // Setup the possible options
        @SuppressWarnings("unchecked") // This cast is correct
        String[] pairType = dcInput.getPairsType().split("\\.");
        DCValue[] pairs = item.getMetadata(pairType[0], pairType[1], pairType.length == 3 ? pairType[2] : null, org.dspace.content.Item.ANY);

        for (DCValue pair : pairs) {
            String display = pair.value;
            String value = pair.value;
            select.addOption(value, display);
        }

        // Setup the field's pre-selected values
        for (DCValue dcValue : dcValues) {
            select.setOptionSelected(dcValue.value);
        }

    }

    /**
     * Render a select-from-list field to the DRI document.
     * This field consists of either a series of checkboxes
     * (if repeatable) or a series of radio buttons (if not repeatable).
     * <p/>
     * Note: This is NOT the same as a List element
     * (org.dspace.app.xmlui.wing.element.List).  It's just unfortunately
     * similarly named.
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  Is the field readonly
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderSelectFromListField(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        Field listField;

        //if repeatable, this list of fields should be checkboxes
        if (dcInput.isRepeatable()) {
            listField = form.addItem().addCheckBox(fieldName);
        } else //otherwise this is a list of radio buttons
        {
            listField = form.addItem().addRadio(fieldName);
        }

        if (readonly) {
            listField.setDisabled();
        }

        //      Setup the field
        listField.setLabel(dcInput.getLabel());
        listField.setHelp(cleanHints(dcInput.getHints()));
        if (dcInput.isRequired())
            listField.setRequired();
        if (isFieldInError(fieldName))
            listField.addError(T_required_field);


        //Setup each of the possible options
        java.util.List<String> pairs = dcInput.getPairs();
        for (int i = 0; i < pairs.size(); i += 2) {
            String display = pairs.get(i);
            String value = pairs.get(i + 1);

            if (listField instanceof CheckBox) {
                ((CheckBox) listField).addOption(value, display);
            } else if (listField instanceof Radio) {
                ((Radio) listField).addOption(value, display);
            }
        }

        // Setup the field's pre-selected values
        for (DCValue dcValue : dcValues) {
            if (listField instanceof CheckBox) {
                ((CheckBox) listField).setOptionSelected(dcValue.value);
            } else if (listField instanceof Radio) {
                ((Radio) listField).setOptionSelected(dcValue.value);
            }
        }
    }

    /**
     * Render a simple text field to the DRI document
     *
     * @param form      The form list to add the field too
     * @param fieldName The field's name.
     * @param dcInput   The field's input deffinition
     * @param dcValues  The field's pre-existing values.
     * @param readonly  Is the field readonly
     * @throws org.dspace.app.xmlui.wing.WingException
     *          ...
     */
    private static void renderOneboxField(List form, String fieldName, DCInput dcInput, DCValue[] dcValues, boolean readonly) throws WingException {
        // Both onebox and twobox consist a free form text field
        // that the user may enter any value. The difference between
        // the two is that a onebox should be rendered in one column
        // as twobox should be listed in a two column format. Since this
        // decision is not something the Aspect can effect we merely place
        // as a render hint.
        Text text = form.addItem().addText(fieldName, "submit-text");

        // Setup the select field
        text.setLabel(dcInput.getLabel());
        text.setHelp(cleanHints(dcInput.getHints()));
        text.setSize(50);
        String fieldKey = MetadataAuthorityManager.makeFieldKey(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier());
        boolean isAuth = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);
        if (isAuth) {
            text.setAuthorityControlled();
            text.setAuthorityRequired(MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey));
        }
        if (ChoiceAuthorityManager.getManager().isChoicesConfigured(fieldKey)) {
            text.setChoices(fieldKey);
            text.setChoicesPresentation(ChoiceAuthorityManager.getManager().getPresentation(fieldKey));
            text.setChoicesClosed(ChoiceAuthorityManager.getManager().isClosed(fieldKey));
        }

        if (dcInput.isRequired())
            text.setRequired();
        if (isFieldInError(fieldName))
            text.addError(T_required_field);
        if (dcInput.isRepeatable() && !readonly)
            text.enableAddOperation();
        if ((dcInput.isRepeatable() || dcValues.length > 1) && !readonly)
            text.enableDeleteOperation();

        if (readonly) {
            text.setDisabled();
        }

        // Setup the field's values
        if (dcInput.isRepeatable() || dcValues.length > 1) {
            for (DCValue dcValue : dcValues) {
                Instance ti = text.addInstance();
                ti.setValue(dcValue.value);
                if (isAuth) {
                    if (dcValue.authority == null || dcValue.authority.equals(""))
                        ti.setAuthorityValue("", "blank");
                    else
                        ti.setAuthorityValue(dcValue.authority, Choices.getConfidenceText(dcValue.confidence));
                }
            }
        } else if (dcValues.length == 1) {
            text.setValue(dcValues[0].value);
            if (isAuth) {
                if (dcValues[0].authority == null || dcValues[0].authority.equals(""))
                    text.setAuthorityValue("", "blank");
                else
                    text.setAuthorityValue(dcValues[0].authority, Choices.getConfidenceText(dcValues[0].confidence));
                if(fieldKey.equalsIgnoreCase("prism_publicationName")) {
		    text.setDisabled(true);
		    Hidden hiddenpub = form.addItem().addHidden(fieldName);
		    hiddenpub.setValue(dcValues[0].value);
		}
            }
        }
    }

    private static void renderFileField(String contextPath, List form, String fieldName, DCInput dcInput, org.dspace.content.Item item) throws SQLException, WingException {
        Bitstream filePresent = null;
        Bundle[] bundles = item.getBundles();
        for (Bundle bundle : bundles) {
            Bitstream[] bits = bundle.getBitstreams();
            for (Bitstream bit : bits) {
                if (fieldName.equals(bit.getDescription())) {
                    filePresent = bit;
                }
            }
        }

        if (filePresent == null) {
            //We do not have a file (yet) so show an upload box

            org.dspace.app.xmlui.wing.element.Item fileItem = form.addItem();
            File file = fileItem.addFile(fieldName);
            file.setLabel(dcInput.getLabel());
            fileItem.addHidden(fieldName + "-description").setValue(fieldName);


            if (dcInput.isRequired())
                file.setRequired();
            if (isFieldInError(fieldName))
                file.addError(T_required_field);


            file.setHelp(dcInput.getHints());
        } else {
            form.addLabel(dcInput.getLabel());
            //We have the file create an url for it
            String url = makeBitstreamLink(contextPath, item, filePresent);

            //org.dspace.app.xmlui.wing.element.Item fileItem = form.addItem();
            Item fileItem = form.addItem("submission-file-" + fieldName, "");
            
            fileItem.addHighlight("head").addContent(message("xmlui.Submission.submit.UploadStep.column2"));
            fileItem.addHighlight("head").addContent(message("xmlui.Submission.submit.UploadStep.column3"));
            fileItem.addHighlight("head").addContent(message("xmlui.Submission.submit.UploadStep.column5"));
            fileItem.addHighlight("head").addContent(message("xmlui.Submission.submit.UploadStep.column7"));

            fileItem.addHighlight("content").addXref(url, filePresent.getName());
            fileItem.addHighlight("content").addContent((filePresent.getSize() / 1000) + "Kb");
            fileItem.addHighlight("content").addContent(filePresent.getFormat().getDescription());
            //Also create a remove button
            fileItem.addHighlight("content").addButton("submit_" + fieldName + "_remove_" + filePresent.getID()).setValue("Remove");
        }
    }

    /**
     * Check if the given fieldname is listed as being in error.
     *
     * @param fieldName the name of the field
     * @return if this field is in error
     */
    private static boolean isFieldInError(String fieldName) {
        return errorFields.contains(fieldName);
    }


        /**
     * Returns canonical link to a bitstream in the item.
     *
     * @param item      The DSpace Item that the bitstream is part of
     * @param bitstream The bitstream to link to
     * @return a String link to the bistream
     */
    public static String makeBitstreamLink(String contextPath, org.dspace.content.Item item, Bitstream bitstream) {
        String name = bitstream.getName();
        StringBuilder result = new StringBuilder(contextPath);
        result.append("/bitstream/item/").append(String.valueOf(item.getID()));
        // append name although it isn't strictly necessary
        try {
            if (name != null) {
                result.append("/").append(Util.encodeBitstreamName(name, "UTF-8"));
            }
        }
        catch (UnsupportedEncodingException uee) {
            // just ignore it, we don't have to have a pretty
            // name on the end of the url because the sequence id will
            // locate it. However it means that links in this file might
            // not work....
        }
        result.append("?sequence=").append(String.valueOf(bitstream.getSequenceID()));
        return result.toString();
    }

    /**
     * There is a problem with the way hints are handled. The class that we use to
     * read the input-forms.xml configuration will append and prepend HTML to hints.
     * This causes all sorts of confusion when inserting into the DRI page, so this
     * method will strip that extra HTML and just leave the cleaned comments.
     * <p/>
     * <p/>
     * However this method will not remove naughty or sexual innuendoes from the
     * field's hints.
     * <p/>
     * dirtyHints HTML-ized hints
     * Hints without HTML.
     */
    private static final String HINT_HTML_PREFIX = "<tr><td colspan=\"4\" class=\"submitFormHelp\">";
    private static final String HINT_HTML_POSTFIX = "</td></tr>";

    private static String cleanHints(String dirtyHints) {
        String clean = (dirtyHints != null ? dirtyHints : "");

        if (clean.startsWith(HINT_HTML_PREFIX)) {
            clean = clean.substring(HINT_HTML_PREFIX.length());
        }

        if (clean.endsWith(HINT_HTML_POSTFIX)) {
            clean = clean.substring(0, clean.length() - HINT_HTML_POSTFIX.length());
        }

        return clean;
    }

}
