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

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
public class ResumableUploadStep extends UploadStep{
    
    protected static final Message T_file_help =
            message("xmlui.Submission.submit.ResumableUploadStep.file_help");
    private static final Message T_column_select =
            message("xmlui.Submission.submit.ResumableUploadStep.select");
    private static final Message T_column_status =
            message("xmlui.Submission.submit.ResumableUploadStep.status");
    private static final Message T_column_info =
            message("xmlui.Submission.submit.ResumableUploadStep.info");
    private static final Message T_column_delete =
            message("xmlui.Submission.submit.ResumableUploadStep.delete");
    
    private static final Message T_clickdrop =
            message("xmlui.Submission.submit.ResumableUploadStep.clickdrop");

    private static final Message T_delete_message =
            message("xmlui.Submission.submit.ResumableUploadStep.dialog.delete");
    private static final Message T_deletesf_message =
            message("xmlui.Submission.submit.ResumableUploadStep.dialog.deletesf");
    private static final Message T_deleteunmatch_message =
            message("xmlui.Submission.submit.ResumableUploadStep.dialog.deleteunmatch");

    private static final Message T_upload_error =
            message("xmlui.Submission.submit.ResumableUploadStep.uploaderror");
    private static final Message T_create_error =
            message("xmlui.Submission.submit.ResumableUploadStep.createerror");
    private static final Message T_no_space =
            message("xmlui.Submission.submit.ResumableUploadStep.nospace");

    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.aspect.submission.submit.UploadStep#addBody(org.dspace.app.xmlui.wing.element.Body)
     */
    public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
    {
        super.addBody(body);

        boolean disableFileEditing = (submissionInfo.isInWorkflow()) &&
                !ConfigurationManager.getBooleanProperty("workflow", "reviewer.file-edit");
        if (!disableFileEditing){

            Item item = submission.getItem();
            Collection collection = submission.getCollection();

            String actionURL = contextPath + "/handle/" + collection.getHandle() +
                    "/submit/" + knot.getId() + ".continue?" +
                    org.dspace.submit.step.ResumableUploadStep.RESUMABLE_PARAM + "=true";
            Division div = body.addInteractiveDivision(
                    "resumable-upload",
                    actionURL,
                    Division.METHOD_POST,
                    "primary submission");
            div.setHead(T_submission_head);
            addSubmissionProgressList(div);

            Division uploadDiv = div.addDivision("submit-file-upload");
            uploadDiv.addPara(T_file_help);            
            
            // click / drag and drop area
            Division drop = uploadDiv.addDivision("resumable-drop", "col-md-12");
            drop.addPara();
            drop.addPara(T_clickdrop);

            // progress bar and button
            uploadDiv.addDivision("progress-button");
            Division progressDiv = uploadDiv.addDivision("progress");
            progressDiv.addDivision("progress-bar");

            List upload = div.addList("submit-upload-new-list", List.TYPE_SIMPLE);

            div.addHidden("submit-id").setValue(submission.getID());

            Bundle[] bundles = item.getBundles("ORIGINAL");
            Bitstream[] bitstreams = new Bitstream[0];
            if (bundles.length > 0)
            {
                bitstreams = bundles[0].getBitstreams();
            }

            Table summary = uploadDiv.addTable("resumable-upload-summary",(bitstreams.length * 2) + 2,7);
            summary.setHead(T_head2);

            Row header = summary.addRow(Row.ROLE_HEADER);
            header.addCellContent(T_column0); // primary bitstream
            header.addCellContent(T_column_select);
            header.addCellContent(T_column2); // file name
            header.addCellContent(T_column4); // description
            header.addCellContent(T_column_status);
            header.addCellContent(T_column_info);
            header.addCellContent(T_column_delete);

            for (Bitstream bitstream : bitstreams)
            {
                int id = bitstream.getID();
                Row row = summary.addRow("bitstream-" + id, "data", "resumable-bitstream");

                // Add radio-button to select this as the primary bitstream
                Radio primary = row.addCell("primary-" + id, Cell.ROLE_DATA, "file-primary").addRadio("primary_bitstream_id");
                primary.addOption(String.valueOf(id));

                // If this bitstream is already marked as the primary bitstream
                // mark it as such.
                if(bundles[0].getPrimaryBitstreamID() == id) {
                    primary.setOptionSelected(String.valueOf(id));
                }

                // select file
                CheckBox select = row.addCell("select-" + id, Cell.ROLE_DATA, "file-select").addCheckBox("select");
                select.addOption(String.valueOf(id));

                String url = makeBitstreamLink(item, bitstream);
                row.addCell().addXref(url, bitstream.getName());

                // description
                row.addCell().addText("description-" + id).setValue(bitstream.getDescription());

                // status
                row.addCell("status-" + id, Cell.ROLE_DATA, "file-status-success");

                // info
                Cell info = row.addCell("info-" + id, Cell.ROLE_DATA, "file-info");
                info.addHidden("file-extra-bytes-" + id).setValue(String.valueOf(bitstream.getSize()));
                info.addHidden("file-extra-format-" + id).setValue(bitstream.getFormatDescription());
                info.addHidden("file-extra-algorithm-" + id).setValue(bitstream.getChecksumAlgorithm());
                info.addHidden("file-extra-checksum-" + id).setValue(bitstream.getChecksum());

                // delete
                row.addCell("delete-" + id, Cell.ROLE_DATA, "file-delete");
            }

            // some messages and that the client needs
            Division messages = div.addDivision("text-messages", "hide");
            messages.addHidden("text-delete-msg").setValue(T_delete_message);
            messages.addHidden("text-delete-sf").setValue(T_deletesf_message);
            messages.addHidden("text-delete-unmatch").setValue(T_deleteunmatch_message);

            // some item related data
            Division data = div.addDivision("item-data", "hide");
            long max = ConfigurationManager.getLongProperty("upload.item.max", 536870912);
            data.addHidden("item-space").setValue(Long.toString(Math.max(max - item.getSize(), 0)));
            
            // error messages
            uploadDiv.addPara("upload-failed", "alert alert-danger hide").addContent(T_upload_error);
            uploadDiv.addPara("create-failed", "alert alert-danger hide").addContent(T_create_error);
            uploadDiv.addPara("no-space", "alert alert-danger hide").addContent(T_no_space.parameterize(max));
            
            if(this.errorFlag == org.dspace.submit.step.UploadStep.STATUS_NO_FILES_ERROR)
            {
                uploadDiv.addPara("files-missing", "alert alert-danger").addContent(T_file_error);
            }

            // add standard control/paging buttons
            addControlButtons(upload);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.aspect.submission.AbstractStep#addPageMeta(org.dspace.app.xmlui.wing.element.PageMeta)
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException,
        SAXException, SQLException, AuthorizeException, IOException
    {
        super.addPageMeta(pageMeta);
        pageMeta.addMetadata("javascript", "static").addContent("static/js/upload-resumable.js");
    }

    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.aspect.submission.submit.UploadStep#addReviewSection(org.dspace.app.xmlui.wing.element.List)
     */
    @Override
    public List addReviewSection(List reviewList)
            throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        return super.addReviewSection(reviewList);
    }
}
