/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.exception.InvalidEnumeratedDataValueException;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.nbevent.service.dto.NBTopic;
import org.dspace.app.nbevent.service.impl.NBEventServiceImpl;
import org.dspace.app.rest.model.NBTopicRest;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(NBTopicRest.CATEGORY+ "." + NBTopicRest.NAME)
public class NBTopicRestRepository extends DSpaceRestRepository<NBTopicRest, String> {

	@Autowired
	private NBEventService nbEventService;
	
	private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
		.getLogger(NBTopicRestRepository.class);
	
	@Override
    @PreAuthorize("permitAll()")
	public NBTopicRest findOne(Context context, String id) {
		NBTopic nbTopic = null;
		try {
			nbTopic = nbEventService.findTopicByTopicId(NBEventServiceImpl.OpenstarSupportedTopic.restToSolr(id));
		} catch (InvalidEnumeratedDataValueException e) {
			log.error("Invalid topic in request", e);
			throw new InternalServerErrorException(e);
		} catch (IOException | SolrServerException e) {
			log.error("I/O or Solr communication error", e);
			throw new BadRequestException(e);
		}
		if(nbTopic == null) {
			return null;
		}
		return converter.toRest(nbTopic, utils.obtainProjection());
	}

	@Override
	public Page<NBTopicRest> findAll(Context context, Pageable pageable) {
		List<NBTopic> nbTopics = null;
		int count = 0;
		try {
			nbTopics = nbEventService.findAllTopics(context, pageable.getOffset(), pageable.getPageSize());
			count = nbEventService.countTopics(context);
		} catch (SolrServerException | IOException e) {
			log.error("I/O or Solr communication error", e);
			throw new InternalServerErrorException(e);
		} catch (InvalidEnumeratedDataValueException e) {
			log.error("Invalid data on Solr", e);
			throw new BadRequestException(e);
		}
		if(nbTopics == null) {
			return null;
		}
		return converter.toRestPage(nbTopics, pageable, count, utils.obtainProjection());	
	}

	@Override
	public Class<NBTopicRest> getDomainClass() {
		return NBTopicRest.class;
	}

}
