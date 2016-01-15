package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

public class ResumableUploadStep extends UploadStep{
    
    private static final Logger log = Logger.getLogger(ResumableUploadStep.class);
    
    public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
    { 
        Division div = body.addDivision("submit-upload");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);
        
        List upload = div.addList("submit-upload-new", List.TYPE_SIMPLE);
        //upload.setHead(T_head);
        upload.setHead("jings");
        
        //submissionInfo.get
        submission.getID();
        
        log.info("-------------------------------");
        log.info(submission);
        log.info(submissionInfo.getSubmissionItem().getID());
        log.info(submission.getID());
        
        SubmissionInfo si = FlowUtils.obtainSubmissionInfo(this.objectModel, String.valueOf(submission.getID()));
        log.info(si);
        SubmissionInfo si2 = FlowUtils.obtainSubmissionInfo(this.objectModel, String.valueOf(submissionInfo.getSubmissionItem().getID()));
        log.info(si2.getSubmissionItem().getID());

        div.addHidden("submit-id").setValue(submission.getID());
        
        // add standard control/paging buttons
        addControlButtons(upload);
    }
    
    public void addPageMeta(PageMeta pageMeta) throws WingException,
        SAXException, SQLException, AuthorizeException, IOException
    {
        //super.addPageMeta(pageMeta);
        //this.
        pageMeta.addMetadata("javascript", "static").addContent("static/js/upload-resumable.js");
        pageMeta.addMetadata("jings", "crivvens");
    }

    @Override
    public List addReviewSection(List reviewList)
            throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        return super.addReviewSection(reviewList);
    }
}
