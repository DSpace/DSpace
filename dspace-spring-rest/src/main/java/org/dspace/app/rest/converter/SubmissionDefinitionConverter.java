/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
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

	private static final Logger log = Logger.getLogger(SubmissionDefinitionConverter.class);
	
	@Autowired
	private SubmissionSectionConverter panelConverter;
	
	@Autowired
	private RequestService requestService;
	
	@Autowired
	private CollectionConverter collectionConverter;
	
	@Override
	public SubmissionDefinitionRest fromModel(SubmissionConfig obj) {
		SubmissionDefinitionRest sd = new SubmissionDefinitionRest();
		sd.setName(obj.getSubmissionName());
		sd.setDefaultConf(obj.isDefaultConf());
		List<SubmissionSectionRest> panels = new LinkedList<SubmissionSectionRest>();
		for (int idx = 0; idx < obj.getNumberOfSteps(); idx++) {
			SubmissionStepConfig step = obj.getStep(idx);
			SubmissionSectionRest sp = panelConverter.convert(step);				
			panels.add(sp);
		}
		
		HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
		Context context = null;
		try {
			context = ContextUtil.obtainContext(request);
			List<Collection> collections = panelConverter.getSubmissionConfigReader().getCollectionsBySubmissionConfig(context, obj.getSubmissionName());
			List<CollectionRest> collectionsRest = collections.stream().map((collection) -> collectionConverter.convert(collection)).collect( Collectors.toList());
			sd.setCollections(collectionsRest);
		} catch (SQLException | IllegalStateException | SubmissionConfigReaderException e) {
			log.error(e.getMessage(), e);
		}		
		sd.setPanels(panels);
		return sd;
	}

	@Override
	public SubmissionConfig toModel(SubmissionDefinitionRest obj) {
		throw new NotImplementedException();
	}
}