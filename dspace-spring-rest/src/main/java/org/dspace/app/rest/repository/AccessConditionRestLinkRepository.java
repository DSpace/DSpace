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
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.AccessConditionsConverter;
import org.dspace.app.rest.model.AccessConditionRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.hateoas.AccessConditionResource;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of default access condition
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME + "." + CollectionRest.DEFAULT_ACCESS_CONDITIONS)
public class AccessConditionRestLinkRepository extends AbstractDSpaceRestRepository
		implements LinkRestRepository<AccessConditionRest> {

	@Autowired
	CollectionService collectionService;
	
	@Autowired
	AuthorizeService authorizeService;
	
	@Autowired
	AccessConditionsConverter accessConditionsConverter;
	
	@Autowired
	Utils utils;
	
	@Override
	public ResourceSupport wrapResource(AccessConditionRest model, String... rels) {
		return new AccessConditionResource(model);
	}
	
	public Page<AccessConditionRest> getDefaultBitstreamPoliciesForCollection(HttpServletRequest request, UUID uuid, Pageable pageable, String projection) throws Exception {
		Context context = obtainContext();
		Collection collection = collectionService.find(context, uuid);
		
		List<AccessConditionRest> results = new ArrayList<AccessConditionRest>();
		
		List<ResourcePolicy> defaultCollectionPolicies = authorizeService.getPoliciesActionFilter(context, collection, Constants.DEFAULT_BITSTREAM_READ);
		
		for(ResourcePolicy pp : defaultCollectionPolicies) {
			AccessConditionRest accessCondition = accessConditionsConverter.convert(pp);
			if (accessCondition != null) {
				results.add(accessCondition);
			}
		}
		return utils.getPage(results, pageable);
	}
	
}
