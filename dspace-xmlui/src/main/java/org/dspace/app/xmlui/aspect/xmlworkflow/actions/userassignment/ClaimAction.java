/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow.actions.userassignment;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.xmlworkflow.AbstractXMLUIAction;
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
 * User interface for an action where x number of users
 * have to accept a task from a designated pool
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ClaimAction extends AbstractXMLUIAction {

    protected static final Message T_info1=
        message("xmlui.XMLWorkflow.workflow.ClaimAction.info1");
        protected static final Message T_take_help =
        message("xmlui.XMLWorkflow.workflow.ClaimAction.take_help");
    protected static final Message T_take_submit =
        message("xmlui.XMLWorkflow.workflow.ClaimAction.take_submit");
    protected static final Message T_leave_help =
        message("xmlui.XMLWorkflow.workflow.ClaimAction.leave_help");
    protected static final Message T_leave_submit =
        message("xmlui.XMLWorkflow.workflow.ClaimAction.leave_submit");

    protected static final Message T_workflow_head =
        message("xmlui.XMLWorkflow.workflow.ClaimAction.title");
    protected static final Message T_back_overview =
        message("xmlui.XMLWorkflow.workflow.ClaimAction.back");


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/xmlworkflow";

        Request request = ObjectModelHelper.getRequest(objectModel);
        String showfull = request.getParameter("submit_full_item_info");

        // if the user selected showsimple, remove showfull.
        if (showfull != null && request.getParameter("submit_simple_item_info") != null)
            showfull = null;

        // Generate a form asking the user two questions: multiple
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
