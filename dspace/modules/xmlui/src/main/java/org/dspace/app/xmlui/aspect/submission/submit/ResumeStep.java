/*
 * ResumeStep.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 17:02:24 +0000 (Sat, 11 Apr 2009) $
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
package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.DryadWorkflowUtils;
import org.xml.sax.SAXException;

import javax.security.auth.login.Configuration;
import java.io.IOException;
import java.sql.SQLException;

/**
 * This step is used when the a user clicks an unfinished submission
 * from the submissions page. Here we present the full item and then
 * give the user the option to resume the item's submission.
 * <P>
 * This is not a true "step" in the submission process, it just
 * kicks off editing an unfinished submission.
 *
 * FIXME: We should probably give the user the option to remove the
 * submission as well.
 *
 * @author Scott Phillips
 * @author Tim Donohue (small updates for Configurable Submission)
 *
 * This class has been altered to support the dryad data model
 */
public class ResumeStep extends AbstractStep
{
	/** Language Strings **/
    protected static final Message T_submit_resume =
        message("xmlui.Submission.submit.ResumeStep.submit_resume");
    protected static final Message T_submit_cancel =
        message("xmlui.general.cancel");
    private static final Message T_ACTIONS_HEAD = message("xmlui.Submission.submit.ResumeStep.actions.head");

    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public ResumeStep()
	{
		this.requireWorkspace = true;
	}


	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{
		// Get any metadata that may be removed by unselecting one of these options.
		org.dspace.content.Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

		Request request = ObjectModelHelper.getRequest(objectModel);
		String showfull = request.getParameter("showfull");

		// if the user selected showsimple, remove showfull.
		if (showfull != null && request.getParameter("showsimple") != null)
			showfull = null;

		Division div = body.addInteractiveDivision("resume-submission", actionURL, Division.METHOD_POST, "primary submission");
		div.setHead(T_submission_head);


		if (showfull == null)
		{
			ReferenceSet referenceSet = div.addReferenceSet("submission",ReferenceSet.TYPE_SUMMARY_VIEW);
			referenceSet.addReference(item);
			div.addPara().addButton("showfull").setValue(T_showfull);
		}
		else
		{
			ReferenceSet referenceSet = div.addReferenceSet("submission",ReferenceSet.TYPE_DETAIL_VIEW);
			referenceSet.addReference(item);
			div.addPara().addButton("showsimple").setValue(T_showsimple);

			div.addHidden("showfull").setValue("true");
		}


        //We do not show the resume controls IF our item is awaiting more datasets
        DCValue[] workflowStatus = item.getMetadata("internal", "workflow", "submitted", org.dspace.content.Item.ANY);
        //We also do not show the resume controls IF our item is a dataset BUT the publication it is linked to isn't done yet !
        org.dspace.content.Item pubItem = DryadWorkflowUtils.getDataPackage(context, item);
        boolean publicationNotSubmitted = false;
        WorkspaceItem pubWs = null;
        if(pubItem != null){
            pubWs = WorkspaceItem.findByItemId(context, pubItem.getID());
        }


        if(pubWs != null){
            //Our publication is still a workspaceItem so euhm we haven't finished it
            //Check if perhaps we are awaiting datasets in which case a resume is allowed
            DCValue[] wfStatus = pubWs.getItem().getMetadata("internal", "workflow", "submitted", org.dspace.content.Item.ANY);
            if(0 == wfStatus.length || !Boolean.valueOf(wfStatus[0].value))
                publicationNotSubmitted = true;
        }



        if(publicationNotSubmitted){
            List form = div.addList("resume-submission",List.TYPE_FORM);

            Item help = form.addItem();
            help.addContent("Before submission on this data file may continue you need to finish ");
            help.addXref(contextPath + "/submit?workspaceID=" + pubWs.getID(), "the parent publication");
            help.addContent(" first.");


            org.dspace.app.xmlui.wing.element.Item actions = form.addItem();
            actions.addButton("submit_cancel").setValue(T_submit_cancel);


        }
        else if(0 < workflowStatus.length && Boolean.valueOf(workflowStatus[0].value))
        {
            Table table = div.addTable("workflow-actions", 1, 1);
            table.setHead(T_ACTIONS_HEAD);
            //We have something that has already gone through the workflow, so show the controls !
            boolean isPublication = ConfigurationManager.getProperty("submit.publications.collection").equals(submission.getCollection().getHandle());
            if(isPublication){
                //We have a publication
                //They get the choice to push it to the reviewer(s)
                Row row = table.addRow();
                Cell cell = row.addCell();

                cell.addContent("If you wish to complete this publication so it can be reviewed please select \"Complete publication\".");

                
                cell = row.addCell();
                cell.addButton("submit_continue_reviewer").setValue("Complete publication");


            }else{
                //We have a dataset, no actions but cancel & refer to the publication
                //Find the publication of this dataset
                String url = contextPath + "/submissions";
                org.dspace.content.Item publication = DryadWorkflowUtils.getDataPackage(context, item);
                if(publication != null){
                    //We have a publication but we need the workspaceId
                    WorkspaceItem wsPub = WorkspaceItem.findByItemId(context, publication.getID());
                    url = contextPath + "/submit?workspaceID=" + wsPub.getID();
                }

                Cell cell = table.addRow().addCell(0, 2);

                cell.addContent(message("xmlui.submit.dataset.no-actions"));
                cell.addXref(url).addContent(message("xmlui.submit.dataset.no-actions.data-package"));
                cell.addContent(message("xmlui.submit.dataset.no-actions.2"));
            }
            // Everyone can just cancel
            Row row = table.addRow();
            row.addCell().addContent("Return to the submission page");
            row.addCell().addButton("submit_cancel").setValue(T_submit_cancel);

        }else{
            List form = div.addList("resume-submission",List.TYPE_FORM);

            org.dspace.app.xmlui.wing.element.Item actions = form.addItem();
            actions.addButton("submit_resume").setValue(T_submit_resume);
            actions.addButton("submit_cancel").setValue(T_submit_cancel);
        }


	}
}