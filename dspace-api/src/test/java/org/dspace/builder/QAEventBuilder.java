/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.util.Date;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventService;

/**
 * Builder to construct Quality Assurance Broker Event objects
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class QAEventBuilder extends AbstractBuilder<QAEvent, QAEventService> {

    private Item item;
    private QAEvent target;
    private String source = QAEvent.OPENAIRE_SOURCE;
    /**
     * the title of the DSpace object
     * */
    private String title;
    /**
     * the name of the Quality Assurance Event Topic
     * */
    private String topic;
    /**
     * thr original QA Event imported
     * */
    private String message;
    /**
     * uuid of the targeted DSpace object
     * */
    private String relatedItem;
    private double trust = 0.5;
    private Date lastUpdate = new Date();

    protected QAEventBuilder(Context context) {
        super(context);
    }

    public static QAEventBuilder createTarget(final Context context, final Collection col, final String name) {
        QAEventBuilder builder = new QAEventBuilder(context);
        return builder.create(context, col, name);
    }

    public static QAEventBuilder createTarget(final Context context, final Item item) {
        QAEventBuilder builder = new QAEventBuilder(context);
        return builder.create(context, item);
    }

    private QAEventBuilder create(final Context context, final Collection col, final String name) {
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

    private QAEventBuilder create(final Context context, final Item item) {
        this.context = context;
        this.item = item;
        return this;
    }

    public QAEventBuilder withTopic(final String topic) {
        this.topic = topic;
        return this;
    }
    public QAEventBuilder withSource(final String source) {
        this.source = source;
        return this;
    }
    public QAEventBuilder withTitle(final String title) {
        this.title = title;
        return this;
    }
    public QAEventBuilder withMessage(final String message) {
        this.message = message;
        return this;
    }
    public QAEventBuilder withTrust(final double trust) {
        this.trust = trust;
        return this;
    }
    public QAEventBuilder withLastUpdate(final Date lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public QAEventBuilder withRelatedItem(String relatedItem) {
        this.relatedItem = relatedItem;
        return this;
    }

    @Override
    public QAEvent build() {
        target = new QAEvent(source, "oai:www.dspace.org:" + item.getHandle(), item.getID().toString(), title, topic,
            trust, message, lastUpdate);
        target.setRelated(relatedItem);
        try {
            qaEventService.store(context, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return target;
    }

    @Override
    public void cleanup() throws Exception {
        qaEventService.deleteEventByEventId(target.getEventId());
    }

    @Override
    protected QAEventService getService() {
        return qaEventService;
    }

    @Override
    public void delete(Context c, QAEvent dso) throws Exception {
        qaEventService.deleteEventByEventId(target.getEventId());

//        qaEventService.deleteTarget(dso);
    }
}