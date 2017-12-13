/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.List;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataUpload;
import org.dspace.app.rest.model.step.UploadBitstreamRest;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Request;

/**
 * Upload step for DSpace Spring Rest. Expose information about the bitstream
 * uploaded for the in progress submission.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class UploadStep extends org.dspace.submit.step.UploadStep implements AbstractRestProcessingStep {

	
	@Override
	public DataUpload getData(SubmissionService submissionService, WorkspaceItem obj, SubmissionStepConfig config) throws Exception {

		DataUpload result = new DataUpload();
		List<Bundle> bundles = itemService.getBundles(obj.getItem(), Constants.CONTENT_BUNDLE_NAME);
		for (Bundle bundle : bundles) {
			for (Bitstream source : bundle.getBitstreams()) {
				UploadBitstreamRest b = submissionService.buildUploadBitstream(configurationService, source);
				result.getFiles().add(b);
			}
		}
		return result;
	}

	@Override
	public void doPatchProcessing(Context context, Request currentRequest, WorkspaceItem source, Operation op) throws Exception {
		
		String instance = "";
		if("remove".equals(op.getOp())) {
			if(op.getPath().contains(UPLOAD_STEP_METADATA_PATH)) {
				instance = UPLOAD_STEP_METADATA_OPERATION_ENTRY;
			}
			else if(op.getPath().contains(UPLOAD_STEP_ACCESSCONDITIONS_OPERATION_ENTRY)) {
				instance = UPLOAD_STEP_ACCESSCONDITIONS_OPERATION_ENTRY;
			}
			else {
				instance = UPLOAD_STEP_REMOVE_OPERATION_ENTRY;	
			}			
		}
		else if("move".equals(op.getOp())) {
			if(op.getPath().contains(UPLOAD_STEP_METADATA_PATH)) {
				instance = UPLOAD_STEP_METADATA_OPERATION_ENTRY;
			}
			else {
				instance = UPLOAD_STEP_MOVE_OPERATION_ENTRY;	
			}		
		}
		else {
			if(op.getPath().contains(UPLOAD_STEP_ACCESSCONDITIONS_OPERATION_ENTRY)) {
				instance = UPLOAD_STEP_ACCESSCONDITIONS_OPERATION_ENTRY;
			}
			else {
				instance = UPLOAD_STEP_METADATA_OPERATION_ENTRY;
			}
		}
		PatchOperation<?> patchOperation = new PatchOperationFactory().instanceOf(instance, op.getOp());
		patchOperation.perform(context, currentRequest, source, op);

	}

}
