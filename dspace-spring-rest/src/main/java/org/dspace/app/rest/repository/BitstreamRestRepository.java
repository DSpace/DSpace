/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Bitstream Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(BitstreamRest.CATEGORY + "." + BitstreamRest.NAME)
public class BitstreamRestRepository extends DSpaceRestRepository<BitstreamRest, UUID> {

	@Autowired
	BitstreamService bs;

	@Autowired
	BitstreamConverter converter;
	
	public BitstreamRestRepository() {
		System.out.println("Repository initialized by Spring");
	}

	@Override
	public BitstreamRest findOne(Context context, UUID id) {
		Bitstream bit = null;
		try {
			bit = bs.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (bit == null) {
			return null;
		}
		return converter.fromModel(bit);
	}

	@Override
	public Page<BitstreamRest> findAll(Context context, Pageable pageable) {
		List<Bitstream> bit = new ArrayList<Bitstream>();
		Iterator<Bitstream> it = null;
		int total = 0;
		try {
			total = bs.countTotal(context);
			it = bs.findAll(context, pageable.getPageSize(), pageable.getOffset());
			while(it.hasNext()) {
				bit.add(it.next());
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<BitstreamRest> page = new PageImpl<Bitstream>(bit, pageable, total).map(converter);
		return page;
	}
	
	@Override
	public Class<BitstreamRest> getDomainClass() {
		return BitstreamRest.class;
	}
	
	@Override	
	public BitstreamResource wrapResource(BitstreamRest bs, String... rels) {
		return new BitstreamResource(bs, utils, rels);
	}

	public InputStream retrieve(UUID uuid) {
		Context context = obtainContext();
		Bitstream bit = null;
		try {
			bit = bs.find(context, uuid);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (bit == null) {
			return null;
		}
		InputStream is;
		try {
			is = bs.retrieve(context, bit);
		} catch (IOException | SQLException | AuthorizeException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		context.abort();
		return is;
	}
}