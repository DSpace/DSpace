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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
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
import org.apache.solr.common.params.FacetParams;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.handle.service.HandleService;
import org.dspace.qaevent.AutomaticProcessingAction;
import org.dspace.qaevent.QAEventAutomaticProcessingEvaluation;
import org.dspace.qaevent.QASource;
import org.dspace.qaevent.QATopic;
import org.dspace.qaevent.dao.QAEventsDAO;
import org.dspace.qaevent.dao.impl.QAEventsDAOImpl;
import org.dspace.qaevent.service.QAEventActionService;
import org.dspace.qaevent.service.QAEventSecurityService;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


/**
 * Implementation of {@link QAEventService} that use Solr to store events. When
 * the user performs an action on the event (such as accepting the suggestion or
 * rejecting it) then the event is removed from solr and saved in the database
 * (see {@link QAEventsDAO}) so that it is no longer proposed.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAEventServiceImpl implements QAEventService {

    private static final Logger log = LogManager.getLogger();

    public static final String QAEVENTS_SOURCES = "qaevents.sources";

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected QAEventSecurityService qaSecurityService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired
    private HandleService handleService;

    @Autowired
    private QAEventsDAOImpl qaEventsDao;

    @Autowired(required = false)
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
    public long countTopics() {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery("*:*");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
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
    public long countTopicsBySource(Context context, String sourceName) {
        var currentUser = context.getCurrentUser();
        if (isNotSupportedSource(sourceName) || !qaSecurityService.canSeeSource(context, currentUser, sourceName)) {
            return 0;
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context, currentUser, sourceName);
        solrQuery.setQuery(securityQuery.orElse("*:*"));
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TOPIC);
        solrQuery.addFilterQuery(SOURCE + ":\"" + sourceName + "\"");
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return response.getFacetField(TOPIC).getValueCount();
    }

    @Override
    public QATopic findTopicBySourceAndNameAndTarget(Context context, String sourceName, String topicName,
                                                     UUID target) {
        if (isNotSupportedSource(sourceName)
                || !qaSecurityService.canSeeSource(context, context.getCurrentUser(), sourceName)) {
            return null;
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context,
                context.getCurrentUser(), sourceName);
        solrQuery.setQuery(securityQuery.orElse("*:*"));

        solrQuery.addFilterQuery(SOURCE + ":\"" + sourceName + "\"");
        solrQuery.addFilterQuery(TOPIC + ":\"" + topicName + "\"");
        if (target != null) {
            solrQuery.addFilterQuery(RESOURCE_UUID + ":\"" + target.toString() + "\"");
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
    public QATopic findTopicByTopicId(String topicId) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(TOPIC + ":" + topicId.replaceAll("!", "/"));
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TOPIC);
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
            FacetField facetField = response.getFacetField(TOPIC);
            for (Count c : facetField.getValues()) {
                if (c.getName().equals(topicId.replace("!", "/"))) {
                    QATopic topic = new QATopic();
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
    public List<QATopic> findAllTopics(Context context, long offset, long count, String orderField, boolean ascending) {
        return findAllTopicsBySource(context, null, offset, count, orderField, ascending);
    }

    @Override
    public List<QATopic> findAllTopicsBySource(Context context, String source, long offset,
        long count, String orderField, boolean ascending) {
        return findAllTopicsBySourceAndTarget(context, source, null, offset, count, orderField, ascending);
    }

    @Override
    public List<QATopic> findAllTopicsBySourceAndTarget(Context context, String source, UUID target, long offset,
        long pageSize, String orderField, boolean ascending) {
        if (isNotSupportedSource(source)
                || !qaSecurityService.canSeeSource(context, context.getCurrentUser(), source)) {
            return List.of();
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        if (orderField != null) {
            solrQuery.setSort(orderField, ascending ? ORDER.asc : ORDER.desc);
            solrQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
        }
        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context,
                context.getCurrentUser(), source);
        solrQuery.setQuery(securityQuery.orElse("*:*"));
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit((int) (offset + pageSize));
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
        if (qaAutomaticProcessingMap == null) {
            return;
        }
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

    /**
     * Sends an email notification to the system administrator about a new
     * Quality Assurance (QA) request event. The email includes details such as the
     * topic, target, and message associated with the QA event.
     *
     * @param qaEvent The Quality Assurance event for which the notification is generated.
     */
    public void sentEmailToAdminAboutNewRequest(QAEvent qaEvent) {
        try {
            String uiUrl = configurationService.getProperty("dspace.ui.url");
            Email email = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), "qaevent_admin_notification"));
            email.addRecipient(configurationService.getProperty("qaevents.mail.notification"));
            email.addArgument(qaEvent.getTopic());
            email.addArgument(uiUrl + "/items/" + qaEvent.getTarget());
            email.addArgument(parsJson(qaEvent.getMessage()));
            email.send();
        } catch (Exception e) {
            log.warn("Error during sending email of Withdrawn/Reinstate request for item with uuid:  {}",
                     qaEvent.getTarget(), e);
        }
    }

    private String parsJson(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return jsonNode.get("reason").asText();
        } catch (Exception e) {
            log.warn("Unable to parse the JSON:  {}", jsonString);
            return jsonString;
        }
    }

    @Override
    public QAEvent findEventByEventId(String eventId) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.addFilterQuery(EVENT_ID + ":\"" + eventId + "\"");
        try {
            QueryResponse response = getSolr().query(solrQuery);
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
    public List<QAEvent> findEventsByTopic(Context context, String sourceName, String topic, long offset, int pageSize,
                                           String orderField, boolean ascending) {
        EPerson currentUser = context.getCurrentUser();
        if (isNotSupportedSource(sourceName) || !qaSecurityService.canSeeSource(context, currentUser, sourceName)) {
            return List.of();
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setStart(((Long) offset).intValue());
        if (pageSize != -1) {
            solrQuery.setRows(pageSize);
        }
        solrQuery.setSort(orderField, ascending ? ORDER.asc : ORDER.desc);
        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context, currentUser, sourceName);
        solrQuery.setQuery(securityQuery.orElse("*:*"));

        solrQuery.setQuery(TOPIC + ":" + topic.replaceAll("!", "/"));
        solrQuery.addFilterQuery(SOURCE + ":\"" + sourceName + "\"");

        try {
            QueryResponse response = getSolr().query(solrQuery);
            if (response != null) {
                SolrDocumentList solrDocuments = response.getResults();
                List<QAEvent> responseItem = new ArrayList<>();
                for (SolrDocument doc : solrDocuments) {
                    QAEvent item = getQAEventFromSOLR(doc);
                    responseItem.add(item);
                }
                return responseItem;
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return List.of();
    }

    @Override
    public long countEventsByTopic(Context context, String sourceName, String topic) {
        EPerson currentUser = context.getCurrentUser();
        if (isNotSupportedSource(sourceName) || !qaSecurityService.canSeeSource(context, currentUser, sourceName)) {
            return 0;
        }

        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context, currentUser, sourceName);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(securityQuery.orElse("*:*"));
        solrQuery.addFilterQuery(SOURCE + ":\"" + sourceName + "\"");
        solrQuery.setQuery(TOPIC + ":" + topic.replaceAll("!", "/"));
        try {
            return getSolr().query(solrQuery).getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public QASource findSource(Context context, String sourceName) {
        String[] split = sourceName.split(":");
        return findSource(context, split[0], split.length == 2 ? UUID.fromString(split[1]) : null);
    }

    @Override
    public QASource findSource(Context context, String sourceName, UUID target) {
        EPerson currentUser = context.getCurrentUser();
        if (isNotSupportedSource(sourceName) || !qaSecurityService.canSeeSource(context, currentUser, sourceName)) {
            return null;
        }

        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context, currentUser, sourceName);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(securityQuery.orElse("*:*"));
        solrQuery.setRows(0);
        solrQuery.addFilterQuery(SOURCE + ":\"" + sourceName + "\"");
        if (target != null) {
            solrQuery.addFilterQuery("resource_uuid:" + target.toString());
        }
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(SOURCE);

        try {
            QueryResponse response = getSolr().query(solrQuery);
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
            throw new RuntimeException(e.getMessage(), e);
        }

        QASource source = new QASource();
        source.setName(sourceName);
        source.setTotalEvents(0L);

        return source;
    }

    @Override
    public List<QASource> findAllSources(Context context, long offset, int pageSize) {
        return Arrays.stream(getSupportedSources())
                     .map((sourceName) -> findSource(context, sourceName))
            .filter(Objects::nonNull)
                     .sorted(comparing(QASource::getTotalEvents)
                     .reversed())
                     .skip(offset)
                     .limit(pageSize)
                     .collect(Collectors.toList());
    }

    @Override
    public long countSources(Context context) {
        return Arrays.stream(getSupportedSources())
                .map((sourceName) -> findSource(context, sourceName))
                .filter(Objects::nonNull)
                .filter(source -> source.getTotalEvents() > 0)
                .count();
    }

    @Override
    public long countSourcesByTarget(Context context, UUID target) {
        return Arrays.stream(getSupportedSources())
                .map((sourceName) -> findSource(context, sourceName, target))
                .filter(Objects::nonNull)
                .filter(source -> source.getTotalEvents() > 0)
                .count();
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
        String resourceUUID = getResourceUUID(context, dto.getOriginalId());
        if (resourceUUID == null) {
            resourceUUID = dto.getTarget();
            /*throw new IllegalArgumentException("Skipped event " + checksum +
                " related to the oai record " + dto.getOriginalId() + " as the record was not found");*/
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
            return originalId;
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

    @Override
    public boolean qaEventsInSource(Context context, EPerson user, String eventId, String source) {
        SolrQuery solrQuery = new SolrQuery();
        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context, user, source);
        solrQuery.setQuery(securityQuery.orElse("*:*"));
        solrQuery.addFilterQuery(EVENT_ID + ":\"" + eventId + "\"");
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
            if (response != null) {
                return response.getResults().getNumFound() == 1;
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Exception querying Solr", e);
        }
        return false;
    }

    @Override
    public long countEventsByTopicAndTarget(Context context, String sourceName, String topic, UUID target) {
        var currentUser = context.getCurrentUser();
        if (isNotSupportedSource(sourceName) || !qaSecurityService.canSeeSource(context, currentUser, sourceName)) {
            return 0;
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context, currentUser, sourceName);
        solrQuery.setQuery(securityQuery.orElse("*:*"));
        if (target != null) {
            solrQuery.addFilterQuery(RESOURCE_UUID + ":\"" + target.toString() + "\"");
        }
        solrQuery.addFilterQuery(SOURCE + ":\"" + sourceName + "\"");
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
    public List<QAEvent> findEventsByTopicAndTarget(Context context, String source, String topic, UUID target,
            long offset, int pageSize) {
        var currentUser = context.getCurrentUser();
        if (isNotSupportedSource(source) || !qaSecurityService.canSeeSource(context, currentUser, source)) {
            return List.of();
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setStart(((Long) offset).intValue());
        solrQuery.setRows(pageSize);
        solrQuery.setSort(TRUST, ORDER.desc);
        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context, currentUser, source);
        solrQuery.setQuery(securityQuery.orElse("*:*"));
        if (target != null) {
            solrQuery.addFilterQuery(RESOURCE_UUID + ":\"" + target.toString() + "\"");
        }
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        solrQuery.addFilterQuery(TOPIC + ":\"" + topic + "\"");

        try {
            QueryResponse response = getSolr().query(solrQuery);
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

    private boolean isNotSupportedSource(String source) {
        return !ArrayUtils.contains(getSupportedSources(), source);
    }

    private String[] getSupportedSources() {
        return configurationService.getArrayProperty(QAEVENTS_SOURCES,
            new String[] { QAEvent.OPENAIRE_SOURCE, QAEvent.COAR_NOTIFY_SOURCE });
    }

    @Override
    public long countTopicsBySourceAndTarget(Context context, String source, UUID target) {
        var currentUser = context.getCurrentUser();
        if (isNotSupportedSource(source) || !qaSecurityService.canSeeSource(context, currentUser, source)) {
            return 0;
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        Optional<String> securityQuery = qaSecurityService.generateQAEventFilterQuery(context, currentUser, source);
        solrQuery.setQuery(securityQuery.orElse("*:*"));
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TOPIC);
        solrQuery.addFilterQuery(SOURCE + ":\"" + source + "\"");
        if (target != null) {
            solrQuery.addFilterQuery(RESOURCE_UUID + ":\"" + target.toString() + "\"");
        }
        try {
            QueryResponse response = getSolr().query(solrQuery);
            return response.getFacetField(TOPIC).getValueCount();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<QASource> findAllSourcesByTarget(Context context, UUID target, long offset, int pageSize) {
        return Arrays.stream(getSupportedSources())
                     .map((sourceName) -> findSource(context, sourceName, target))
                     .filter(Objects::nonNull)
                     .sorted(comparing(QASource::getTotalEvents).reversed())
                     .filter(source -> source.getTotalEvents() > 0)
                     .skip(offset)
                     .limit(pageSize)
                     .collect(Collectors.toList());
    }

}
