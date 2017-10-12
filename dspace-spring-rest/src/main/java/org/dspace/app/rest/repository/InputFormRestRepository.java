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

import org.dspace.app.rest.converter.InputFormConverter;
import org.dspace.app.rest.model.InputFormRest;
import org.dspace.app.rest.model.hateoas.InputFormResource;
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
@Component(InputFormRest.CATEGORY + "." + InputFormRest.NAME)
public class InputFormRestRepository extends DSpaceRestRepository<InputFormRest, String> implements LinkRestRepository<InputFormRest> {

	private DCInputsReader inputReader;

	@Autowired
	private InputFormConverter converter;

	public InputFormRestRepository() throws DCInputsReaderException {
		inputReader = new DCInputsReader();
	}

	@Override
	public InputFormRest findOne(Context context, String submitName) {
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
	public Page<InputFormRest> findAll(Context context, Pageable pageable) {
		List<DCInputSet> subConfs = new ArrayList<DCInputSet>();
		int total = inputReader.countInputs();
		try {
			subConfs = inputReader.getAllInputs(pageable.getPageSize(), pageable.getOffset());
		} catch (DCInputsReaderException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		Page<InputFormRest> page = new PageImpl<DCInputSet>(subConfs, pageable, total).map(converter);
		return page;
	}

	@Override
	public Class<InputFormRest> getDomainClass() {
		return InputFormRest.class;
	}

	@Override
	public InputFormResource wrapResource(InputFormRest sd, String... rels) {
		return new InputFormResource(sd, utils, rels);
	}
}