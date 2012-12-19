package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.actions.Action;
import org.dspace.workflow.actions.processingaction.AcceptAction;
import org.dspace.workflow.actions.processingaction.EditMetadataAction;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 10-aug-2010
 * Time: 10:49:39
 */
public class EditMetadataActionXMLUI extends AbstractXMLUIAction {


    protected static final Message T_info1=
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.info1");

    protected static final Message T_info2=
            message("xmlui.Submission.workflow.RejectTaskStep.info1");


    private static final Message T_HEAD = message("xmlui.Submission.workflow.EditMetadataActionXMLUI.head");

    protected static final Message T_approve_help =
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.approve_help");
    protected static final Message T_approve_submit =
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.approve_submit");
    protected static final Message T_reject_help =
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.reject_help");
    protected static final Message T_reject_submit =
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.reject_submit");

    protected static final Message T_edit_help =
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.edit_help");
    protected static final Message T_edit_submit =
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.edit_submit");

    protected static final Message T_delete_help =
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.delete_help");
    protected static final Message T_delete_submit =
            message("xmlui.Submission.workflow.EditMetadataActionXMLUI.delete_submit");

    /** Reject page messages **/
    protected static final Message T_reason =
            message("xmlui.Submission.workflow.RejectTaskStep.reason");
    protected static final Message T_submit_reject =
            message("xmlui.Submission.workflow.RejectTaskStep.submit_reject");
    protected static final Message T_reason_required =
            message("xmlui.Submission.workflow.RejectTaskStep.reason_required");

    protected static final Message T_submit_cancel =
            message("xmlui.general.cancel");




    protected static final Message T_workflow_head =
            message("xmlui.Submission.general.workflow.head");
    protected static final Message T_cancel_submit =
            message("xmlui.general.cancel");


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";

        //Retrieve our pagenumber
        int page = AcceptAction.MAIN_PAGE;
        if(request.getAttribute("page") != null){
            page = Integer.parseInt(request.getAttribute("page").toString());
        }

        // Generate a from asking the user two questions: multiple
        // titles & published before.
        Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead(T_HEAD);

        addWorkflowItemInformation(div, item, request);

        switch (page){
            case EditMetadataAction.MAIN_PAGE:
                renderMainPage(div);
                break;
            case AcceptAction.REJECT_PAGE:
                renderRejectPage(div);
                break;
        }

        div.addHidden("submission-continue").setValue(knot.getId());
    }

    private void renderMainPage(Division div) throws WingException {
        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);


        //TODO: if we have a last task change button name to archive !
        // Approve task
        Row row = table.addRow();
        row.addCellContent(T_approve_help);
        row.addCell().addButton("submit_approve").setValue(T_approve_submit);

        // Reject item
        row = table.addRow();
        row.addCellContent(T_reject_help);
        row.addCell().addButton("submit_reject").setValue(T_reject_submit);


        // Edit metadata
        row = table.addRow();
        row.addCellContent(T_edit_help);
        row.addCell().addButton("submit_edit").setValue(T_edit_submit);


        // Delete versioned tem
        Item item = workflowItem.getItem();

        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        VersionHistory history = versioningService.findVersionHistory(context, item.getID());
        if(history != null){
            Version itemVersion = history.getVersion(item);
            if(itemVersion != null)
            {
                row = table.addRow();
                row.addCellContent(T_delete_help);
                row.addCell().addButton("submit_remove").setValue(T_delete_submit);

            }
        }

        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue(T_cancel_submit);

        div.addHidden("page").setValue(AcceptAction.MAIN_PAGE);
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


        div.addHidden("page").setValue(AcceptAction.REJECT_PAGE);

        org.dspace.app.xmlui.wing.element.Item actions = form.addItem();
        actions.addButton("submit_reject").setValue(T_submit_reject);
        actions.addButton("submit_cancel").setValue(T_submit_cancel);

    }
}
