/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.UUID;

import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

/**
 * Collection step for DSpace Spring Rest. Expose the collection information of the in progress submission.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class CollectionStep extends org.dspace.submit.step.SelectCollectionStep implements AbstractRestProcessingStep {

	@Override
	public UUID getData(SubmissionService submissionService, WorkspaceItem obj, SubmissionStepConfig config) {
		if(obj.getCollection()!=null) {
			return obj.getCollection().getID();
		}
		return null;
	}

	@Override
	public void doPatchProcessing(Context context, Request currentRequest, WorkspaceItem source, String operation,
			String path, Object value) {
		switch (operation) {
		case "move":
			
			break;
		default:
			throw new RuntimeException("Operation "+operation+" not yet implemented!");
		}
	}
}
