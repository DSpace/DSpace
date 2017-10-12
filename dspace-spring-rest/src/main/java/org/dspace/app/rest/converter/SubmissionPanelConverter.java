/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.lang.NotImplementedException;
import org.dspace.app.rest.model.SubmissionPanelRest;
import org.dspace.app.rest.model.SubmissionVisibilityRest;
import org.dspace.app.rest.model.VisibilityEnum;
import org.dspace.app.util.SubmissionStepConfig;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the SubmissionStepConfig in the DSpace API data
 * model and the REST data model
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class SubmissionPanelConverter extends DSpaceConverter<SubmissionStepConfig, SubmissionPanelRest> {

	@Override
	public SubmissionPanelRest fromModel(SubmissionStepConfig step) {
		SubmissionPanelRest sp = new SubmissionPanelRest();
		sp.setMandatory(step.isMandatory());
		sp.setHeader(step.getHeading());
		sp.setPanelType(step.getType());
		sp.setId(step.getId());
		sp.setVisibility(new SubmissionVisibilityRest(VisibilityEnum.fromString(step.getVisibility()),
				VisibilityEnum.fromString(step.getVisibilityOutside())));	
		return sp;
	}

	@Override
	public SubmissionStepConfig toModel(SubmissionPanelRest obj) {
		throw new NotImplementedException();
	}
}