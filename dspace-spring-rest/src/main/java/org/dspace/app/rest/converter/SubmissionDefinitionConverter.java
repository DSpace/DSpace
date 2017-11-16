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
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionStepConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the SubmissionConfig in the DSpace API data
 * model and the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SubmissionDefinitionConverter extends DSpaceConverter<SubmissionConfig, SubmissionDefinitionRest> {

	@Autowired
	private SubmissionSectionConverter panelConverter;
	
	@Override
	public SubmissionDefinitionRest fromModel(SubmissionConfig obj) {
		SubmissionDefinitionRest sd = new SubmissionDefinitionRest();
		sd.setName(obj.getSubmissionName());
		sd.setDefaultConf(obj.isDefaultConf());
		List<SubmissionSectionRest> panels = new LinkedList<SubmissionSectionRest>();
		for (int idx = 0; idx < obj.getNumberOfSteps(); idx++) {
			SubmissionStepConfig step = obj.getStep(idx);
			if (step.isVisible()) {
				SubmissionSectionRest sp = panelConverter.convert(step);				
				panels.add(sp);
			}
		}
		sd.setPanels(panels);
		return sd;
	}

	@Override
	public SubmissionConfig toModel(SubmissionDefinitionRest obj) {
		throw new NotImplementedException();
	}
}