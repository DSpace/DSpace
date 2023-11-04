/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service.impl;

import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.dspace.content.QAEvent.OPENAIRE_SOURCE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.qaevent.AutomaticProcessingAction;
import org.dspace.qaevent.QAEventAutomaticProcessingEvaluation;
import org.dspace.qaevent.QASource;
import org.dspace.qaevent.QATopic;
import org.dspace.qaevent.dao.QAEventsDao;
import org.dspace.qaevent.dao.impl.QAEventsDaoImpl;
import org.dspace.qaevent.service.QAEventActionService;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Implementation of {@link QAEventService} that use Solr to store events. When
 * the user performs an action on the event (such as accepting the suggestion or
 * rejecting it) then the event is removed from solr and saved in the database
 * (see {@link QAEventsDao}) so that it is no longer proposed.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAEventServiceImpl implements QAEventService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(QAEventServiceImpl.class);

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired
    private HandleService handleService;

    @Autowired
    private QAEventsDaoImpl qaEventsDao;

    @Autowired
    @Qualifier("qaAutomaticProcessingMap")
    private Map<String, QAEventAutomaticProcessingEvaluation> qaAutomaticProcessingMap;

    @Autowired
    private QAEventActionService qaEventActionService;

    private ObjectMapper jsonMapper;

    public QAEventServiceImpl() {
        jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    protected SolrClient solr = null;

    public static final String SOURCE = "source";
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
                    .getProperty("qaevents.solr.server", "http://localhost:8983/solr/qaevent");
            return new HttpSolrClient.Builder(solrService).build();
        }
        return solr;
    }

    @Override
    public long countTopicsBySource(String source) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery("*:*");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TOPIC);
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return response.getFacetField(TOPIC).getValueCount();
    }

    @Override
    public long countTopicsBySourceAndTarget(String source, UUID target) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery("*:*");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TOPIC);
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        if (target != null) {
            solrQuery.addFilterQuery(RESOURCE_UUID + ":\"" + target.toString() + "\"");
        }
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return response.getFacetField(TOPIC).getValueCount();
    }

    @Override
    public void deleteEventByEventId(String id) {
        try {
            getSolr().deleteById(id);
            getSolr().commit();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteEventsByTargetId(UUID targetId) {
        try {
            getSolr().deleteByQuery(RESOURCE_UUID + ":" + targetId.toString());
            getSolr().commit();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public QATopic findTopicBySourceAndNameAndTarget(String sourceName, String topicName, UUID target) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.addFilterQuery(SOURCE + ":\"" + sourceName + "\"");
        solrQuery.addFilterQuery(TOPIC + ":\"" + topicName + "\"");
        if (target != null) {
            solrQuery.setQuery(RESOURCE_UUID + ":\"" + target.toString() + "\"");
        } else {
            solrQuery.setQuery("*:*");
        }
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TOPIC);
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
            FacetField facetField = response.getFacetField(TOPIC);
            for (Count c : facetField.getValues()) {
                if (c.getName().equals(topicName)) {
                    QATopic topic = new QATopic();
                    topic.setSource(sourceName);
                    topic.setKey(c.getName());
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

    @Override
    public List<QATopic> findAllTopicsBySource(String source, long offset, int count) {
        return findAllTopicsBySourceAndTarget(source, null, offset, count);
    }

    @Override
    public List<QATopic> findAllTopicsBySourceAndTarget(String source, UUID target, long offset, int count) {
        if (source != null && isNotSupportedSource(source)) {
            return null;
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery("*:*");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit((int) (offset + count));
        solrQuery.addFacetField(TOPIC);
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        if (target != null) {
            solrQuery.addFilterQuery(RESOURCE_UUID + ":" + target.toString());
        }
        QueryResponse response;
        List<QATopic> topics = new ArrayList<>();
        try {
            response = getSolr().query(solrQuery);
            FacetField facetField = response.getFacetField(TOPIC);
            topics = new ArrayList<>();
            int idx = 0;
            for (Count c : facetField.getValues()) {
                if (idx < offset) {
                    idx++;
                    continue;
                }
                QATopic topic = new QATopic();
                topic.setSource(source);
                topic.setKey(c.getName());
                topic.setFocus(target);
                topic.setTotalEvents(c.getCount());
                topic.setLastEvent(new Date());
                topics.add(topic);
                idx++;
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return topics;
    }

    @Override
    public void store(Context context, QAEvent dto) {

        if (isNotSupportedSource(dto.getSource())) {
            throw new IllegalArgumentException("The source of the given event is not supported: " + dto.getSource());
        }

        if (StringUtils.isBlank(dto.getTopic())) {
            throw new IllegalArgumentException("A topic is mandatory for an event");
        }

        String checksum = dto.getEventId();
        try {
            if (!qaEventsDao.isEventStored(context, checksum)) {

                SolrInputDocument doc = createSolrDocument(context, dto, checksum);

                UpdateRequest updateRequest = new UpdateRequest();

                updateRequest.add(doc);
                updateRequest.process(getSolr());

                getSolr().commit();

                performAutomaticProcessingIfNeeded(context, dto);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void performAutomaticProcessingIfNeeded(Context context, QAEvent qaEvent) {
        QAEventAutomaticProcessingEvaluation evaluation = qaAutomaticProcessingMap.get(qaEvent.getSource());

        if (evaluation == null) {
            return;
        }

        AutomaticProcessingAction action = evaluation.evaluateAutomaticProcessing(context, qaEvent);

        if (action == null) {
            return;
        }

        switch (action) {
            case REJECT:
                qaEventActionService.reject(context, qaEvent);
                break;
            case IGNORE:
                qaEventActionService.discard(context, qaEvent);
                break;
            case ACCEPT:
                qaEventActionService.accept(context, qaEvent);
                break;
            default:
                throw new IllegalStateException("Unknown automatic action requested " + action);
        }

    }

    @Override
    public QAEvent findEventByEventId(String eventId) {
        SolrQuery param = new SolrQuery(EVENT_ID + ":" + eventId);
        QueryResponse response;
        try {
            response = getSolr().query(param);
            if (response != null) {
                SolrDocumentList list = response.getResults();
                if (list != null && list.size() == 1) {
                    SolrDocument doc = list.get(0);
                    return getQAEventFromSOLR(doc);
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Exception querying Solr", e);
        }
        return null;
    }

    @Override
    public List<QAEvent> findEventsByTopicAndPage(String source, String topic, long offset, int pageSize) {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setStart(((Long) offset).intValue());
        if (pageSize != -1) {
            solrQuery.setRows(pageSize);
        }
        solrQuery.setSort(TRUST, ORDER.desc);
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        solrQuery.setQuery(TOPIC + ":\"" + topic + "\"");

        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
            if (response != null) {
                SolrDocumentList list = response.getResults();
                List<QAEvent> responseItem = new ArrayList<>();
                for (SolrDocument doc : list) {
                    QAEvent item = getQAEventFromSOLR(doc);
                    responseItem.add(item);
                }
                return responseItem;
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }

        return List.of();
    }

    @Override
    public List<QAEvent> findEventsByTopicAndPageAndTarget(String source, String topic, long offset, int pageSize,
            UUID target) {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setStart(((Long) offset).intValue());
        solrQuery.setRows(pageSize);
        solrQuery.setSort(TRUST, ORDER.desc);
        if (target != null) {
            solrQuery.setQuery(RESOURCE_UUID + ":\"" + target.toString() + "\"");
        } else {
            solrQuery.setQuery("*:*");
        }
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        solrQuery.addFilterQuery(TOPIC + ":\"" + topic + "\"");

        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
            if (response != null) {
                SolrDocumentList list = response.getResults();
                List<QAEvent> responseItem = new ArrayList<>();
                for (SolrDocument doc : list) {
                    QAEvent item = getQAEventFromSOLR(doc);
                    responseItem.add(item);
                }
                return responseItem;
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }

        return List.of();
    }

    @Override
    public long countEventsByTopic(String source, String topic) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(TOPIC + ":\"" + topic + "\"");
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            return response.getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countEventsByTopicAndTarget(String source, String topic, UUID target) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        if (target != null) {
            solrQuery.setQuery(RESOURCE_UUID + ":\"" + target.toString() + "\"");
        } else {
            solrQuery.setQuery("*:*");
        }
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        solrQuery.addFilterQuery(TOPIC + ":\"" + topic + "\"");
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            return response.getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public QASource findSource(String sourceName) {
        String[] split = sourceName.split(":");
        return findSource(split[0], split.length == 2 ? UUID.fromString(split[1]) : null);
    }

    @Override
    public QASource findSource(String sourceName, UUID target) {

        if (isNotSupportedSource(sourceName)) {
            return null;
        }

        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.setRows(0);
        solrQuery.addFilterQuery(SOURCE + ":\"" + sourceName + "\"");
        if (target != null) {
            solrQuery.addFilterQuery("resource_uuid:" + target.toString());
        }
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(SOURCE);

        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
            FacetField facetField = response.getFacetField(SOURCE);
            for (Count c : facetField.getValues()) {
                if (c.getName().equalsIgnoreCase(sourceName)) {
                    QASource source = new QASource();
                    source.setName(c.getName());
                    source.setFocus(target);
                    source.setTotalEvents(c.getCount());
                    source.setLastEvent(new Date());
                    return source;
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }

        QASource source = new QASource();
        source.setName(sourceName);
        source.setTotalEvents(0L);

        return source;
    }

    @Override
    public List<QASource> findAllSources(long offset, int pageSize) {
        return Arrays.stream(getSupportedSources())
            .map((sourceName) -> findSource(sourceName))
            .sorted(comparing(QASource::getTotalEvents).reversed())
            .skip(offset)
            .limit(pageSize)
            .collect(Collectors.toList());
    }

    @Override
    public long countSources() {
        return getSupportedSources().length;
    }

    @Override
    public List<QASource> findAllSourcesByTarget(UUID target, long offset, int pageSize) {
        return Arrays.stream(getSupportedSources())
                .map((sourceName) -> findSource(sourceName, target))
                .sorted(comparing(QASource::getTotalEvents).reversed())
                .filter(source -> source.getTotalEvents() > 0)
                .skip(offset)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    @Override
    public long countSourcesByTarget(UUID target) {
        return getSupportedSources().length;
    }

    @Override
    public boolean isRelatedItemSupported(QAEvent qaevent) {
        // Currently only PROJECT topics related to OPENAIRE supports related items
        return qaevent.getSource().equals(OPENAIRE_SOURCE) && endsWith(qaevent.getTopic(), "/PROJECT");
    }

    private SolrInputDocument createSolrDocument(Context context, QAEvent dto, String checksum) throws Exception {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(SOURCE, dto.getSource());
        doc.addField(EVENT_ID, checksum);
        doc.addField(ORIGINAL_ID, dto.getOriginalId());
        doc.addField(TITLE, dto.getTitle());
        doc.addField(TOPIC, dto.getTopic());
        doc.addField(TRUST, dto.getTrust());
        doc.addField(MESSAGE, dto.getMessage());
        doc.addField(LAST_UPDATE, new Date());
        final String resourceUUID = getResourceUUID(context, dto.getOriginalId());
        if (resourceUUID == null) {
            throw new IllegalArgumentException("Skipped event " + checksum +
                " related to the oai record " + dto.getOriginalId() + " as the record was not found");
        }
        doc.addField(RESOURCE_UUID, resourceUUID);
        doc.addField(RELATED_UUID, dto.getRelated());
        return doc;
    }

    private String getResourceUUID(Context context, String originalId) throws Exception {
        String id = getHandleFromOriginalId(originalId);
        if (id != null) {
            Item item = (Item) handleService.resolveToObject(context, id);
            if (item != null) {
                final String itemUuid = item.getID().toString();
                context.uncacheEntity(item);
                return itemUuid;
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Malformed originalId " + originalId);
        }
    }

    // oai:www.openstarts.units.it:10077/21486
    private String getHandleFromOriginalId(String originalId) {
        int startPosition = originalId.lastIndexOf(':');
        if (startPosition != -1) {
            return originalId.substring(startPosition + 1, originalId.length());
        } else {
            return null;
        }
    }

    private QAEvent getQAEventFromSOLR(SolrDocument doc) {
        QAEvent item = new QAEvent();
        item.setSource((String) doc.get(SOURCE));
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

    private boolean isNotSupportedSource(String source) {
        return !ArrayUtils.contains(getSupportedSources(), source);
    }

    private String[] getSupportedSources() {
        return configurationService.getArrayProperty("qaevent.sources",
            new String[] { QAEvent.OPENAIRE_SOURCE, QAEvent.COAR_NOTIFY_SOURCE });
    }

}
