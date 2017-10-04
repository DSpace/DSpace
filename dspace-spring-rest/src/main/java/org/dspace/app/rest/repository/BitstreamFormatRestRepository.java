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

import org.dspace.app.rest.converter.BitstreamFormatConverter;
import org.dspace.app.rest.model.BitstreamFormatRest;
import org.dspace.app.rest.model.hateoas.BitstreamFormatResource;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;


/**
 * This is the repository responsible to manage BitstreamFormat Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(BitstreamFormatRest.CATEGORY + "." + BitstreamFormatRest.NAME)
public class BitstreamFormatRestRepository extends DSpaceRestRepository<BitstreamFormatRest, Integer> {

	@Autowired
	BitstreamFormatService bfs;

	@Autowired
	BitstreamFormatConverter converter;

	public BitstreamFormatRestRepository() {
		System.out.println("Repository initialized by Spring");
	}

	@Override
	public BitstreamFormatRest findOne(Context context, Integer id) {
		BitstreamFormat bit = null;
		try {
			bit = bfs.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (bit == null) {
			return null;
		}
		return converter.fromModel(bit);
	}

	@Override
	public Page<BitstreamFormatRest> findAll(Context context, Pageable pageable) {
		List<BitstreamFormat> bit = null;
		try {
			bit = bfs.findAll(context);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<BitstreamFormatRest> page = utils.getPage(bit, pageable).map(converter);
		return page;
	}

	@Override
	public Class<BitstreamFormatRest> getDomainClass() {
		return BitstreamFormatRest.class;
	}

	@Override
	public BitstreamFormatResource wrapResource(BitstreamFormatRest bs, String... rels) {
		return new BitstreamFormatResource(bs, utils, rels);
	}
}