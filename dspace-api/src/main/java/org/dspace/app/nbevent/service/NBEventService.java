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
import org.dspace.content.NBSourceName;
import org.dspace.core.Context;

public interface NBEventService {

    public List<NBTopic> findAllTopics(Context context, long offset, long pageSize);

    public List<NBTopic> findAllTopicsBySource(Context context, NBSourceName source, long offset, long count);

    public long countTopics(Context context);

    public long countTopicsBySource(Context context, NBSourceName source);

    public List<NBEvent> findEventsByTopicAndPage(Context context, String topic,
            long offset, int pageSize,
            String orderField, boolean ascending);

    public long countEventsByTopic(Context context, String topic);

    public NBEvent findEventByEventId(Context context, String id);

    public void store(Context context, NBEvent event);

    public void deleteEventByEventId(Context context, String id);

    public void deleteEventsByTargetId(Context context, UUID targetId);

    public NBTopic findTopicByTopicId(String topicId);

    public NBSource findSource(NBSourceName source);

    public List<NBSource> findAllSources(Context context, long offset, int pageSize);

}
