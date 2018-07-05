package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

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
    private static final Message T_save_changes = message("xmlui.general.save");
    private static final Message T_head_has_part = message("xmlui.ArtifactBrowser.ItemViewer.head_hasPart");
    private static final Message T_STEPS_HEAD_1 = message("xmlui.Submission.submit.OverviewStep.steps.1");
    private static final Message T_STEPS_HEAD_2 = message("xmlui.Submission.submit.OverviewStep.steps.2");


    @Override
     public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        org.dspace.content.Item publication = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";

    	Division mainDiv = body.addInteractiveDivision("review-submission-overview", actionURL, Division.METHOD_POST, "review submission");
        mainDiv.setHead("Review submission");

        Division actionsDiv = mainDiv.addDivision("review-actions");
        //TODO: add to msgs.props


        //First of all add all the publication info
        Division pubDiv = actionsDiv.addDivision("puboverviewdivision", "odd subdiv");
        pubDiv.setHead(T_STEPS_HEAD_1);

        ReferenceSet referenceSet = pubDiv.addReferenceSet("collection-viewer", ReferenceSet.TYPE_SUMMARY_VIEW);
        org.dspace.app.xmlui.wing.element.Reference itemRef = referenceSet.addReference(publication);

        //Second add info & edit/delete buttons for all our data files
        Division dataDiv = actionsDiv.addDivision("dataoverviewdivision", "even subdiv");
        dataDiv.setHead(T_STEPS_HEAD_2);

        Table dataSetList = dataDiv.addTable("datasets", 1, 3, "datasets");
        Cell addCell = dataSetList.addRow().addCell("add_file", null, 1, 3, "add_file");
        addCell.addButton("submit_adddataset").setValue(T_BUTTON_DATAFILE_ADD);

        HashMap<Integer, Integer> fileStatuses = new HashMap<Integer, Integer>();
        DCValue[] fileStatusDCVs = publication.getMetadata("workflow.review.fileStatus");
        if (fileStatusDCVs != null && fileStatusDCVs.length > 0) {
            for (DCValue dcValue : fileStatusDCVs) {
                fileStatuses.put(Integer.valueOf(dcValue.value), dcValue.confidence);
            }
        }

        org.dspace.content.Item[] datasets = DryadWorkflowUtils.getDataFiles(context, publication);
        for (org.dspace.content.Item dataset : datasets) {
            int fileStatus = Choices.CF_ACCEPTED;
            if (fileStatuses.containsKey(dataset.getID())) {
                fileStatus = fileStatuses.get(dataset.getID());
            } else {
                publication.addMetadata("workflow.review.fileStatus", Item.ANY, String.valueOf(dataset.getID()), "USER", fileStatus);
                publication.updateMetadata();
            }
            //Our current item has already been added
            InProgressSubmission wfi;
            wfi = WorkflowItem.findByItemId(context, dataset.getID());
            if(wfi != null){
                 Cell actionCell = FlowUtils.renderDatasetItem(context, dataSetList, dataset);
                 Radio fileButtons = actionCell.addRadio("filestatus_" + dataset.getID(), "filestatus");
                 fileButtons.addOption(Choices.CF_ACCEPTED, "Keep");
                 fileButtons.addOption(Choices.CF_REJECTED, "Delete");
                 switch (fileStatus) {
                     case Choices.CF_REJECTED:
                         fileButtons.setOptionSelected(Choices.CF_REJECTED);
                         break;
                     case Choices.CF_ACCEPTED:
                         fileButtons.setOptionSelected(Choices.CF_ACCEPTED);
                         break;
                     default:
                         break;
                 }
            }
        }

        Para buttonsPara = actionsDiv.addPara();
        buttonsPara.addButton("submit_leave").setValue(T_cancel_submit);
        buttonsPara.addButton("save_review_changes").setValue(T_save_changes);

        mainDiv.addHidden("submission-continue").setValue(knot.getId());
    }
}
