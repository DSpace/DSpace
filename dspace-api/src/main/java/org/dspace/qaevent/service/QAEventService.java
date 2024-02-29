/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service;

import java.util.List;
import java.util.UUID;

import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.qaevent.QASource;
import org.dspace.qaevent.QATopic;

/**
 * Service that handles {@link QAEvent}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface QAEventService {

    /**
     * Find all the event's topics.
     *
     * @param  context     the DSpace context
     * @param  offset      the offset to apply
     * @param  orderField  the field to order for
     * @param  ascending   true if the order should be ascending, false otherwise
     * @return             the topics list
     */
    public List<QATopic> findAllTopics(Context context, long offset, long count, String orderField, boolean ascending);

    /**
     * Find all the event's topics related to the given source.
     *
     * @param  context     the DSpace context
     * @param  source      the source to search for
     * @param  offset      the offset to apply
     * @param  count       the page size
     * @param  orderField  the field to order for
     * @param  ascending   true if the order should be ascending, false otherwise
     * @return             the topics list
     */
    public List<QATopic> findAllTopicsBySource(Context context, String source, long offset, long count,
        String orderField, boolean ascending);

    /**
     * Find a specific topic by its name, source and optionally a target.
     *
     * @param  context the DSpace context
     * @param  sourceName the name of the source
     * @param  topicName  the topic name to search for
     * @param  target     (nullable) the uuid of the target to focus on
     * @return            the topic
     */
    public QATopic findTopicBySourceAndNameAndTarget(Context context, String sourceName, String topicName, UUID target);

    /**
     * Count all the event's topics.
     *
     * @return the count result
     */
    public long countTopics();

    /**
     * Count all the event's topics related to the given source.
     *
     * @param  context the DSpace context
     * @param  source  the source to search for
     * @return         the count result
     */
    public long countTopicsBySource(Context context, String source);

    /**
     * Find all the events by topic.
     *
     * @param  context    the DSpace context
     * @param  sourceName the source name
     * @param  topic      the topic to search for
     * @param  offset     the offset to apply
     * @param  size       the page size
     * @param  orderField  the field to order for
     * @param  ascending   true if the order should be ascending, false otherwise
     * @return            the events
     */
    public List<QAEvent> findEventsByTopic(Context context, String sourceName, String topic, long offset, int size,
                                           String orderField, boolean ascending);

    /**
     * Find all the events by topic.
     *
     * @param  context     the DSpace context
     * @param  sourceName  the source name
     * @param  topic       the topic to search for
     * @return             the events count
     */
    public long countEventsByTopic(Context context, String sourceName, String topic);

    /**
     * Find an event by the given id. Please note that no security filter are applied by this method.
     *
     * @param  id      the id of the event to search for
     * @return         the event
     */
    public QAEvent findEventByEventId(String id);

    /**
     * Store the given event.
     *
     * @param context the DSpace context
     * @param event   the event to store
     */
    public void store(Context context, QAEvent event);

    /**
     * Delete an event by the given id.
     *
     * @param id      the id of the event to delete
     */
    public void deleteEventByEventId(String id);

    /**
     * Delete events by the given target id.
     *
     * @param targetId the id of the target id
     */
    public void deleteEventsByTargetId(UUID targetId);

    /**
     * Find a specific topid by the given id.
     *
     * @param  topicId the topic id to search for
     * @return         the topic
     */
    public QATopic findTopicByTopicId(String topicId);

    /**
     * Find a specific source by the given name.
     *
     * @param  context the DSpace context
     * @param  source  the source name
     * @return         the source
     */
    public QASource findSource(Context context, String source);

    /**
     * Find a specific source by the given name including the stats focused on the target item.
     *
     * @param  context the DSpace context
     * @param  source  the source name
     * @param  target  the uuid of the item target
     * @return         the source
     */
    public QASource findSource(Context context, String source, UUID target);

    /**
     * Find all the event's sources.
     *
     * @param  context  the DSpace context
     * @param  offset   the offset to apply
     * @param  pageSize the page size
     * @return          the sources list
     */
    public List<QASource> findAllSources(Context context, long offset, int pageSize);

    /**
     * Count all the event's sources.
     *
     * @param  context the DSpace context
     * @return         the count result
     */
    public long countSources(Context context);

    /**
     * Count all the event's sources related to a specific item
     *
     * @param  context the DSpace context
     * @param  target  the item uuid
     * @return         the count result
     */
    public long countSourcesByTarget(Context context, UUID target);

    /**
     * Count all the event's topics related to the given source referring to a specific item
     *
     * @param  context the DSpace context
     * @param  target  the item uuid
     * @param  source  the source to search for
     * @return         the count result
     */
    public long countTopicsBySourceAndTarget(Context context, String source, UUID target);

    /**
     * Check if the given QA event supports a related item.
     * 
     * @param  qaevent the event to be verified
     * @return         true if the event supports a related item, false otherwise.
     */
    public boolean isRelatedItemSupported(QAEvent qaevent);

    /**
     * Find a list of QA events according to the pagination parameters for the specified topic and target sorted by
     * trust descending
     *
     * @param  context  the DSpace context
     * @param  source   the source name
     * @param  topic    the topic to search for
     * @param  offset   the offset to apply
     * @param  pageSize the page size
     * @param  target   the uuid of the QA event's target
     * @return          the events
     */
    public List<QAEvent> findEventsByTopicAndTarget(Context context, String source, String topic, UUID target,
                                                    long offset, int pageSize);

    /**
     * Check if a qaevent with the provided id is visible to the current user according to the source security
     *
     * @param  context the DSpace context
     * @param  user    the user to consider for the security check
     * @param  eventId the id of the event to check for existence
     * @param  source  the qa source name
     * @return <code>true</code> if the event exists
     */
    public boolean qaEventsInSource(Context context, EPerson user, String eventId, String source);

    /**
     * Count the QA events related to the specified topic and target
     *
     * @param  context  the DSpace context
     * @param  source   the source name
     * @param  topic    the topic to search for
     * @param  target   the uuid of the QA event's target
     * @return          the count result
     */
    public long countEventsByTopicAndTarget(Context context, String source, String topic, UUID target);

    /**
     * Find all the event's sources related to a specific item
     *
     * @param  context   the DSpace context
     * @param  target    the item referring to
     * @param  offset    the offset to apply
     * @param  pageSize  the page size
     * @return           the source list
     */
    public List<QASource> findAllSourcesByTarget(Context context, UUID target, long offset, int pageSize);

    /**
     * Find all the event's topics related to the given source for a specific item
     *
     * @param  context   the DSpace context
     * @param  source    (not null) the source to search for
     * @param  target    the item referring to
     * @param  offset    the offset to apply
     * @param  pageSize  the page size
     * @return           the topics list
     */
    public List<QATopic> findAllTopicsBySourceAndTarget(Context context, String source, UUID target, long offset,
            long pageSize, String orderField, boolean ascending);

}
