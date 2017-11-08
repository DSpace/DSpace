/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.SubmissionDefinitionConverter;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.hateoas.SubmissionDefinitionResource;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage MetadataField Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(SubmissionDefinitionRest.CATEGORY + "." + SubmissionDefinitionRest.NAME)
public class SubmissionDefinitionRestRepository extends DSpaceRestRepository<SubmissionDefinitionRest, String> {
	private SubmissionConfigReader submissionConfigReader;

	private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

	@Autowired
	private SubmissionDefinitionConverter converter;

	public SubmissionDefinitionRestRepository() throws SubmissionConfigReaderException {
		submissionConfigReader = new SubmissionConfigReader();
	}

	@Override
	public SubmissionDefinitionRest findOne(Context context, String submitName) {
		SubmissionConfig subConfig = submissionConfigReader.getSubmissionConfigByName(submitName);
		if (subConfig == null) {
			return null;
		}
		return converter.convert(subConfig);
	}

	@Override
	public Page<SubmissionDefinitionRest> findAll(Context context, Pageable pageable) {
		List<SubmissionConfig> subConfs = new ArrayList<SubmissionConfig>();
		int total = submissionConfigReader.countSubmissionConfigs();
		subConfs = submissionConfigReader.getAllSubmissionConfigs(pageable.getPageSize(), pageable.getOffset());
		Page<SubmissionDefinitionRest> page = new PageImpl<SubmissionConfig>(subConfs, pageable, total).map(converter);
		return page;
	}

	@SearchRestMethod(name = "findByCollection")
	public SubmissionDefinitionRest findByCollection(@Param(value = "uuid") UUID collectionUuid) throws SQLException {
		Collection col = collectionService.find(obtainContext(), collectionUuid);
		if (col == null) {
			return null;
		}
		SubmissionDefinitionRest def = converter
				.convert(submissionConfigReader.getSubmissionConfigByCollection(col.getHandle()));
		return def;
	}

	@Override
	public Class<SubmissionDefinitionRest> getDomainClass() {
		return SubmissionDefinitionRest.class;
	}

	@Override
	public SubmissionDefinitionResource wrapResource(SubmissionDefinitionRest sd, String... rels) {
		return new SubmissionDefinitionResource(sd, utils, rels);
	}
}