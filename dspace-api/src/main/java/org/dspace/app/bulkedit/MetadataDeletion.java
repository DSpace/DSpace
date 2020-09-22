/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.sql.SQLException;

import org.apache.commons.cli.ParseException;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * {@link DSpaceRunnable} implementation to delete all the values of the given
 * metadata field.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class MetadataDeletion extends DSpaceRunnable<MetadataDeletionScriptConfiguration<MetadataDeletion>> {

    private String metadataField;

    @Override
    public void internalRun() throws Exception {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try {
            performMetadataValuesDeletion(context);
        } catch (SQLException e) {
            handler.handleException(e);
        }

        context.restoreAuthSystemState();
        context.complete();
    }

    private void performMetadataValuesDeletion(Context context) throws SQLException {
        MetadataValueService metadataValueService = ContentServiceFactory.getInstance()
            .getMetadataValueService();

        MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance()
            .getMetadataFieldService();

        MetadataField field = metadataFieldService.findByString(context, metadataField, '.');
        if (field == null) {
            throw new IllegalArgumentException("No metadata field found with name " + metadataField);
        }

        metadataValueService.deleteByMetadataField(context, field);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MetadataDeletionScriptConfiguration<MetadataDeletion> getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("metadata-deletion", MetadataDeletionScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        metadataField = commandLine.getOptionValue('m');
    }

}
