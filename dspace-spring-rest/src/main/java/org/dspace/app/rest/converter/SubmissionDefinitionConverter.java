/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.InputFormRest;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionPanelRest;
import org.dspace.app.rest.model.SubmissionVisibilityRest;
import org.dspace.app.rest.model.VisibilityEnum;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionStepConfig;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the SubmissionConfig in the DSpace API data
 * model and the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SubmissionDefinitionConverter extends DSpaceConverter<SubmissionConfig, SubmissionDefinitionRest> {

	@Override
	public SubmissionDefinitionRest fromModel(SubmissionConfig obj) {
		SubmissionDefinitionRest sd = new SubmissionDefinitionRest();
		sd.setName(obj.getSubmissionName());
		sd.setDefaultConf(obj.isDefaultConf());
		List<SubmissionPanelRest> panels = new LinkedList<SubmissionPanelRest>();
		for (int idx = 0; idx < obj.getNumberOfSteps(); idx++) {
			SubmissionStepConfig step = obj.getStep(idx);
			if (step.isVisible()) {
				SubmissionPanelRest sp = getPanel(step);
				panels.add(sp);
			}
		}
		sd.setPanels(panels);
		return sd;
	}

	private SubmissionPanelRest getPanel(SubmissionStepConfig step) {
		SubmissionPanelRest sp = new SubmissionPanelRest();
		sp.setMandatory(step.isMandatory());
		sp.setHeader(step.getHeading());
		sp.setType(step.getType());
		sp.setId(step.getId());
		sp.setVisibility(new SubmissionVisibilityRest(VisibilityEnum.fromString(step.getVisibility()),
				VisibilityEnum.fromString(step.getVisibilityOutside())));
		return sp;
	}

	@Override
	public SubmissionConfig toModel(SubmissionDefinitionRest obj) {
		throw new NotImplementedException();
	}
}