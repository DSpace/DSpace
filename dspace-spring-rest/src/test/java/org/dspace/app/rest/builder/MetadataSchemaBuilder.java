/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public class MetadataSchemaBuilder extends AbstractBuilder<MetadataSchema, MetadataSchemaService> {

    /* Log4j logger*/
    private static final Logger log = Logger.getLogger(MetadataSchemaBuilder.class);

    private MetadataSchema metadataSchema;

    protected MetadataSchemaBuilder(Context context) {
        super(context);
    }

    @Override
    protected MetadataSchemaService getService() {
        return metadataSchemaService;
    }

    @Override
    protected void cleanup() throws Exception {
        delete(metadataSchema);
    }

    @Override
    public MetadataSchema build() {
        try {

            metadataSchemaService.update(context, metadataSchema);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException e) {
            log.error(e);
        } catch (SQLException e) {
            log.error(e);
        } catch (AuthorizeException e) {
            log.error(e);
            ;
        } catch (NonUniqueMetadataException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return this;
    }
}
