/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class SubmissionLookupUtils {
    private static final Logger log = LogManager.getLogger(SubmissionLookupUtils.class);

    /**
     * Default constructor
     */
    private SubmissionLookupUtils() { }

    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    /**
     * Location of config file
     */
    private static final String configFilePath = configurationService
        .getProperty("dspace.dir")
        + File.separator
        + "config"
        + File.separator + "crosswalks" + File.separator;

    // Patter to extract the converter name if any
    private static final Pattern converterPattern = Pattern.compile(".*\\((.*)\\)");

    protected static final MetadataSchemaService metadataSchemaService =
        ContentServiceFactory.getInstance().getMetadataSchemaService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public static LookupProvidersCheck getProvidersCheck(Context context,
                                                         Item item, String dcSchema, String dcElement,
                                                         String dcQualifier) {
        try {
            LookupProvidersCheck check = new LookupProvidersCheck();
            List<MetadataSchema> schemas = metadataSchemaService.findAll(context);
            List<MetadataValue> values = itemService.getMetadata(item, dcSchema, dcElement,
                                                                 dcQualifier, Item.ANY);

            for (MetadataSchema schema : schemas) {
                boolean error = false;
                if (schema.getNamespace().startsWith(
                    SubmissionLookupService.SL_NAMESPACE_PREFIX)) {
                    List<MetadataValue> slCache = itemService.getMetadata(item, schema.getName(),
                                                                          dcElement, dcQualifier, Item.ANY);
                    if (slCache.isEmpty()) {
                        continue;
                    }

                    if (slCache.size() != values.size()) {
                        error = true;
                    } else {
                        for (int idx = 0; idx < values.size(); idx++) {
                            MetadataValue v = values.get(idx);
                            MetadataValue sl = slCache.get(idx);
                            // FIXME gestire authority e possibilita' multiple:
                            // match non sicuri, affiliation, etc.
                            if (!v.getValue().equals(sl.getValue())) {
                                error = true;
                                break;
                            }
                        }
                    }
                    if (error) {
                        check.getProvidersErr().add(schema.getName());
                    } else {
                        check.getProvidersOk().add(schema.getName());
                    }
                }
            }
            return check;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    public static String normalizeDOI(String doi) {
        if (doi != null) {
            return doi.trim().replaceAll("^http://dx.doi.org/", "")
                      .replaceAll("^doi:", "");
        }
        return null;

    }

    public static String getFirstValue(Record rec, String field) {
        List<Value> values = rec.getValues(field);
        String value = null;
        if (values != null && values.size() > 0) {
            value = values.get(0).getAsString();
        }
        return value;
    }

    public static List<String> getValues(Record rec, String field) {
        List<String> result = new ArrayList<>();
        List<Value> values = rec.getValues(field);
        if (values != null && values.size() > 0) {
            for (Value value : values) {
                result.add(value.getAsString());
            }
        }
        return result;
    }

    public static String getPrintableString(Record record) {
        StringBuilder result = new StringBuilder();

        result.append("\nPublication {\n");

        for (String field : record.getFields()) {
            result.append("--").append(field).append(":\n");
            List<Value> values = record.getValues(field);
            for (Value value : values) {
                result.append("\t").append(value.getAsString()).append("\n");
            }
        }

        result.append("}\n");

        return result.toString();
    }
}
