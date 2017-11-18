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

import org.dspace.app.rest.converter.BrowseIndexConverter;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.hateoas.BrowseIndexResource;
import org.dspace.app.rest.model.hateoas.CollectionResource;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to Browse Index Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(BrowseIndexRest.CATEGORY + "." + BrowseIndexRest.NAME)
public class BrowseIndexRestRepository extends DSpaceRestRepository<BrowseIndexRest, String> {
	@Autowired
	BrowseIndexConverter converter;
	
	@Override
	public BrowseIndexRest findOne(Context context, String name) {
		BrowseIndexRest bi = null;
		BrowseIndex bix;
		try {
			bix = BrowseIndex.getBrowseIndex(name);
		} catch (BrowseException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (bix != null) {
			bi = converter.convert(bix);
		}
		return bi;
	}

	@Override
	public Page<BrowseIndexRest> findAll(Context context, Pageable pageable) {
		List<BrowseIndexRest> it = null;
		List<BrowseIndex> indexesList = new ArrayList<BrowseIndex>();
		int total = 0;
		try {
			BrowseIndex[] indexes = BrowseIndex.getBrowseIndices();
			total = indexes.length;
			for (BrowseIndex bix: indexes) {
				indexesList.add(bix);
			}
		} catch (BrowseException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<BrowseIndexRest> page = new PageImpl<BrowseIndex>(indexesList, pageable, total).map(converter);
		return page;
	}
	
	@Override
	public Class<BrowseIndexRest> getDomainClass() {
		return BrowseIndexRest.class;
	}
	
	@Override
	public BrowseIndexResource wrapResource(BrowseIndexRest bix, String... rels) {
		return new BrowseIndexResource(bix, utils, rels);
	}

}