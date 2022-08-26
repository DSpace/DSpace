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
import org.apache.commons.lang3.ArrayUtils;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * {@link DSpaceRunnable} implementation to delete all the values of the given
 * metadata field.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class MetadataDeletion extends DSpaceRunnable<MetadataDeletionScriptConfiguration<MetadataDeletion>> {

    private MetadataValueService metadataValueService;

    private MetadataFieldService metadataFieldService;

    private ConfigurationService configurationService;

    private String metadataField;

    private boolean list;

    @Override
    public void internalRun() throws Exception {

        if (list) {
            listErasableMetadata();
            return;
        }

        Context context = new Context();

        try {
            context.turnOffAuthorisationSystem();
            performMetadataValuesDeletion(context);
        } finally {
            context.restoreAuthSystemState();
            context.complete();
        }

    }

    private void listErasableMetadata() {
        String[] erasableMetadata = getErasableMetadata();
        if (ArrayUtils.isEmpty(erasableMetadata)) {
            handler.logInfo("No fields has been configured to be cleared via bulk deletion");
        } else {
            handler.logInfo("The fields that can be bulk deleted are: " + String.join(", ", erasableMetadata));
        }
    }

    private void performMetadataValuesDeletion(Context context) throws SQLException {

        MetadataField field = metadataFieldService.findByString(context, metadataField, '.');
        if (field == null) {
            throw new IllegalArgumentException("No metadata field found with name " + metadataField);
        }

        if (!ArrayUtils.contains(getErasableMetadata(), metadataField)) {
            throw new IllegalArgumentException("The given metadata field cannot be bulk deleted");
        }

        handler.logInfo(String.format("Deleting the field '%s' from all objects", metadataField));

        metadataValueService.deleteByMetadataField(context, field);
    }

    private String[] getErasableMetadata() {
        return configurationService.getArrayProperty("bulkedit.allow-bulk-deletion");
    }

    @Override
    @SuppressWarnings("unchecked")
    public MetadataDeletionScriptConfiguration<MetadataDeletion> getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("metadata-deletion", MetadataDeletionScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {

        metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        metadataField = commandLine.getOptionValue('m');
        list = commandLine.hasOption('l');

        if (!list && metadataField == null) {
            throw new ParseException("One of the following parameters is required: -m or -l");
        }

    }

}
