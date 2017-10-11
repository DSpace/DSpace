/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.converter.MetadataFieldConverter;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.model.hateoas.MetadataFieldResource;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage MetadataField Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(MetadataFieldRest.CATEGORY + "." + MetadataFieldRest.NAME)
public class MetadataFieldRestRepository extends DSpaceRestRepository<MetadataFieldRest, Integer> {

	@Autowired
	MetadataFieldService metaFieldService;

	@Autowired
	MetadataFieldConverter converter;

	public MetadataFieldRestRepository() {
	}

	@Override
	public MetadataFieldRest findOne(Context context, Integer id) {
		MetadataField metadataField = null;
		try {
			metadataField = metaFieldService.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (metadataField == null) {
			return null;
		}
		return converter.fromModel(metadataField);
	}

	@Override
	public Page<MetadataFieldRest> findAll(Context context, Pageable pageable) {
		List<MetadataField> metadataField = null;
		try {
			metadataField = metaFieldService.findAll(context);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<MetadataFieldRest> page = utils.getPage(metadataField, pageable).map(converter);
		return page;
	}

	@Override
	public Class<MetadataFieldRest> getDomainClass() {
		return MetadataFieldRest.class;
	}

	@Override
	public MetadataFieldResource wrapResource(MetadataFieldRest bs, String... rels) {
		return new MetadataFieldResource(bs, utils, rels);
	}
}