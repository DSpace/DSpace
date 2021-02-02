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
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;

/**
 * Builder to construct Notification Broker Event objects
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class NBEventBuilder extends AbstractBuilder<NBEvent, NBEventService> {

    private Item item;
    private NBEvent target;

    private String title;
    private String topic;
    private String message;
    private String relatedItem;
    private double trust = 0.5;
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
            this.title = name;
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
    public NBEventBuilder withTitle(final String title) {
        this.title = title;
        return this;
    }
    public NBEventBuilder withMessage(final String message) {
        this.message = message;
        return this;
    }
    public NBEventBuilder withTrust(final double trust) {
        this.trust = trust;
        return this;
    }
    public NBEventBuilder withLastUpdate(final Date lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public NBEventBuilder withRelatedItem(String relatedItem) {
        this.relatedItem = relatedItem;
        return this;
    }

    @Override
    public NBEvent build() {
        target = new NBEvent("oai:www.dspace.org:" + item.getHandle(), item.getID().toString(), title, topic, trust,
                message, lastUpdate);
        target.setRelated(relatedItem);
        try {
            nbEventService.store(context, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return target;
    }

    @Override
    public void cleanup() throws Exception {
        nbEventService.deleteEventByEventId(context, target.getEventId());
    }

    @Override
    protected NBEventService getService() {
        return nbEventService;
    }

    @Override
    public void delete(Context c, NBEvent dso) throws Exception {
        nbEventService.deleteEventByEventId(context, target.getEventId());

//        nbEventService.deleteTarget(dso);
    }
}