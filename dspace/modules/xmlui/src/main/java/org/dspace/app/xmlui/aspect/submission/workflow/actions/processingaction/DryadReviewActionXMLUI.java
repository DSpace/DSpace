package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 18-aug-2010
 * Time: 10:42:33
 *
 * The user interface for the review stage
 */
public class DryadReviewActionXMLUI extends AbstractXMLUIAction {

    private static final Message T_BUTTON_DATAFILE_ADD = message("xmlui.Submission.submit.OverviewStep.button.add-datafile");
    private static final Message T_ADD_DATAFILE_help = message("xmlui.Submission.submit.OverviewStep.help.add-datafile");

    protected static final Message T_info1= message("xmlui.Submission.workflow.DryadReviewActionXMLUI.info1");

    protected static final Message T_cancel_submit = message("xmlui.general.cancel");


    @Override
     public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";

        //Retrieve our pagenumber
        // Generate a from asking the user two questions: multiple
        // titles & published before.
    	Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        //TODO: add to msgs.props
        div.setHead("Review submission");

        addWorkflowItemInformation(div, item, request);


        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);

        // Approve task
        Row row = table.addRow();
        row.addCellContent(T_ADD_DATAFILE_help);
        row.addCell().addButton("submit_adddataset").setValue(T_BUTTON_DATAFILE_ADD);

        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue(T_cancel_submit);


        div.addHidden("submission-continue").setValue(knot.getId());
    }
}
