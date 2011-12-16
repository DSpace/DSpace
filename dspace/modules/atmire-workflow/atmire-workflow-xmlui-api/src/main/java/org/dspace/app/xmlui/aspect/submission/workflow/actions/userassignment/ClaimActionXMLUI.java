package org.dspace.app.xmlui.aspect.submission.workflow.actions.userassignment;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
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
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 3-aug-2010
 * Time: 16:43:57
 * To change this template use File | Settings | File Templates.
 */
public class ClaimActionXMLUI extends AbstractXMLUIAction {

    protected static final Message T_info1=
        message("xmlui.Submission.workflow.ClaimActionXMLUI.info1");
        protected static final Message T_take_help =
        message("xmlui.Submission.workflow.ClaimActionXMLUI.take_help");
    protected static final Message T_take_submit =
        message("xmlui.Submission.workflow.ClaimActionXMLUI.take_submit");
    protected static final Message T_leave_help =
        message("xmlui.Submission.workflow.ClaimActionXMLUI.leave_help");
    protected static final Message T_leave_submit =
        message("xmlui.Submission.workflow.ClaimActionXMLUI.leave_submit");

    protected static final Message T_workflow_head =
        message("xmlui.Submission.workflow.ClaimActionXMLUI.title");
    protected static final Message T_showfull =
        message("xmlui.Submission.general.showfull");
    protected static final Message T_showsimple =
        message("xmlui.Submission.general.showsimple");
    protected static final Message T_back_overview =
        message("xmlui.Submission.workflow.ClaimActionXMLUI.back");


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";

        Request request = ObjectModelHelper.getRequest(objectModel);
        String showfull = request.getParameter("submit_full_item_info");

        // if the user selected showsimple, remove showfull.
        if (showfull != null && request.getParameter("submit_simple_item_info") != null)
            showfull = null;

        // Generate a from asking the user two questions: multiple
        // titles & published before.
    	Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead(T_workflow_head);


        addWorkflowItemInformation(div, item, request);

        
        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);

        // Take task
        Row row = table.addRow();
        row.addCellContent(T_take_help);
        row.addCell().addButton("submit_take_task").setValue(T_take_submit);

        // Leave task
        row = table.addRow();
        row.addCellContent(T_leave_help);
        row.addCell().addButton("submit_leave").setValue(T_leave_submit);

        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue(T_back_overview);

        div.addHidden("submission-continue").setValue(knot.getId());

    }
}
