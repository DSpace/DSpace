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

import org.dspace.app.rest.converter.MetadataSchemaConverter;
import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.app.rest.model.hateoas.MetadataSchemaResource;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage MetadataSchema Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(MetadataSchemaRest.CATEGORY + "." + MetadataSchemaRest.NAME)
public class MetadataSchemaRestRepository extends DSpaceRestRepository<MetadataSchemaRest, Integer> {

	@Autowired
	MetadataSchemaService metaScemaService;

	@Autowired
	MetadataSchemaConverter converter;

	public MetadataSchemaRestRepository() {
	}

	@Override
	public MetadataSchemaRest findOne(Context context, Integer id) {
		MetadataSchema metadataSchema = null;
		try {
			metadataSchema = metaScemaService.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (metadataSchema == null) {
			return null;
		}
		return converter.fromModel(metadataSchema);
	}

	@Override
	public Page<MetadataSchemaRest> findAll(Context context, Pageable pageable) {
		List<MetadataSchema> metadataSchema = null;
		try {
			metadataSchema = metaScemaService.findAll(context);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<MetadataSchemaRest> page = utils.getPage(metadataSchema, pageable).map(converter);
		return page;
	}

	@Override
	public Class<MetadataSchemaRest> getDomainClass() {
		return MetadataSchemaRest.class;
	}

	@Override
	public MetadataSchemaResource wrapResource(MetadataSchemaRest bs, String... rels) {
		return new MetadataSchemaResource(bs, utils, rels);
	}
}