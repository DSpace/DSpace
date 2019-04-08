/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public class MetadataFieldBuilder extends AbstractBuilder<MetadataField, MetadataFieldService> {

    /* Log4j logger*/
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataFieldBuilder.class);

    private MetadataField metadataField;

    protected MetadataFieldBuilder(Context context) {
        super(context);
    }

    @Override
    protected MetadataFieldService getService() {
        return metadataFieldService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(metadataField);
    }

    @Override
    public MetadataField build() {
        try {

            metadataFieldService.update(context, metadataField);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return metadataField;
    }

    public void delete(MetadataField dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            MetadataField attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }


    public static MetadataFieldBuilder createMetadataField(Context context, String element, String qualifier,
                                                           String scopeNote) throws SQLException, AuthorizeException {
        MetadataFieldBuilder metadataFieldBuilder = new MetadataFieldBuilder(context);
        return metadataFieldBuilder.create(context, element, qualifier, scopeNote);
    }

    public static MetadataFieldBuilder createMetadataField(Context context, MetadataSchema schema, String element,
            String qualifier, String scopeNote) throws SQLException, AuthorizeException {
        MetadataFieldBuilder metadataFieldBuilder = new MetadataFieldBuilder(context);
        return metadataFieldBuilder.create(context, schema, element, qualifier, scopeNote);
    }

    private MetadataFieldBuilder create(Context context, String element, String qualifier, String scopeNote)
        throws SQLException, AuthorizeException {

        create(context, metadataSchemaService.find(context, "dc"), element, qualifier, scopeNote);
        return this;
    }

    private MetadataFieldBuilder create(Context context, MetadataSchema schema, String element, String qualifier,
            String scopeNote) throws SQLException, AuthorizeException {

        this.context = context;

        try {
            metadataField = metadataFieldService
                .create(context, schema, element, qualifier, scopeNote);
        } catch (NonUniqueMetadataException e) {
            e.printStackTrace();
        }

        return this;
    }
}
