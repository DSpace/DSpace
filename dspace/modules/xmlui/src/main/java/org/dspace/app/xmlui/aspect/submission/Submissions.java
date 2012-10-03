/*
 * Submissions.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 19:02:24 +0200 (za, 11 apr 2009) $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.WorkflowActionConfig;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * @author Scott Phillips
 *
 * This class has been altered to support the dryad data model
 */
public class Submissions extends AbstractDSpaceTransformer
{
	/** General Language Strings */
    protected static final Message T_title =
        message("xmlui.Submission.Submissions.title");
    protected static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    protected static final Message T_trail =
        message("xmlui.Submission.Submissions.trail");
    protected static final Message T_head =
        message("xmlui.Submission.Submissions.head");
    protected static final Message T_untitled =
        message("xmlui.Submission.Submissions.untitled");
    protected static final Message T_email =
        message("xmlui.Submission.Submissions.email");

    // used by the workflow section
    protected static final Message T_w_head1 =
        message("xmlui.Submission.Submissions.workflow_head1");
    protected static final Message T_w_info1 =
        message("xmlui.Submission.Submissions.workflow_info1");
    protected static final Message T_w_head2 =
        message("xmlui.Submission.Submissions.workflow_head2");
    protected static final Message T_w_column1 =
        message("xmlui.Submission.Submissions.workflow_column1");
    protected static final Message T_w_column2 =
        message("xmlui.Submission.Submissions.workflow_column2");
    protected static final Message T_w_column3 =
        message("xmlui.Submission.Submissions.workflow_column3");
    protected static final Message T_w_column4 =
        message("xmlui.Submission.Submissions.workflow_column4");
    protected static final Message T_w_column5 =
        message("xmlui.Submission.Submissions.workflow_column5");
    protected static final Message T_w_submit_return =
        message("xmlui.Submission.Submissions.workflow_submit_return");
    protected static final Message T_w_info2 =
        message("xmlui.Submission.Submissions.workflow_info2");
    protected static final Message T_w_head3 =
        message("xmlui.Submission.Submissions.workflow_head3");
    protected static final Message T_w_submit_take =
        message("xmlui.Submission.Submissions.workflow_submit_take");
    protected static final Message T_w_info3 =
        message("xmlui.Submission.Submissions.workflow_info3");

    // used by the unfinished submissions section
    protected static final Message T_s_head1 =
        message("xmlui.Submission.Submissions.submit_head1");
    protected static final Message T_s_info1a =
        message("xmlui.Submission.Submissions.submit_info1a");
    protected static final Message T_s_info1b =
        message("xmlui.Submission.Submissions.submit_info1b");
    protected static final Message T_s_info1c =
        message("xmlui.Submission.Submissions.submit_info1c");
    protected static final Message T_s_head2 =
        message("xmlui.Submission.Submissions.submit_head2");
    protected static final Message T_s_info2a =
        message("xmlui.Submission.Submissions.submit_info2a");
    protected static final Message T_s_info2b =
        message("xmlui.Submission.Submissions.submit_info2b");
    protected static final Message T_s_info2c =
        message("xmlui.Submission.Submissions.submit_info2c");
    protected static final Message T_s_column1 =
        message("xmlui.Submission.Submissions.submit_column1");
    protected static final Message T_s_column2 =
        message("xmlui.Submission.Submissions.submit_column2");
    protected static final Message T_s_column3 =
        message("xmlui.Submission.Submissions.submit_column3");
    protected static final Message T_s_column4 =
        message("xmlui.Submission.Submissions.submit_column4");
    protected static final Message T_s_head3 =
        message("xmlui.Submission.Submissions.submit_head3");
    protected static final Message T_s_info3 =
        message("xmlui.Submission.Submissions.submit_info3");
    protected static final Message T_s_head4 =
        message("xmlui.Submission.Submissions.submit_head4");
    protected static final Message T_s_submit_remove =
        message("xmlui.Submission.Submissions.submit_submit_remove");

	// Used in the in progress section
    protected static final Message T_p_head1 =
        message("xmlui.Submission.Submissions.progress_head1");
    protected static final Message T_p_info1 =
        message("xmlui.Submission.Submissions.progress_info1");
    protected static final Message T_p_column1 =
        message("xmlui.Submission.Submissions.progress_column1");
    protected static final Message T_p_column2 =
        message("xmlui.Submission.Submissions.progress_column2");
    protected static final Message T_p_column3 =
        message("xmlui.Submission.Submissions.progress_column3");

    // The workflow status messages
    protected static final Message T_status_0 =
        message("xmlui.Submission.Submissions.status_0");
    protected static final Message T_status_1 =
        message("xmlui.Submission.Submissions.status_1");
    protected static final Message T_status_2 =
        message("xmlui.Submission.Submissions.status_2");
    protected static final Message T_status_3 =
        message("xmlui.Submission.Submissions.status_3");
    protected static final Message T_status_4 =
        message("xmlui.Submission.Submissions.status_4");
    protected static final Message T_status_5 =
        message("xmlui.Submission.Submissions.status_5");
    protected static final Message T_status_6 =
        message("xmlui.Submission.Submissions.status_6");
    protected static final Message T_status_7 =
        message("xmlui.Submission.Submissions.status_7");
    protected static final Message T_status_unknown =
        message("xmlui.Submission.Submissions.status_unknown");

    private static Logger log = Logger.getLogger(Submissions.class);
    private static final Message T_curator_review_head = message("xmlui.Submission.Submissions.curator.review.head");
    private static final Message T_curator_delete_head = message("xmlui.Submission.Submissions.curator.delete.head");
    private static final Message T_curator_delete_column = message("xmlui.Submission.Submissions.curator.delete.column.date");


    public void addPageMeta(PageMeta pageMeta) throws SAXException,
	WingException, UIException, SQLException, IOException,
	AuthorizeException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/submissions",T_trail);
	}


    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        Division div = body.addInteractiveDivision("submissions", contextPath+"/submissions", Division.METHOD_POST,"primary");
        div.setHead(T_head);

        this.addWorkflowTasks(div);
        this.addUnfinishedSubmissions(div);
        this.addSubmissionsInWorkflow(div);
        this.addCuratorItems(div);
    }


    private void addCuratorItems(Division division) throws WingException, SQLException, IOException {
        boolean isCurator = false;
        try{
            Collection dataSetColl = (Collection) HandleManager.resolveToObject(context, ConfigurationManager.getProperty("submit.publications.collection"));
            Workflow workflow = WorkflowFactory.getWorkflow(dataSetColl);
            Role curatorRole = workflow.getRoles().get("curator");
            Group curators = WorkflowUtils.getRoleGroup(context, dataSetColl.getID(), curatorRole);
            if(curators != null && curators.isMember(context.getCurrentUser())){
                isCurator = true;
            }

        }catch (WorkflowConfigurationException e){
            log.error(LogManager.getHeader(context, "Error while verifying if the user is a curator", "eperson:" + context.getCurrentUser().getID()), e);
        }

        if(isCurator){
            Division workflow = division.addDivision("workflow-curator");

            List<ClaimedTask> reviewTasks = ClaimedTask.findAllInStep(context, "reviewStep");
            if(0 < reviewTasks.size()){
                // Items in the review stage
                Table table = workflow.addTable("workflow-tasks", + 2,5);
                table.setHead(T_curator_review_head);
                Row header = table.addRow(Row.ROLE_HEADER);
                header.addCellContent(T_w_column1);
                header.addCellContent(T_w_column2);
                header.addCellContent(T_w_column3);
                header.addCellContent(T_w_column4);
                header.addCellContent(T_w_column5);

                for (ClaimedTask claimedTask : reviewTasks){
                    int workflowItemID = claimedTask.getWorkflowItemID();
                    String step_id = claimedTask.getStepID();
                    String action_id = claimedTask.getActionID();
                    WorkflowItem dataPackage = null;
                    try {
                        dataPackage = WorkflowItem.find(context, workflowItemID);
                        Row row = table.addRow();
                        renderWorkflowItemRow(row, dataPackage, step_id, action_id, false, true, false);

                        //Now that we have rendered our main data file, render our datasets
                        /*
			  Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, dataPackage.getItem());
                        for (Item datafile : dataFiles) {
                            row = table.addRow();

                            WorkflowItem wfDataset = WorkflowItem.findByItemId(context, datafile.getID());
                            renderWorkflowItemRow(row, wfDataset, step_id, action_id, false, true, true);
                        }
			*/
                    } catch (WorkflowConfigurationException e) {
                        Row row = table.addRow();
                        row.addCell().addContent("Error: Configuration error in workflow.");
                        log.error(LogManager.getHeader(context, "configuration error in workflow", "workflow curator"), e);
                    } catch (Exception e) {
                        log.error(LogManager.getHeader(context, "configuration error in workflow", "workflow curator"), e);
                    }

                }
            }

            List<ClaimedTask> deletionItems = ClaimedTask.findAllInStep(context, "pendingdelete");
            if(0 < deletionItems.size()){
                // Tasks you own
                Table table = workflow.addTable("workflow-tasks", + 2,5);
                table.setHead(T_curator_delete_head);
                Row header = table.addRow(Row.ROLE_HEADER);
                header.addCellContent(T_w_column1);
                header.addCellContent(T_w_column2);
                header.addCellContent(T_w_column3);
                header.addCellContent(T_w_column4);
                header.addCellContent(T_w_column5);
                header.addCellContent(T_curator_delete_column);

                for (ClaimedTask claimedTask : deletionItems){
                    int workflowItemID = claimedTask.getWorkflowItemID();
                    String step_id = claimedTask.getStepID();
                    String action_id = claimedTask.getActionID();
                    WorkflowItem dataPackage;
                    try {
                        dataPackage = WorkflowItem.find(context, workflowItemID);
                        Row row = table.addRow();
                        renderWorkflowItemRow(row, dataPackage, step_id, action_id, false, true, false);

                        //Retrieve the deletion date
                        DCValue[] rejectDate = dataPackage.getItem().getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "rejectDate", Item.ANY);
                        if(0 < rejectDate.length){
                            row.addCellContent(rejectDate[0].value.substring(0, rejectDate[0].value.indexOf("T")));
                        }else
                            row.addCellContent("Unknown");


                        //Now that we have rendered our main data file, render our datasets
			/*
                        Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, dataPackage.getItem());
                        for (Item datafile : dataFiles) {
                            row = table.addRow();

                            WorkflowItem wfDataset = WorkflowItem.findByItemId(context, datafile.getID());
                            renderWorkflowItemRow(row, wfDataset, step_id, action_id, false, true, true);

                            if(0 < rejectDate.length){
                                row.addCellContent(rejectDate[0].value.substring(0, rejectDate[0].value.indexOf("T")));
                            }else
                                row.addCellContent("Unknown");
                        }
			*/
                    } catch (WorkflowConfigurationException e) {
                        Row row = table.addRow();
                        row.addCell().addContent("Error: Configuration error in workflow.");
                        log.error(LogManager.getHeader(context, "configuration error in workflow", "pendingdelete"), e);
                    } catch (Exception e) {
                        log.error(LogManager.getHeader(context, "configuration error in workflow", "pendingdelete"), e);
                    }

                }
            }
        }
    }


    /**
     * If the user has any workflow tasks, either assigned to them or in an
     * available pool of tasks, then build two tables listing each of these queues.
     *
     * If the user dosn't have any workflows then don't do anything.
     *
     * @param division The division to add the two queues too.
     */
    private void addWorkflowTasks(Division division) throws SQLException, WingException, AuthorizeException, IOException {
    	@SuppressWarnings("unchecked") // This cast is correct
    	java.util.List<ClaimedTask> ownedItems = ClaimedTask.findByEperson(context, context.getCurrentUser().getID());
    	@SuppressWarnings("unchecked") // This cast is correct.
    	java.util.List<PoolTask> pooledItems = PoolTask.findByEperson(context, context.getCurrentUser().getID());

    	if (!(ownedItems.size() > 0 || pooledItems.size() > 0))
    		// No tasks, so don't show the table.
    		return;


    	Division workflow = division.addDivision("workflow-tasks");
    	workflow.setHead(T_w_head1);
    	workflow.addPara(T_w_info1);

    	// Tasks you own
    	Table table = workflow.addTable("workflow-tasks",ownedItems.size() + 2,5);
        table.setHead(T_w_head2);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_w_column1);
        header.addCellContent(T_w_column2);
        header.addCellContent(T_w_column3);
        header.addCellContent(T_w_column4);
        header.addCellContent(T_w_column5);


        //Sort the workflow items by date 
        TreeMap<String, ClaimedTask> map = new TreeMap<String, ClaimedTask>();
        if (ownedItems.size() > 0)
        {
            for (ClaimedTask owned : ownedItems)
        	{
                int workflowItemID = owned.getWorkflowItemID();
                WorkflowItem dataPackage = WorkflowItem.find(context, workflowItemID);
                Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, dataPackage.getItem());
                Date lastModifiedDate = dataPackage.getItem().getLastModified();
                //Check our data files if one has been altered after this one
                for (Item dataFile : dataFiles) {
                    if (dataFile.getLastModified().after(lastModifiedDate))
                        lastModifiedDate = dataFile.getLastModified();
                }
                //Store the date with the task (also add the workflowItemId to the date to make sure we have something unique)
                map.put(new DCDate(lastModifiedDate).toString() + "_" + workflowItemID, owned);
            }
        }
        else
        {
        	Row row = table.addRow();
        	row.addCell(0,5).addHighlight("italic").addContent(T_w_info2);
        }

        //Render all of the claimed task ordered by date
        for(String lastModified : map.descendingKeySet()){
            ClaimedTask owned = map.get(lastModified);

            int workflowItemID = owned.getWorkflowItemID();
            String step_id = owned.getStepID();
            String action_id = owned.getActionID();
            try {
                WorkflowItem dataPackage = WorkflowItem.find(context, workflowItemID);
                Row row = table.addRow();
                //TODO: reenable option to resend an item to the pool
                renderWorkflowItemRow(row, dataPackage, step_id, action_id, false, false, false);

                //Now that we have rendered our main data file, render our datasets
		/*
                Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, dataPackage.getItem());
                for (Item datafile : dataFiles) {
                    row = table.addRow();

                    WorkflowItem wfDataset = WorkflowItem.findByItemId(context, datafile.getID());
                    renderWorkflowItemRow(row, wfDataset, step_id, action_id, false, false, true);
                }
		*/
            } catch (WorkflowConfigurationException e) {
                Row row = table.addRow();
                row.addCell().addContent("Error: Configuration error in workflow.");
                log.error(LogManager.getHeader(context, "configuration error in workflow", "claimedtasks"), e);
            } catch (Exception e) {
                log.error(LogManager.getHeader(context, "configuration error in workflow", "claimedtasks"), e);
            }
        }




        // Tasks in the pool
        table = workflow.addTable("workflow-tasks",pooledItems.size()+2,5);
        table.setHead(T_w_head3);

        header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_w_column1);
        header.addCellContent(T_w_column2);
        header.addCellContent(T_w_column3);
        header.addCellContent(T_w_column4);
        header.addCellContent(T_w_column5);

        if (pooledItems.size() > 0)
        {
            for (PoolTask pooled : pooledItems)
        	{
                String step_id = pooled.getStepID();
                int workflowItemID = pooled.getWorkflowItemID();
                String action_id = pooled.getActionID();
                WorkflowItem item = null;
                try {
                    item = WorkflowItem.find(context, workflowItemID);
                    Workflow wf = WorkflowFactory.getWorkflow(item.getCollection());
                    Step step = wf.getStep(step_id);
                    WorkflowItem dataPackage = null;
                    try {
                        dataPackage = WorkflowItem.find(context, workflowItemID);
                        Row row = table.addRow();
                        renderWorkflowItemRow(row, dataPackage, step_id, action_id, true, false, false);

                        //Now that we have rendered our main data file, render our datasets
			/*
                        Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, dataPackage.getItem());
                        for (Item datafile : dataFiles) {
                            row = table.addRow();

                            WorkflowItem wfDataset = WorkflowItem.findByItemId(context, datafile.getID());
                            renderWorkflowItemRow(row, wfDataset, step_id, action_id, true, false, true);
                        }
			*/
                    } catch (WorkflowConfigurationException e) {
                        Row row = table.addRow();
                        row.addCell().addContent("Error: Configuration error in workflow.");
                        log.error(LogManager.getHeader(context, "configuration error in workflow", "pooleditems"), e);
                    } catch (AuthorizeException e) {
                        log.error(LogManager.getHeader(context, "configuration error in workflow", "pooleditems"), e);
                    } catch (IOException e) {
                        log.error(LogManager.getHeader(context, "configuration error in workflow", "pooleditems"), e);
                    } catch (Exception e) {
                        log.error(LogManager.getHeader(context, "configuration error in workflow", "pooleditems"), e);
                    }
                    
                } catch (WorkflowConfigurationException e) {
                    Row row = table.addRow();
                    row.addCell().addContent("Error: Configuration error in workflow.");
                    log.error(LogManager.getHeader(context, "configuration error in workflow", "pooleditems 2"), e);

                } catch (AuthorizeException e) {
                    log.error(LogManager.getHeader(context, "configuration error in workflow", "pooleditems 2"), e);
                } catch (IOException e) {
                    log.error(LogManager.getHeader(context, "configuration error in workflow", "pooleditems 2"), e);
                } catch (Exception e) {
                    log.error(LogManager.getHeader(context, "configuration error in workflow", "pooleditems 2"), e);
                }
            }
            Row row = table.addRow();
             row.addCell(0,5).addButton("submit_take_tasks").setValue(T_w_submit_take);
        }
        else
        {
        	Row row = table.addRow();
        	row.addCell(0,5).addHighlight("italic").addContent(T_w_info3);
        }
    }


    private void renderWorkflowItemRow(Row row, WorkflowItem item, String step_id, String action_id, boolean addCheckbox, boolean viewOnly, boolean inlineRow) throws WingException, SQLException, TransformerException, IOException, SAXException, WorkflowConfigurationException, ParserConfigurationException, AuthorizeException {
        Workflow wf = WorkflowFactory.getWorkflow(item.getCollection());
        Step step = wf.getStep(step_id);
        //Time to determine our workflowId
        int id;
        if(DryadWorkflowUtils.isDataPackage(item))
            id = item.getID();
        else{
            //Check if our data package is archived !
            Item dataPackageItem = DryadWorkflowUtils.getDataPackage(context, item.getItem());
            if(dataPackageItem.isArchived()){
                //No problem, add the data file as id
                id = item.getID();
            }else{
                id = WorkflowItem.findByItemId(context, dataPackageItem.getID()).getID();
            }
        }

        String url;
        if(viewOnly){
            url = contextPath +"/handle/" + item.getItem().getHandle();
        } else
            url = contextPath+"/handle/"+item.getCollection().getHandle()+"/workflow?workflowID="+id+"&stepID="+step_id+"&actionID="+action_id;
        DCValue[] titles = item.getItem().getDC("title", null, Item.ANY);
        String collectionName = item.getCollection().getMetadata("name");
        EPerson submitter = item.getSubmitter();
        String submitterName = submitter.getFullName();
        String submitterEmail = submitter.getEmail();

        if(addCheckbox){
            CheckBox remove = row.addCell("workflowitemcell_" + wf.getID(), Cell.ROLE_DATA, inlineRow ? "inlineRow" : "").addCheckBox("workflowandstepID");
            remove.setLabel("selected");
            remove.addOption(item.getID() + ":" + step.getId());
        } else
            row.addCell().addContent("");

        // The task description
        row.addCell("workflowitemcelltitle_" + wf.getID(), Cell.ROLE_DATA, (inlineRow && !addCheckbox) ? "inlineRow" : "").addXref(url,message("xmlui.Submission.Submissions." + wf.getID() + "." + step_id + "." + action_id));

        // The item description
        if (titles != null && titles.length > 0)
        {
            String displayTitle = titles[0].value;
            if (displayTitle.length() > 50)
                displayTitle = displayTitle.substring(0,50)+ " ...";
            row.addCell().addXref(url,displayTitle);
        }
        else
            row.addCell().addXref(url,T_untitled);

        // Submitted too
        row.addCell().addXref(url,collectionName);

        // Submitted by
        Cell cell = row.addCell();
        cell.addContent(T_email);
        cell.addXref("mailto:"+submitterEmail,submitterName);
    }
    


    private Map<String, Object> groupInProgressSubmissions(java.util.List<InProgressSubmission> pooledItems) throws SQLException {
        String publicationsCollHandle = ConfigurationManager.getProperty("submit.publications.collection");
        String datasetCollHandle = ConfigurationManager.getProperty("submit.dataset.collection");
        //What we need to do is group all the items by which ones are publications and which ones are just datasets
        Map<InProgressSubmission, List<InProgressSubmission>> groupedPooledItems = new HashMap<InProgressSubmission, List<InProgressSubmission>>();
        // A list of datasets whose publication has already been approved !
        List<InProgressSubmission> loneDatasets = new ArrayList<InProgressSubmission>();
        
        Map<Integer, InProgressSubmission> itemIdToPublication = new HashMap<Integer, InProgressSubmission>();
        //Start by indexing all the items by the handle
        for(InProgressSubmission pooled : pooledItems){
            if(pooled.getCollection().getHandle().equals(publicationsCollHandle)){
                //We have a publication so index it in our handletoworkflowItem
                itemIdToPublication.put(pooled.getItem().getID(), pooled);
                //Also create a new arraylist for this publication
                groupedPooledItems.put(pooled, new ArrayList<InProgressSubmission>());
            }
        }


        for (InProgressSubmission pooledItem : pooledItems)
        {
            if(datasetCollHandle.equals(pooledItem.getCollection().getHandle())){
                //We found a dataset item, so put it in it's place
                //Its place is below the publication (if there is one of course)
                Item pubItem = DryadWorkflowUtils.getDataPackage(context, pooledItem.getItem());

                //Also just make sure we avoid nullpointers
                if(pubItem != null){
                    InProgressSubmission publicationWfItem = itemIdToPublication.get(pubItem.getID());
                    if(publicationWfItem != null){
                        List<InProgressSubmission> datasets = groupedPooledItems.get(publicationWfItem);
                        //Add it to the list with datasets & reput in the hashmap
                        datasets.add(pooledItem);
                        groupedPooledItems.put(publicationWfItem, datasets);
                        //We are done with this item
                        continue;
                    }
                }

                //This item is a dataset, but it's publication has already been approved so add it to the lone datasets list
                loneDatasets.add(pooledItem);
            }
        }
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("groupeditems", groupedPooledItems);
        result.put("lonedatasets", loneDatasets);
        return result;
    }

    /**
     * There are two options, the user has some unfinished submissions
     * or the user does not.
     *
     * If the user does not, then we just display a simple paragraph
     * explaining that the user may submit new items to dspace.
     *
     * If the user does have unfinisshed submissions then a table is
     * presented listing all the unfinished submissions that this user has.
     *
     */
    private void addUnfinishedSubmissions(Division division) throws SQLException, WingException
    {

        // User's WorkflowItems
    	WorkspaceItem[] unfinishedItemsArray = WorkspaceItem.findByEPerson(context,context.getCurrentUser());
        java.util.List<InProgressSubmission> unfinishedItems = new ArrayList<InProgressSubmission>();
        unfinishedItems.addAll(Arrays.asList(unfinishedItemsArray));

        SupervisedItem[] supervisedItems = SupervisedItem.findbyEPerson(context, context.getCurrentUser());

    	if (unfinishedItems.size() <= 0 && supervisedItems.length <= 0)
    	{
    		Collection[] collections = Collection.findAuthorized(context, null, Constants.ADD);

    		if (collections.length > 0)
    		{
    			Division start = division.addDivision("start-submision");
	        	start.setHead(T_s_head1);
	        	Para p = start.addPara();
	        	p.addContent(T_s_info1a);
                if(ConfigurationManager.getProperty("submit.publications.collection") != null){
                    p.addXref(contextPath+"/handle/" + ConfigurationManager.getProperty("submit.publications.collection") + "/submit",T_s_info1b);
                } else {
                    p.addXref(contextPath+"/submit",T_s_info1b);
                }

	        	p.addContent(T_s_info1c);
	        	return;
    		}
    	}

        //First of all filter all out the items that have gone through the submission process, these will end up in a special list !
        List<InProgressSubmission> submittedItems = new ArrayList<InProgressSubmission>();
        for (InProgressSubmission inProgressSubmission : unfinishedItems) {
            DCValue[] submittedVals = inProgressSubmission.getItem().getMetadata("internal", "workflow", "submitted", Item.ANY);
            if(0 < submittedVals.length && Boolean.valueOf(submittedVals[0].value))
            {
                //We have an already submitted item, add it to the "special" list
                submittedItems.add(inProgressSubmission);
            }
        }
        //Remove our submitted items from our unifinished list
        unfinishedItems.removeAll(submittedItems);


        if(0 < submittedItems.size()){
            //Create a UI for this
            Division submitted = division.addInteractiveDivision("submitted-submisions", contextPath+"/submit", Division.METHOD_POST);
            submitted.setHead("Publication submissions awaiting data file(s)");
            Para p = submitted.addPara();
            p.addContent("These are publications (with data file(s) below) which have gone through the submission process but the publications still have an incomplete data file.");

            // Calculate the number of rows.
            // Each list pluss the top header and bottom row for the button.
            int rows = submittedItems.size() + 2;
            Table submittedTable = submitted.addTable("submitted-submissions", rows, 5);
            Row header = submittedTable.addRow(Row.ROLE_HEADER);
            header.addCellContent(T_s_column1);
            header.addCellContent(T_s_column2);
            header.addCellContent(T_s_column3);
            header.addCellContent(T_s_column4);

            Map<String, Object> result = groupInProgressSubmissions(submittedItems);
            Map<InProgressSubmission, List<InProgressSubmission>> groupOwnedItems = (Map<InProgressSubmission, List<InProgressSubmission>>) result.get("groupeditems");
            List<InProgressSubmission> loneDatasets = (List<InProgressSubmission>) result.get("lonedatasets");


            for (InProgressSubmission publicationItem : groupOwnedItems.keySet())
            {
                Row row = submittedTable.addRow();

                renderWorkspaceItemRow(row, (WorkspaceItem) publicationItem, "submit-overview", true, false);

                //Now under this publication add it's datasets
		/*
                List<InProgressSubmission> datasets = groupOwnedItems.get(publicationItem);
                for (InProgressSubmission datasetWsItem : datasets) {
                    row = submittedTable.addRow();
                    // If the item has a handle, it can be deleted
                    // If not it means that this submission most likely depends on the publication
                    renderWorkspaceItemRow(row, (WorkspaceItem) datasetWsItem, "submit-overview", (datasetWsItem.getItem().getHandle() != null), true);
                }
		*/
            }
	    
	    /*
            //These shouldn't be here but just in case
            for (InProgressSubmission dataset : loneDatasets){
                //Render all the datasets that do NOT have a publication
                Row row = submittedTable.addRow();

                renderWorkspaceItemRow(row, (WorkspaceItem) dataset,"submit-overview", true, false);
            }
	    */

            //TODO: show lone datasets ?
            Row lastRow = submittedTable.addRow();
            Cell lastCell = lastRow.addCell(0,5);
            lastCell.addButton("submit_submissions_remove").setValue(T_s_submit_remove);

        }


    	Division unfinished = division.addInteractiveDivision("unfinished-submisions", contextPath+"/submissions", Division.METHOD_POST);
    	unfinished.setHead(T_s_head2);
    	Para p = unfinished.addPara();
    	p.addContent(T_s_info2a);
        if(ConfigurationManager.getProperty("submit.publications.collection") != null){
            p.addHighlight("bold").addXref(contextPath+"/handle/" + ConfigurationManager.getProperty("submit.publications.collection") + "/submit",T_s_info2b);
        } else {
            p.addHighlight("bold").addXref(contextPath+"/submit",T_s_info2b);
        }

        p.addContent(T_s_info2c);

    	// Calculate the number of rows.
    	// Each list pluss the top header and bottom row for the button.
    	int rows = unfinishedItems.size() + supervisedItems.length + 2;
    	if (supervisedItems.length > 0 && unfinishedItems.size() > 0)
    		rows++; // Authoring heading row
    	if (supervisedItems.length > 0)
    		rows++; // Supervising heading row


    	Table table = unfinished.addTable("unfinished-submissions",rows,5);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_s_column1);
        header.addCellContent(T_s_column2);
        header.addCellContent(T_s_column3);
        header.addCellContent(T_s_column4);

        if (supervisedItems.length > 0 && unfinishedItems.size() > 0)
        {
            header = table.addRow();
            header.addCell(null,Cell.ROLE_HEADER,0,5,null).addContent(T_s_head3);
        }

        if (unfinishedItems.size() > 0)
        {
            Map<String, Object> result = groupInProgressSubmissions(unfinishedItems);
            Map<InProgressSubmission, List<InProgressSubmission>> groupOwnedItems = (Map<InProgressSubmission, List<InProgressSubmission>>) result.get("groupeditems");
            List<InProgressSubmission> loneDatasets = (List<InProgressSubmission>) result.get("lonedatasets");

            int index = 0;
	        for (InProgressSubmission publicationItem : groupOwnedItems.keySet())
	        {
                Row row = table.addRow();

                renderWorkspaceItemRow(row, (WorkspaceItem) publicationItem, "submit", true, false);

                //Now under this publication add it's datasets
                List<InProgressSubmission> datasets = groupOwnedItems.get(publicationItem);
                for (InProgressSubmission datasetWsItem : datasets) {
                    row = table.addRow();
                    // If the item has a handle, it can be deleted
                    // If not it means that this submission most likely depends on the publication
                    renderWorkspaceItemRow(row, (WorkspaceItem) datasetWsItem, "submit", (datasetWsItem.getItem().getHandle() != null), true);
                }
                index ++;
                //TODO: find something else ?
                //Add a filler row
//                if(index < groupOwnedItems.size())
//                    table.addRow().addCell(1, 4).addContent("");
	        }

		/*
            for (InProgressSubmission dataset : loneDatasets){
                //Render all the datasets that do NOT have a publication
                Row row = table.addRow();

                renderWorkspaceItemRow(row, (WorkspaceItem) dataset, "submit", true, false);
            }
		*/
        }
        else
        {
        	header = table.addRow();
        	header.addCell(0,5).addHighlight("italic").addContent(T_s_info3);
        }

        if (supervisedItems.length > 0)
        {
            header = table.addRow();
            header.addCell(null,Cell.ROLE_HEADER,0,5,null).addContent(T_s_head4);
        }

        for (WorkspaceItem workspaceItem : supervisedItems)
        {

        	DCValue[] titles = workspaceItem.getItem().getDC("title", null, Item.ANY);
        	EPerson submitterEPerson = workspaceItem.getItem().getSubmitter();

        	int workspaceItemID = workspaceItem.getID();
        	String url = contextPath+"/submit?workspaceID="+workspaceItemID;
        	String submitterName = submitterEPerson.getFullName();
        	String submitterEmail = submitterEPerson.getEmail();
        	String collectionName = workspaceItem.getCollection().getMetadata("name");


        	Row row = table.addRow(Row.ROLE_DATA);
        	CheckBox selected = row.addCell().addCheckBox("workspaceID");
        	selected.setLabel("select");
        	selected.addOption(workspaceItemID);

        	if (titles.length > 0)
        	{
        		String displayTitle = titles[0].value;
    			if (displayTitle.length() > 50)
    				displayTitle = displayTitle.substring(0,50)+ " ...";
        		row.addCell().addXref(url,displayTitle);
        	}
        	else
        		row.addCell().addXref(url,T_untitled);
        	row.addCell().addXref(url,collectionName);
        	Cell cell = row.addCell();
        	cell.addContent(T_email);
        	cell.addXref("mailto:"+submitterEmail,submitterName);
        }


        header = table.addRow();
        if (unfinishedItems.size() > 0 || supervisedItems.length > 0) {
            Cell lastCell = header.addCell(0,5);
            lastCell.addButton("submit_submissions_remove").setValue(T_s_submit_remove);
        }
    }

    private void renderWorkspaceItemRow(Row row, WorkspaceItem item, String urlPrefix, boolean addCheckbox, boolean inlineRow) throws WingException, SQLException {
        DCValue[] titles = item.getItem().getDC("title", null, Item.ANY);
        EPerson submitterEPerson = item.getItem().getSubmitter();

        int workspaceItemID = item.getID();
        String url = contextPath + "/" + urlPrefix + "?workspaceID="+workspaceItemID;
        String submitterName = submitterEPerson.getFullName();
        String submitterEmail = submitterEPerson.getEmail();
        String collectionName = item.getCollection().getMetadata("name");

        if(addCheckbox){
            CheckBox remove = row.addCell("workspaceitemcell_" + item.getID(), Cell.ROLE_DATA, inlineRow ? "inlineRow" : "").addCheckBox("workspaceID");
            remove.setLabel("remove");
            remove.addOption(workspaceItemID);
        } else
            row.addCell().addContent("");

        if (titles.length > 0)
        {
            String displayTitle = titles[0].value;
            if (displayTitle.length() > 50)
                displayTitle = displayTitle.substring(0,50)+ " ...";
            row.addCell().addXref(url,displayTitle);
        }
        else
            row.addCell().addXref(url,T_untitled);
        row.addCell().addXref(url,collectionName);
        Cell cell = row.addCell();
        cell.addContent(T_email);
        cell.addXref("mailto:"+submitterEmail,submitterName);
    }


    /**
     * This section lists all the submissions that this user has submitted which are currently under review.
     *
     * If the user has none, this nothing is displayed.
     */
    private void addSubmissionsInWorkflow(Division division) throws SQLException, WingException
    {
        try {
            WorkflowItem[] inprogressItems = WorkflowItem.findByEPerson(context,context.getCurrentUser());

            // If there is nothing in progress then don't add anything.
            if (!(inprogressItems.length > 0))
                    return;

            Table table = null;

            for (WorkflowItem workflowItem : inprogressItems)
            {
                DCValue[] titles = workflowItem.getItem().getDC("title", null, Item.ANY);
                String collectionName = workflowItem.getCollection().getMetadata("name");
                java.util.List<PoolTask> pooltasks = PoolTask.find(context,workflowItem);
                java.util.List<ClaimedTask> claimedtasks = ClaimedTask.find(context, workflowItem);
                Workflow wf = WorkflowFactory.getWorkflow(workflowItem.getCollection());

                Message state = message("xmlui.Submission.Submissions.step.unknown");
                for(PoolTask task: pooltasks){
                    state = message("xmlui.Submission.Submissions." + task.getActionID());
                }
                for(ClaimedTask task: claimedtasks){
                    Step step = wf.getStep(task.getStepID());
                    state = message("xmlui.Submission.Submissions." + wf.getID() + "." + step.getId() + "." + task.getActionID());
                }

                if(!DryadWorkflowUtils.isDataPackage(workflowItem) && pooltasks.size() == 0 && claimedtasks.size() == 0){
                    //We have a data file, get the state from our data package
                    Item dataPackage = DryadWorkflowUtils.getDataPackage(context, workflowItem.getItem());
                    if(dataPackage == null){
                        log.error(LogManager.getHeader(context, "Error while retrieving data package for workflow item in submission", "No data package found for item: " + workflowItem.getItem().getID()));
                        continue;
                    }
                    WorkflowItem wfDataPackage = WorkflowItem.findByItemId(context, dataPackage.getID());
                    pooltasks = PoolTask.find(context,wfDataPackage);
                    claimedtasks = ClaimedTask.find(context, wfDataPackage);

                    for(PoolTask task: pooltasks){
                        state = message("xmlui.Submission.Submissions." + task.getActionID());
                    }
                    for(ClaimedTask task: claimedtasks){
                        Step step = wf.getStep(task.getStepID());
                        state = message("xmlui.Submission.Submissions." + wf.getID() + "." + step.getId() + "." + task.getActionID());
                    }
                }

                if(table == null){
                    //We have a valid workflow item, add the UI elements to show it
                    Division inprogress = division.addDivision("submissions-inprogress");
                    inprogress.setHead(T_p_head1);
                    inprogress.addPara(T_p_info1);

                    table = inprogress.addTable("submissions-inprogress",inprogressItems.length+1,3);
                    Row header = table.addRow(Row.ROLE_HEADER);
                    header.addCellContent(T_p_column1);
                    header.addCellContent(T_p_column2);
                    header.addCellContent(T_p_column3);
                }

                Row row = table.addRow();

                // Add the title column
                if (titles.length > 0)
                {
                    String displayTitle = titles[0].value;
                    if (displayTitle.length() > 50)
                        displayTitle = displayTitle.substring(0,50)+ " ...";
                    row.addCellContent(displayTitle);
                }
                else
                    row.addCellContent(T_untitled);

                // Collection name column
                row.addCellContent(collectionName);

                // Status column
                row.addCellContent(state);
            }
        }  catch (Exception e) {
            Row row = division.addTable("table0",1,1).addRow();
            row.addCell().addContent("Error: Configuration error in workflow.");
            log.error(LogManager.getHeader(context, "configuration error in workflow", "in workflow submissions"), e);

        }
    }


}
