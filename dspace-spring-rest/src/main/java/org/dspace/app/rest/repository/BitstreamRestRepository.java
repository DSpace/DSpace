package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(BitstreamRest.NAME)
public class BitstreamRestRepository extends DSpaceRestRepository<BitstreamRest, UUID> {
	BitstreamService bs = ContentServiceFactory.getInstance().getBitstreamService();
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
		return converter.fromModel(bit);
	}

	@Override
	public Page<BitstreamRest> findAll(Context context, Pageable pageable) {
		List<Bitstream> bit = null;
		int total = 0;
		try {
			total = bs.countTotal(context);
			bit = bs.findAll(context);
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
}