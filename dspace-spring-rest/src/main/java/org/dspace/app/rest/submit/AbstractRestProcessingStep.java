/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.submit.step.validation.Validation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;

/**
 * Interface to retrieve information about section
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public interface AbstractRestProcessingStep {

	public <T extends Serializable> T getData(SubmissionService submissionService, WorkspaceItem obj, SubmissionStepConfig config) throws Exception;
	
	default public List<ErrorRest> validate(SubmissionService submissionService, WorkspaceItem obj, SubmissionStepConfig config) throws Exception
	{
		List<ErrorRest> errors = new ArrayList<ErrorRest>();
		List<Validation> validations = DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(Validation.class);
		if(validations != null) {
			for(Validation validation : validations) {
				if(validation.getName().equals(config.getType())) {
					errors.addAll(validation.validate(submissionService, obj, config));
				}
			}
		}
		return errors;
	}

	public void doPatchProcessing(Context context, Request currentRequest, WorkspaceItem source, String operation,
			String path, Object value) throws Exception;

}
