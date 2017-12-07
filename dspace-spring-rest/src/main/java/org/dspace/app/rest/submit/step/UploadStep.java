/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.dspace.app.rest.converter.BitstreamFormatConverter;
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
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

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
	public void doPatchProcessing(Context context, Request currentRequest, WorkspaceItem source, String operation,
			String path, Object value) throws Exception {
		
		String[] split = path.split("/");
		String instance = "";
		if("remove".equals(operation)) {
			instance = "bitstreamremove";
		}
		else {
			instance = split[2];
		}
		PatchOperation<?> patchOperation = new PatchOperationFactory().instanceOf(instance, operation);
		patchOperation.perform(context, currentRequest, source, path, value);

	}

}
