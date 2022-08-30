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
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            metadataField = c.reloadEntity(metadataField);
            if (metadataField != null) {
                delete(c, metadataField);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public void delete(Context c, MetadataField dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    public MetadataField build() {
        try {

            metadataFieldService.update(context, metadataField);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException | AuthorizeException
                | NonUniqueMetadataException | IOException e) {
            log.error("Failed to complete MetadataField", e);
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

    /**
     * Delete the Test MetadataField referred to by the given ID
     * @param id Integer of Test MetadataField to delete
     * @throws SQLException
     * @throws IOException
     */
    public static void deleteMetadataField(Integer id) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            MetadataField metadataField = metadataFieldService.find(c, id);
            if (metadataField != null) {
                try {
                    metadataFieldService.delete(c, metadataField);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
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
            log.error("Failed to create MetadataField", e);
        }

        return this;
    }
}
