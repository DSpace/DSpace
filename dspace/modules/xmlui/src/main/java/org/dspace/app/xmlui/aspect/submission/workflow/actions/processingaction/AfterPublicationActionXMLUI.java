package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.xml.sax.SAXException;



public class AfterPublicationActionXMLUI extends AbstractXMLUIAction {
    // actions you may perform
    private static final Message T_HEAD =
            message("xmlui.Submission.workflow.AfterPublicationActionXMLUI.head");

    private static final Message T_after_blackout_help =
            message("xmlui.Submission.workflow.AfterPublicationActionXMLUI.after_blackout_help");
    private static final Message T_after_blackout_submit =
            message("xmlui.Submission.workflow.AfterPublicationActionXMLUI.after_blackout_submit");
    private static final Message T_cancel_submit =
            message("xmlui.general.cancel");

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";


        // Generate a from asking the user two questions: multiple
        // titles & published before.
        Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead(T_HEAD);
        Table table = div.addTable("workflow-actions", 1, 2);

        // One option: send to archive
        Row row = table.addRow();
        row.addCellContent(T_after_blackout_help);
        row.addCell().addButton("after_blackout_submit").setValue(T_after_blackout_submit);

        row = table.addRow();
        row.addCell(0,2).addButton("after_blackout_leave").setValue(T_cancel_submit);
        div.addHidden("submission-continue").setValue(knot.getId());
    }

}