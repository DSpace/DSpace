/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.Date;

public class CrosswalkUtils {

    protected static final MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    protected static final MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    /**
     * Scans metadata for elements not defined in this DSpace instance. It then takes action based
     * on a configurable parameter (fail, ignore, add).
     */
    public static void checkMetadata(Context context, String schema, String element, String qualifier, boolean forceCreate) throws SQLException, AuthorizeException, CrosswalkException {
        // The two options, with three possibilities each: add, ignore, fail
        String schemaChoice = ConfigurationManager.getProperty("oai", "harvester.unknownSchema");
        if (schemaChoice == null)
        {
            schemaChoice = "fail";
        }

        String fieldChoice = ConfigurationManager.getProperty("oai", "harvester.unknownField");
        if (fieldChoice == null)
        {
            fieldChoice = "fail";
        }

        // Verify that the schema exists
        MetadataSchema mdSchema = metadataSchemaService.find(context, schema);
        if (mdSchema == null) {
            // add a new schema, giving it a namespace of "unknown". Possibly a very bad idea.
            if (forceCreate && schemaChoice.equals("add"))
            {
                try {
                    mdSchema = metadataSchemaService.create(context, schema, String.valueOf(new Date().getTime()));
                    mdSchema.setNamespace("unknown"+mdSchema.getSchemaID());
                    metadataSchemaService.update(context, mdSchema);
                } catch (NonUniqueMetadataException e) {
                    // This case should not be possible
                    e.printStackTrace();
                }
            }
            // ignore the offending schema, quietly dropping all of its metadata elements before they clog our gears
            else if (!schemaChoice.equals("ignore")) {
                throw new CrosswalkException("The '" + schema + "' schema has not been defined in this DSpace instance. ");
            }
        }

        if (mdSchema != null) {
            // Verify that the element exists; this part is reachable only if the metadata schema is valid
            MetadataField mdField = metadataFieldService.findByElement(context, mdSchema, element, qualifier);
            if (mdField == null) {
                if (forceCreate && fieldChoice.equals("add")) {
                    try {
                        metadataFieldService.create(context, mdSchema, element, qualifier, null);
                    } catch (NonUniqueMetadataException e) {
                        // This case should also not be possible
                        e.printStackTrace();
                    }
                }
                else if (!fieldChoice.equals("ignore")) {
                    throw new CrosswalkException("The '" + element + "." + qualifier + "' element has not been defined in this DSpace instance. ");
                }
            }
        }
    }
}
