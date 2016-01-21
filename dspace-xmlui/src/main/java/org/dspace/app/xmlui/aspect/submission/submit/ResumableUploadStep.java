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
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
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
        
        List upload = div.addList("submit-upload-new-list", List.TYPE_SIMPLE);
        submission.getID();
        
        SubmissionInfo si = FlowUtils.obtainSubmissionInfo(this.objectModel, 'S' + String.valueOf(submission.getID()));
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
        header.addCellContent(T_column2); // file name
        header.addCellContent(T_column4); // description
        header.addCellContent("Status");
        header.addCellContent("Info");
        header.addCellContent("Delete");
        
        for (Bitstream bitstream : bitstreams)
        {
            int id = bitstream.getID();
            String name = bitstream.getName();
            String url = makeBitstreamLink(item, bitstream);
            //long bytes = bitstream.getSize();
            //String desc = bitstream.getDescription();
            //String algorithm = bitstream.getChecksumAlgorithm();
            //String checksum = bitstream.getChecksum();


            Row row = summary.addRow("bitstream-" + id, "data", "bitstream-" + id);

            // Add radio-button to select this as the primary bitstream
            Radio primary = row.addCell().addRadio("primary_bitstream_id");
            primary.addOption(String.valueOf(id));

            // If this bitstream is already marked as the primary bitstream
            // mark it as such.
            if(bundles[0].getPrimaryBitstreamID() == id) {
                primary.setOptionSelected(String.valueOf(id));
            }

            row.addCell().addXref(url,name);
            
            // status
            row.addCell("status-" + id, Cell.ROLE_DATA, "file-status-success").addFigure("/", null, null);
            
            // info
            row.addCell("info-" + id, Cell.ROLE_DATA, "file-info").addFigure("/", null, null);
            
            // delete
            row.addCell("delete-" + id, Cell.ROLE_DATA, "file-delete").addFigure("/", null, null);
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
