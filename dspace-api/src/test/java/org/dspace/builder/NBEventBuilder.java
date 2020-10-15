/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.util.Date;

import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.nbevent.service.dto.NBTopic;
import org.dspace.app.suggestion.SuggestionService;
import org.dspace.app.suggestion.SuggestionTarget;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Builder to construct Notification Broker Event objects
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class NBEventBuilder extends AbstractBuilder<NBEvent, NBEventService> {
    @Autowired
    private NBEventService nbEventService;
    private Item item;
    private NBEvent target;
    
    private String title;
    private String topic;
    private String message;
    private double trust = 0.5;
    private String originalId;
    private Date lastUpdate = new Date();

    protected NBEventBuilder(Context context) {
        super(context);
    }

    public static NBEventBuilder createTarget(final Context context, final Collection col, final String name) {
        NBEventBuilder builder = new NBEventBuilder(context);
        return builder.create(context, col, name);
    }

    public static NBEventBuilder createTarget(final Context context, final Item item) {
        NBEventBuilder builder = new NBEventBuilder(context);
        return builder.create(context, item);
    }

    private NBEventBuilder create(final Context context, final Collection col, final String name) {
        this.context = context;

        try {
            ItemBuilder itemBuilder = ItemBuilder.createItem(context, col).withTitle(name);
            item = itemBuilder.build();
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    private NBEventBuilder create(final Context context, final Item item) {
        this.context = context;
        this.item = item;
        return this;
    }

    public NBEventBuilder withTopic(final String topic) {
        this.topic = topic;
        return this;
    }

    @Override
    public NBEvent build() {
        target = new NBEvent(originalId, item.getID().toString(), title,
                topic, trust, message, lastUpdate);
        nbEventService.store(context, target);
        return target;
    }

    @Override
    public void cleanup() throws Exception {
        nbEventService.deleteTarget(target);
    }

    @Override
    protected NBEventService getService() {
        return nbEventService;
    }

    @Override
    public void delete(Context c, NBEvent dso) throws Exception {
        nbEventService.deleteTarget(dso);
    }
}