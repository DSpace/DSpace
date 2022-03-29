/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public class EntityTypeBuilder extends AbstractBuilder<EntityType, EntityTypeService> {

    /* Log4j logger*/
    private static final Logger log = LogManager.getLogger();

    private EntityType entityType;

    protected EntityTypeBuilder(Context context) {
        super(context);
    }

    @Override
    protected EntityTypeService getService() {
        return entityTypeService;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            entityType = c.reloadEntity(entityType);
            if (entityType != null) {
                delete(entityType);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public void delete(Context c, EntityType dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    public EntityType build() {
        try {

            entityTypeService.update(context, entityType);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException | AuthorizeException e) {
            log.error(e);
        }
        return entityType;
    }

    public void delete(EntityType entityType) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            EntityType attachedEntityType = c.reloadEntity(entityType);
            if (attachedEntityType != null) {
                getService().delete(c, attachedEntityType);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static EntityTypeBuilder createEntityTypeBuilder(Context context, String entityType) {
        EntityTypeBuilder entityTypeBuilder = new EntityTypeBuilder(context);
        return entityTypeBuilder.create(context, entityType);
    }

    private EntityTypeBuilder create(Context context, String entityType) {
        try {

            this.context = context;
            this.entityType = entityTypeService.create(context, entityType);

        } catch (SQLException | AuthorizeException e) {
            log.warn("Failed to create the EntityType", e);
        }

        return this;
    }
}