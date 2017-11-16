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

import org.dspace.app.rest.converter.SubmissionFormConverter;
import org.dspace.app.rest.model.SubmissionFormRest;
import org.dspace.app.rest.model.hateoas.SubmissionFormResource;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage InputForm Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(SubmissionFormRest.CATEGORY + "." + SubmissionFormRest.NAME)
public class SubmissionFormRestRepository extends DSpaceRestRepository<SubmissionFormRest, String> implements LinkRestRepository<SubmissionFormRest> {

	private DCInputsReader inputReader;

	@Autowired
	private SubmissionFormConverter converter;

	public SubmissionFormRestRepository() throws DCInputsReaderException {
		inputReader = new DCInputsReader();
	}

	@Override
	public SubmissionFormRest findOne(Context context, String submitName) {
		DCInputSet inputConfig;
		try {
			inputConfig = inputReader.getInputsByFormName(submitName);
		} catch (DCInputsReaderException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		if (inputConfig == null) {
			return null;
		}
		return converter.convert(inputConfig);
	}

	@Override
	public Page<SubmissionFormRest> findAll(Context context, Pageable pageable) {
		List<DCInputSet> subConfs = new ArrayList<DCInputSet>();
		int total = inputReader.countInputs();
		try {
			subConfs = inputReader.getAllInputs(pageable.getPageSize(), pageable.getOffset());
		} catch (DCInputsReaderException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		Page<SubmissionFormRest> page = new PageImpl<DCInputSet>(subConfs, pageable, total).map(converter);
		return page;
	}

	@Override
	public Class<SubmissionFormRest> getDomainClass() {
		return SubmissionFormRest.class;
	}

	@Override
	public SubmissionFormResource wrapResource(SubmissionFormRest sd, String... rels) {
		return new SubmissionFormResource(sd, utils, rels);
	}
}