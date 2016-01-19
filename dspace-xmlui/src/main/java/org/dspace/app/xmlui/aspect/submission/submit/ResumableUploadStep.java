package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

public class ResumableUploadStep extends UploadStep{
    
    private static final Logger log = Logger.getLogger(ResumableUploadStep.class);
    
    public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
    { 
        Item item = submission.getItem();
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        Division div = body.addInteractiveDivision("submit-upload", actionURL, Division.METHOD_POST, "primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);
        
        
        Division progressDiv = div.addDivision("submit-file-upload");
        
        
        //Division div = body.addDivision("submit-upload-new");
        List upload = div.addList("submit-upload-new-list", List.TYPE_SIMPLE);
        //upload.setHead(T_head);
        //upload.setHead("jings");
        
        //submissionInfo.get
        submission.getID();
        
        log.info("-------------------------------");
        log.info(submission);
        log.info(submissionInfo.getSubmissionItem().getID());
        log.info(submission.getID());
        
        SubmissionInfo si = FlowUtils.obtainSubmissionInfo(this.objectModel, 'S' + String.valueOf(submission.getID()));
        log.info(si.getSubmissionItem().getID());

        div.addHidden("submit-id").setValue(submission.getID());
        
        Bundle[] bundles = item.getBundles("ORIGINAL");
        Bitstream[] bitstreams = new Bitstream[0];
        if (bundles.length > 0)
        {
            bitstreams = bundles[0].getBitstreams();
        }
        
        Table summary = progressDiv.addTable("submit-upload-summary",(bitstreams.length * 2) + 2,7);
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
            int id = bitstream.getID();
            String name = bitstream.getName();
            String url = makeBitstreamLink(item, bitstream);
            long bytes = bitstream.getSize();
            String desc = bitstream.getDescription();
            String algorithm = bitstream.getChecksumAlgorithm();
            String checksum = bitstream.getChecksum();


            Row row = summary.addRow("bitstream-" + id, "data", "bitstream-" + id);

            // Add radio-button to select this as the primary bitstream
            Radio primary = row.addCell().addRadio("primary_bitstream_id");
            primary.addOption(String.valueOf(id));

            // If this bitstream is already marked as the primary bitstream
            // mark it as such.
            if(bundles[0].getPrimaryBitstreamID() == id) {
                primary.setOptionSelected(String.valueOf(id));
            }

            row.addCell();

            row.addCell().addXref(url,name);
            //row.addCellContent(bytes + " bytes");
            row.addCell().addHidden("bytes").setValue(bytes + " bytes");
            
            Text description = row.addCell().addText("description");
            if (desc != null && desc.length() > 0)
            {
                //row.addCellContent(T_unknown_name);
                //row.addCell().addText("description");
                description.setValue(desc);
            }
            /*else
            {
                //row.addCellContent(desc);
                row.addCell().addText(name);
            }*/
            

            BitstreamFormat format = bitstream.getFormat();
            if (format == null)
            {
                //row.addCellContent(T_unknown_format);
                row.addCell().addHidden("bitstream-format").setValue(T_unknown_format);
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

            //Button edit = row.addCell().addButton("submit_edit_"+id);
            //edit.setValue(T_submit_edit);

            /*Row checksumRow = summary.addRow();
            checksumRow.addCell();
            Cell checksumCell = checksumRow.addCell(null, null, 0, 6, null);
            checksumCell.addHighlight("bold").addContent(T_checksum);
            checksumCell.addContent(" ");
            checksumCell.addContent(algorithm + ":" + checksum);*/
        }
        
        // add standard control/paging buttons
        addControlButtons(upload);
    }
    
    public void addPageMeta(PageMeta pageMeta) throws WingException,
        SAXException, SQLException, AuthorizeException, IOException
    {
        pageMeta.addMetadata("javascript", "static").addContent("static/js/upload-resumable.js");
        pageMeta.addMetadata("jings", "crivvens");
    }

    @Override
    public List addReviewSection(List reviewList)
            throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        return super.addReviewSection(reviewList);
    }
}
