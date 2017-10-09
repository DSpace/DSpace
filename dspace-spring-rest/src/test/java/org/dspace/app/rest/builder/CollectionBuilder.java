package org.dspace.app.rest.builder;

import java.sql.SQLException;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * TODO TOM UNIT TEST
 */
public class CollectionBuilder extends AbstractBuilder {

    private Collection collection;
    private Context context;

    public CollectionBuilder createCollection(final Context context, final Community parent) {
        this.context = context;
        try {
            this.collection = collectionService.create(context, parent);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public CollectionBuilder withName(final String name) {
        try {
            collectionService.setMetadataSingleValue(context, collection, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY, name);
        } catch (SQLException e) {
            return handleException(e);
        }

        return this;
    }

    public Collection build() {
        context.dispatchEvents();
        try {
            indexingService.commit();
        } catch (SearchServiceException e) {
            return handleException(e);
        }
        return collection;
    }
}
