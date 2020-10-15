/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.exception.InvalidEnumeratedDataValueException;
import org.dspace.app.nbevent.service.dto.NBEventImportDto;
import org.dspace.app.nbevent.service.dto.NBEventQueryDto;
import org.dspace.app.nbevent.service.dto.NBTopic;
import org.dspace.core.Context;
import org.dspace.util.SolrImportExportException;

public interface NBEventService {

	NBTopic findTopicByTopicId(String topicId) throws SolrServerException, IOException, InvalidEnumeratedDataValueException;

	List<NBTopic> findAllTopics(Context context, long offset, Integer count) throws SolrServerException, IOException, InvalidEnumeratedDataValueException;

	public int countTopics(Context context) throws SolrServerException, IOException;
	
	List<NBEventQueryDto> findEventsByTopicAndPage(Context context, String topic, long offset, int pageSize) throws SolrServerException, IOException, InvalidEnumeratedDataValueException;

	Long countEventsByTopic(Context context, String topic) throws InvalidEnumeratedDataValueException, SolrServerException, IOException;

	NBEventQueryDto findEventByEventId(Context context, String id) throws SolrServerException, IOException, InvalidEnumeratedDataValueException;

	void store(Context context, List<NBEventImportDto> entries)
			throws SolrImportExportException, SolrServerException, IOException, SQLException;

	NBEventImportDto deleteEventByResourceUUID(String id) throws SolrServerException, IOException;
	
}
