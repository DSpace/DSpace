package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;


/**
 * User: kevin (kevin at atmire.com)
 * Date: 14-sep-2011
 * Time: 15:53:58
 */
public class InternalItemTransformer extends AbstractDSpaceTransformer {

    private static Logger log = Logger.getLogger(InternalItemTransformer.class);

    private static final int ACTION_TO_POOL = 1;
    private static final int ACTION_TO_SUBMISSION = 2;
    private static final int ACTION_TO_STEP = 3;
    private static final int ACTION_WITHDRAW = 4;

    private static final Message T_context_head = message("xmlui.administrative.Navigation.context_head");
    private static final Message T_context_edit_item = message("xmlui.administrative.Navigation.context_edit_item");
    
    private static final Message T_show_simple = message("xmlui.ArtifactBrowser.ItemViewer.show_simple");

    private static final Message T_show_full = message("xmlui.ArtifactBrowser.ItemViewer.show_full");

    private static final Message T_head_has_part = message("xmlui.ArtifactBrowser.ItemViewer.head_hasPart");

    private static final Message T_head_parent_collections = message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_submit_cancel = message("xmlui.general.cancel");
    private static final Message T_details_head = message("xmlui.discovery.InternalItemTransformer.details");
    private static final Message T_details_submission_title = message("xmlui.discovery.InternalItemTransformer.details.submission-title");
    private static final Message T_details_submitter = message("xmlui.discovery.InternalItemTransformer.details.submitter-name");
    private static final Message T_details_status = message("xmlui.discovery.InternalItemTransformer.details.status");
    private static final Message T_details_status_submission = message("xmlui.discovery.InternalItemTransformer.details.status.submission");
    private static final Message T_details_status_workflow = message("xmlui.discovery.InternalItemTransformer.details.status.workflow");
    private static final Message T_details_current_step = message("xmlui.discovery.InternalItemTransformer.details.current-step");
    private static final Message T_details_status_withdrawn = message("xmlui.discovery.InternalItemTransformer.details.status.withdrawn");
    private static final Message T_details_task_owner = message("xmlui.discovery.InternalItemTransformer.details.task-owner");
    private static final Message T_details_task_owner_pool = message("xmlui.discovery.InternalItemTransformer.details.task-owner.pool");
    private static final Message T_details_unknown = message("xmlui.discovery.InternalItemTransformer.unknown");
    private static final Message T_action_pool_head = message("xmlui.discovery.InternalItemTransformer.action.pool.head");
    private static final Message T_action_pool_help = message("xmlui.discovery.InternalItemTransformer.action.pool.help");
    private static final Message T_action_pool_confirm = message("xmlui.discovery.InternalItemTransformer.action.pool.confirm");
    private static final Message T_action_submitter_head = message("xmlui.discovery.InternalItemTransformer.action.submitter.head");
    private static final Message T_action_submitter_help = message("xmlui.discovery.InternalItemTransformer.action.submitter.help");
    private static final Message T_action_submitter_confirm = message("xmlui.discovery.InternalItemTransformer.action.submitter.confirm");
    private static final Message T_action_withdraw_head = message("xmlui.discovery.InternalItemTransformer.action.withdraw.head");
    private static final Message T_action_withdraw_help = message("xmlui.discovery.InternalItemTransformer.action.withdraw.help");
    private static final Message T_action_withdraw_confirm = message("xmlui.discovery.InternalItemTransformer.action.withdraw.confirm");
    private static final Message T_option_pool = message("xmlui.discovery.InternalItemTransformer.option.pool");
    private static final Message T_option_submitter = message("xmlui.discovery.InternalItemTransformer.option.submitter");
    private static final Message T_option_workflow = message("xmlui.discovery.InternalItemTransformer.option.workflow");
    private static final Message T_option_withdraw = message("xmlui.discovery.InternalItemTransformer.option.withdraw");
    private static final Message T_non_archived_trail = message("xmlui.discovery.InternalItemTransformer.trail.non-archived");
    private static final Message T_internal_trail = message("xmlui.discovery.InternalItemTransformer.trail.internal");
    private static final Message T_action_alter_step_head = message("xmlui.discovery.InternalItemTransformer.action.alter-step.head");
    private static final Message T_action_alter_step_help = message("xmlui.discovery.InternalItemTransformer.action.alter-step.help");
    private static final Message T_action_alter_step_confirm = message("xmlui.discovery.InternalItemTransformer.action.alter-step.confirm");
    private static final Message T_workflow_finalize = message("xmlui.discovery.InternalItemTransformer.workflow.finalize");
    private static final Message T_workflow_continue = message("xmlui.discovery.InternalItemTransformer.workflow.continue");
    private static final Message T_workflow_none = message("xmlui.discovery.InternalItemTransformer.workflow.none");
    private static final Message T_workflow_task_claim = message("xmlui.discovery.InternalItemTransformer.workflow.task.claim");
    private static final Message T_workflow_task_perform = message("xmlui.discovery.InternalItemTransformer.workflow.task.perform");
    private static final Message T_workflow_task_waiting = message("xmlui.discovery.InternalItemTransformer.workflow.task.wait");
    private static final Message T_workflow_task = message("xmlui.discovery.InternalItemTransformer.workflow.task");
    private static final Message T_in_workflow = message("xmlui.DryadItemSummary.in_workflow");
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item;
        Request request = ObjectModelHelper.getRequest(objectModel);
        int itemId = Util.getIntParameter(request, "itemID");
        item = Item.find(context, itemId);

        pageMeta.addMetadata("title").addContent(getTitle(item));

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/submissions", T_non_archived_trail);
        pageMeta.addTrail().addContent(T_internal_trail);
    }

    private String getTitle(Item item) {
        String title = item.getName();
        if(title == null){
            try {
                title = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e) {
                title = "Untitled";
            }
        }
        return title;
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item;
        Context context = ContextUtil.obtainContext(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);
        //Attempt to retrieve our item !
        int itemId = Util.getIntParameter(request, "itemID");
        item = Item.find(context, itemId);

        //Ensure that we have read access, else throw an authorizeException !
        if(item == null || !AuthorizeManager.authorizeActionBoolean(context, item, Constants.READ)){
            throw new AuthorizeException();
        }

        Division division = body.addInteractiveDivision("item-view", contextPath + "/internal-item", Division.METHOD_POST, "primary");
        division.setHead(getTitle(item));



        // Adding message for withdrawn or workflow item
        addWarningMessage(item, division);

		Para showfullPara = division.addPara(null,
				"item-view-toggle item-view-toggle-top");

		if (showFullItem(objectModel)) {
			String link = contextPath + "/handle/" + item.getHandle();
			showfullPara.addXref(link).addContent(T_show_simple);
		}
		else {
			String link = contextPath + "/handle/" + item.getHandle()
					+ "?show=full";
			showfullPara.addXref(link).addContent(T_show_full);
		}


        ReferenceSet referenceSet;
        if (showFullItem(objectModel)) {
            referenceSet = division.addReferenceSet("collection-viewer",ReferenceSet.TYPE_DETAIL_VIEW);
        }
        else {
            referenceSet = division.addReferenceSet("collection-viewer",ReferenceSet.TYPE_SUMMARY_VIEW);
        }

        /*
         * reference any isPartOf items to create listing...
         */
        org.dspace.app.xmlui.wing.element.Reference itemRef = referenceSet.addReference(item);

        if (item.getMetadata("dc.relation.haspart").length > 0){
            ReferenceSet hasParts;
            hasParts = itemRef.addReferenceSet("embeddedView", null, "hasPart");
            hasParts.setHead(T_head_has_part);

            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, item);

            for(Item obj : dataFiles){
                hasParts.addReference(obj);
            }
        }

        ReferenceSet appearsInclude = itemRef.addReferenceSet(ReferenceSet.TYPE_DETAIL_LIST, null, "hierarchy");
        appearsInclude.setHead(T_head_parent_collections);

        //Reference all collections the item appears in.
        for (Collection collection : item.getCollections()) {
            appearsInclude.addReference(collection);
        }

        InProgressSubmission inProgressSubmission;
        //Attempt to resolve our item to a workflow/workspace item !
        inProgressSubmission = WorkspaceItem.findByItemId(context, itemId);
        if(inProgressSubmission == null){
            //Perhaps we have a workflow item ?
            inProgressSubmission = WorkflowItem.findByItemId(context, itemId);
        }

        //Add a new division containing the workflow/workspace details !
        List workflowDetails = division.addList("workflow-details");
        workflowDetails.setHead(T_details_head);

        workflowDetails.addLabel(T_details_submission_title);
        workflowDetails.addItem().addContent(item.getName());

        EPerson submitter = item.getSubmitter();
        if(submitter != null){
            workflowDetails.addLabel(T_details_submitter);
            workflowDetails.addItem().addXref("mailto:" + submitter.getEmail(), submitter.getFullName());
        }
        workflowDetails.addLabel(T_details_status);

        String currentStep = null;
        if(inProgressSubmission != null){
            currentStep = addInProgressDetails(context, inProgressSubmission, workflowDetails, currentStep);
        }else{
            //Our item is withdrawn !
            workflowDetails.addItem().addContent(T_details_status_withdrawn);
        }

        addActions(request, itemId, division, inProgressSubmission, currentStep);

    }

    private void addActions(Request request, int itemId, Division division, InProgressSubmission inProgressSubmission, String currentStep) throws WingException, IOException {
        int action = Util.getIntParameter(request, "action");
        switch (action){
            case ACTION_TO_POOL:
                List toPoolAction = division.addList("submitter_pool", List.TYPE_FORM);
                toPoolAction.setHead(T_action_pool_head);

                toPoolAction.addItem(T_action_pool_help);

                org.dspace.app.xmlui.wing.element.Item poolButtons = toPoolAction.addItem();
                poolButtons.addButton("submit_back_pool").setValue(T_action_pool_confirm);
                poolButtons.addButton("submit_cancel").setValue(T_submit_cancel);
                poolButtons.addHidden("itemID").setValue(itemId);
                break;
            case ACTION_TO_SUBMISSION:
                List toSubmissionAction = division.addList("submitter_pool", List.TYPE_FORM);
                toSubmissionAction.setHead(T_action_submitter_head);

                toSubmissionAction.addItem(T_action_submitter_help);

                TextArea reason = toSubmissionAction.addItem().addTextArea("reason");
                reason.setSize(15, 50);

                org.dspace.app.xmlui.wing.element.Item buttons = toSubmissionAction.addItem();
                buttons.addButton("submit_back_submission").setValue(T_action_submitter_confirm);
                buttons.addButton("submit_cancel").setValue(T_submit_cancel);
                buttons.addHidden("itemID").setValue(itemId);
                break;
            case ACTION_TO_STEP:
                if(inProgressSubmission != null){
                    List alterStepAction = division.addList("submitter_pool", List.TYPE_FORM);
                    alterStepAction.setHead(T_action_alter_step_head);

                    alterStepAction.addItem(T_action_alter_step_help);


                    Select stepsSelect = alterStepAction.addItem().addSelect("step");
                    try {
                        Workflow workflow = WorkflowFactory.getWorkflow(inProgressSubmission.getCollection());
                        java.util.List<String> steps = workflow.getStepIdentifiers();
                        for (String step : steps) {
                            stepsSelect.addOption(step.equals(currentStep), step, step);
                        }


                        org.dspace.app.xmlui.wing.element.Item alterStepButtons = alterStepAction.addItem();
                        alterStepButtons.addButton("submit_alter_step").setValue(T_action_alter_step_confirm);
                        alterStepButtons.addButton("submit_cancel").setValue(T_submit_cancel);
                        alterStepButtons.addHidden("itemID").setValue(itemId);

                    } catch (WorkflowConfigurationException e) {
                        log.error(e);
                    }
                }
                break;
            case ACTION_WITHDRAW:
                List withdrawAction = division.addList("submitter_withdraw", List.TYPE_FORM);
                withdrawAction.setHead(T_action_withdraw_head);

                withdrawAction.addItem(T_action_withdraw_help);

                org.dspace.app.xmlui.wing.element.Item withButtons = withdrawAction.addItem();
                withButtons.addButton("submit_withdraw").setValue(T_action_withdraw_confirm);
                withButtons.addButton("submit_cancel").setValue(T_submit_cancel);
                withButtons.addHidden("itemID").setValue(itemId);
                break;
        }
    }

    private String addInProgressDetails(Context context, InProgressSubmission inProgressSubmission, List workflowDetails, String currentStep) throws WingException, SQLException {
        if(inProgressSubmission instanceof WorkspaceItem){
            workflowDetails.addItem().addContent(T_details_status_submission);
        }else{
            workflowDetails.addItem().addContent(T_details_status_workflow);

            workflowDetails.addLabel(T_details_current_step);
            //Add the current step
            java.util.List<PoolTask> poolTasks = PoolTask.find(context, (WorkflowItem) inProgressSubmission);
            java.util.List<ClaimedTask> claimedTasks = ClaimedTask.find(context, (WorkflowItem) inProgressSubmission);
            if(0 < poolTasks.size()){
                currentStep = poolTasks.get(0).getStepID();
                workflowDetails.addItem().addContent(currentStep);
                //We have a pool task add the message
                workflowDetails.addLabel(T_details_task_owner);
                workflowDetails.addItem().addContent(T_details_task_owner_pool);
            }

            if(0 < claimedTasks.size()){
                //For NESCent there is one user who needs to execute a step
                ClaimedTask claimedTask = claimedTasks.get(0);
                currentStep = claimedTask.getStepID();
                workflowDetails.addItem().addContent(currentStep);
                workflowDetails.addLabel(T_details_task_owner);

                //Attempt to get the owner
                EPerson owner = EPerson.find(context, claimedTask.getOwnerID());
                if(owner != null){
                    workflowDetails.addItem().addXref("mailto:" + owner.getEmail(), owner.getFullName());
                }else{
                    workflowDetails.addItem().addContent(T_details_unknown);
                }
            }
        }
        workflowDetails.addLabel(T_workflow_task);
        org.dspace.app.xmlui.wing.element.Item taskItem = workflowDetails.addItem();

        if(inProgressSubmission instanceof WorkspaceItem){
            if(context.getCurrentUser().equals(inProgressSubmission.getSubmitter())){
                DCValue[] submittedVals = inProgressSubmission.getItem().getMetadata("internal", "workflow", "submitted", Item.ANY);
                if(0 < submittedVals.length && Boolean.valueOf(submittedVals[0].value))
                {
                    //Submission finished send to the overview page !
                    String url = contextPath + "/submit-overview?workspaceID="+ inProgressSubmission.getID();
                    taskItem.addXref(url, T_workflow_finalize);
                }
                else
                {
                    //Submission isn't finished yet !
                    String url = contextPath + "/submit?workspaceID="+ inProgressSubmission.getID();
                    taskItem.addXref(url, T_workflow_continue);
                }
            }else{
                taskItem.addContent(T_workflow_none);
            }
        }else{
            String stepId = null;
            String actionId = null;
            Message taskMessage = null;
            PoolTask poolTask = PoolTask.findByWorkflowIdAndEPerson(context, inProgressSubmission.getID(),context.getCurrentUser().getID());
            if(poolTask != null){
                stepId = poolTask.getStepID();
                actionId = poolTask.getActionID();
                taskMessage = T_workflow_task_claim;
            }else{
                ClaimedTask claimedTask = ClaimedTask.findByWorkflowIdAndEPerson(context, inProgressSubmission.getID(), context.getCurrentUser().getID());
                if(claimedTask != null){
                    stepId = claimedTask.getStepID();
                    actionId = claimedTask.getActionID();
                    taskMessage = T_workflow_task_perform;
                }
            }

            if(stepId != null && actionId != null){
                String url = contextPath+"/handle/"+ inProgressSubmission.getCollection().getHandle()+"/workflow?" +
                        "workflowID="+inProgressSubmission.getID()+"&" +
                        "stepID="+stepId+"&" +
                        "actionID="+actionId;
                taskItem.addXref(url, taskMessage);
            }else{
                taskItem.addContent(T_workflow_task_waiting);
            }
        }
        return currentStep;
    }

    @Override
    public void addOptions(Options options) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Context context = ContextUtil.obtainContext(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);
        int itemId = Util.getIntParameter(request, "itemID");
        Item item = Item.find(this.context, itemId);

        //Ensure that we have read access, else throw an authorizeException !
        if(item == null || !AuthorizeManager.authorizeActionBoolean(this.context, item, Constants.READ)){
            throw new AuthorizeException();
        }

        if (AuthorizeManager.isAdmin(context))
        {
            List contextList = options.addList("context");
            contextList.setHead(T_context_head);
            contextList.addItem().addXref(contextPath+"/admin/item?itemID="+item.getID(), T_context_edit_item);

            WorkflowItem wfItem = WorkflowItem.findByItemId(context, itemId);

            if(wfItem != null){
                java.util.List<ClaimedTask> claimedTasks = ClaimedTask.findByWorkflowId(context, wfItem.getID());
                if(0 < claimedTasks.size()){
                    //Item has been claimed by at least ONE person, so ensure a back to pool action !
                    contextList.addItem().addXref(contextPath + "/internal-item?itemID=" + item.getID() + "&action=" + ACTION_TO_POOL, T_option_pool);
                }
                contextList.addItem().addXref(contextPath + "/internal-item?itemID=" + item.getID() + "&action=" + ACTION_TO_SUBMISSION, T_option_submitter);
                contextList.addItem().addXref(contextPath + "/internal-item?itemID=" + item.getID() + "&action=" + ACTION_TO_STEP, T_option_workflow);

            }
            WorkspaceItem wsItem = WorkspaceItem.findByItemId(context, itemId);
            if(wfItem != null || wsItem != null){
                //Also add a complete withdraw !
                contextList.addItem().addXref(contextPath + "/internal-item?itemID=" + item.getID() + "&action=" + ACTION_WITHDRAW, T_option_withdraw);
            }
        }
    }


    private void addWarningMessage(Item item, Division division) throws WingException, SQLException, AuthorizeException, IOException {

        log.warn("InternalItemTransformer - addWarningMessage");

        WorkflowItem wfi = WorkflowItem.findByItemId(context, item.getID());

        log.warn("InternalItemTransformer - addWarningMessage() wfi: " + wfi);

        if (wfi != null) {
            DCValue[] values = item.getMetadata("workflow.step.reviewerKey");

            log.warn("InternalItemTransformer - addWarningMessage() values: " + values);

            if(values!=null && values.length > 0){
                addMessage(division, T_in_workflow, null, null);
            }
        }
    }

    private void addMessage(Division main, Message message, String link, Message linkMessage) throws WingException {
        Division div = main.addDivision("notice", "notice");
        Para p = div.addPara();
        p.addContent(message);
        p.addXref(link, linkMessage);

    }



    /**
	 * Determine if the full item should be referenced or just a summary.
	 */
	public static boolean showFullItem(Map objectModel) {
		Request request = ObjectModelHelper.getRequest(objectModel);
		String show = request.getParameter("show");

        return show != null && show.length() > 0;
    }
}
