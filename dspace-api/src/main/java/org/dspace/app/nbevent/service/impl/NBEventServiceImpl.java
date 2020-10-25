/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.nbevent.NBTopic;
import org.dspace.app.nbevent.dao.impl.NBEventsDaoImpl;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class NBEventServiceImpl implements NBEventService {

    private static final Logger log = Logger.getLogger(NBEventServiceImpl.class);

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired
    private HandleService handleService;

    @Autowired
    private NBEventsDaoImpl nbEventsDao;

    private ObjectMapper jsonMapper;

    public NBEventServiceImpl() {
        jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    protected SolrClient solr = null;

    public static final String ORIGINAL_ID = "original_id";
    public static final String TITLE = "title";
    public static final String TOPIC = "topic";
    public static final String TRUST = "trust";
    public static final String MESSAGE = "message";
    public static final String EVENT_ID = "event_id";
    public static final String RESOURCE_UUID = "resource_uuid";
    public static final String LAST_UPDATE = "last_update";
    public static final String RELATED_UUID = "related_uuid";

    protected SolrClient getSolr() {
        if (solr == null) {
            String solrService = DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getProperty("oaire-nbevents.solr.server", "http://localhost:8983/solr/nbevent");
            return new HttpSolrClient.Builder(solrService).build();
        }
        return solr;
    }

    @Override
    public long countTopics(Context context) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery("*:*");
        solrQuery.setFacet(true);
        // we would like to get eventually topic that has no longer active nb events
        solrQuery.setFacetMinCount(0);
        solrQuery.addFacetField(TOPIC);
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return response.getFacetField(TOPIC).getValueCount();
    }

    @Override
    public void deleteEventByEventId(Context context, String id) {
        try {
            getSolr().deleteById(id);
            getSolr().commit();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NBTopic findTopicByTopicId(String topicId) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(TOPIC + ":" + topicId.replaceAll("!", "/"));
        solrQuery.setFacet(true);
        // we would like to get eventually topic that has no longer active nb events
        solrQuery.setFacetMinCount(0);
        solrQuery.addFacetField(TOPIC);
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
            FacetField facetField = response.getFacetField(TOPIC);
            for (Count c : facetField.getValues()) {
                if (c.getName().equals(topicId.replace("!", "/"))) {
                    NBTopic topic = new NBTopic();
                    topic.setKey(c.getName());
//                    topic.setName(OpenstarSupportedTopic.sorlToRest(c.getName()));
                    topic.setTotalEvents(c.getCount());
                    topic.setLastEvent(new Date());
                    return topic;
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Method to get all topics and the number of entries for each topic
     * 
     * @param context DSpace context
     * @param offset  number of results to skip
     * @param count   number of result to fetch
     * @return list of topics with number of events
     * @throws IOException
     * @throws SolrServerException
     * @throws InvalidEnumeratedDataValueException
     * 
     */
    @Override
    public List<NBTopic> findAllTopics(Context context, long offset, long count) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery("*:*");
        solrQuery.setFacet(true);
        // we would like to get eventually topic that has no longer active nb events
        solrQuery.setFacetMinCount(0);
        solrQuery.setFacetLimit((int) (offset + count));
        solrQuery.addFacetField(TOPIC);
        QueryResponse response;
        List<NBTopic> nbTopics = null;
        try {
            response = getSolr().query(solrQuery);
            FacetField facetField = response.getFacetField(TOPIC);
            nbTopics = new ArrayList<>();
            int idx = 0;
            for (Count c : facetField.getValues()) {
                if (idx < offset) {
                    idx++;
                    continue;
                }
                NBTopic topic = new NBTopic();
                topic.setKey(c.getName());
                // topic.setName(c.getName().replaceAll("/", "!"));
                topic.setTotalEvents(c.getCount());
                topic.setLastEvent(new Date());
                nbTopics.add(topic);
                idx++;
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return nbTopics;
    }

    @Override
    public void store(Context context, NBEvent dto) {
        UpdateRequest updateRequest = new UpdateRequest();
        String topic = dto.getTopic();
        if (topic != null) {
            String checksum = dto.getEventId();
            try {
                if (!nbEventsDao.isEventStored(context, checksum)) {
                    SolrInputDocument doc = new SolrInputDocument();
                    doc.addField(EVENT_ID, checksum);
                    doc.addField(ORIGINAL_ID, dto.getOriginalId());
                    doc.addField(TITLE, dto.getTitle());
                    doc.addField(TOPIC, topic);
                    doc.addField(TRUST, dto.getTrust());
                    doc.addField(MESSAGE, dto.getMessage());
                    doc.addField(LAST_UPDATE, new Date());
                    doc.addField(RESOURCE_UUID, getResourceUUID(context, dto.getOriginalId()));
                    doc.addField(RELATED_UUID, dto.getRelated());
                    updateRequest.add(doc);
                    updateRequest.process(getSolr());
                    getSolr().commit();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public NBEvent findEventByEventId(Context context, String eventId) {
        SolrQuery param = new SolrQuery(EVENT_ID + ":" + eventId);
        QueryResponse response;
        try {
            response = getSolr().query(param);
            if (response != null) {
                SolrDocumentList list = response.getResults();
                if (list != null && list.size() == 1) {
                    SolrDocument doc = list.get(0);
                    return getNBEventFromSOLR(doc);
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Exception querying Solr", e);
        }
        return null;
    }

    private NBEvent getNBEventFromSOLR(SolrDocument doc) {
        NBEvent item = new NBEvent();
        item.setEventId((String) doc.get(EVENT_ID));
        item.setLastUpdate((Date) doc.get(LAST_UPDATE));
        item.setMessage((String) doc.get(MESSAGE));
        item.setOriginalId((String) doc.get(ORIGINAL_ID));
        item.setTarget((String) doc.get(RESOURCE_UUID));
        item.setTitle((String) doc.get(TITLE));
        item.setTopic((String) doc.get(TOPIC));
        item.setTrust((double) doc.get(TRUST));
        item.setRelated((String) doc.get(RELATED_UUID));
        return item;
    }

    @Override
    public List<NBEvent> findEventsByTopicAndPage(Context context, String topic, long offset, int pageSize) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setStart(((Long) offset).intValue());
        solrQuery.setRows(pageSize);
        solrQuery.setQuery(TOPIC + ":" + topic.replaceAll("!", "/"));
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
            if (response != null) {
                SolrDocumentList list = response.getResults();
                List<NBEvent> responseItem = new ArrayList<>();
                for (SolrDocument doc : list) {
                    NBEvent item = getNBEventFromSOLR(doc);
                    responseItem.add(item);
                }
                return responseItem;
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public long countEventsByTopic(Context context, String topic) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(TOPIC + ":" + topic.replace("!", "/"));
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            return response.getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getResourceUUID(Context context, String originalId) throws Exception {
        try {
            String id = getHandleFromOriginalId(originalId);
            if (id != null) {
                Item item = (Item) handleService.resolveToObject(context, id);
                if (item != null) {
                    return item.getID().toString();
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        } catch (RuntimeException | SQLException e) {
            log.warn("OriginalID " + originalId + " not found");
            throw e;
        }
    }

    // oai:www.openstarts.units.it:10077/21486
    private String getHandleFromOriginalId(String originalId) {
        Integer startPosition = originalId.lastIndexOf(':');
        if (startPosition != -1) {
            return originalId.substring(startPosition + 1, originalId.length());
        } else {
            return null;
        }
    }

}
