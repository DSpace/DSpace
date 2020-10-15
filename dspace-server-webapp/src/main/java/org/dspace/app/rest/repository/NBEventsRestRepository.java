/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.exception.InvalidEnumeratedDataValueException;
import org.dspace.app.nbevent.dao.NBEventsDao;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.nbevent.service.dto.NBEventImportDto;
import org.dspace.app.nbevent.service.dto.NBEventQueryDto;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.NBEventRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import org.slf4j.LoggerFactory;

@Component(NBEventRest.CATEGORY+ "." + NBEventRest.NAME)
public class NBEventsRestRepository extends DSpaceRestRepository<NBEventRest, String> {

	@Autowired
	private NBEventService nbEventService;
	
	@Autowired
	private NBEventsDao nbEventDao;
	
    @Autowired
    private ItemService itemService;

    @Autowired
    private EPersonService ePersonService;

	private Logger log = org.slf4j.LoggerFactory.getLogger(NBEventsRestRepository.class);
	
	@Override
    @PreAuthorize("permitAll()")
	public NBEventRest findOne(Context context, String id) {
		NBEventQueryDto nbEvent = null;
		try {
			nbEvent = nbEventService.findEventByEventId(context, id);
		} catch (SolrServerException | IOException e) {
			log.error("I/O or Solr communication error", e);
		} catch (InvalidEnumeratedDataValueException e) {
			log.error("Bad request", e);
			throw new BadRequestException();
		}
		if(nbEvent == null) {
			return null;
		}
		return converter.toRest(nbEvent, utils.obtainProjection());
	}
	
    @SearchRestMethod(name = "findByTopic")
    @PreAuthorize("permitAll()")
	public Page<NBEventRest> findByTopic(Context context, @RequestParam String topic, Pageable pageable) {
    	List<NBEventQueryDto> nbEvents = null;
    	Long count= 0L;
    	try {
			nbEvents = nbEventService.findEventsByTopicAndPage(context, topic, pageable.getOffset(), pageable.getPageSize());
	    	count = nbEventService.countEventsByTopic(context, topic);
	    	if(count == null) {
	    		count = 0L;
	    	}
		} catch (SolrServerException | IOException e) {
			log.error("I/O or Solr communication error", e);
		} catch (InvalidEnumeratedDataValueException e) {
			log.error("Bad request", e);
			throw new BadRequestException();
		}
    	if(nbEvents == null) {
    		return null;
    	}
		return converter.toRestPage(nbEvents, pageable, count, utils.obtainProjection());	
    }


	@Override
	protected void delete(Context context, String id) throws AuthorizeException {
		try {
            Item item = itemService.find(context, UUID.fromString(id));
            EPerson eperson = context.getCurrentUser();
			NBEventImportDto nbEvent = nbEventService.deleteEventByResourceUUID(id);
			if(nbEvent != null) {
				nbEventDao.storeEvent(context, nbEvent.getHashString(), eperson, item);
			}
		} catch (SolrServerException | IOException e) {
			log.error("Solr query error for UUID "+id, e);
			throw new BadRequestException(e);
		} catch (SQLException e) {
			log.error("DB Error");
			throw new InternalServerErrorException(e);
		}
	}

	@Override
	public Page<NBEventRest> findAll(Context context, Pageable pageable) {
		throw new RepositoryMethodNotImplementedException(NBEventRest.NAME, "findAll");
	}

	@Override
	public NBEventRest patch(HttpServletRequest request, String apiCategory, String model, String id, Patch patch)
			throws UnprocessableEntityException, DSpaceBadRequestException {
		// TODO Auto-generated method stub
		return super.patch(request, apiCategory, model, id, patch);
	}

	@Override
	public Class<NBEventRest> getDomainClass() {
		return NBEventRest.class;
	}

}
