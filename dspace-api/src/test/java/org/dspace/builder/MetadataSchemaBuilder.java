/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public class MetadataSchemaBuilder extends AbstractBuilder<MetadataSchema, MetadataSchemaService> {

    /* Log4j logger*/
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataSchemaBuilder.class);

    private MetadataSchema metadataSchema;

    protected MetadataSchemaBuilder(Context context) {
        super(context);
    }

    @Override
    protected MetadataSchemaService getService() {
        return metadataSchemaService;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            metadataSchema = c.reloadEntity(metadataSchema);
            if (metadataSchema != null) {
                delete(c, metadataSchema);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public void delete(Context c, MetadataSchema dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    public MetadataSchema build() {
        try {

            metadataSchemaService.update(context, metadataSchema);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException | AuthorizeException e) {
            log.error(e);
        } catch (NonUniqueMetadataException e) {
            log.error("Failed to complete MetadataSchema", e);
        }
        return metadataSchema;
    }

    public void delete(MetadataSchema dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            MetadataSchema attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }

    /**
     * Delete the Test MetadataSchema referred to by the given ID
     * @param id Integer of Test MetadataSchema to delete
     * @throws SQLException
     * @throws IOException
     */
    public static void deleteMetadataSchema(Integer id) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            MetadataSchema metadataSchema = metadataSchemaService.find(c, id);
            if (metadataSchema != null) {
                try {
                    metadataSchemaService.delete(c, metadataSchema);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }

    public static MetadataSchemaBuilder createMetadataSchema(Context context, String name, String namespace)
        throws SQLException, AuthorizeException {
        MetadataSchemaBuilder metadataSchemaBuilder = new MetadataSchemaBuilder(context);
        return metadataSchemaBuilder.create(context, name, namespace);
    }

    private MetadataSchemaBuilder create(Context context, String name, String namespace)
        throws SQLException, AuthorizeException {
        this.context = context;

        try {
            metadataSchema = metadataSchemaService.create(context, name, namespace);
        } catch (NonUniqueMetadataException e) {
            log.error("Failed to create MetadataSchema", e);
        }

        return this;
    }
}
