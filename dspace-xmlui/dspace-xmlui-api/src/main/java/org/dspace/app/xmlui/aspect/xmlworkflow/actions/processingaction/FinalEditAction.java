/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow.actions.processingaction;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.xmlworkflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Processing class of an action that allows users to
 * accept/reject a workflow item
 * 
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class FinalEditAction extends AbstractXMLUIAction {

    protected static final Message T_info1=
        message("xmlui.XMLWorkflow.workflow.EditMetadataAction.info1");

    private static final Message T_HEAD = message("xmlui.XMLWorkflow.workflow.EditMetadataAction.head");

    protected static final Message T_approve_help =
        message("xmlui.XMLWorkflow.workflow.EditMetadataAction.approve_help");
    protected static final Message T_approve_submit =
        message("xmlui.XMLWorkflow.workflow.EditMetadataAction.approve_submit");

    protected static final Message T_edit_help =
        message("xmlui.XMLWorkflow.workflow.EditMetadataAction.edit_help");
    protected static final Message T_edit_submit =
        message("xmlui.XMLWorkflow.workflow.EditMetadataAction.edit_submit");

    protected static final Message T_submit_cancel =
        message("xmlui.general.cancel");



    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/xmlworkflow";

        // Generate a from asking the user two questions: multiple
        // titles & published before.
    	Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead(T_HEAD);

        addWorkflowItemInformation(div, item, request);

        renderMainPage(div);

        div.addHidden("submission-continue").setValue(knot.getId());
    }

    private void renderMainPage(Division div) throws WingException {
        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);

        // Approve task
        Row row = table.addRow();
        row.addCellContent(T_approve_help);
        row.addCell().addButton("submit_approve").setValue(T_approve_submit);

        // Edit metadata
        row = table.addRow();
        row.addCellContent(T_edit_help);
        row.addCell().addButton("submit_edit").setValue(T_edit_submit);


        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue(T_submit_cancel);

        div.addHidden("page").setValue(ReviewAction.MAIN_PAGE);
    }
}
