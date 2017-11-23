/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.dspace.app.rest.converter.SubmissionSectionConverter;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.model.hateoas.SubmissionSectionResource;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Submission Step (aka Panel) Rest object
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@Component(SubmissionDefinitionRest.CATEGORY + "." + SubmissionSectionRest.NAME)
public class SubmissionPanelRestRepository extends DSpaceRestRepository<SubmissionSectionRest, String> {

	private SubmissionConfigReader submissionConfigReader;

	@Autowired
	private SubmissionSectionConverter converter;
	
	public SubmissionPanelRestRepository() throws SubmissionConfigReaderException {
		submissionConfigReader = new SubmissionConfigReader();
	}
	
	@Override
	public SubmissionSectionRest findOne(Context context, String id) {
		try {
			SubmissionStepConfig step = submissionConfigReader.getStepConfig(id);
			return converter.convert(step);
		} catch (SubmissionConfigReaderException e) {
			//TODO wrap with a specific exception
			throw new RuntimeException(e.getMessage(), e);
		}		
	}

	@Override
	public Page<SubmissionSectionRest> findAll(Context context, Pageable pageable) {
		List<SubmissionConfig> subConfs = new ArrayList<SubmissionConfig>();
		subConfs = submissionConfigReader.getAllSubmissionConfigs(pageable.getPageSize(), pageable.getOffset());
		int total = 0;
		List<SubmissionStepConfig> stepConfs = new ArrayList<>();
		for(SubmissionConfig config : subConfs) {
			total =+ config.getNumberOfSteps();
			for(int i = 0; i<config.getNumberOfSteps(); i++) {
				SubmissionStepConfig step = config.getStep(i);
				stepConfs.add(step);
			}
		}
		Page<SubmissionSectionRest> page = new PageImpl<SubmissionStepConfig>(stepConfs, pageable, total).map(converter);
		return page;
	}

	@Override
	public Class<SubmissionSectionRest> getDomainClass() {
		return SubmissionSectionRest.class;
	}

	@Override
	public SubmissionSectionResource wrapResource(SubmissionSectionRest model, String... rels) {
		return new SubmissionSectionResource(model, utils, rels);
	}

}