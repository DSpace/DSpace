/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.service;

import java.util.List;
import java.util.UUID;

import org.dspace.app.nbevent.NBSource;
import org.dspace.app.nbevent.NBTopic;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;

/**
 * Service that handles {@link NBEvent}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface NBEventService {

    /**
     * Find all the event's topics.
     *
     * @param  context  the DSpace context
     * @param  offset   the offset to apply
     * @param  pageSize the page size
     * @return          the topics list
     */
    public List<NBTopic> findAllTopics(Context context, long offset, long pageSize);

    /**
     * Find all the event's topics related to the given source.
     *
     * @param  context  the DSpace context
     * @param  source   the source to search for
     * @param  offset   the offset to apply
     * @param  pageSize the page size
     * @return          the topics list
     */
    public List<NBTopic> findAllTopicsBySource(Context context, String source, long offset, long count);

    /**
     * Count all the event's topics.
     *
     * @param  context the DSpace context
     * @return         the count result
     */
    public long countTopics(Context context);

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
     * @param  topic      the topic to search for
     * @param  offset     the offset to apply
     * @param  pageSize   the page size
     * @param  orderField the field to order for
     * @param  ascending  true if the order should be ascending, false otherwise
     * @return            the events
     */
    public List<NBEvent> findEventsByTopicAndPage(Context context, String topic,
            long offset, int pageSize,
            String orderField, boolean ascending);

    /**
     * Find all the events by topic.
     *
     * @param  context the DSpace context
     * @param  topic   the topic to search for
     * @return         the events count
     */
    public long countEventsByTopic(Context context, String topic);

    /**
     * Find an event by the given id.
     *
     * @param  context the DSpace context
     * @param  id      the id of the event to search for
     * @return         the event
     */
    public NBEvent findEventByEventId(Context context, String id);

    /**
     * Store the given event.
     *
     * @param context the DSpace context
     * @param event   the event to store
     */
    public void store(Context context, NBEvent event);

    /**
     * Delete an event by the given id.
     *
     * @param context the DSpace context
     * @param id      the id of the event to delete
     */
    public void deleteEventByEventId(Context context, String id);

    /**
     * Delete events by the given target id.
     *
     * @param context the DSpace context
     * @param id      the id of the target id
     */
    public void deleteEventsByTargetId(Context context, UUID targetId);

    /**
     * Find a specific topid by the given id.
     *
     * @param  topicId the topic id to search for
     * @return         the topic
     */
    public NBTopic findTopicByTopicId(String topicId);

    /**
     * Find a specific source by the given name.
     *
     * @param  source the source name
     * @return        the source
     */
    public NBSource findSource(String source);

    /**
     * Find all the event's sources.
     *
     * @param  context  the DSpace context
     * @param  offset   the offset to apply
     * @param  pageSize the page size
     * @return          the sources list
     */
    public List<NBSource> findAllSources(Context context, long offset, int pageSize);

}
