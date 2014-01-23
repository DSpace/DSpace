package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.workflow.actions.processingaction.AfterPublicationAction;

public class AfterPublicationActionXMLUI extends EditMetadataActionXMLUI {
    // actions you may perform
    private static final Message T_HEAD =
            message("xmlui.Submission.workflow.AfterPublicationActionXMLUI.head");

    private static final Message T_after_blackout_help =
            message("xmlui.Submission.workflow.AfterPublicationActionXMLUI.after_blackout_help");
    private static final Message T_after_blackout_submit =
            message("xmlui.Submission.workflow.AfterPublicationActionXMLUI.after_blackout_submit");

    /*
     * For items in blackout, we reuse much of EditMetadataActionXMLUI to avoid
     * duplicating static content.  However, this action is for items already in
     * blackout, so we don't provide the same options for sending to blackout or 
     * archiving directly.
     */
    @Override
    protected void renderMainPage(Division div) throws WingException {
        Table table = div.addTable("workflow-actions", 1, 2);
        // First option is approve+exit blackout
        Row row = table.addRow();
        row.addCellContent(T_after_blackout_help);
        row.addCell().addButton("after_blackout_submit").setValue(T_after_blackout_submit);

        // Reject item
        row = table.addRow();
        row.addCellContent(T_reject_help);
        row.addCell().addButton("submit_reject").setValue(T_reject_submit);

        // Edit metadata
        row = table.addRow();
        row.addCellContent(T_edit_help);
        row.addCell().addButton("submit_edit").setValue(T_edit_submit);

        // Return to the pool
        row = table.addRow();
        row.addCellContent(T_return_help);
        row.addCell().addButton("submit_return").setValue(T_return_submit);

        row = table.addRow();
        row.addCell(0,2).addButton("submit_leave").setValue(T_cancel_submit);
        div.addHidden("submission-continue").setValue(knot.getId());
        div.addHidden("page").setValue(AfterPublicationAction.MAIN_PAGE);
    }

}