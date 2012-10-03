/*
 * CompletedStep.java
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
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;
import org.apache.cocoon.environment.ObjectModelHelper;

import javax.servlet.http.HttpSession;

/**
 * This is a conformation page informing the user that they have
 * completed the submission of the item. It tells them what to
 * expect next, i.e. the workflow, and gives the option to go home
 * or start another submission.
 *
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 *
 * Page has been adjusted to show different data
 */
public class CompletedStep extends AbstractSubmissionStep
{

	/** Language Strings **/
	protected static final Message T_head =
        message("xmlui.submit.complete.dataset.head");
	protected static final Message T_info1 =
        message("xmlui.Submission.submit.CompletedStep.info1");
    protected static final Message T_go_submission =
        message("xmlui.Submission.submit.CompletedStep.go_submission");
	protected static final Message T_submit_again =
        message("xmlui.Submission.submit.CompletedStep.submit_again");
    private static final Message T_HELP = message("xmlui.submit.complete.dataset.help");
    private static final Message T_LABEL_PUB = message("xmlui.submit.complete.dataset.label.pub");
    private static final Message T_LABEL_PUB2 = message("xmlui.submit.complete.dataset.label.pub-only");
    private static final Message T_LABEL_DATA = message("xmlui.submit.complete.dataset.label.data");

    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public CompletedStep()
	{
		this.requireHandle = true;
	}

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        pageMeta.addMetadata("title").addContent("Dryad Submission");

        pageMeta.addTrailLink(contextPath + "/", "Dryad Home");
        pageMeta.addTrail().addContent("Submission");
    }

    public void addBody(Body body) throws SAXException, WingException,
	SQLException, IOException, AuthorizeException
	{
        HttpSession session = ObjectModelHelper.getRequest(objectModel).getSession();

        Division div = body.addDivision("submit-complete");
		div.setHead(T_head);

		div.addPara().addContent(T_HELP);
        /*
        List resultList = div.addList("paralist", List.TYPE_SIMPLE);

        if(session.getAttribute("dataset_handle") == null)
        {
            resultList.addLabel(T_LABEL_PUB2);
            resultList.addItem().addXref(HandleManager.resolveToURL(context, (String) session.getAttribute("publication_handle")), HandleManager.resolveToURL(context, (String) session.getAttribute("publication_handle")));

            //Check if we need to add all our datasets
            if(session.getAttribute("datasets_showall") != null && Boolean.valueOf(session.getAttribute("datasets_showall").toString())){
                //Retrieve all the datasets submitted by this person
                org.dspace.content.Item publication = (org.dspace.content.Item) HandleManager.resolveToObject(context, (String) session.getAttribute("publication_handle"));
                DCValue[] dataSetUrls = publication.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", org.dspace.content.Item.ANY);
                if(0 < dataSetUrls.length)
                    resultList.addLabel(T_LABEL_DATA);

                for (DCValue dataSetUrl : dataSetUrls) {
                    resultList.addItem().addXref(dataSetUrl.value, dataSetUrl.value);
                }
            }

        }else{
            org.dspace.content.Item dataset = (org.dspace.content.Item) HandleManager.resolveToObject(context, session.getAttribute("dataset_handle").toString());
            DCValue[] publicationMetadata = dataset.getMetadata("dc", "relation", "ispartof", org.dspace.content.Item.ANY);


            if(0 < publicationMetadata.length){
                resultList.addLabel(T_LABEL_PUB);
                resultList.addItem().addXref(publicationMetadata[0].value, publicationMetadata[0].value);
            }

            resultList.addLabel(T_LABEL_DATA);
            resultList.addItem().addXref(HandleManager.resolveToURL(context, dataset.getHandle()), HandleManager.resolveToURL(context, dataset.getHandle()));

            if(0 < publicationMetadata.length){
                //Get the rest
                WorkflowItem[] workflowItems = WorkflowItem.findByEPerson(context, context.getCurrentUser());
                for (WorkflowItem workflowItem : workflowItems) {
                    DCValue[] workFlowPubMetadata = workflowItem.getItem().getMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", org.dspace.content.Item.ANY);
                    for (DCValue partUrl : workFlowPubMetadata) {
                        if (publicationMetadata[0].value.equals(partUrl.value) && !workflowItem.getItem().getHandle().equals(dataset.getHandle())){
                            String url = HandleManager.resolveToURL(context, workflowItem.getItem().getHandle());
                            resultList.addItem().addXref(url, url);
                        }
                    }
                }

                WorkspaceItem[] workspaceItems = WorkspaceItem.findByEPerson(context, context.getCurrentUser());
                for (WorkspaceItem workspaceItem : workspaceItems) {
                    DCValue[] workspacePubMetadata = workspaceItem.getItem().getMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", org.dspace.content.Item.ANY);
                    for (DCValue partUrl : workspacePubMetadata) {
                        if (publicationMetadata[0].value.equals(partUrl.value) && workspaceItem.getItem().getHandle() != null && !workspaceItem.getItem().getHandle().equals(dataset.getHandle())) {
                            String url = HandleManager.resolveToURL(context, workspaceItem.getItem().getHandle());
                            resultList.addItem().addXref(url, url);
                        }
                    }
                }
            }

        }
        */

        div.addPara().addXref(contextPath+"/submissions",T_go_submission);
	}

    /**
     * Each submission step must define its own information to be reviewed
     * during the final Review/Verify Step in the submission process.
     * <P>
     * The information to review should be tacked onto the passed in
     * List object.
     * <P>
     * NOTE: To remain consistent across all Steps, you should first
     * add a sub-List object (with this step's name as the heading),
     * by using a call to reviewList.addList().   This sublist is
     * the list you return from this method!
     *
     * @param reviewList
     *      The List to which all reviewable information should be added
     * @return
     *      The new sub-List object created by this step, which contains
     *      all the reviewable information.  If this step has nothing to
     *      review, then return null!
     */
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, SQLException, IOException,
        AuthorizeException
    {
        //nothing to review, since submission is now Completed!
        return null;
    }
}