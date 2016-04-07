/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
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
import java.util.HashMap;
import java.util.Map;

public class CrosswalkMetadataValidator {

    protected MetadataSchemaService metadataSchemaService;
    protected MetadataFieldService metadataFieldService;

    private String schemaChoice;
    private String fieldChoice;

    private Map<Triple<String, String, String>, MetadataField> validatedMetadataFields;

    public CrosswalkMetadataValidator() {
        metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

        validatedMetadataFields = new HashMap<>();

        // The two options, with three possibilities each: add, ignore, fail
        schemaChoice = ConfigurationManager.getProperty("oai", "harvester.unknownSchema");
        if (schemaChoice == null)
        {
            schemaChoice = "fail";
        }

        fieldChoice = ConfigurationManager.getProperty("oai", "harvester.unknownField");
        if (fieldChoice == null)
        {
            fieldChoice = "fail";
        }
    }

    /**
     * Scans metadata for elements not defined in this DSpace instance. It then takes action based
     * on a configurable parameter (fail, ignore, add).
     */
    public MetadataField checkMetadata(Context context, String schema, String element, String qualifier, boolean forceCreate) throws SQLException, AuthorizeException, CrosswalkException {
        if(!validatedBefore(schema, element, qualifier)) {
            // Verify that the schema exists
            MetadataSchema mdSchema = metadataSchemaService.find(context, schema);
            MetadataField mdField = null;

            if (mdSchema == null) {
                // add a new schema, giving it a namespace of "unknown". Possibly a very bad idea.
                if (forceCreate && schemaChoice.equals("add")) {
                    try {
                        mdSchema = metadataSchemaService.create(context, schema, String.valueOf(new Date().getTime()));
                        mdSchema.setNamespace("unknown" + mdSchema.getID());
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
                mdField = metadataFieldService.findByElement(context, mdSchema, element, qualifier);
                if (mdField == null) {
                    if (forceCreate && fieldChoice.equals("add")) {
                        try {
                            metadataFieldService.create(context, mdSchema, element, qualifier, null);
                        } catch (NonUniqueMetadataException e) {
                            // This case should also not be possible
                            e.printStackTrace();
                        }
                    } else if (!fieldChoice.equals("ignore")) {
                        throw new CrosswalkException("The '" + element + "." + qualifier + "' element has not been defined in this DSpace instance. ");
                    }
                }
            }

            validatedMetadataFields.put(createKey(schema, element, qualifier), mdField);
        }

        return validatedMetadataFields.get(createKey(schema, element, qualifier));
    }

    private boolean validatedBefore(String schema, String element, String qualifier) {
        return validatedMetadataFields.containsKey(createKey(schema, element, qualifier));
    }

    private ImmutableTriple<String, String, String> createKey(final String schema, final String element, final String qualifier) {
        return new ImmutableTriple<String, String, String>(schema, element, qualifier);
    }
}
