/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.service;

import java.util.List;

import org.dspace.app.nbevent.NBTopic;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;

public interface NBEventService {

    public NBTopic findTopicByTopicId(String topicId);

    public List<NBTopic> findAllTopics(Context context, long offset, long pageSize);

    public long countTopics(Context context);

    public List<NBEvent> findEventsByTopicAndPage(Context context, String topic, long offset, int pageSize);

    public long countEventsByTopic(Context context, String topic);

    public NBEvent findEventByEventId(Context context, String id);

    public void store(Context context, NBEvent event);

    public void deleteEventByEventId(Context context, String id);

}
