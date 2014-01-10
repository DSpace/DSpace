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
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.xmlworkflow.state.actions.Action;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User interface class of an accept/reject action
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ReviewAction extends AbstractXMLUIAction {

    private static final Message T_HEAD = message("xmlui.XMLWorkflow.workflow.AcceptAction.head");

    /** Main page messages **/
    protected static final Message T_info1=
        message("xmlui.XMLWorkflow.workflow.AcceptAction.info1");

    protected static final Message T_info2=
        message("xmlui.Submission.workflow.RejectTaskStep.info1");

    protected static final Message T_approve_help =
        message("xmlui.XMLWorkflow.workflow.AcceptAction.approve_help");
    protected static final Message T_approve_submit =
        message("xmlui.XMLWorkflow.workflow.AcceptAction.approve_submit");
    protected static final Message T_reject_help =
        message("xmlui.XMLWorkflow.workflow.AcceptAction.reject_help");
    protected static final Message T_reject_submit =
        message("xmlui.XMLWorkflow.workflow.AcceptAction.reject_submit");
    protected static final Message T_reason_required =
        message("xmlui.Submission.workflow.RejectTaskStep.reason_required");

    /** Reject page messages **/
    protected static final Message T_reason =
        message("xmlui.Submission.workflow.RejectTaskStep.reason");
    protected static final Message T_submit_reject =
        message("xmlui.Submission.workflow.RejectTaskStep.submit_reject");

    protected static final Message T_submit_cancel =
        message("xmlui.general.cancel");



    protected static final Message T_cancel_submit =
        message("xmlui.general.cancel");


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/xmlworkflow";

        //Retrieve our pagenumber
        int page = org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction.MAIN_PAGE;
        if(request.getAttribute("page") != null){
            page = Integer.parseInt(request.getAttribute("page").toString());
        }

        // Generate a from asking the user two questions: multiple
        // titles & published before.
    	Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead(T_HEAD);

        addWorkflowItemInformation(div, item, request);


        switch (page){
            case org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction.MAIN_PAGE:
                renderMainPage(div);
                break;
            case org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction.REJECT_PAGE:
                renderRejectPage(div);
                break;
        }

        div.addHidden("submission-continue").setValue(knot.getId());

    }

    private void renderMainPage(Division div) throws WingException {
        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);

        // Approve task
        Row row = table.addRow();
        row.addCellContent(T_approve_help);
        row.addCell().addButton("submit_approve").setValue(T_approve_submit);

        // Reject item
        row = table.addRow();
        row.addCellContent(T_reject_help);
        row.addCell().addButton("submit_reject").setValue(T_reject_submit);


        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue(T_cancel_submit);

        div.addHidden("page").setValue(org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction.MAIN_PAGE);
    }

    private void renderRejectPage(Division div) throws WingException {
        Request request = ObjectModelHelper.getRequest(objectModel);

        List form = div.addList("reject-workflow",List.TYPE_FORM);

        form.addItem(T_info2);

        TextArea reason = form.addItem().addTextArea("reason");
        reason.setLabel(T_reason);
        reason.setRequired();
        reason.setSize(15, 50);

        if (Action.getErrorFields(request).contains("reason"))
        	reason.addError(T_reason_required);


        div.addHidden("page").setValue(org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction.REJECT_PAGE);

        org.dspace.app.xmlui.wing.element.Item actions = form.addItem();
        actions.addButton("submit_reject").setValue(T_submit_reject);
        actions.addButton("submit_cancel").setValue(T_submit_cancel);

    }
}
