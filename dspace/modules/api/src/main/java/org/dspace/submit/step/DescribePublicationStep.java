package org.dspace.submit.step;

import org.dspace.submit.AbstractProcessingStep;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.app.util.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.LinkedList;
import java.util.Enumeration;
import org.dspace.usagelogging.EventLogger;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 29-jan-2010
 * Time: 16:17:38
 *
 * The processing of the describe page for a data package
 */
public class DescribePublicationStep extends AbstractProcessingStep {

    /**
     * hash of all submission forms details
     */
    private static DCInputsReader inputsReader;

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

    private static final int FILE_UPLOAD_ERROR = 0;
    private static final int FILE_NO_UPLOAD = 1;
    private static final int FILE_UPLOAD_OK = 2;

    // the metadata language qualifier
    public static final String LANGUAGE_QUALIFIER = getDefaultLanguageQualifier();

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(DescribePublicationStep.class);

   /**
    * Constructor
    * @throws javax.servlet.ServletException thrown if we have problems with our dcinputsreader
    */
   public DescribePublicationStep() throws ServletException {
       //load the DCInputsReader
       getInputsReader();
   }



    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        // check what submit button was pressed in User Interface
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

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
            else if (inputType.equals("dropdown") || inputType.equals("list") || inputType.equals("metadatadropdown") ||
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
                List quals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_qualifier");
                List vals = getRepeatedParameter(request, schema + "_"
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
                        //Ignore
                        log.error("Error while removing bitstream:" + buttonPressed, e);
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

        // Step 4:
        // Save changes to database
        subInfo.getSubmissionItem().update();

        // commit changes
        context.commit();

        if (moreInput)
        {
            EventLogger.log(context, "submission-describe-publication", "error=more_input_requested");
            return STATUS_MORE_INPUT_REQUESTED;
        }
        // if one or more fields errored out, return
        else if (getErrorFields(request) != null && getErrorFields(request).size() > 0)
        {
            EventLogger.log(context, "submission-describe-publication", "error=missing_required_fields");
            return STATUS_MISSING_REQUIRED_FIELDS;
        }

        // completed without errors
        EventLogger.log(context, "submission-describe-publication", "status=complete");
        return STATUS_COMPLETE;

    }

    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
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
     * @return the current DCInputsReader
     * @throws javax.servlet.ServletException thrown if we have problems with the dcinputsreader
     */
    public static DCInputsReader getInputsReader() throws ServletException {
        // load inputsReader only the first time
        if (inputsReader == null) {
            // read configurable submissions forms data
            try {
                inputsReader = new DCInputsReader();
            }
            catch (DCInputsReaderException e) {
                throw new ServletException(e);
            }
        }

        return inputsReader;
    }

    protected int processUploadFile(Context context, HttpServletRequest request,
                                        SubmissionInfo subInfo, String fileParam, boolean isReadmeFile)
            throws ServletException, IOException, SQLException,
            AuthorizeException {
//        boolean formatKnown = true;
        boolean fileOK = false;
        BitstreamFormat bf;
        Bitstream b;

        //NOTE: File should already be uploaded.
        //Manakin does this automatically via Cocoon.
        //For JSP-UI, the SubmissionController.uploadFiles() does the actual upload

        Enumeration attNames = request.getAttributeNames();

        //loop through our request attributes
        while (attNames.hasMoreElements()) {
            String attr = (String) attNames.nextElement();

            //if this ends with "-path", this attribute
            //represents a newly uploaded file
            if (attr.equals(fileParam + "-path")) {
                //strip off the -path to get the actual parameter
                //that the file was uploaded as

                // Load the file's path and input stream and description
                String filePath = (String) request.getAttribute(fileParam + "-path");
                InputStream fileInputStream = (InputStream) request
                        .getAttribute(fileParam + "-inputstream");

                //attempt to get description from attribute first, then direct from a parameter
                String fileDescription = (String) request
                        .getAttribute(fileParam + "-description");
                if (fileDescription == null || fileDescription.length() == 0)
                    fileDescription = request.getParameter(fileParam + "-description");

                // if information wasn't passed by User Interface, we had a problem
                // with the upload
                if (filePath == null || fileInputStream == null)
                    return FILE_UPLOAD_ERROR;

                if (subInfo != null) {
                    // Create the bitstream
                    Item item = subInfo.getSubmissionItem().getItem();

                    // do we already have a bundle?
                    Bundle[] bundles = item.getBundles("ORIGINAL");

                    if (bundles.length < 1) {
                        // set bundle's name to ORIGINAL
                        b = item.createSingleBitstream(fileInputStream, "ORIGINAL");
                    } else {
                        // we have a bundle already, just add bitstream
                        b = bundles[0].createBitstream(fileInputStream);
                    }

                    // Strip all but the last filename. It would be nice
                    // to know which OS the file came from.
                    String noPath = filePath;

                    while (noPath.indexOf('/') > -1) {
                        noPath = noPath.substring(noPath.indexOf('/') + 1);
                    }

                    while (noPath.indexOf('\\') > -1) {
                        noPath = noPath.substring(noPath.indexOf('\\') + 1);
                    }

                    //For a readme file we change the name to README
                    if(isReadmeFile)
                        b.setName("README" + noPath.substring(noPath.lastIndexOf(".")));
                    else
                        b.setName(noPath);
                    b.setSource(filePath);
                    b.setDescription(fileDescription);

                    // Identify the format
                    bf = FormatIdentifier.guessFormat(context, b);
                    b.setFormat(bf);

                    // Update to DB
                    b.update();
                    item.update();

                    if (bf == null || !bf.isInternal()) {
                        fileOK = true;
                    } else {
                        log.warn("Attempt to upload file format marked as internal system use only");

                        // remove bitstream from bundle..
                        // delete bundle if it's now empty
                        Bundle[] bnd = b.getBundles();

                        bnd[0].removeBitstream(b);

                        Bitstream[] bitstreams = bnd[0].getBitstreams();

                        // remove bundle if it's now empty
                        if (bitstreams.length < 1) {
                            item.removeBundle(bnd[0]);
                            item.update();
                        }

                        subInfo.setBitstream(null);
                    }
                }// if subInfo not null
                else {
                    // In any event, if we don't have the submission info, the request
                    // was malformed
                    return FILE_UPLOAD_ERROR;
                }

                // as long as everything completed ok, commit changes. Otherwise show
                // error page.
                if (fileOK) {
                    context.commit();

                    // save this bitstream to the submission info, as the
                    // bitstream we're currently working with
                    subInfo.setBitstream(b);

                    return FILE_UPLOAD_OK;
                    //if format was not identified
//                    if (bf == null)
//                    {
                    // the bitstream format is unknown!
//                        formatKnown=false;
//                    }
                } else {
                    // if we get here there was a problem uploading the file!
                    return FILE_UPLOAD_ERROR;
                }
            }//end if attribute ends with "-path"
        }//end while

        return FILE_NO_UPLOAD;

    }


    private void removeBitstream(Context context, Item item, int bitId) throws SQLException, AuthorizeException, IOException {
        Bitstream bit = Bitstream.find(context, bitId);
        if(bit != null){
            Bundle owningBundle = bit.getBundles()[0];
            owningBundle.removeBitstream(bit);
            // remove bundle if it's now empty
            Bitstream[] bitstreams = owningBundle.getBitstreams();
            if (bitstreams.length < 1)
            {
                item.removeBundle(owningBundle);
                item.update();
            }
        }
    }

    /**
     * @return the default language qualifier for metadata
     */

    public static String getDefaultLanguageQualifier()
    {
       String language;
       language = ConfigurationManager.getProperty("default.language");
       if (language == null)
       {
	   // if no language is set, ensure it is blank
           language = "";
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
        List firsts = new LinkedList();
        List lasts = new LinkedList();
        List auths = new LinkedList();
        List confs = new LinkedList();

        if (repeated)
        {
            firsts = getRepeatedParameter(request, metadataField, metadataField
                    + "_first");
            lasts = getRepeatedParameter(request, metadataField, metadataField
                    + "_last");
            auths = getRepeatedParameter(request, metadataField, metadataField
                    + "_authority");
            confs = getRepeatedParameter(request, metadataField, metadataField
                    + "_confidence");

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
                auths.remove(valToRemove);
                confs.remove(valToRemove);
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
                lasts.add(lastName);
            if (firstNames != null)
                firsts.add(firstNames);
            auths.add(authority == null ? "" : authority);
            confs.add(confidence == null ? "" : confidence);
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < lasts.size(); i++)
        {
            String f = (String) firsts.get(i);
            String l = (String) lasts.get(i);

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
                    String authKey = auths.size() > i ? (String)auths.get(i) : null;
                    String sconf = (authKey != null && confs.size() > i) ? (String)confs.get(i) : null;
                    if (MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey) &&
                        (authKey == null || authKey.length() == 0))
                    {
                        log.warn("Skipping value of "+metadataField+" because the required Authority key is missing or empty.");
                        addErrorField(request, metadataField);
                    }
                    else
                        item.addMetadata(schema, element, qualifier, null,
                                new DCPersonName(l, f).toString(), authKey,
                                (sconf != null && sconf.length() > 0) ?
                                    Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED);
                }
                else
                    item.addMetadata(schema, element, qualifier, null,
                        new DCPersonName(l, f).toString());
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
            String element, String qualifier, boolean repeated, String lang, String splitChar)
    {
        // FIXME: Of course, language should be part of form, or determined
        // some other way
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);

        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        boolean isAuthorityControlled = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);

        // Values to add
        List vals = null;
        List auths = null;
        List confs = null;

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
		// ensure the title starts with "Data from:"
                if (ConfigurationManager.getBooleanProperty("datapackage.title.datafrom", true) && element.equals("title") && qualifier == null && !s.toLowerCase().startsWith("data from:")) {
                    s = "Data from: " + s;
                }

		// ensure the article DOI is in the proper format if it's a DOI
                if (element.equals("relation") && qualifier.equals("isreferencedby")) {
		    if(s.toLowerCase().startsWith("http://dx.doi.org/")) {
			s = "doi:" + s.substring("http://dx.doi.org/".length());
		    } else if(!s.toLowerCase().startsWith("doi:") &&
                            !s.toLowerCase().startsWith("pmid:")) {
			s = "doi:" + s;
		    }
                }


		
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
     * @throws SQLException ...
     */
    protected void readDate(HttpServletRequest request, Item item, String schema,
            String element, String qualifier) throws SQLException
    {
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);

        int year = Util.getIntParameter(request, metadataField + "_year");
        int month = Util.getIntParameter(request, metadataField + "_month");
        int day = Util.getIntParameter(request, metadataField + "_day");

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DATE, day);


        // FIXME: Probably should be some more validation
        // Make a standard format date
        DCDate d = new DCDate(cal.getTime());

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
        List series = new LinkedList();
        List numbers = new LinkedList();

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
                    n = "";

                series.add(s);
                numbers.add(n);
            }
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < series.size(); i++)
        {
            String s = ((String) series.get(i)).trim();
            String n = ((String) numbers.get(i)).trim();

            // Only add non-empty
            if (!s.equals("") || !n.equals(""))
            {
                item.addMetadata(schema, element, qualifier, null,
                        new DCSeriesNumber(s, n).toString());
            }
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
    protected void readSeriesNumbersWithYear(HttpServletRequest request, Item item,
            String schema, String element, String qualifier, boolean repeated)
    {
        String metadataField = MetadataField
                .formKey(schema, element, qualifier);

        // Names to add
        List series = new LinkedList();
        List numbers = new LinkedList();
        List years = new LinkedList();

        if (repeated)
        {
            series = getRepeatedParameter(request, metadataField, metadataField
                    + "_series");
            numbers = getRepeatedParameter(request, metadataField,
                    metadataField + "_number");

            years = getRepeatedParameter(request, metadataField,
                    metadataField + "_year");

            // Find out if the relevant "remove" button was pressed
            String buttonPressed = Util.getSubmitButton(request, "");
            String removeButton = "submit_" + metadataField + "_remove_";

            if (buttonPressed.startsWith(removeButton))
            {
                int valToRemove = Integer.parseInt(buttonPressed
                        .substring(removeButton.length()));

                series.remove(valToRemove);
                numbers.remove(valToRemove);
                years.remove(valToRemove);

            }
        }
        else
        {
            // Just a single name
            String s = request.getParameter(metadataField + "_series");
            String n = request.getParameter(metadataField + "_number");
            String y = request.getParameter(metadataField + "_year");

            // Only put it in if there was a name present
            if ((s != null) && !s.equals(""))
            {
                // if number is null, just set to a nullstring
                if (n == null)
                    n = "";
                if (y == null)
                    y = "";

                series.add(s);
                numbers.add(n);
                years.add(y);
            }
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < series.size(); i++)
        {
            String s = ((String) series.get(i)).trim();
            String n = ((String) numbers.get(i)).trim();
            String y = ((String) years.get(i)).trim();

            // Only add non-empty
            if (!s.equals("") || !n.equals("") || !y.equals(""))
            {
                item.addMetadata(schema, element, qualifier, null,
                        new DCSeriesNumber(s, n, y).toString());
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
    protected List getRepeatedParameter(HttpServletRequest request, String metadataField, String param)
    {
        return getRepeatedParameter(request, metadataField, param, null);
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
    protected List getRepeatedParameter(HttpServletRequest request,
            String metadataField, String param, String splitChar)
    {
        List vals = new LinkedList();

        int i = 1;    //start index at the first of the previously entered values
        boolean foundLast = false;
        String buttonPressed = Util.getSubmitButton(request, "");

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

                if (selected != null && buttonPressed.equals("submit_" + metadataField + "_delete"))
                {
                    for (String aSelected : selected) {
                        if (aSelected.equals(metadataField + "_" + i)) {
                            addValue = false;
                        }
                    }
                // see if we got a button-press for deleting an author
                } else if(buttonPressed.equals("submit_" + metadataField + "_" + i + "_delete")) {
                    addValue = false;
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

    /**
     * Return the HTML / DRI field name for the given input.
     *
     * @param input ...
     * @return ...
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
