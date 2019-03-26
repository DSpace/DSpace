/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.sherpa.SHERPAJournal;
import org.dspace.app.sherpa.SHERPAPublisher;
import org.dspace.app.sherpa.SHERPAResponse;
import org.dspace.app.sherpa.submit.SHERPASubmitService;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.File;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

/**
 * This is a step of the item submission processes. The upload
 * stages allow the user to upload files into the submission. The
 * form is optimized for one file, but allows the user to upload
 * more if needed.
 * <P>
 * The form is broken up into three sections:
 * <P>
 * Part A: Ask the user to upload a file
 * Part B: List previously uploaded files
 * Part C: The standard action bar
 *
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class UploadStep extends AbstractSubmissionStep
{
    /** Language Strings for Uploading **/
    protected static final Message T_head =
            message("xmlui.Submission.submit.UploadStep.head");
    protected static final Message T_file =
            message("xmlui.Submission.submit.UploadStep.file");
    protected static final Message T_file_help =
            message("xmlui.Submission.submit.UploadStep.file_help");
    protected static final Message T_file_error =
            message("xmlui.Submission.submit.UploadStep.file_error");
    protected static final Message T_upload_error =
            message("xmlui.Submission.submit.UploadStep.upload_error");

    protected static final Message T_virus_checker_error =
            message("xmlui.Submission.submit.UploadStep.virus_checker_error");
    protected static final Message T_virus_error =
            message("xmlui.Submission.submit.UploadStep.virus_error");

    protected static final Message T_description =
            message("xmlui.Submission.submit.UploadStep.description");
    protected static final Message T_description_help =
            message("xmlui.Submission.submit.UploadStep.description_help");
    protected static final Message T_submit_upload =
            message("xmlui.Submission.submit.UploadStep.submit_upload");
    protected static final Message T_head2 =
            message("xmlui.Submission.submit.UploadStep.head2");
    protected static final Message T_column0 =
            message("xmlui.Submission.submit.UploadStep.column0");
    protected static final Message T_column1 =
            message("xmlui.Submission.submit.UploadStep.column1");
    protected static final Message T_column2 =
            message("xmlui.Submission.submit.UploadStep.column2");
    protected static final Message T_column3 =
            message("xmlui.Submission.submit.UploadStep.column3");
    protected static final Message T_column4 =
            message("xmlui.Submission.submit.UploadStep.column4");
    protected static final Message T_column5 =
            message("xmlui.Submission.submit.UploadStep.column5");
    protected static final Message T_column6 =
            message("xmlui.Submission.submit.UploadStep.column6");
    protected static final Message T_unknown_name =
            message("xmlui.Submission.submit.UploadStep.unknown_name");
    protected static final Message T_unknown_format =
            message("xmlui.Submission.submit.UploadStep.unknown_format");
    protected static final Message T_supported =
            message("xmlui.Submission.submit.UploadStep.supported");
    protected static final Message T_known =
            message("xmlui.Submission.submit.UploadStep.known");
    protected static final Message T_unsupported =
            message("xmlui.Submission.submit.UploadStep.unsupported");
    protected static final Message T_submit_edit =
            message("xmlui.Submission.submit.UploadStep.submit_edit");
    protected static final Message T_checksum =
            message("xmlui.Submission.submit.UploadStep.checksum");
    protected static final Message T_submit_remove =
            message("xmlui.Submission.submit.UploadStep.submit_remove");


    protected static final Message T_sherpa_consult =
        message("xmlui.aspect.sherpa.submission.consult");

    protected static final Message T_sherpa_title =
        message("xmlui.aspect.sherpa.submission.title");

    protected static final Message T_sherpa_journal =
        message("xmlui.aspect.sherpa.submission.journal");

    protected static final Message T_sherpa_publisher =
        message("xmlui.aspect.sherpa.submission.publisher");

    protected static final Message T_sherpa_colour =
        message("xmlui.aspect.sherpa.submission.colour");

    protected static final Message T_sherpa_more =
            message("xmlui.aspect.sherpa.submission.more");

    /**
     * Global reference to edit file page
     * (this is used when a user requests to edit a bitstream)
     **/
    private EditFileStep editFile = null;

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Establish our required parameters, abstractStep will enforce these.
     */
    public UploadStep()
    {
        this.requireSubmission = true;
        this.requireStep = true;
    }


    /**
     * Check if user has requested to edit information about an
     * uploaded file.
     * @throws org.apache.cocoon.ProcessingException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.io.IOException passed through.
     */
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters)
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver,objectModel,src,parameters);

        // If this page for editing an uploaded file's information
        // was requested, then we need to load EditFileStep instead!
        if(this.errorFlag==org.dspace.submit.step.UploadStep.STATUS_EDIT_BITSTREAM)
        {
            this.editFile = new EditFileStep();
            this.editFile.setup(resolver, objectModel, src, parameters);
        }
        else
        {
            this.editFile = null;
        }
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // If we are actually editing information of an uploaded file,
        // then display that body instead!
        if(this.editFile!=null)
        {
            editFile.addBody(body);
            return;
        }

        // Get a list of all files in the original bundle
        Item item = submission.getItem();
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        boolean disableFileEditing = (submissionInfo.isInWorkflow()) && !DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("workflow.reviewer.file-edit");
        java.util.List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
        java.util.List<Bitstream> bitstreams = new ArrayList<>();
        if (bundles.size() > 0)
        {
            bitstreams = bundles.get(0).getBitstreams();
        }

        // Part A:
        //  First ask the user if they would like to upload a new file (may be the first one)
        Division div = body.addInteractiveDivision("submit-upload", actionURL, Division.METHOD_MULTIPART, "primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);

        List upload = null;
        if (!disableFileEditing)
        {
            // Only add the upload capabilities for new item submissions
            upload = div.addList("submit-upload-new", List.TYPE_FORM);
            upload.setHead(T_head);

            File file = upload.addItem().addFile("file");
            file.setLabel(T_file);
            file.setHelp(T_file_help);
            file.setRequired();

            // if no files found error was thrown by processing class, display it!
            if (this.errorFlag==org.dspace.submit.step.UploadStep.STATUS_NO_FILES_ERROR)
            {
                file.addError(T_file_error);
            }

            // if an upload error was thrown by processing class, display it!
            if (this.errorFlag == org.dspace.submit.step.UploadStep.STATUS_UPLOAD_ERROR)
            {
                file.addError(T_upload_error);
            }

            // if virus checking was attempted and failed in error then let the user know
            if (this.errorFlag == org.dspace.submit.step.UploadStep.STATUS_VIRUS_CHECKER_UNAVAILABLE)
            {
                file.addError(T_virus_checker_error);
            }

            // if virus checking was attempted and a virus found then let the user know
            if (this.errorFlag == org.dspace.submit.step.UploadStep.STATUS_CONTAINS_VIRUS)
            {
                file.addError(T_virus_error);
            }

            Text description = upload.addItem().addText("description");
            description.setLabel(T_description);
            description.setHelp(T_description_help);

            Button uploadSubmit = upload.addItem().addButton("submit_upload");
            uploadSubmit.setValue(T_submit_upload);
        }

        make_sherpaRomeo_submission(item, div);

        // Part B:
        //  If the user has already uploaded files provide a list for the user.
        if (bitstreams.size() > 0 || disableFileEditing)
        {
            Table summary = div.addTable("submit-upload-summary",(bitstreams.size() * 2) + 2,7);
            summary.setHead(T_head2);

            Row header = summary.addRow(Row.ROLE_HEADER);
            header.addCellContent(T_column0); // primary bitstream
            header.addCellContent(T_column1); // select checkbox
            header.addCellContent(T_column2); // file name
            header.addCellContent(T_column3); // size
            header.addCellContent(T_column4); // description
            header.addCellContent(T_column5); // format
            header.addCellContent(T_column6); // edit button

            for (Bitstream bitstream : bitstreams)
            {
                UUID id = bitstream.getID();
                String name = bitstream.getName();
                String url = makeBitstreamLink(item, bitstream);
                long bytes = bitstream.getSize();
                String desc = bitstream.getDescription();
                String algorithm = bitstream.getChecksumAlgorithm();
                String checksum = bitstream.getChecksum();


                Row row = summary.addRow();

                // Add radio-button to select this as the primary bitstream
                Radio primary = row.addCell().addRadio("primary_bitstream_id");
                primary.addOption(String.valueOf(id));

                // If this bitstream is already marked as the primary bitstream
                // mark it as such.
                if(bundles.get(0).getPrimaryBitstream() != null && bundles.get(0).getPrimaryBitstream().getID().equals(id)) {
                    primary.setOptionSelected(String.valueOf(id));
                }

                if (!disableFileEditing)
                {
                    // Workflow users can not remove files.
                    CheckBox remove = row.addCell().addCheckBox("remove");
                    remove.setLabel("remove");
                    remove.addOption(id.toString());
                }
                else
                {
                    row.addCell();
                }

                row.addCell(null,null,"break-all").addXref(url, name);
                row.addCellContent(bytes + " bytes");
                if (desc == null || desc.length() == 0)
                {
                    row.addCellContent(T_unknown_name);
                }
                else
                {
                    row.addCell(null,null,"break-all").addContent(desc);
                }

                BitstreamFormat format = bitstream.getFormat(context);
                if (format == null)
                {
                    row.addCellContent(T_unknown_format);
                }
                else
                {
                    int support = format.getSupportLevel();
                    Cell cell = row.addCell();
                    cell.addContent(format.getMIMEType());
                    cell.addContent(" ");
                    switch (support)
                    {
                        case 1:
                            cell.addContent(T_supported);
                            break;
                        case 2:
                            cell.addContent(T_known);
                            break;
                        case 3:
                            cell.addContent(T_unsupported);
                            break;
                    }
                }

                Button edit = row.addCell().addButton("submit_edit_"+id);
                edit.setValue(T_submit_edit);

                Row checksumRow = summary.addRow();
                checksumRow.addCell();
                Cell checksumCell = checksumRow.addCell(null, null, 0, 6, null);
                checksumCell.addHighlight("bold").addContent(T_checksum);
                checksumCell.addContent(" ");
                checksumCell.addContent(algorithm + ":" + checksum);
            }

            if (!disableFileEditing)
            {
                // Workflow users can not remove files.
                Row actionRow = summary.addRow();
                actionRow.addCell();
                Button removeSeleceted = actionRow.addCell(null, null, 0, 6, null).addButton("submit_remove_selected");
                removeSeleceted.setValue(T_submit_remove);
            }

            upload = div.addList("submit-upload-new-part2", List.TYPE_FORM);
        }

        // Part C:
        // add standard control/paging buttons
        addControlButtons(upload);
    }


    /**
     * Sherpa RoMEO submission support.
     * @param item the item in question.
     * @param divIn add to this division.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */

    public void make_sherpaRomeo_submission(Item item, Division divIn)
            throws WingException {

        if (DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.submission.sherparomeo-policy-enabled", true)){

            SHERPASubmitService sherpaSubmitService = new DSpace().getSingletonService(SHERPASubmitService.class);

            if(sherpaSubmitService.hasISSNs(context, item)) {
                List div = divIn.addList("submit-upload-new", List.TYPE_FORM);
                div.setHead(T_sherpa_title);

                //Since sherpa web service doesn't work reliable with more than 1 issn, perform the service for each issn
                Set<String> issns = sherpaSubmitService.getISSNs(context, item);
                Iterator<String> issnsIterator = issns.iterator();

                int i = 0;
                while (issnsIterator.hasNext()) {
                    SHERPAResponse shresp = sherpaSubmitService.searchRelatedJournalsByISSN(issnsIterator.next());

                    java.util.List<SHERPAJournal> journals = shresp.getJournals();
                    java.util.List<SHERPAPublisher> publishers = shresp.getPublishers();

                    if (CollectionUtils.isNotEmpty(journals)) {
                        for (SHERPAJournal journ : journals) {

                            List sherpaList = div.addList("sherpaList" + (i + 1), "simple", "sherpaList");
                            sherpaList.addItem().addFigure(contextPath + "/static/images/" + (i == 0 ? "romeosmall" : "clear") + ".gif", "http://www.sherpa.ac.uk/romeo/", "sherpaLogo");

                            sherpaList.addItem().addHighlight("sherpaBold").addContent(T_sherpa_journal);
                            sherpaList.addItem(journ.getTitle() + " (" + journ.getIssn() + ")");

                            if(CollectionUtils.isNotEmpty(publishers)) {
                                SHERPAPublisher pub = publishers.get(0);
                                sherpaList.addItem().addHighlight("sherpaBold").addContent(T_sherpa_publisher);
                                sherpaList.addItemXref(pub.getHomeurl(), pub.getName());

                                sherpaList.addItem().addHighlight("sherpaBold").addContent(T_sherpa_colour);
                                sherpaList.addItem().addHighlight("sherpaStyle " + pub.getRomeocolour()).addContent(message("xmlui.aspect.sherpa.submission." + pub.getRomeocolour()));
                            }

                            sherpaList.addItem().addXref("http://www.sherpa.ac.uk/romeo/search.php?issn=" + journ.getIssn(), T_sherpa_more, "sherpaMoreInfo");

                            i = i + 1;
                        }
                    }
                }

                List sherpaList = div.addList("sherpaListEnd","simple","sherpaList");
                sherpaList.addItem(T_sherpa_consult);
            }
        }
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
     * by using a call to reviewList.addList().  This sublist is
     * the list you return from this method!
     *
     * @param reviewList
     *      The List to which all reviewable information should be added
     * @return
     *      The new sub-List object created by this step, which contains
     *      all the reviewable information.  If this step has nothing to
     *      review, then return null!   
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public List addReviewSection(List reviewList) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Create a new list section for this step (and set its heading)
        List uploadSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
        uploadSection.setHead(T_head);

        // Review all uploaded files
        Item item = submission.getItem();
        java.util.List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
        java.util.List<Bitstream> bitstreams = new ArrayList<>();
        if (bundles.size() > 0)
        {
            bitstreams = bundles.get(0).getBitstreams();
        }

        for (Bitstream bitstream : bitstreams)
        {
            BitstreamFormat bitstreamFormat = bitstream.getFormat(context);

            String name = bitstream.getName();
            String url = makeBitstreamLink(item, bitstream);
            String format = bitstreamFormat.getShortDescription();
            Message support = ReviewStep.T_unknown;
            if (bitstreamFormat.getSupportLevel() == BitstreamFormat.KNOWN)
            {
                support = T_known;
            }
            else if (bitstreamFormat.getSupportLevel() == BitstreamFormat.SUPPORTED)
            {
                support = T_supported;
            }

            org.dspace.app.xmlui.wing.element.Item file = uploadSection.addItem(null,"break-all");
            file.addXref(url,name);
            file.addContent(" - "+ format + " ");
            file.addContent(support);

        }

        // return this new "upload" section
        return uploadSection;
    }

    /**
     * Returns canonical link to a bitstream in the item.
     *
     * @param item The DSpace Item that the bitstream is part of
     * @param bitstream The bitstream to link to
     * @returns a String link to the bitstream
     */
    private String makeBitstreamLink(Item item, Bitstream bitstream)
    {
        String name = bitstream.getName();
        StringBuilder result = new StringBuilder(contextPath);
        result.append("/bitstream/item/").append(String.valueOf(item.getID()));
        // append name although it isn't strictly necessary
        try
        {
            if (name != null)
            {
                result.append("/").append(Util.encodeBitstreamName(name, "UTF-8"));
            }
        }
        catch (UnsupportedEncodingException uee)
        {
            // just ignore it, we don't have to have a pretty
            // name on the end of the url because the sequence id will
            // locate it. However it means that links in this file might
            // not work....
        }
        result.append("?sequence=").append(String.valueOf(bitstream.getSequenceID()));
        return result.toString();
    }
}
        
