/*
 * RemoveSubmissionAction.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 19:02:24 +0200 (za, 11 apr 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
import java.util.*;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Remove all selected submissions. This action is used by the
 * submission page, the user may check each unfinished submission
 * and when thy click the remove submissions button this action
 * will remove them all.
 *
 * @author Scott Phillips
 *
 * This class has been altered so that when the submitter deletes his data package that the data file is also removed
 */
public class RemoveSubmissionsAction extends AbstractAction
{

    private static final Logger log = Logger.getLogger(RemoveSubmissionsAction.class);


    /**
     * Remove all selected submissions
     *
     * @param pattern
     *            un-used.
     * @param objectModel
     *            Cocoon's object model
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {

        Context context = ContextUtil.obtainContext(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);


    	String[] workspaceIDs = request.getParameterValues("workspaceID");

    	if (workspaceIDs != null)
    	{
        	for (String workspaceID : workspaceIDs)
        	{
        		// If they selected to remove the item then delete everything.
    			WorkspaceItem workspaceItem = WorkspaceItem.find(context, Integer.valueOf(workspaceID));
			//If we are removing a data package, also remove the data files
			try {
			    Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, workspaceItem.getItem());
			    for (Item dataFile : dataFiles) {
				WorkspaceItem datafileItem = WorkspaceItem.findByItemId(context, dataFile.getID());
				//Found so delete it
				datafileItem.deleteAll();
			    }
			} catch(Exception e) {
                log.error("Error while removing submission", e);
			}

                //Make sure we remove all child datasets
                removeDatasets(context, workspaceItem.getItem());

                

//                workspaceItem.deleteWrapper();
                workspaceItem.deleteAll();
        	}
        	context.commit();
    	}

        return null;
    }

    private void removeDatasets(Context context, Item publication) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        //If our publication doesn't have a handle this probably implies that we have no datasets so just return
        if(publication.getHandle() == null)
            return;

        String pubUrl = HandleManager.resolveToURL(context, publication.getHandle());

        //We have handle already so find & remove that one
        // We need to find an inprogressSubmission for this item so we can delete that one
        List<InProgressSubmission> allUserSubmissions = new ArrayList<InProgressSubmission>();
        Collections.addAll(allUserSubmissions, WorkflowItem.findByEPerson(context, context.getCurrentUser()));
        Collections.addAll(allUserSubmissions, WorkspaceItem.findByEPerson(context, context.getCurrentUser()));

        for (InProgressSubmission inProgressSubmission : allUserSubmissions){
            DCValue[] pubs = inProgressSubmission.getItem().getMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", Item.ANY);
            //Check if one of our values is the puburl, thus implying that this item is a dataset for this publication
            for (DCValue parentUrl : pubs) {
                if (parentUrl != null && parentUrl.value != null && parentUrl.value.equals(pubUrl)) {
                    //We have a match remove this item !
                    if (inProgressSubmission instanceof WorkspaceItem)
                        ((WorkspaceItem) inProgressSubmission).deleteAll();
                    else {
                        WorkflowItem workFlowItemToDel = (WorkflowItem) inProgressSubmission;
                        workFlowItemToDel.deleteWrapper();
                        //TODO: make sure that no email is sent out
                        WorkspaceItem wsi = WorkflowManager.rejectWorkflowItem(context, workFlowItemToDel, context.getCurrentUser(), null, "Deleted publication", false);
                        wsi.deleteAll();
                    }

                }
            }
            

        }


    }

}
