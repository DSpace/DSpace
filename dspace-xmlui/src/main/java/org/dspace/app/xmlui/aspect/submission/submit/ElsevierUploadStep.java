/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.xmlui.aspect.administrative.importer.external.fileaccess.FileAccessUI;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.fileaccess.factory.FileAccessServiceFactory;
import org.dspace.fileaccess.service.FileAccessFromMetadataService;
import org.dspace.core.ConfigurationManager;
import org.dspace.importer.external.scidir.entitlement.ArticleAccess;
import org.dspace.importer.external.scidir.entitlement.OpenAccessArticleCheck;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

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
public class ElsevierUploadStep extends UploadStep{

    protected static final Message T_file_access =
            message("xmlui.file-access.FileAccessUI.label");
    protected static final Message T_open_access =
            message("xmlui.Submission.submit.ElsevierUploadStep.open_access");
    protected static final Message T_restricted_access =
            message("xmlui.Submission.submit.ElsevierUploadStep.restricted_access");

    protected FileAccessFromMetadataService fileAccessFromMetadataService = FileAccessServiceFactory.getInstance().getFileAccessFromMetadataService();

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
        boolean disableFileEditing = (submissionInfo.isInWorkflow()) && !ConfigurationManager.getBooleanProperty("workflow", "reviewer.file-edit");
        String bundleName = "ORIGINAL";
        java.util.List<Bundle> bundles = itemService.getBundles(item, bundleName);
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

        OpenAccessArticleCheck openAccessArticleCheck = OpenAccessArticleCheck.getInstance();
        ArticleAccess openAccess = openAccessArticleCheck.check(item);

        List upload = null;
        if (!disableFileEditing)
        {
            // Only add the upload capabilities for new item submissions
            upload = div.addList("submit-upload-new", List.TYPE_FORM);
            upload.setHead(T_head);

            if (openAccess != null) {
                String link = openAccessArticleCheck.getLink();
                if ("Public".equals(openAccess.getAudience())) {
                    upload.addLabel(T_open_access);
                }
                else {
                    upload.addLabel(T_restricted_access);
                }

                if(StringUtils.isNotBlank(link)) {
                    upload.addItem().addXref(link, link, "external");
                }
            }

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

            boolean fileAccessError = this.errorFlag == org.dspace.submit.step.ElsevierUploadStep.STATUS_NO_FIlE_ACCESS_ERROR;
            Radio radio = FileAccessUI.addAccessSelection(upload, "file-access", fileAccessError);

            FileAccessUI.addEmbargoDateField(upload,openAccess);

            if(openAccess!=null) {
                if ("Public".equals(openAccess.getAudience()) && StringUtils.isNotBlank(openAccess.getStartDate())) {
                    radio.setOptionSelected("embargo");
                } else if ("Public".equals(openAccess.getAudience())) {
                    radio.setOptionSelected("public");
                } else {
                    radio.setOptionSelected("restricted");
                }
            }

            Button uploadSubmit = upload.addItem().addButton("submit_upload");
            uploadSubmit.setValue(T_submit_upload);
        }

        make_sherpaRomeo_submission(item, div);

        // Part B:
        //  If the user has already uploaded files provide a list for the user.
        if (bitstreams.size() > 0 || disableFileEditing)
        {
            Table summary = div.addTable("submit-upload-summary", (bitstreams.size() * 2) + 2, 7);
            summary.setHead(T_head2);

            Row header = summary.addRow(Row.ROLE_HEADER);
            header.addCellContent(T_column0); // primary bitstream
            header.addCellContent(T_column1); // select checkbox
            header.addCellContent(T_column2); // file name
            header.addCellContent(T_column3); // size
            header.addCellContent(T_column4); // description
            header.addCellContent(T_column5); // format
            header.addCellContent(T_file_access);
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

                row.addCell().addXref(url,name);
                row.addCellContent(bytes + " bytes");
                if (desc == null || desc.length() == 0)
                {
                    row.addCellContent(T_unknown_name);
                }
                else
                {
                    row.addCellContent(desc);
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

                ArticleAccess fileAccess = fileAccessFromMetadataService.getFileAccess(context, bitstream);
                row.addCell().addContent(fileAccess.getAudience());

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
}
        
