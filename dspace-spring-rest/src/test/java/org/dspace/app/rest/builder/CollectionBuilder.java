/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

import java.sql.SQLException;

/**
 * Builder to construct Collection objects
 */
public class CollectionBuilder extends AbstractBuilder<Collection> {

    private Collection collection;

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
        return setMetadataSingleValue(collection, MetadataSchema.DC_SCHEMA, "title", null, name);
    }

    @Override
    public Collection build() {
        try {
            collectionService.update(context, collection);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            return handleException(e);
        }
        return collection;
    }

    @Override
    protected DSpaceObjectService<Collection> getDsoService() {
        return collectionService;
    }
}
